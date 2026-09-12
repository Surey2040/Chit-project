# Jothi Vel Chits — Android App Development Brief
**For: Android Developer (Java) + Figma UI Designer + Backend (JWT Auth)**
**Business: Jothi Vel Chits (P) Ltd., Lakshmi Jewellers Maadi, Thuvarankurichi**
**Register No. reference format: 214/2020 style**

---

## 1. PROJECT SUMMARY

Build a native Android application (Java) for a chit fund (சீட்டு) company that manages multiple chit groups of varying values (₹50,000 to ₹10,00,000+), each running 20 monthly installments with 20 subscribers. Every month, an auction determines a "kasaru" (discount) which changes the installment amount for all members and the payout to that month's winner. The app must handle group creation, member management, monthly auction entry, collections, payouts, and reporting — with a clean, professional, trust-inspiring UI (this is a money app; the design must feel secure and credible, not flashy).

---

## 2. ROLE-BASED PROMPT INSTRUCTIONS

Use this section as literal instructions when prompting an AI coding assistant or briefing your team.

### A. Act as a Senior Android Developer (Java)
> "Act as a senior Android developer with 8+ years of experience building fintech/chit-fund/microfinance apps in Java. Build a native Android app (minSdk 24, targetSdk latest stable) using MVVM architecture, Room DB for offline-first local storage, Retrofit for API calls, and WorkManager for background sync and reminder notifications. Use Material Design 3 components. Structure the app in clean layers: `data` (Room entities, DAOs, Retrofit services, repositories), `domain` (use-cases for auction calculation, installment generation), `ui` (activities/fragments with ViewModel + LiveData/StateFlow), and `utils` (currency formatting in ₹ INR, Tamil/English locale switch, date utils). Ensure the app works offline for field collection agents and syncs when network is available. Handle configuration changes properly and support both phone and tablet layouts."

### B. Act as a Figma UI/UX Expert
> "Act as a senior Figma product designer specializing in fintech and BFSI apps. Design a minimal, high-trust, content-first UI for a chit fund management app. Avoid clutter — this app is used by collection agents in the field and by office admins, often under time pressure, so every screen should have a clear primary action. Use a restrained color palette: deep green/teal as primary (matching the brand's existing green identity), white/off-white backgrounds, amber/gold as an accent for 'payout' and 'prize' related elements, and red only for overdue/alerts. Typography: one clean sans-serif family (e.g., Inter or Poppins), strong number legibility since this is a numbers-heavy app — use tabular figures for all currency and installment tables. Design for both Tamil and English text rendering (Tamil script needs slightly taller line-height). Build a full design system: color tokens, type scale, spacing scale (8pt grid), button states, form inputs, data table component, card component, and empty/error/loading states. Then design these core screens: Login, Dashboard (admin), Group List, Group Detail (with the installment table view mirroring a physical chit chart), Add/Edit Group wizard, Member List, Member Profile, Monthly Auction Entry screen, Collection Entry screen (agent view, optimized for one-handed field use), Payout screen, Reports dashboard, and Member self-service view (read-only schedule + receipts). Prioritize thumb-reachable primary actions on mobile, large tap targets (min 48dp), and a bottom navigation bar with max 5 items."

### C. Act as a Backend/Auth Engineer
> "Act as a backend engineer implementing JWT-based authentication for a multi-role fintech app. Design an auth flow with: access token (short-lived, 15 min) + refresh token (long-lived, 7-30 days, stored securely via Android EncryptedSharedPreferences/Keystore), role-based claims in the JWT payload (ADMIN, AGENT, MEMBER), token refresh interceptor in Retrofit (OkHttp Authenticator), and secure logout that invalidates the refresh token server-side. Passwords hashed with bcrypt; support OTP-based login via phone number as an alternative to password (common for agents/members in this user base). Enforce role-based access control at the API layer — agents can only access their assigned members, members can only view their own data, admins have full access."

---

## 3. FUNCTIONAL REQUIREMENTS (Compliance section removed per request — this list is everything else)

