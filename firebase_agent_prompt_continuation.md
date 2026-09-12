# CONTINUATION PROMPT: Finish the Firebase Agent (Labour) Collection System

## CONTEXT

This continues the work started from `firebase_agent_prompt.md` in this same repo (`d:\Chits\Chits`). A large chunk of the plumbing is already implemented and committed to the working tree (uncommitted — nothing has been git-committed yet). This document is the handoff: what's done, what's verified, and exactly what's left before the app compiles and the feature is testable end-to-end.

**Do not re-plan from scratch.** Read this file, confirm the "DONE" section still matches the code on disk, then pick up at "REMAINING WORK" in order — later steps depend on earlier ones.

---

## KEY ARCHITECTURE DECISIONS ALREADY MADE (deviations from the original prompt, made for good reason — keep them)

1. **Package naming**: All new Labour/Agent UI lives in `com.jothivel.chits.ui.labour`, NOT `com.jothivel.chits.ui.agent` — that package already exists and holds the unrelated AI chat-assistant feature (`LocalAssistantEngine.kt`, `AgentScreen` composable in `ApprovedAppFlow.kt`). Keep using `ui.labour` for anything new to avoid confusion, even though Firestore document *field names* (e.g. `agentId`, `agentName`) still follow the original spec's wording.

2. **Room DB migration reused, no new tables.** Instead of syncing to brand-new Room tables, the agent's phone downloads data from Firestore straight into the *existing* `ChitGroupEntity` / `MemberEntity` / `ChitMembershipEntity` / `InstallmentEntity` tables (see `AgentDataSync.kt`). This means `CollectionService.record()` — which is reused unmodified for the agent's local save — just works, because the agent's local Room DB now looks like a scoped-down copy of the admin's. This was necessary: the original prompt's Firestore schema only covered `chitGroups` + `members`, but `CollectionService.record()` also needs `ChitMembershipEntity` (for the active-membership check) and `InstallmentEntity` (for due calculation) locally, or it throws. So the Firestore schema was extended with two more top-level collections not in the original spec:
   - `chitMemberships/{membershipId}` — mirrors `ChitMembershipEntity`
   - `installments/{installmentId}` — mirrors `InstallmentEntity`
   Both are written by `FirestoreDataSync.kt` (admin push) and read by `AgentDataSync.kt` (agent pull), scoped to the agent's `assignedGroups`.

3. **Firestore `collections/{docId}` uses the same UUID as the Room `requestId`** as the *document ID* (not an auto-id as the original spec showed). This makes the write idempotent — a retried push after a flaky network can't create a duplicate — and lets `FirebaseSyncService` dedupe against `CollectionReceiptDao.getByRequestIdSync()` for free.

4. **`FirebaseSyncService` is a plain Kotlin singleton `object`** with a Firestore `addSnapshotListener`, started/stopped from `MainHostActivity.onCreate`/`onDestroy` — not an Android `Service`/foreground service. This is simpler and sufficient: it only needs to run while the admin's app process is alive with `MainHostActivity` on screen, matching "when admin app starts" from the original spec.

5. **Offline queueing for agent collections** is implemented as a small SharedPreferences-backed JSON queue (`AgentCollectionSync.kt`), not a new Room table or WorkManager job. `push()` tries Firestore directly; on failure it queues. `flushPending()` retries the queue and must be called from the agent UI (on screen entry / pull-to-refresh) — **this wiring is one of the remaining steps below, it is not automatic yet.**

6. **`google-services.json` is still missing** (user has not provided it). The Gradle setup handles this gracefully: `app/build.gradle` only `apply plugin: 'com.google.gms.google-services'` if the file exists on disk, so the project keeps building without it. All Firestore access goes through `FirebaseSetup.firestoreOrNull(context)`, which returns `null` instead of crashing when Firebase isn't initialized — every caller must keep respecting that contract.

7. **PIN hashing was refactored** in `AppPreferences.kt` into companion object statics `AppPreferences.hashPin(pin)` / `AppPreferences.verifyPinHash(pin, stored)` (same salted-SHA-256 `v2$salt$hash` format, unchanged behavior for the admin PIN). Reuse these — do not reimplement hashing anywhere else.

---

## DONE (files created — verify they still exist and match this description before continuing)

