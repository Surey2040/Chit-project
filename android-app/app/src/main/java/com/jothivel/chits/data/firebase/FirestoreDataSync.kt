package com.jothivel.chits.data.firebase

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.CollectionService
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.ChitMembershipEntity
import com.jothivel.chits.data.local.entity.InstallmentEntity
import com.jothivel.chits.data.local.entity.MemberEntity

/**
 * Admin-only, one-time (repeatable) push of the reference data agents need to work offline:
 * active chit groups, their members, memberships and installment schedules. Triggered manually
 * from the Labour screen's "Sync Data to Cloud" button whenever the admin adds/changes members —
 * this is reference data, not a live feed, so a snapshot listener isn't needed here.
 */
object FirestoreDataSync {

    data class SyncResult(val groups: Int, val members: Int, val memberships: Int, val installments: Int)

    suspend fun syncAllToCloud(context: Context): Result<SyncResult> {
        val firestore = FirebaseSetup.firestoreIfSignedIn(context)
            ?: return Result.failure(IllegalStateException("Firebase not configured, or not signed in. Add app/google-services.json and check connectivity."))
        return try {
            val db = AppDatabase.getDatabase(context)
            val groups = db.groupDao().getAllGroupsSync().filter { it.status == "ACTIVE" }
            val groupIds = groups.mapTo(hashSetOf()) { it.id }
            val members = db.memberDao().getAllMembersSync().filter { it.isActive }
            val memberships = db.membershipDao().getAllActiveSync().filter { it.groupId in groupIds }
            val installments = groupIds.flatMap { db.installmentDao().getInstallmentsForGroupSync(it) }

            writeInBatches(firestore, FirestoreSchema.CHIT_GROUPS, groups) { it.id to groupMap(it) }
            writeInBatches(firestore, FirestoreSchema.MEMBERS, members) { it.id to memberMap(it) }
            writeInBatches(firestore, FirestoreSchema.CHIT_MEMBERSHIPS, memberships) { it.id to membershipMap(it) }
            writeInBatches(firestore, FirestoreSchema.INSTALLMENTS, installments) { it.id to installmentMap(it) }

            Result.success(SyncResult(groups.size, members.size, memberships.size, installments.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class RestoreResult(val groups: Int, val members: Int, val memberships: Int, val installments: Int, val collections: Int)

    /**
     * Admin-only pull of everything back down from Firestore into local Room - the counterpart
     * to [syncAllToCloud]. Meant for the "I uninstalled/lost the app, reinstalled, need my data
     * back" scenario: as long a "Sync Data to Cloud" push happened at some point before that,
     * this rebuilds the local database from what's in the cloud. Uses the same insert/upsert
     * helpers as [AgentDataSync] (IGNORE-on-conflict for groups/members/installments, REPLACE
     * for memberships), so it's safe to run even if some local data already exists.
     */
    suspend fun restoreAllFromCloud(context: Context): Result<RestoreResult> {
        val firestore = FirebaseSetup.firestoreIfSignedIn(context)
            ?: return Result.failure(IllegalStateException("Firebase not configured, or not signed in. Add app/google-services.json and check connectivity."))
        return try {
            val db = AppDatabase.getDatabase(context)

            val groups = firestore.collection(FirestoreSchema.CHIT_GROUPS).get().await().documents.map { doc ->
                ChitGroupEntity().apply {
                    id = doc.id
                    name = doc.getString("name")
                    registerNo = doc.getString("registerNo")
                    chitValue = (doc.getLong("chitValue") ?: 0L).toInt()
                    durationMonths = (doc.getLong("durationMonths") ?: 0L).toInt()
                    subscriberCount = (doc.getLong("subscriberCount") ?: 0L).toInt()
                    branch = doc.getString("branch")
                    startDate = doc.getString("startDate")
                    status = doc.getString("status") ?: "ACTIVE"
                }
            }
            db.groupDao().insertAll(groups)

            val members = firestore.collection(FirestoreSchema.MEMBERS).get().await().documents.map { doc ->
                MemberEntity().apply {
                    id = doc.id
                    name = doc.getString("name")
                    phone = doc.getString("phone")
                    ticketNo = doc.getString("ticketNo")
                    selectedChitId = doc.getString("selectedChitId")
                    installmentAmount = doc.getString("installmentAmount")
                    isActive = doc.getBoolean("isActive") ?: true
                }
            }
            db.memberDao().insertAll(members)

            val memberships = firestore.collection(FirestoreSchema.CHIT_MEMBERSHIPS).get().await().documents.mapNotNull { doc ->
                val memberId = doc.getString("memberId") ?: return@mapNotNull null
                val groupId = doc.getString("groupId") ?: return@mapNotNull null
                ChitMembershipEntity().apply {
                    id = doc.id
                    this.memberId = memberId
                    this.groupId = groupId
                    ticketNo = doc.getString("ticketNo")
                    installmentAmountPaise = doc.getLong("installmentAmountPaise") ?: 0L
                    joiningDate = doc.getString("joiningDate")
                    dueDate = doc.getString("dueDate")
                    isActive = doc.getBoolean("isActive") ?: true
                }
            }
            db.membershipDao().upsertAll(memberships)

            val installments = firestore.collection(FirestoreSchema.INSTALLMENTS).get().await().documents.map { doc ->
                InstallmentEntity().apply {
                    id = doc.id
                    groupId = doc.getString("groupId")
                    installmentNo = (doc.getLong("installmentNo") ?: 0L).toInt()
                    baseAmount = (doc.getLong("baseAmount") ?: 0L).toInt()
                    kasaruAmount = doc.getLong("kasaruAmount")?.toInt()
                    payoutAmount = doc.getLong("payoutAmount")?.toInt()
                    auctionDate = doc.getString("auctionDate")
                    status = doc.getString("status") ?: "PENDING"
                    winningMemberId = doc.getString("winningMemberId")
                }
            }
            db.installmentDao().insertAll(installments)

            // Restore actual payment/receipt history too - CollectionService.record() is
            // idempotent on requestId, so re-running restore (or restoring on top of a device
            // that already has some of these) never creates duplicate payment rows.
            var restoredCollections = 0
            firestore.collection(FirestoreSchema.COLLECTIONS).get().await().documents.forEach { doc ->
                val requestId = doc.getString(FirestoreSchema.Collection.REQUEST_ID) ?: doc.id
                val memberId = doc.getString(FirestoreSchema.Collection.MEMBER_ID) ?: return@forEach
                val groupId = doc.getString(FirestoreSchema.Collection.GROUP_ID) ?: return@forEach
                val amountPaise = doc.getLong(FirestoreSchema.Collection.AMOUNT_PAISE) ?: return@forEach
                if (amountPaise <= 0) return@forEach
                val memberName = doc.getString(FirestoreSchema.Collection.MEMBER_NAME).orEmpty()
                val mode = doc.getString(FirestoreSchema.Collection.MODE) ?: "Cash"
                val referenceNo = doc.getString(FirestoreSchema.Collection.REFERENCE_NO)
                val notes = doc.getString(FirestoreSchema.Collection.NOTES).orEmpty()
                val businessDate = doc.getString(FirestoreSchema.Collection.BUSINESS_DATE) ?: CollectionService.todayKey()
                val agentName = doc.getString(FirestoreSchema.Collection.AGENT_NAME)?.ifBlank { null }
                val agentId = doc.getString(FirestoreSchema.Collection.AGENT_ID)?.ifBlank { null }
                runCatching {
                    CollectionService.record(db, requestId, memberId, memberName, groupId, amountPaise, mode, referenceNo, notes, businessDate, agentName, agentId)
                    restoredCollections++
                }
            }

            Result.success(RestoreResult(groups.size, members.size, memberships.size, installments.size, restoredCollections))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> writeInBatches(firestore: FirebaseFirestore, collection: String, items: List<T>, toDoc: (T) -> Pair<String, Map<String, Any?>>) {
        items.chunked(400).forEach { chunk ->
            val batch: WriteBatch = firestore.batch()
            chunk.forEach { item ->
                val (id, data) = toDoc(item)
                batch.set(firestore.collection(collection).document(id), data)
            }
            batch.commit().await()
        }
    }

    private fun groupMap(group: ChitGroupEntity): Map<String, Any?> = mapOf(
        "name" to group.name,
        "registerNo" to group.registerNo,
        "chitValue" to group.chitValue,
        "durationMonths" to group.durationMonths,
        "subscriberCount" to group.subscriberCount,
        "branch" to group.branch,
        "startDate" to group.startDate,
        "status" to group.status
    )

    private fun memberMap(member: MemberEntity): Map<String, Any?> = mapOf(
        "name" to member.name,
        "phone" to member.phone,
        "ticketNo" to member.ticketNo,
        "selectedChitId" to member.selectedChitId,
        "installmentAmount" to member.installmentAmount,
        "isActive" to member.isActive
    )

    private fun membershipMap(membership: ChitMembershipEntity): Map<String, Any?> = mapOf(
        "memberId" to membership.memberId,
        "groupId" to membership.groupId,
        "ticketNo" to membership.ticketNo,
        "installmentAmountPaise" to membership.installmentAmountPaise,
        "joiningDate" to membership.joiningDate,
        "dueDate" to membership.dueDate,
        "isActive" to membership.isActive
    )

    private fun installmentMap(installment: InstallmentEntity): Map<String, Any?> = mapOf(
        "groupId" to installment.groupId,
        "installmentNo" to installment.installmentNo,
        "baseAmount" to installment.baseAmount,
        "kasaruAmount" to installment.kasaruAmount,
        "payoutAmount" to installment.payoutAmount,
        "auctionDate" to installment.auctionDate,
        "status" to installment.status,
        "winningMemberId" to installment.winningMemberId
    )
}