### 3.1 Authentication & Roles
- JWT login (username/password + OTP-via-phone alternative) for online/synced sessions
- **PIN-based offline login (4-digit)** as a fallback/primary mode for field agents with no/weak internet — PIN is set by admin per user, never self-registered publicly. This lets the app open and be usable fully offline (view schedules, enter collections) even with zero network, syncing later when online. Store the PIN hash locally (not plain text) and re-validate against server hash on next sync.
- Roles: **Admin**, **Agent/Collector**, **Member** (self-service)
- Secure token storage, auto-refresh, forced logout on token expiry/tamper
- Role-scoped navigation — each role sees a different home screen

### 3.2 Group / Scheme Management
- Create chit scheme: chit value (₹), duration (months), number of subscribers, branch, start date
- Auto-generate the installment table on creation (mirrors your physical charts):
  - Installment number, base installment amount, kasaru (editable monthly), payout amount
  - Formula engine: as kasaru changes, recompute all members' payable amount and running totals for that month
- View group as a table (looks like your printed chit chart) and as a summary card
- Track slot status per member: open / filled / already won auction

### 3.3 Member Management
- Member profile: name, phone, address, photo, ID proof (Aadhar — store encrypted at rest, never plain text), nominee details, alias name (optional, for members commonly known by a nickname)
- Link member to one or more groups + their slot number
- Member payment history and dues at a glance
- **Member status tag: VIP / Normal / Blocked**
  - *VIP* — trusted, high-value, or long-standing members; admin can mark manually. Used to flag members for priority follow-up, relationship management, or eligibility for future high-value schemes.
  - *Blocked* — members with repeated defaults, payment disputes, or fraud risk. A blocked member cannot be added to a new group (enforced at the API level, not just a UI warning), and existing group screens show a clear red "Blocked" badge next to their name so agents/admins see the risk before extending trust.
  - Status change must be logged (who changed it, when, and why — a short reason field) since this affects a person's ability to join schemes and can be disputed.