- `android-app/app/src/main/java/com/jothivel/chits/data/firebase/FirebaseSetup.kt` — safe `FirebaseApp.initializeApp` wrapper + `firestoreOrNull(context)`.
- `.../data/firebase/FirestoreSchema.kt` — collection name + field key constants.
- `.../data/firebase/FirebaseTaskExt.kt` — `Task<T>.await()` coroutine extension (avoids adding the `kotlinx-coroutines-play-services` dependency).
- `.../data/firebase/AgentAuthRepository.kt` — agent login (phone+PIN lookup against `agents/`, with offline cache fallback via `AppPreferences`), plus admin-side `listAgents`, `createAgent`, `setActive`, `resetPin`, `setAssignedGroups`.
- `.../data/firebase/FirestoreDataSync.kt` — **admin → cloud** one-shot push of active `chitGroups`, `members`, `chitMemberships`, `installments` (batched writes, 400/batch).
- `.../data/firebase/AgentDataSync.kt` — **cloud → agent Room** pull of the same four collections, scoped to `AppPreferences.getAgentAssignedGroups()`.
- `.../data/firebase/AgentCollectionSync.kt` — push-or-queue a collection doc to `collections/{requestId}`, plus `flushPending()` / `pendingCount()`.
- `.../data/firebase/FirebaseSyncService.kt` — admin-only real-time listener on `collections` where `syncedToAdmin == false`; mirrors into Room via `CollectionService.record(...)`, then flags `syncedToAdmin = true`.
- `.../ui/auth/AgentLoginViewModel.kt` — Kotlin `AndroidViewModel` wrapping `AgentAuthRepository.login`.

## DONE (files modified — verify against the descriptions, don't redo)

