package com.jothivel.chits.data.firebase

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.jothivel.chits.utils.AppPreferences
import org.json.JSONObject

sealed class AgentLoginResult {
    data class Success(val agentId: String, val name: String, val assignedGroups: List<String>) : AgentLoginResult()
    data class Failure(val message: String) : AgentLoginResult()
}

data class AgentSummary(
    val id: String,
    val name: String,
    val phone: String,
    val isActive: Boolean,
    val assignedGroups: List<String>
)

/**
 * Firestore-backed identity for field collection agents (the "Labour" feature). Distinct from
 * the admin's PIN login in [AppPreferences] — agents are looked up by phone number, not a
 * single shared PIN, and their session is cached locally so login still works offline.
 */
object AgentAuthRepository {

    // Client-side login throttling only (mirrors the pattern backend/src/routes/auth.js uses
    // server-side for its own login). This slows down someone brute-forcing PINs THROUGH the
    // app's UI, but does nothing against an attacker who bypasses the app and reads/queries
    // Firestore directly (see firestore.rules header comment for why that path can't be closed
    // with rules alone here) - it is a mitigation, not a fix.
    private const val LOGIN_ATTEMPTS_PREFS = "jvc_agent_login_attempts"
    private const val LOGIN_ATTEMPT_LIMIT = 5
    private const val LOGIN_ATTEMPT_WINDOW_MS = 15 * 60 * 1000L
    private const val GENERIC_LOGIN_FAILURE = "Invalid phone number or PIN."

    suspend fun login(context: Context, phone: String, pin: String): AgentLoginResult {
        val prefs = AppPreferences(context)
        val throttled = throttleMessage(context, phone)
        if (throttled != null) return AgentLoginResult.Failure(throttled)
        val firestore = FirebaseSetup.firestoreIfSignedIn(context) ?: return loginFromCache(prefs, phone, pin)
        return try {
            val snapshot = firestore.collection(FirestoreSchema.AGENTS)
                .whereEqualTo(FirestoreSchema.Agent.PHONE, phone)
                .limit(1)
                .get()
                .await()
            val doc = snapshot.documents.firstOrNull()
                ?: return if (prefs.getAgentPhone() == phone && prefs.getAgentId().isNotBlank()) {
                    loginFromCache(prefs, phone, pin)
                } else {
                    recordLoginFailure(context, phone)
                    AgentLoginResult.Failure(GENERIC_LOGIN_FAILURE)
                }
            val isActive = doc.getBoolean(FirestoreSchema.Agent.IS_ACTIVE) ?: false
            if (!isActive) return AgentLoginResult.Failure("Your account has been disconnected. Contact admin.")
            val storedHash = doc.getString(FirestoreSchema.Agent.PIN_HASH).orEmpty()
            if (!AppPreferences.verifyPinHash(pin, storedHash)) {
                recordLoginFailure(context, phone)
                return AgentLoginResult.Failure(GENERIC_LOGIN_FAILURE)
            }
            val name = doc.getString(FirestoreSchema.Agent.NAME).orEmpty()
            @Suppress("UNCHECKED_CAST")
            val assignedGroups = (doc.get(FirestoreSchema.Agent.ASSIGNED_GROUPS) as? List<String>).orEmpty()
            prefs.saveAgentSession(doc.id, name, phone, storedHash, true, assignedGroups)
            clearLoginFailures(context, phone)
            AgentLoginResult.Success(doc.id, name, assignedGroups)
        } catch (e: Exception) {
            loginFromCache(prefs, phone, pin)
        }
    }

