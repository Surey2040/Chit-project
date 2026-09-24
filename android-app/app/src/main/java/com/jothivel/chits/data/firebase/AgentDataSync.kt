package com.jothivel.chits.data.firebase

import android.content.Context
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.ChitMembershipEntity
import com.jothivel.chits.data.local.entity.InstallmentEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Agent-side counterpart to [FirestoreDataSync]: downloads the chit groups this agent is
 * assigned to (plus their members/memberships/installments) into the agent phone's own Room DB.
 * CollectionService.record() needs those rows locally to work — an agent phone otherwise has no
 * seed data of its own, since Room is per-device local storage.
 */
object AgentDataSync {

    suspend fun syncAssignedGroups(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val prefs = AppPreferences(context)
        val assignedGroupIds = prefs.getAgentAssignedGroups()
        if (assignedGroupIds.isEmpty()) return@withContext Result.success(0)
        val firestore = FirebaseSetup.firestoreIfSignedIn(context)
            ?: return@withContext Result.failure(IllegalStateException("No internet connection."))
        try {
            val db = AppDatabase.getDatabase(context)

            val groups = assignedGroupIds.mapNotNull { groupId ->
                val doc = firestore.collection(FirestoreSchema.CHIT_GROUPS).document(groupId).get().await()
                if (!doc.exists()) null else ChitGroupEntity().apply {
                    id = groupId
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

            val memberships = assignedGroupIds.flatMap { groupId ->
                firestore.collection(FirestoreSchema.CHIT_MEMBERSHIPS)
                    .whereEqualTo("groupId", groupId).get().await().documents
            }.mapNotNull { doc ->
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

            val memberIds = memberships.map { it.memberId }.distinct()
            val members = memberIds.mapNotNull { memberId ->
                val doc = firestore.collection(FirestoreSchema.MEMBERS).document(memberId).get().await()
                if (!doc.exists()) return@mapNotNull null
                // Fall back to the existing local row for any field the cloud document doesn't
                // carry - insertAll below REPLACEs the whole row, so a missing field here would
                // otherwise null it out even though it's already correctly stored locally.
                val existing = db.memberDao().getMemberByIdSync(memberId)
                MemberEntity().apply {
                    id = memberId
                    name = doc.getString("name") ?: existing?.name
                    phone = doc.getString("phone") ?: existing?.phone
                    photoUrl = doc.getString("photoUrl") ?: existing?.photoUrl
                    nomineeName = doc.getString("nomineeName") ?: existing?.nomineeName
                    nomineePhone = doc.getString("nomineePhone") ?: existing?.nomineePhone
                    role = doc.getString("role") ?: existing?.role
                    isActive = doc.getBoolean("isActive") ?: existing?.isActive ?: true
                    dob = doc.getString("dob") ?: existing?.dob
                    gender = doc.getString("gender") ?: existing?.gender
                    addressLine = doc.getString("addressLine") ?: existing?.addressLine
                    city = doc.getString("city") ?: existing?.city
                    state = doc.getString("state") ?: existing?.state
                    pincode = doc.getString("pincode") ?: existing?.pincode
                    aadhaarNoEncrypted = doc.getString("aadhaarNoEncrypted") ?: existing?.aadhaarNoEncrypted
                    panNo = doc.getString("panNo") ?: existing?.panNo
                    aadhaarDocumentPath = doc.getString("aadhaarDocumentPath") ?: existing?.aadhaarDocumentPath
                    panDocumentPath = doc.getString("panDocumentPath") ?: existing?.panDocumentPath
                    selectedChitId = doc.getString("selectedChitId") ?: existing?.selectedChitId
                    ticketNo = doc.getString("ticketNo") ?: existing?.ticketNo
                    installmentAmount = doc.getString("installmentAmount") ?: existing?.installmentAmount
                    joiningDate = doc.getString("joiningDate") ?: existing?.joiningDate
                    dueDate = doc.getString("dueDate") ?: existing?.dueDate
                    nomineeRelationship = doc.getString("nomineeRelationship") ?: existing?.nomineeRelationship
                }
            }
            db.memberDao().insertAll(members)

            val installments = assignedGroupIds.flatMap { groupId ->
                firestore.collection(FirestoreSchema.INSTALLMENTS)
                    .whereEqualTo("groupId", groupId).get().await().documents
            }.map { doc ->
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

            Result.success(groups.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