- `android-app/build.gradle` — uncommented `classpath 'com.google.gms:google-services:4.4.0'`.
- `android-app/app/build.gradle` — added `firebase-bom:32.7.0`, `firebase-auth`, `firebase-firestore`; plugin applied conditionally at the bottom (`if (file('google-services.json').exists())`).
- `data/local/entity/PaymentEntity.java` — added `collectedBy` / `collectedByAgentId` (String, nullable).
- `data/local/AppDatabase.java` — version bumped **12 → 13**, `MIGRATION_12_13` added (two `ALTER TABLE payments ADD COLUMN`), registered in `getDatabase()`.
- `data/local/dao/MembershipDao.java` — added `upsertAll()` (REPLACE strategy; the pre-existing `insertAll` uses IGNORE and won't update existing rows, needed a REPLACE variant for sync).
- `data/local/CollectionService.kt` — `record(...)` gained two **optional, defaulted-to-null** trailing params `collectedBy`, `collectedByAgentId`, threaded through into the `PaymentEntity` rows it creates. Existing admin call site is unaffected (it doesn't pass them).
- `utils/AppPreferences.kt` — PIN hashing refactored into companion statics (see decision #7); added `ROLE_ADMIN`/`ROLE_AGENT` constants and full agent session storage: `getUserRole/isAgent/getAgentId/getAgentName/getAgentPhone/getAgentAssignedGroups/isAgentActiveCached/saveAgentSession(...)/getCachedAgentPinHash/clearAgentSession()`.
- `ui/auth/LoginScreen.kt` — `LoginScreen()` gained an optional `agentViewModel: AgentLoginViewModel? = null` param and internal `loginMode` state; added `RoleModeToggle` (Admin/Labour pill switch) and a new `AgentPinLoginScreen` composable (phone number field + reused `PinDot`/`NumberPad`). The original `PinLoginScreen` is untouched except for two new optional params (`showLabourToggle`, `onSwitchToLabour`) that default to off.
- `ui/auth/LoginActivity.kt` — instantiates `AgentLoginViewModel`, observes its `loginSuccess`, passes it into `LoginScreen(...)`. The "skip straight to dashboard" gate now also accepts a cached agent session: `(appPreferences.isAdminSetup() || appPreferences.isAgent()) && isSessionActive`.
- `ui/MainHostActivity.kt` — `onCreate` starts `FirebaseSyncService.start(this)` when `AppPreferences(this).getUserRole() == ROLE_ADMIN`; added `onDestroy` override calling `FirebaseSyncService.stop()`.
- `ui/ApprovedAppFlow.kt` — **partially done, see below.** So far:
  - `AppDestination` enum gained `LABOUR`.
  - `ApprovedAppFlow(...)` now reads the role at the very top and does `if (userRole == ROLE_AGENT) { AgentAppFlow(onLogout); return }` before any of its existing state/Scaffold — the entire rest of the admin function is byte-for-byte unchanged below that guard.
  - `LABOUR` added to the bottom-bar-hidden `setOf(...)`, and a `when` branch routes it to `com.jothivel.chits.ui.labour.LabourManagementScreen(onBack = ...)` (**file not created yet — see step 1 below**).
  - `SettingsScreenApproved(...)` gained an `onLabour: () -> Unit` param and a new `MoreRow(Icons.Default.Groups, "Labour (field agents)", onLabour)` row; the call site in `ApprovedAppFlow` passes `onLabour = { open(AppDestination.LABOUR) }`.
  - New private composables added (after `ApprovedAppFlow`, before `ApprovedBottomBar`): `AgentDestination` enum `{ MY_CHITS, MEMBER_LIST, COLLECT, TODAY_SUMMARY }`, `AgentAppFlow(onLogout)` (own Scaffold/bottom bar/back-handler, routes to `com.jothivel.chits.ui.labour.AgentMyChitsScreen` / `AgentMemberListScreen` / `AgentTodaySummaryScreen` — **file not created yet** — and to the *existing* private `CollectionScreen` composable directly, reused as-is), and `AgentBottomBar` (visually identical clone of `ApprovedBottomBar`, 3 tabs: My Chits / Collect / Today).
  - Inside `CollectionScreen(...)`: added `val agentPrefs = remember { AppPreferences(context) }` and `val isAgentMode = remember { agentPrefs.isAgent() }` right after the existing `scope`/`lookup` declarations.

## IN PROGRESS — THE VERY NEXT EDIT

The interrupted edit was inside `CollectionScreen`'s local `saveCollection()` function (around line ~836 pre-edit). **This still needs to be done first.** Currently `saveCollection()` still calls `CollectionService.record(AppDatabase.getDatabase(context), UUID.randomUUID().toString(), member.id, member.name, group.id, enteredPaise, mode, reference, notes, businessDate)` with a throwaway inline UUID and no agent attribution, and does nothing Firestore-related on success.

Change it to:
1. Hoist the UUID into a named `val requestId = UUID.randomUUID().toString()` computed once before `scope.launch { ... }` (so the same id is used for both the Room write and the Firestore push).
2. Pass `collectedBy = if (isAgentMode) agentPrefs.getAgentName() else null` and `collectedByAgentId = if (isAgentMode) agentPrefs.getAgentId() else null` as the two new trailing args to `CollectionService.record(...)`.
3. Inside `result.onSuccess { receipt -> ... }`, after the existing due-recalculation lines, add: `if (isAgentMode) { withContext(Dispatchers.IO) { com.jothivel.chits.data.firebase.AgentCollectionSync.push(context, com.jothivel.chits.data.firebase.AgentCollectionDoc(requestId = requestId, agentId = agentPrefs.getAgentId(), agentName = agentPrefs.getAgentName(), memberId = member.id, memberName = member.name, groupId = group.id, chitNo = group.registerNo ?: group.id, amountPaise = receipt.amountPaise, mode = receipt.mode, referenceNo = receipt.referenceNo, receiptNo = receipt.receiptNo, notes = notes, businessDate = receipt.businessDate, status = "PAID")) } }`. Double-check `SavedCollection`'s actual field names in `CollectionService.kt` before wiring this (`receiptNo, amountPaise, mode, businessDate, referenceNo` — confirmed present as of this writing).

---

## REMAINING WORK (in dependency order)

### 1. Create `ui/labour/AgentFieldScreens.kt` (new file, package `com.jothivel.chits.ui.labour`)

Three **public** composables, referenced by `AgentAppFlow` in `ApprovedAppFlow.kt` (which supplies its own `BrandTopBar` wrapper — these composables should render body content only, no top bar):

- **`AgentMyChitsScreen(onOpenGroup: (groupId: String, label: String) -> Unit)`**
  - On first composition (`LaunchedEffect(Unit)`), call `AgentDataSync.syncAssignedGroups(context)` (IO dispatcher) to refresh local Room from Firestore, then also call `AgentCollectionSync.flushPending(context)` to retry any queued offline collections — this is where that wiring from decision #5 belongs.
  - Welcome banner ("🙏 Vanakkam, {agentName}!" + today's date) using `AppPreferences(context).getAgentName()`.
  - Quick stats: today's total collected + count, computed from local Room `PaymentDao` filtered to `collectedByAgentId == AppPreferences(context).getAgentId()` and `paidAt` falling on today (use `CollectionService.todayKey()` pattern already established elsewhere in the file for date bucketing, or filter by day boundaries on `paidAt`).
  - List of the agent's assigned chit groups: query local Room `ChitGroupEntity` filtered to `id in AppPreferences(context).getAgentAssignedGroups()`, each card showing register no, chit value (`money(chitValue / 100)` — reuse the `money()` helper from `ApprovedAppFlow.kt`, it's file-private so this must either take a pre-formatted string or you duplicate the one-liner; duplicating `NumberFormat.getNumberInstance(Locale("en","IN"))` formatting locally is fine, it's a single line), member count (`MembershipDao.countActiveForGroupSync(groupId)`), and pending-this-month count (reuse `CollectionService.calculateDueBreakdown` per member, or a lighter aggregate — don't over-engineer, a simple loop over active memberships is fine at this scale).
  - Tap a card → `onOpenGroup(group.id, "${group.registerNo} • ${group.name}")`.
  - Match styling: `MaroonPrimary`/`MaroonBackground`/`AccentGold`/`AccentGreen` from `com.jothivel.chits.ui.theme`, `RoundedCornerShape(20.dp)` cards, `Surface` + `BorderStroke(1.dp, DividerGray)` pattern used throughout `ApprovedAppFlow.kt` — go re-read a couple of existing cards there (e.g. `CustomerSummaryCard`) for the exact visual language before writing new ones.

- **`AgentMemberListScreen(groupId: String, onCollect: (memberName: String, chitLabel: String) -> Unit)`**
  - Loads members of `groupId` from local Room (`MembershipDao.getForMemberSync` doesn't fit — you want "members of a group", so query `ChitMembershipEntity` where `groupId == groupId && isActive`, then resolve each via `MemberDao.getMemberByIdSync`).
  - Per member row: name, phone, ticket/slot no, payment status this month (✅/⏳/❌ — derive from `CollectionService.calculateDueBreakdown(db, memberId, groupId)`; `pendingPaise <= 0` → Paid, `overdueDays > 0` → Overdue, else Pending), due amount.
  - Row actions: **Call** (`Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))`), **WhatsApp** (`Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$phone?text=..."))` — the manifest already declares `<queries>` for `com.whatsapp`/`com.whatsapp.w4b`, no new manifest change needed), **Collect** → `onCollect(member.name, group.registerNo ?: group.id)`.

- **`AgentTodaySummaryScreen()`**
  - On composition, `AgentCollectionSync.flushPending(context)` (pull-to-refresh candidate too, but at minimum on entry).
  - Total collected today / cash vs UPI breakdown / count, and a list of today's collections sorted by time descending. Per the original spec these should read from Firestore `collections/` filtered by `agentId` + `businessDate == today`, but since the agent's own Room DB already has these rows locally (written by `CollectionService.record` in the same `saveCollection()` flow, tagged with `collectedByAgentId`), **prefer reading from local Room** (`PaymentDao` filtered by `collectedByAgentId` + today) — it's simpler, works fully offline, and avoids a redundant Firestore round-trip for data the phone already wrote itself. Only reach for Firestore here if you have a concrete reason local Room is insufficient (e.g. wanting to show `pendingCount` from `AgentCollectionSync.pendingCount(context)` as an "X not yet synced" chip, which is a nice touch worth adding).

### 2. Create `ui/labour/LabourManagementScreen.kt` (new file, admin-only)

`fun LabourManagementScreen(onBack: () -> Unit)` — public composable, own `BrandTopBar`-style header (duplicate the small top-bar snippet, ~10 lines, matching `MaroonPrimary` surface / 52.dp height / back arrow — it's `private` in `ApprovedAppFlow.kt` so can't be imported).

- List agents via `AgentAuthRepository.listAgents(context)` (call in `produceState`/`LaunchedEffect`, IO dispatcher). Each card: name, phone, Active ✅ / Disconnected ❌ badge (grayed card when disconnected), assigned-groups count.
- FAB "Add Labour" → dialog/sheet collecting Name, Phone, 4-digit PIN → `AgentAuthRepository.createAgent(context, name, phone, pin)`.
- Per-agent actions: **Disconnect** → `AgentAuthRepository.setActive(context, id, false)`; **Reconnect / Set New PIN** → dialog collecting a new 4-digit PIN → `AgentAuthRepository.resetPin(context, id, newPin)` (this also flips `isActive = true` per the repository implementation — confirm that's the desired UX, it matches the original spec's "Reconnect... enter new PIN... updates pin and sets isActive=true").
- **Assign Chit Groups**: multi-select from local Room `ChitGroupEntity` (active groups) → `AgentAuthRepository.setAssignedGroups(context, id, groupIds)`.
- **"Sync Data to Cloud" button**: calls `FirestoreDataSync.syncAllToCloud(context)`, shows a toast/snackbar with the `SyncResult` counts (groups/members/memberships/installments) or the failure message. Mention in the UI copy that this should be run after adding/changing members, per the original spec's guidance.
- If `FirebaseSetup.firestoreOrNull(context) == null` anywhere on this screen (i.e., Firebase isn't configured), show a clear inline banner: "Firebase not configured — add google-services.json and rebuild" rather than silently failing or showing an empty list forever.

### 3. Firestore Security Rules

Create `android-app/firestore.rules` with the permissive-MVP rules from PART 8 of `firebase_agent_prompt.md` verbatim, but also add rules for the two collections added in decision #2 above (`chitMemberships`, `installments`) mirroring the `chitGroups`/`members` rule shape (`allow read: if true; allow write: if true;`). Add a one-line comment at the top noting these must be deployed via the Firebase Console (or `firebase deploy --only firestore:rules` if the user sets up the Firebase CLI) — this repo has no Firebase CLI config (`firebase.json`) yet, so just having the `.rules` file checked in is documentation until the user wires up a real project.

### 4. Wire `AgentDataSync` + `AgentCollectionSync.flushPending` calls

Already called out inside step 1's `AgentMyChitsScreen`/`AgentTodaySummaryScreen` bullets — make sure both actually land in the `LaunchedEffect` blocks, since without this the whole offline-sync design is inert.

### 5. Compile & regression-check

- Run a Gradle build (`./gradlew assembleDebug` from `android-app/`, or via the IDE) and fix whatever surfaces — this has **not been compiled even once** yet, so expect at least a few typos/import misses across the ~9 new files and the `ApprovedAppFlow.kt` edits.
- Grep for any other call sites of `SettingsScreenApproved(` or `LoginScreen(` to make sure the new required/optional params don't break another caller (there shouldn't be any outside `ApprovedAppFlow.kt` / `LoginActivity.kt`, but verify).
- Manually exercise the **admin flow** end-to-end (PIN login → Dashboard → Ledger → Collection → Pending → Settings → back) to confirm zero regressions — this should work with no Firebase project at all, since `userRole` defaults to `ADMIN` and every Firestore call in the admin path is either behind the new `Labour` settings row (opt-in) or a no-op via `firestoreOrNull` returning null.
- Agent-flow testing needs a real Firebase project + `google-services.json` + the rules from step 3 deployed — flag this to the user as the point where they need to hand over Firebase credentials.

### 6. Optional, nice-to-have (do only if time remains and the above is solid)

- PART 6.3 from the original spec: show "Collected by: {agentName}" in the admin's ledger member-detail view next to payments where `PaymentEntity.collectedBy != null`. Locate the payment-row rendering inside `ui/ledger/` (`LedgerScreen`/`ResizableLedgerSheet`) or wherever `ApprovedAppFlow.kt`'s `LedgerScreenApproved`/`InstallmentRow`/`DetailedInstallmentRow` render individual payments, and add a small caption line when the field is non-null. Keep it minimal — one `Text` line, muted color, don't restyle the row.

---

## THINGS TO DOUBLE-CHECK WHILE FINISHING (easy to get subtly wrong)

- `ChitGroupEntity`, `MemberEntity`, `InstallmentEntity` are **Java POJOs with public mutable fields**, constructed in `AgentDataSync.kt` via Kotlin's `SomeEntity().apply { field = value }` — make sure every `@NonNull` field (`id`, `memberId`, `groupId` on `ChitMembershipEntity`) is actually set before the object is used, or Room will throw at insert time despite compiling fine.
- `AgentAuthRepository.listAgents`/`createAgent`/etc. all silently no-op or return empty/failure when Firebase isn't configured — `LabourManagementScreen` must surface that state to the admin (see step 2's last bullet) rather than showing a confusing empty list forever.
- The `AgentDestination.MEMBER_LIST` case hides the bottom bar (matches the admin's `CUSTOMER_PROFILE`-style drill-down pattern) — confirm the back button in its `BrandTopBar` reliably returns to `MY_CHITS`, and that `BackHandler` in `AgentAppFlow` does the same (already wired, just verify after the remaining screens exist and the whole thing compiles).
- `money()` in `ApprovedAppFlow.kt` is `private` — new files in `ui.labour` cannot import it. Either duplicate the one-liner locally (simplest, it's genuinely one line) or don't bother chasing DRY here.
