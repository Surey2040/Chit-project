package com.jothivel.chits.data.firebase

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.jothivel.chits.data.local.AppDatabase
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
        val firestore = FirebaseSetup.firestoreOrNull(context)
            ?: return Result.failure(IllegalStateException("Firebase not configured. Add app/google-services.json first."))
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