    /**
     * Live Firestore check of the *currently logged-in* agent's isActive flag, used to revoke
     * access mid-session - not just at the next login - when the admin deactivates them while
     * their app stays open or they're working offline (see AgentAppFlow's periodic call to this).
     * On success it also refreshes the cached name/assignedGroups/pinHash so a later offline
     * login reflects the latest known state. Returns null (never act on it) when the check
     * couldn't be performed - offline, no cached session, or a Firestore error - so a network
     * hiccup never forces a false logout.
     */
    suspend fun refreshAndCheckActive(context: Context): Boolean? {
        val prefs = AppPreferences(context)
        val agentId = prefs.getAgentId()
        if (agentId.isBlank()) return null
        val firestore = FirebaseSetup.firestoreIfSignedIn(context) ?: return null
        return try {
            val doc = firestore.collection(FirestoreSchema.AGENTS).document(agentId).get().await()
            if (!doc.exists()) return false
            val isActive = doc.getBoolean(FirestoreSchema.Agent.IS_ACTIVE) ?: false
            if (isActive) {
                @Suppress("UNCHECKED_CAST")
                val assignedGroups = (doc.get(FirestoreSchema.Agent.ASSIGNED_GROUPS) as? List<String>).orEmpty()
                val storedHash = doc.getString(FirestoreSchema.Agent.PIN_HASH)?.takeIf { it.isNotBlank() } ?: prefs.getCachedAgentPinHash()
                val name = doc.getString(FirestoreSchema.Agent.NAME)?.takeIf { it.isNotBlank() } ?: prefs.getAgentName()
                prefs.saveAgentSession(agentId, name, prefs.getAgentPhone(), storedHash, true, assignedGroups)
            }
            isActive
        } catch (e: Exception) {
            null
        }
    }

    private fun loginFromCache(prefs: AppPreferences, phone: String, pin: String): AgentLoginResult {
        if (prefs.getAgentPhone() != phone || prefs.getAgentId().isBlank()) {
            return AgentLoginResult.Failure("No internet connection. Connect once to verify your account.")
        }
        if (!prefs.isAgentActiveCached()) return AgentLoginResult.Failure("Your account has been disconnected. Contact admin.")
        if (!AppPreferences.verifyPinHash(pin, prefs.getCachedAgentPinHash())) return AgentLoginResult.Failure(GENERIC_LOGIN_FAILURE)
        return AgentLoginResult.Success(prefs.getAgentId(), prefs.getAgentName(), prefs.getAgentAssignedGroups())
    }

    // ── Login attempt throttling (per phone number, SharedPreferences-backed) ────────────────

    /** Returns a user-facing "too many attempts" message if [phone] is currently throttled, else null. */
    private fun throttleMessage(context: Context, phone: String): String? {
        val entry = readAttempts(context).optJSONObject(phone) ?: return null
        val count = entry.optInt("count", 0)
        val firstAttempt = entry.optLong("firstAttempt", 0L)
        val elapsed = System.currentTimeMillis() - firstAttempt
        if (count >= LOGIN_ATTEMPT_LIMIT && elapsed < LOGIN_ATTEMPT_WINDOW_MS) {
            val retryAfterMinutes = ((LOGIN_ATTEMPT_WINDOW_MS - elapsed) / 60000L) + 1
            return "Too many attempts. Try again in $retryAfterMinutes minute(s)."
        }
        return null
    }

    private fun recordLoginFailure(context: Context, phone: String) {
        val root = readAttempts(context)
        val now = System.currentTimeMillis()
        val existing = root.optJSONObject(phone)
        val firstAttempt = existing?.optLong("firstAttempt", 0L) ?: 0L
        val stillInWindow = existing != null && (now - firstAttempt) < LOGIN_ATTEMPT_WINDOW_MS
        val entry = JSONObject()
        if (stillInWindow) {
            entry.put("count", (existing?.optInt("count", 0) ?: 0) + 1)
            entry.put("firstAttempt", firstAttempt)
        } else {
            entry.put("count", 1)
            entry.put("firstAttempt", now)
        }
        root.put(phone, entry)
        writeAttempts(context, root)
    }

    private fun clearLoginFailures(context: Context, phone: String) {
        val root = readAttempts(context)
        if (root.has(phone)) {
            root.remove(phone)
            writeAttempts(context, root)
        }
    }

