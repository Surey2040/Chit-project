package com.jothivel.chits.data.firebase

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.jothivel.chits.utils.AppPreferences

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

    suspend fun login(context: Context, phone: String, pin: String): AgentLoginResult {
        val prefs = AppPreferences(context)
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
                    AgentLoginResult.Failure("Phone number not registered. Contact admin.")
                }
            val isActive = doc.getBoolean(FirestoreSchema.Agent.IS_ACTIVE) ?: false
            if (!isActive) return AgentLoginResult.Failure("Your account has been disconnected. Contact admin.")
            val storedHash = doc.getString(FirestoreSchema.Agent.PIN_HASH).orEmpty()
            if (!AppPreferences.verifyPinHash(pin, storedHash)) return AgentLoginResult.Failure("Invalid PIN")
            val name = doc.getString(FirestoreSchema.Agent.NAME).orEmpty()
            @Suppress("UNCHECKED_CAST")
            val assignedGroups = (doc.get(FirestoreSchema.Agent.ASSIGNED_GROUPS) as? List<String>).orEmpty()
            prefs.saveAgentSession(doc.id, name, phone, storedHash, true, assignedGroups)
            AgentLoginResult.Success(doc.id, name, assignedGroups)
        } catch (e: Exception) {
            loginFromCache(prefs, phone, pin)
        }
    }

    private fun loginFromCache(prefs: AppPreferences, phone: String, pin: String): AgentLoginResult {
        if (prefs.getAgentPhone() != phone || prefs.getAgentId().isBlank()) {
            return AgentLoginResult.Failure("No internet connection. Connect once to verify your account.")
        }
        if (!prefs.isAgentActiveCached()) return AgentLoginResult.Failure("Your account has been disconnected. Contact admin.")
        if (!AppPreferences.verifyPinHash(pin, prefs.getCachedAgentPinHash())) return AgentLoginResult.Failure("Invalid PIN")
        return AgentLoginResult.Success(prefs.getAgentId(), prefs.getAgentName(), prefs.getAgentAssignedGroups())
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
