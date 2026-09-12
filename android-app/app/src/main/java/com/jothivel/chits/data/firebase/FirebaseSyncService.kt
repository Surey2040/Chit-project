package com.jothivel.chits.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.CollectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Admin-only real-time bridge: agent collections land in Firestore `collections/`, this listens
 * for the ones not yet applied (`syncedToAdmin == false`), mirrors each into the admin's local
 * Room DB via [CollectionService.record] (so the existing Ledger — which reads Room via
 * LiveData — updates automatically), then flags the Firestore doc as synced.
 *
 * Started from MainHostActivity while userRole == ADMIN; a no-op if Firebase isn't configured.
 */
object FirebaseSyncService {
    private const val TAG = "FirebaseSyncService"
    private var registration: ListenerRegistration? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(context: Context) {
        if (registration != null) return
        val appContext = context.applicationContext
        val firestore = FirebaseSetup.firestoreOrNull(appContext) ?: return
        registration = firestore.collection(FirestoreSchema.COLLECTIONS)
            .whereEqualTo(FirestoreSchema.Collection.SYNCED_TO_ADMIN, false)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) {
                    if (error != null) Log.e(TAG, "Listener error", error)
                    return@addSnapshotListener
                }
                snapshots.documentChanges
                    .filter { it.type != DocumentChange.Type.REMOVED }
                    .forEach { change -> scope.launch { applyToRoom(appContext, firestore, change.document) } }
            }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }

    private suspend fun applyToRoom(context: Context, firestore: FirebaseFirestore, doc: DocumentSnapshot) {
        try {
            val memberId = doc.getString(FirestoreSchema.Collection.MEMBER_ID)
            val groupId = doc.getString(FirestoreSchema.Collection.GROUP_ID)
            val amountPaise = doc.getLong(FirestoreSchema.Collection.AMOUNT_PAISE) ?: 0L
            if (memberId != null && groupId != null && amountPaise > 0) {
                val db = AppDatabase.getDatabase(context)
                val requestId = doc.getString(FirestoreSchema.Collection.REQUEST_ID) ?: doc.id
                val memberName = doc.getString(FirestoreSchema.Collection.MEMBER_NAME).orEmpty()
                val mode = doc.getString(FirestoreSchema.Collection.MODE) ?: "Cash"
                val referenceNo = doc.getString(FirestoreSchema.Collection.REFERENCE_NO)
                val notes = doc.getString(FirestoreSchema.Collection.NOTES).orEmpty()
                val businessDate = doc.getString(FirestoreSchema.Collection.BUSINESS_DATE) ?: CollectionService.todayKey()
                val agentName = doc.getString(FirestoreSchema.Collection.AGENT_NAME)
                val agentId = doc.getString(FirestoreSchema.Collection.AGENT_ID)
                runCatching {
                    CollectionService.record(db, requestId, memberId, memberName, groupId, amountPaise, mode, referenceNo, notes, businessDate, agentName, agentId)
                }.onFailure { Log.e(TAG, "CollectionService.record failed for ${doc.id}", it) }
            }
            firestore.collection(FirestoreSchema.COLLECTIONS).document(doc.id)
                .update(FirestoreSchema.Collection.SYNCED_TO_ADMIN, true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync collection ${doc.id}", e)
        }
    }
}