    private fun readAttempts(context: Context): JSONObject {
        val raw = context.getSharedPreferences(LOGIN_ATTEMPTS_PREFS, Context.MODE_PRIVATE).getString("attempts", null)
            ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun writeAttempts(context: Context, root: JSONObject) {
        context.getSharedPreferences(LOGIN_ATTEMPTS_PREFS, Context.MODE_PRIVATE)
            .edit().putString("attempts", root.toString()).apply()
    }

    // ── Admin-side management (Labour screen) ────────────────────────────

    suspend fun listAgents(context: Context): List<AgentSummary> {
        val firestore = FirebaseSetup.firestoreIfSignedIn(context) ?: return emptyList()
        val snapshot = firestore.collection(FirestoreSchema.AGENTS).get().await()
        return snapshot.documents.map { doc ->
            @Suppress("UNCHECKED_CAST")
            AgentSummary(
                id = doc.id,
                name = doc.getString(FirestoreSchema.Agent.NAME).orEmpty(),
                phone = doc.getString(FirestoreSchema.Agent.PHONE).orEmpty(),
                isActive = doc.getBoolean(FirestoreSchema.Agent.IS_ACTIVE) ?: false,
                assignedGroups = (doc.get(FirestoreSchema.Agent.ASSIGNED_GROUPS) as? List<String>).orEmpty()
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun createAgent(context: Context, name: String, phone: String, pin: String): Result<String> {
        val firestore = FirebaseSetup.firestoreIfSignedIn(context)
            ?: return Result.failure(IllegalStateException("Firebase not configured. Add app/google-services.json first."))
        return try {
            // A phone number must map to exactly one agent - without this check, creating a
            // second agent with the same phone silently succeeds, and login (which queries
            // by phone and takes the first match) can then pick the wrong document, making
            // login fail unpredictably even with the correct PIN.
            val existing = firestore.collection(FirestoreSchema.AGENTS)
                .whereEqualTo(FirestoreSchema.Agent.PHONE, phone)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.failure(IllegalStateException("A labour account with this phone number already exists."))
            }
            val data = hashMapOf(
                FirestoreSchema.Agent.NAME to name,
                FirestoreSchema.Agent.PHONE to phone,
                FirestoreSchema.Agent.PIN_HASH to AppPreferences.hashPin(pin),
                FirestoreSchema.Agent.IS_ACTIVE to true,
                FirestoreSchema.Agent.ASSIGNED_GROUPS to emptyList<String>(),
                FirestoreSchema.Agent.CREATED_AT to FieldValue.serverTimestamp(),
                FirestoreSchema.Agent.CREATED_BY to "admin"
            )
            val ref = firestore.collection(FirestoreSchema.AGENTS).add(data).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setActive(context: Context, agentId: String, isActive: Boolean): Result<Unit> =
        update(context, agentId, mapOf(FirestoreSchema.Agent.IS_ACTIVE to isActive))

    suspend fun resetPin(context: Context, agentId: String, newPin: String): Result<Unit> =
        update(
            context, agentId, mapOf(
                FirestoreSchema.Agent.PIN_HASH to AppPreferences.hashPin(newPin),
                FirestoreSchema.Agent.IS_ACTIVE to true
            )
        )

    suspend fun setAssignedGroups(context: Context, agentId: String, groupIds: List<String>): Result<Unit> =
        update(context, agentId, mapOf(FirestoreSchema.Agent.ASSIGNED_GROUPS to groupIds))

    suspend fun deleteAgent(context: Context, agentId: String): Result<Unit> {
        val firestore = FirebaseSetup.firestoreIfSignedIn(context)
            ?: return Result.failure(IllegalStateException("Firebase not configured."))
        return try {
            firestore.collection(FirestoreSchema.AGENTS).document(agentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun update(context: Context, agentId: String, fields: Map<String, Any>): Result<Unit> {
        val firestore = FirebaseSetup.firestoreIfSignedIn(context)
            ?: return Result.failure(IllegalStateException("Firebase not configured."))
        return try {
            firestore.collection(FirestoreSchema.AGENTS).document(agentId).update(fields).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
