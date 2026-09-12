package com.jothivel.chits.data.firebase

import android.content.Context
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AgentCollectionDoc(
    val requestId: String,
    val agentId: String,
    val agentName: String,
    val memberId: String,
    val memberName: String,
    val groupId: String,
    val chitNo: String,
    val amountPaise: Long,
    val mode: String,
    val referenceNo: String?,
    val receiptNo: String,
    val notes: String,
    val businessDate: String,
    val status: String
)

/**
 * Pushes an agent's collection to Firestore `collections/{requestId}` (the admin's
 * [FirebaseSyncService] listens for `syncedToAdmin == false` docs and mirrors them into Room).
 * requestId doubles as the document id so a retried write can never create a duplicate.
 *
 * Offline-first: a failed write is queued in SharedPreferences and retried by [flushPending]
 * the next time the agent app has connectivity (called from the agent screens on start/refresh).
 */
object AgentCollectionSync {
    private const val PREFS_NAME = "jvc_pending_collections"
    private const val KEY_QUEUE = "queue"

    suspend fun push(context: Context, doc: AgentCollectionDoc): Boolean = withContext(Dispatchers.IO) {
        val firestore = FirebaseSetup.firestoreIfSignedIn(context)
        if (firestore == null) {
            queue(context, doc)
            return@withContext false
        }
        try {
            firestore.collection(FirestoreSchema.COLLECTIONS).document(doc.requestId).set(toMap(doc)).await()
            true
        } catch (e: Exception) {
            queue(context, doc)
            false
        }
    }

    suspend fun flushPending(context: Context): Int = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = readQueue(prefs)
        if (pending.isEmpty()) return@withContext 0
        val firestore = FirebaseSetup.firestoreIfSignedIn(context) ?: return@withContext 0
        var flushed = 0
        val remaining = mutableListOf<AgentCollectionDoc>()
        pending.forEach { doc ->
            try {
                firestore.collection(FirestoreSchema.COLLECTIONS).document(doc.requestId).set(toMap(doc)).await()
                flushed++
            } catch (e: Exception) {
                remaining += doc
            }
        }
        writeQueue(prefs, remaining)
        flushed
    }

    fun pendingCount(context: Context): Int =
        readQueue(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)).size

    private fun queue(context: Context, doc: AgentCollectionDoc) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = readQueue(prefs).filterNot { it.requestId == doc.requestId }
        writeQueue(prefs, current + doc)
    }

    private fun toMap(doc: AgentCollectionDoc): Map<String, Any?> = mapOf(
        FirestoreSchema.Collection.AGENT_ID to doc.agentId,
        FirestoreSchema.Collection.AGENT_NAME to doc.agentName,
        FirestoreSchema.Collection.MEMBER_ID to doc.memberId,
        FirestoreSchema.Collection.MEMBER_NAME to doc.memberName,
        FirestoreSchema.Collection.GROUP_ID to doc.groupId,
        FirestoreSchema.Collection.CHIT_NO to doc.chitNo,
        FirestoreSchema.Collection.AMOUNT_PAISE to doc.amountPaise,
        FirestoreSchema.Collection.MODE to doc.mode,
        FirestoreSchema.Collection.REFERENCE_NO to doc.referenceNo,
        FirestoreSchema.Collection.RECEIPT_NO to doc.receiptNo,
        FirestoreSchema.Collection.NOTES to doc.notes,
        FirestoreSchema.Collection.BUSINESS_DATE to doc.businessDate,
        FirestoreSchema.Collection.TIMESTAMP to FieldValue.serverTimestamp(),
        FirestoreSchema.Collection.STATUS to doc.status,
        FirestoreSchema.Collection.SYNCED_TO_ADMIN to false,
        FirestoreSchema.Collection.REQUEST_ID to doc.requestId
    )

    private fun readQueue(prefs: android.content.SharedPreferences): List<AgentCollectionDoc> {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                AgentCollectionDoc(
                    requestId = o.getString("requestId"),
                    agentId = o.getString("agentId"),
                    agentName = o.getString("agentName"),
                    memberId = o.getString("memberId"),
                    memberName = o.getString("memberName"),
                    groupId = o.getString("groupId"),
                    chitNo = o.getString("chitNo"),
                    amountPaise = o.getLong("amountPaise"),
                    mode = o.getString("mode"),
                    referenceNo = o.optString("referenceNo").ifBlank { null },
                    receiptNo = o.getString("receiptNo"),
                    notes = o.optString("notes"),
                    businessDate = o.getString("businessDate"),
                    status = o.getString("status")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeQueue(prefs: android.content.SharedPreferences, items: List<AgentCollectionDoc>) {
        val array = JSONArray()
        items.forEach { doc ->
            array.put(JSONObject().apply {
                put("requestId", doc.requestId)
                put("agentId", doc.agentId)
                put("agentName", doc.agentName)
                put("memberId", doc.memberId)
                put("memberName", doc.memberName)
                put("groupId", doc.groupId)
                put("chitNo", doc.chitNo)
                put("amountPaise", doc.amountPaise)
                put("mode", doc.mode)
                put("referenceNo", doc.referenceNo ?: "")
                put("receiptNo", doc.receiptNo)
                put("notes", doc.notes)
                put("businessDate", doc.businessDate)
                put("status", doc.status)
            })
        }
        prefs.edit().putString(KEY_QUEUE, array.toString()).apply()
    }
}