- Direct call / WhatsApp shortcut buttons from the member profile screen (deep-link to dialer and WhatsApp with the member's number pre-filled)

### 3.4 Monthly Auction / Kasaru Entry
- Record: auction date, winning bid (kasaru amount), winning member, resulting payout
- Auto-recalculate that month's installment for every member in the group
- Lock the entry after confirmation (immutable once finalized — prevents accidental edits, keeps dispute history clean even without a formal audit trail)

### 3.5 Collection & Payment Tracking
- Mark payment: paid / due / partial, per member per month
- Payment mode: cash / UPI / bank transfer + reference number
- Auto-generate digital receipt (your own house rule says "no receipt = no responsibility" — so receipt generation should be mandatory, not optional, before a payment is marked complete)
- Defaulter/overdue flagging with visual red indicator + reminder trigger
- **Duplicate payment warning** — before saving any payment, the app must check if a payment already exists for this exact combination of (member + chit group + installment month) and show a clear confirmation warning ("A payment for this member, this group, this month already exists — record anyway?") instead of silently creating a second entry. This is one of the most common real-world data-entry mistakes in chit collection and must be caught at both the UI layer (immediate warning) and the API layer (server-side check, in case two agents submit near-simultaneously).
- **One-tap installment auto-fill** — tapping into the amount field pre-fills the exact amount due for that member/month from the installment table, reducing manual entry errors; agent can still override if a partial payment is being recorded.
- **Automatic WhatsApp payment confirmation** — the moment a payment is successfully saved, trigger a WhatsApp message to the member confirming amount paid, installment number, and receipt number (via WhatsApp Business API or a message-template deep link if the API isn't set up yet).

### 3.6 Payout Tracking
- Record payout given to the month's auction winner
- Capture proof of receipt (signature capture or photo upload)

### 3.7 Accounts & Reports
- Company commission/margin per group
- Outstanding dues report — member-wise and group-wise
- Daily collection report (for agents to reconcile cash at day-end)
- Profit & loss per chit group
- Final settlement report — including your 3% rebate rule on the last installment for groups that complete on time
- **All reports exportable as PDF**: ledger statements (per member, showing full payment history), monthly collection reports, and chit group summaries. Each PDF must be shareable directly via WhatsApp, email, AirDrop (iOS)/Nearby Share (Android), or sent to a printer — this should be a single share-sheet action, not a multi-step export-then-attach flow.

### 3.8 Notifications & WhatsApp Integration
- SMS/WhatsApp reminders for upcoming/overdue installments
- Auction date reminders to all group members
- Payout confirmation notification to the winning member
- **One-tap "broadcast auction result to all members" button** — the moment an auction is finalized (kasaru + winner recorded), admin taps a single button and every member in that group receives a WhatsApp message with the result (who won, payout amount, next installment amount), instead of the admin messaging each member manually one by one.
- **Automatic WhatsApp payment confirmation on every save** (see 3.5) — no manual step needed by the agent.
- **Customizable WhatsApp message templates, in both English and Tamil**, covering at minimum:
  - Auction announcement / result
  - Payment reminder (upcoming due date)
  - Payment confirmation (after collection)
  - Overdue/defaulter reminder
  - Admin should be able to edit template wording from a Settings screen without needing a developer, using placeholders like `{memberName}`, `{amount}`, `{groupName}`, `{installmentNo}`, `{dueDate}` that get auto-filled at send time.
- Delivery should work via WhatsApp Business API where available; fall back to a "share via WhatsApp" deep link (pre-filled message, manual tap-to-send) if the Business API isn't set up yet, so the feature works from day one even before formal API approval comes through.

### 3.9 Member Self-Service (read-only)
- View own installment schedule, dues, and past receipts
- View which month's auction they won (if any)

### 3.10 Offline-First & Sync
- **App must be fully usable with zero internet connection** — all core data (groups, members, installment tables, payment history) stored locally on-device (Room DB), so agents can view schedules and enter collections in low-signal areas.
- **Auto-sync to server the moment network becomes available**, with a visible sync status indicator (synced / pending / syncing) so agents know whether their entries have actually reached the server.
- Conflict handling: if the same record was edited both offline (on a device) and online (from the admin panel) before sync, do **not** silently overwrite — flag it for manual admin review (see Section 8, edge case #9).

### 3.11 Bilingual UI (English ⇄ Tamil)
- Full UI translation for both languages, switchable anytime from Settings — not just at first app setup.
- Applies everywhere: screen labels, buttons, error messages, PDF reports, and WhatsApp message templates (see 3.8).
- Tamil text must render correctly in generated PDFs (embed a Tamil-compatible font in the PDF generation library — many PDF libraries default to Latin-only fonts and silently show boxes/garbled text for Tamil unless a Unicode Tamil font is explicitly bundled).
- Store all user-facing strings in resource files (`strings.xml` with `values` and `values-ta` folders in Android), never hardcoded in layout/code, so translation stays maintainable.

---

## 4. TECHNICAL STACK RECOMMENDATION

| Layer | Choice |
|---|---|
| Language | Java (Android native) |
| Architecture | MVVM + Repository pattern |
| Local DB | Room (offline-first, syncs when online) |
| Networking | Retrofit + OkHttp (with JWT auth interceptor) |
| Background work | WorkManager (sync, reminders) |
| Auth | JWT (access + refresh token), OTP login option |
| UI | Material Design 3, XML layouts (or Jetpack Compose if team prefers modern approach) |
| Notifications | Firebase Cloud Messaging + WhatsApp Business API/SMS gateway |
| Design | Figma (design system + all screens before dev starts) |
| Backend | Any REST API (Node.js/Spring Boot/Django) — not specified here, developer's choice |

---

## 5. DESIGN PRINCIPLES FOR THE UI

1. **Numbers must be scannable** — every table (installment, dues, collections) should use monospaced/tabular figures, right-aligned currency columns.
2. **One primary action per screen** — especially on the agent's collection screen, which is used in the field, often standing, one-handed.
3. **Status color coding** — green (paid/complete), amber (due soon), red (overdue), gray (upcoming/not yet due).
4. **Bilingual support** — Tamil and English toggle, with Tamil script given proper line-height so it doesn't look cramped.
5. **Trust-first visual language** — this handles people's savings; avoid playful/gamified UI patterns, keep it clean and bank-like.

---

## 6. DATA MODELS (exact fields — give this directly to the developer to avoid ambiguity)

### 6.1 `User`
| Field | Type | Rules |
|---|---|---|
| id | UUID/Long | Primary key, auto-generated |
| name | String | Required, min 2 chars |
| phone | String | Required, unique, exactly 10 digits, validated with regex `^[6-9]\d{9}$` (India) |
| passwordHash | String | bcrypt, never stored/logged in plain text |
| role | Enum | ADMIN / AGENT / MEMBER — required |
| photoUrl | String | Optional |
| idProofUrl | String | Optional, required before member is "Active" |
| nomineeName | String | Optional |
| nomineePhone | String | Optional, same regex as phone |
| createdAt | Timestamp | Auto-set |
| isActive | Boolean | Default true; false = soft-deleted, never hard-delete a user with payment history |

### 6.2 `ChitGroup`
| Field | Type | Rules |
|---|---|---|
| id | UUID/Long | Primary key |
| registerNo | String | Required, unique, format `NNN/YYYY` |
| chitValue | Decimal | Required, > 0, stored in paise/integer to avoid float rounding errors |
| durationMonths | Integer | Required, must equal `subscriberCount` in this business model |
| subscriberCount | Integer | Required, > 0 |
| branch | String | Required |
| startDate | Date | Required |
| status | Enum | DRAFT / ACTIVE / COMPLETED / CANCELLED |
| createdBy | UserId (Admin) | Required |
| createdAt | Timestamp | Auto-set |

**Validation rule (critical bug source):** `durationMonths` MUST equal `subscriberCount` before a group can move from DRAFT to ACTIVE. Block activation otherwise and show a clear error, not a silent failure.

### 6.3 `Installment` (auto-generated row per group, per month — this is the table from your printed charts)
| Field | Type | Rules |
|---|---|---|
| id | UUID/Long | Primary key |
| groupId | FK → ChitGroup | Required |
| installmentNo | Integer | 1 to durationMonths, unique per group |
| baseAmount | Decimal | System-calculated at group creation; never null |
| kasaruAmount | Decimal | Nullable until auction happens; 0 or positive only, never negative |
| payoutAmount | Decimal | Nullable until auction happens |
| winningMemberId | FK → User (nullable) | Set only after auction |
| auctionDate | Date | Nullable until auction happens |
| status | Enum | UPCOMING / AUCTION_DONE / **LOCKED** |
| lockedAt | Timestamp | Set the moment status becomes LOCKED |

**Critical rule:** once `status = LOCKED`, this row becomes **read-only at the API level**, not just hidden in UI. Any edit attempt after lock must return HTTP 403 with a clear message ("Installment #4 is already finalized and cannot be edited"). This prevents the single biggest real-world bug class in chit apps: someone silently editing a past month's numbers and every subsequent month's totals becoming wrong.

### 6.4 `MemberSubscription` (join table: which member is in which group, at which slot)
| Field | Type | Rules |
|---|---|---|
| id | UUID/Long | Primary key |
| groupId | FK → ChitGroup | Required |
| memberId | FK → User | Required |
| slotNo | Integer | Required, 1 to durationMonths, **unique within a group** — enforce this at the DB level with a composite unique constraint `(groupId, slotNo)`, not just app-level validation |
| hasWon | Boolean | Default false |
| wonInstallmentNo | Integer | Nullable |

### 6.5 `Payment`
| Field | Type | Rules |
|---|---|---|
| id | UUID/Long | Primary key |
| installmentId | FK → Installment | Required |
| memberId | FK → User | Required |
| amountPaid | Decimal | Required, > 0 |
| amountDue | Decimal | System-calculated, required |
| status | Enum | PAID / PARTIAL / DUE / OVERDUE |
| mode | Enum | CASH / UPI / BANK_TRANSFER |
| referenceNo | String | Required if mode is UPI or BANK_TRANSFER; optional for CASH |
| collectedBy | FK → User (Agent) | Required |
| receiptNo | String | Auto-generated, unique, sequential — **must be generated before status can be set to PAID** (per your house rule: no receipt, no responsibility) |
| paidAt | Timestamp | Required |

**Critical rule:** `amountPaid` for a PARTIAL payment must never exceed `amountDue`. Reject overpayment at the API layer with a clear validation error, don't let it silently create a negative balance.

### 6.6 `Payout`
| Field | Type | Rules |
|---|---|---|
| id | UUID/Long | Primary key |
| installmentId | FK → Installment | Required, unique (one payout per installment) |
| memberId | FK → User | Required (must match `winningMemberId` on the installment) |
| amount | Decimal | Required, must match `payoutAmount` on the installment |
| proofUrl | String | Signature/photo — required before payout can be marked complete |
| disbursedBy | FK → User (Admin) | Required |
| disbursedAt | Timestamp | Required |

---

## 7. API ENDPOINTS (REST, JWT-protected unless noted)

```
POST   /auth/login                        (public) — phone+password OR phone+OTP request
POST   /auth/otp/verify                   (public)
POST   /auth/refresh                      (public, requires valid refresh token)
POST   /auth/logout                       (auth required)

GET    /groups                            (admin, agent — scoped)
POST   /groups                            (admin only)
GET    /groups/{id}
PUT    /groups/{id}                       (admin only, blocked if status = ACTIVE for value/duration fields)
POST   /groups/{id}/activate              (admin only, runs validation rule 6.2)

GET    /groups/{id}/installments
GET    /groups/{id}/installments/{no}
POST   /groups/{id}/installments/{no}/auction     (admin only — records kasaru, winner, payout; locks row)
GET    /groups/{id}/members
POST   /groups/{id}/members                (assign member to slot — enforces unique slot constraint)

GET    /members/{id}
POST   /members                            (admin only)
PUT    /members/{id}

POST   /payments                           (agent — creates payment, generates receipt)
GET    /payments?memberId=&groupId=&status=
GET    /payments/{id}/receipt              (returns PDF/image)

POST   /payouts                            (admin only)
GET    /payouts?groupId=&installmentNo=

GET    /reports/dues?groupId=&memberId=
GET    /reports/collections?date=&agentId=
GET    /reports/pl?groupId=
GET    /reports/settlement?groupId=        (final settlement incl. 3% rebate calc)

GET    /me/schedule                        (member self-service, scoped to logged-in user only)
GET    /me/receipts
```

**Every endpoint must:**
- Return consistent error shape: `{ "errorCode": "...", "message": "...", "field": "..." }` — never a raw stack trace or generic 500 with no message
- Validate the JWT role claim server-side on every call, not just in the Android app (client-side-only checks are not real security)
- Use pagination (`?page=&size=`) on all list endpoints — never return unbounded lists, this is a common performance bug once a group has years of history

---

## 8. EDGE CASES & VALIDATION CHECKLIST (test every one of these before calling a feature "done")

1. **Duplicate slot assignment** — two members assigned to the same slot in the same group → must be blocked by DB constraint, not just UI validation.
2. **Negative or zero kasaru** — auction entry form must reject negative numbers; zero is allowed (last installment often has 0 kasaru per your charts).
3. **Kasaru greater than base amount** — should trigger a warning/confirmation, since it would make the payout unrealistic; don't silently allow it.
4. **Editing a locked installment** — must fail at API level (403), not just be hidden in UI. Test by calling the API directly, bypassing the app.
5. **Overpayment** — member pays more than `amountDue` → reject or route to "advance payment" handling explicitly, never leave it as an untracked overpayment.
6. **Member removed mid-scheme** — never hard-delete; soft-delete only, and their historical payments/receipts must remain intact and viewable.
7. **Duplicate receipt numbers** — must be impossible; enforce uniqueness at DB level with a sequence, not app-generated random numbers that could collide.
8. **Token expiry mid-action** — if a JWT expires while an agent is mid-way through entering a collection, the app must silently refresh and retry, not lose the entered data.
9. **Offline collection entry** — agent enters a payment with no network; app must queue it locally (Room) and sync when back online, with conflict resolution if the same installment was also updated from the admin panel meanwhile (last-write-wins is NOT acceptable here — flag the conflict for admin review instead).
10. **Rounding errors** — all currency math done in integer paise internally, converted to ₹ only for display. Never do currency math in `float`/`double`.
11. **Group activation with mismatched duration/subscriber count** — must be blocked with a clear error (see 6.2).
12. **Auction entered twice for the same installment** — must be blocked once status is AUCTION_DONE or LOCKED.
13. **Deleting a group with active payments** — must be blocked entirely; only DRAFT groups with zero payments can be deleted.
14. **Phone number reused across roles** — decide explicitly whether one phone can be both an Agent and a Member; if not, enforce uniqueness across the whole `User` table, not per-role.
15. **Timezone/date bugs** — all dates stored in UTC, displayed in IST (India Standard Time); test around midnight boundary cases for "is this installment overdue today."
16. **Empty states** — every list screen (no members yet, no payments yet, no groups yet) must show a proper empty state, never a blank white screen or a crash on null list.
17. **Large numbers** — chit values up to ₹10,00,000+ must display correctly with Indian number formatting (lakh/crore commas: `10,00,000` not `1,000,000`).
18. **Bilingual data entry** — names/addresses entered in Tamil script must save, search, and display correctly (proper UTF-8 handling end to end, including in generated PDF receipts).

---

## 9. ERROR HANDLING & QUALITY STANDARDS

- **No silent failures.** Every API failure shown to the user with a specific, human-readable message — never a generic "Something went wrong."
- **No crashes on null/empty data.** Every screen must handle: no internet, empty list, malformed API response, and slow network (show loading state, not a frozen UI).
- **Input validation on both client and server.** Client-side validation is for UX speed only; server-side validation is the real gate. Never trust the app alone.
- **Idempotency on payment/payout creation.** If the agent double-taps "Submit" or the network retries a request, it must not create a duplicate payment record. Use idempotency keys on POST /payments and POST /payouts.
- **Logging without leaking sensitive data.** Never log passwords, full phone numbers unmasked, or full JWTs in logcat or server logs.
- **Testing requirement before sign-off:** unit tests for the installment/kasaru calculation engine (this is the core financial logic — it must be tested with the exact numbers from your printed charts as test fixtures, e.g. the ₹5,00,000 / 20-month table, to confirm the app reproduces those exact figures), plus instrumented UI tests for the collection entry flow (most-used screen, highest bug risk).
- **Crash reporting** wired in from day one (e.g., Firebase Crashlytics) so bugs in the field are visible immediately, not discovered weeks later from a customer complaint.

---

## 10. SUGGESTED NEXT STEP

Hand this document to your Figma designer first to produce the design system + key screens, then to the Android developer to scaffold the project structure (data layer, auth, navigation) in parallel. Once screens are approved, wire UI to the Room/Retrofit layer. Before go-live, run through Section 8's edge case checklist item by item — don't skip any, since these are the exact bug classes that cause disputes and lost trust in a money app.

---

*Note: The "Compliance" section (Chit Funds Act register tracking, government reporting formats, audit trail) has been intentionally excluded from this brief per your request. If you want it added back later (recommended before going live, since chit funds are a regulated business in Tamil Nadu), just ask and it can be reinserted as a dedicated module.*
