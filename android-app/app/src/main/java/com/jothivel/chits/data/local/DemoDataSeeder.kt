package com.jothivel.chits.data.local

import android.content.Context
import com.jothivel.chits.data.local.entity.ActivityLogEntity
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.InstallmentEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.data.local.entity.PaymentEntity
import com.jothivel.chits.data.local.entity.ChitMembershipEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Idempotent test data: only missing DEMO records are inserted. */
object DemoDataSeeder {
    private val names = listOf(
        "Arun Kumar", "Bala Murugan", "Chinnammal", "Dhanalakshmi", "Elango",
        "Ganesan", "Gopal", "Hariharan", "Indirani", "Johnsi Rani",
        "Kalaiyarasan", "Lakshmi", "Muthusamy", "Nandhini", "Palani",
        "Pushpam", "Rajendran", "Saravanan", "Thangavel", "Vijaya"
    )
    private val areas = listOf("Manapparai", "Singampunari", "Ponnamaravathi", "Thuvarankurichi", "Kottampatti")
    private val villages = listOf("Ponnampatti", "Kannuthu", "Purathakudi", "Kulathupatti", "Elangakurichy")
    private val modes = listOf("CASH", "UPI", "BANK_TRANSFER", "DOOR_COLLECTION")
    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)

    fun seed(context: Context): Int {
        val db = AppDatabase.getDatabase(context)
        val existingMemberIds = db.memberDao().getAllMembersSync().map { it.id }.toSet()
        val existingGroupIds = db.groupDao().getAllGroupsSync().map { it.id }.toSet()
        val groups = buildGroups().filterNot { it.id in existingGroupIds }
        val installments = buildInstallments(db)
        val members = buildMembers().filterNot { it.id in existingMemberIds }
        val memberships = members.map { member -> ChitMembershipEntity().apply {
            id = "${member.id}:${member.selectedChitId}"
            memberId = member.id
            groupId = member.selectedChitId
            ticketNo = member.ticketNo
            installmentAmountPaise = (member.installmentAmount.toLongOrNull() ?: 0L) * 100
            joiningDate = member.joiningDate
            dueDate = member.dueDate
            isActive = member.isActive
        } }
        val payments = buildPayments(members)

        db.runInTransaction {
            if (groups.isNotEmpty()) db.groupDao().insertAll(groups)
            if (installments.isNotEmpty()) db.installmentDao().insertAll(installments)
            if (members.isNotEmpty()) db.memberDao().insertAll(members)
            if (memberships.isNotEmpty()) db.membershipDao().insertAll(memberships)
            if (payments.isNotEmpty()) db.paymentDao().insertAll(payments)
            if (members.isNotEmpty()) {
                db.activityLogDao().insertLog(
                    ActivityLogEntity(
                        actionType = "DEMO_DATA_SEEDED",
                        title = "Demo test data added",
                        description = "${members.size} demo customers and their payment histories were added."
                    )
                )
            }
        }
        return members.size
    }

    private fun buildGroups() = (1..5).map { number ->
        ChitGroupEntity().apply {
            id = "DEMO-G%02d".format(number)
            name = "Demo Chit $number"
            registerNo = "Cits-$number"
            chitValue = listOf(5_000_000, 10_000_000, 20_000_000, 30_000_000, 50_000_000)[number - 1]
            durationMonths = 20
            subscriberCount = 20
            branch = areas[number - 1]
            startDate = date(-number * 2)
            status = "ACTIVE"
        }
    }

    private fun buildInstallments(db: AppDatabase): List<InstallmentEntity> {
        val result = mutableListOf<InstallmentEntity>()
        (1..5).forEach { groupNumber ->
            val groupId = "DEMO-G%02d".format(groupNumber)
            if (db.installmentDao().getInstallmentsForGroupSync(groupId).isNotEmpty()) return@forEach
            val monthlyPaise = listOf(250_000, 500_000, 1_000_000, 1_500_000, 2_500_000)[groupNumber - 1]
            (1..20).forEach { installmentNo ->
                result += InstallmentEntity().apply {
                    id = "$groupId-I%02d".format(installmentNo)
                    this.groupId = groupId
                    this.installmentNo = installmentNo
                    baseAmount = monthlyPaise
                    kasaruAmount = if (installmentNo % 4 == 0) monthlyPaise / 20 else 0
                    payoutAmount = if (installmentNo % 5 == 0) monthlyPaise * 19 else null
                    auctionDate = date(-(21 - installmentNo))
                    status = if (installmentNo <= 12) "COMPLETED" else "UPCOMING"
                    winningMemberId = if (installmentNo <= 12) "DEMO-C%03d".format(((groupNumber - 1) * 20) + installmentNo) else null
                }
            }
        }
        return result
    }

    private fun buildMembers() = (1..100).map { number ->
        val groupNumber = ((number - 1) / 20) + 1
        val startOffset = -groupNumber * 2
        MemberEntity().apply {
            id = "DEMO-C%03d".format(number)
            name = "${names[(number - 1) % names.size]} %03d".format(number)
            phone = (9_000_000_000L + number).toString()
            photoUrl = "demo://customer/$number/photo"
            nomineeName = "${names[number % names.size]} Nominee"
            nomineePhone = (8_000_000_000L + number).toString()
            role = if (number % 10 == 0) "AGENT" else "MEMBER"
            isActive = number % 17 != 0
            dob = "${(number % 27) + 1}-${((number % 12) + 1).toString().padStart(2, '0')}-${1970 + (number % 30)}"
            gender = if (number % 2 == 0) "FEMALE" else "MALE"
            addressLine = "${number}, ${villages[(number - 1) % villages.size]} Main Road"
            city = areas[(groupNumber - 1) % areas.size]
            state = "Tamil Nadu"
            pincode = (621300 + number % 50).toString()
            aadhaarNoEncrypted = "DEMO-ENCRYPTED-AADHAAR-%03d".format(number)
            panNo = "DEMO%04dP".format(number)
            aadhaarDocumentPath = "demo://customer/$number/aadhaar"
            panDocumentPath = "demo://customer/$number/pan"
            selectedChitId = "DEMO-G%02d".format(groupNumber)
            ticketNo = ((number - 1) % 20 + 1).toString()
            installmentAmount = listOf("2500", "5000", "10000", "15000", "25000")[groupNumber - 1]
            joiningDate = date(startOffset)
            dueDate = date(startOffset + 20)
            nomineeRelationship = if (number % 2 == 0) "Spouse" else "Parent"
        }
    }

    private fun buildPayments(newMembers: List<MemberEntity>): List<PaymentEntity> {
        val result = mutableListOf<PaymentEntity>()
        newMembers.forEachIndexed { memberIndex, member ->
            val paidMonths = memberIndex % 21
            val monthlyPaise = (member.installmentAmount.toLongOrNull() ?: 0L) * 100
            (1..paidMonths).forEach { installmentNo ->
                result += PaymentEntity().apply {
                    id = "DEMO-P-${member.id}-%02d".format(installmentNo)
                    memberId = member.id
                    groupId = member.selectedChitId
                    installmentId = installmentNo.toString()
                    amountPaid = if (installmentNo == paidMonths && memberIndex % 7 == 0) monthlyPaise / 2 else monthlyPaise
                    mode = modes[(memberIndex + installmentNo) % modes.size]
                    referenceNo = "DEMO-UTR-${memberIndex + 1}-$installmentNo"
                    receiptNo = "DEMO-RCT-%03d-%02d".format(memberIndex + 1, installmentNo)
                    paidAt = if (installmentNo == paidMonths && memberIndex % 8 == 0) todayAt(9 + memberIndex % 8) else monthsAgo(paidMonths - installmentNo)
                    status = if (amountPaid < monthlyPaise) "PARTIAL" else "PAID"
                }
            }
        }
        return result
    }

    private fun date(offsetMonths: Int): String = dateFormat.format(Calendar.getInstance().apply { add(Calendar.MONTH, offsetMonths) }.time)
    private fun monthsAgo(months: Int): Long = Calendar.getInstance().apply { add(Calendar.MONTH, -months); set(Calendar.DAY_OF_MONTH, 15) }.timeInMillis
    private fun todayAt(hour: Int): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 15); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
}
