package com.jothivel.chits.data.local

import com.jothivel.chits.data.local.entity.ActivityLogEntity
import com.jothivel.chits.data.local.entity.CollectionReceiptEntity
import com.jothivel.chits.data.local.entity.PaymentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SavedCollection(
    val receiptNo: String,
    val amountPaise: Long,
    val mode: String,
    val businessDate: String,
    val referenceNo: String?
)

data class DueBreakdown(
    val payablePaise: Long,
    val paidPaise: Long,
    val pendingPaise: Long,
    val pendingInstallments: List<Int>,
    val earliestDueDate: String,
    val overdueDays: Int,
    val lastPaidAt: Long?
)

data class CalendarScheduledDue(
    val dateKey: String,
    val memberId: String,
    val memberName: String,
    val groupId: String,
    val chitNo: String,
    val installmentNo: Int,
    val remainingPaise: Long
)

object CollectionService {
    private val storageDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayKey(): String = synchronized(storageDate) { storageDate.format(Date()) }

    fun calculateDuePaise(db: AppDatabase, memberId: String, groupId: String, asOf: Date = Date()): Long {
        return calculateDueBreakdown(db, memberId, groupId, asOf).pendingPaise
    }

    /**
     * Builds the complete unpaid monthly schedule used by the dashboard calendar.
     * Direct installment payments and ADVANCE credit are consumed in installment order,
     * so a fully-covered member is never shown again as due for that month.
     */
    fun buildCalendarSchedule(db: AppDatabase): List<CalendarScheduledDue> {
        val members = db.memberDao().getAllMembersSync().associateBy { it.id }
        val groups = db.groupDao().getAllGroupsSync().associateBy { it.id }
        val paymentsByMembership = db.paymentDao().getAllPaymentsSync().groupBy { it.memberId to it.groupId }
        val installmentsByGroup = groups.keys.associateWith { db.installmentDao().getInstallmentsForGroupSync(it) }

        return db.membershipDao().getAllActiveSync().flatMap { membership ->
            val member = members[membership.memberId] ?: return@flatMap emptyList()
            val group = groups[membership.groupId] ?: return@flatMap emptyList()
            val startDate = parseDate(group.startDate) ?: return@flatMap emptyList()
            val payments = paymentsByMembership[membership.memberId to membership.groupId].orEmpty()
            val directPaid = payments
                .filterNot { it.installmentId.equals("ADVANCE", true) }
                .groupBy { it.installmentId }
                .mapValues { (_, rows) -> rows.sumOf { it.amountPaid } }
            var advanceCredit = payments.filter { it.installmentId.equals("ADVANCE", true) }.sumOf { it.amountPaid }

            installmentsByGroup[membership.groupId].orEmpty().mapNotNull { installment ->
                val scheduled = (membership.installmentAmountPaise.takeIf { it > 0 }
                    ?: (installment.baseAmount - (installment.kasaruAmount ?: 0)).toLong()).coerceAtLeast(0L)
                var remaining = (scheduled - directPaid[installment.installmentNo.toString()].orZero()).coerceAtLeast(0L)
                val creditUsed = minOf(advanceCredit, remaining)
                advanceCredit -= creditUsed
                remaining -= creditUsed
                if (remaining <= 0) return@mapNotNull null

                val dueDate = Calendar.getInstance().apply {
                    time = startDate
                    add(Calendar.MONTH, installment.installmentNo - 1)
                }.time
                CalendarScheduledDue(
                    dateKey = synchronized(storageDate) { storageDate.format(dueDate) },
                    memberId = member.id,
                    memberName = member.name,
                    groupId = group.id,
                    chitNo = group.registerNo,
                    installmentNo = installment.installmentNo,
                    remainingPaise = remaining
                )
            }
        }.sortedWith(compareBy<CalendarScheduledDue> { it.dateKey }.thenBy { it.memberName }.thenBy { it.chitNo })
    }

    fun calculateDueBreakdown(db: AppDatabase, memberId: String, groupId: String, asOf: Date = Date()): DueBreakdown {
        val group = db.groupDao().getGroupByIdSync(groupId)
            ?: return DueBreakdown(0, 0, 0, emptyList(), "-", 0, null)
        val installments = db.installmentDao().getInstallmentsForGroupSync(groupId)
        if (installments.isEmpty()) return DueBreakdown(0, 0, 0, emptyList(), "-", 0, null)
        val membership = db.membershipDao().getSync(memberId, groupId)
        val dueCount = installmentsDue(group.startDate, asOf, group.durationMonths).coerceAtMost(installments.size)
        val dueInstallments = installments.take(dueCount)
        fun scheduledAmount(base: Int, kasaru: Int?) = (membership?.installmentAmountPaise?.takeIf { it > 0 }
            ?: (base - (kasaru ?: 0)).toLong()).coerceAtLeast(0L)
        val payments = db.paymentDao().getPaymentsByMemberSync(memberId).filter { it.groupId == groupId }
        val scheduledPayable = dueInstallments.sumOf { scheduledAmount(it.baseAmount, it.kasaruAmount) }
        val totalPaid = payments.sumOf { it.amountPaid }
        var credit = payments.filter { it.installmentId.equals("ADVANCE", true) }.sumOf { it.amountPaid }
        val pendingNumbers = mutableListOf<Int>()
        dueInstallments.forEach { installment ->
            val allocated = payments.filter { it.installmentId == installment.installmentNo.toString() }.sumOf { it.amountPaid }
            var shortfall = (scheduledAmount(installment.baseAmount, installment.kasaruAmount) - allocated).coerceAtLeast(0L)
            val usedCredit = minOf(credit, shortfall)
            credit -= usedCredit
            shortfall -= usedCredit
            if (shortfall > 0) pendingNumbers += installment.installmentNo
        }
        val firstPendingDate = pendingNumbers.firstOrNull()?.let { installmentNo ->
            parseDate(group.startDate)?.let { start -> Calendar.getInstance().apply {
                time = start
                add(Calendar.MONTH, installmentNo - 1)
            }.time }
        }
        val overdueDays = firstPendingDate?.let { due ->
            ((dayStart(asOf).time - dayStart(due).time) / 86_400_000L).coerceAtLeast(0L).toInt()
        } ?: 0
        return DueBreakdown(
            payablePaise = scheduledPayable,
            paidPaise = totalPaid,
            pendingPaise = (scheduledPayable - totalPaid).coerceAtLeast(0L),
            pendingInstallments = pendingNumbers,
            earliestDueDate = firstPendingDate?.let { SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH).format(it) } ?: "-",
            overdueDays = overdueDays,
            lastPaidAt = payments.maxOfOrNull { it.paidAt }
        )
    }

    fun record(
        db: AppDatabase,
        requestId: String,
        memberId: String,
        memberName: String,
        groupId: String,
        amountPaise: Long,
        mode: String,
        referenceNo: String?,
        notes: String,
        businessDate: String = todayKey(),
        collectedBy: String? = null,
        collectedByAgentId: String? = null
    ): SavedCollection {
        require(amountPaise > 0) { "Enter a valid amount" }
        require(mode == "Cash" || !referenceNo.isNullOrBlank()) { "Reference / UTR is required" }

        db.collectionReceiptDao().getByRequestIdSync(requestId)?.let {
            return SavedCollection(it.receiptNo, it.amountPaidPaise, it.mode, it.businessDate, it.referenceNo)
        }

        val group = db.groupDao().getGroupByIdSync(groupId) ?: error("Selected chit was not found")
        require(db.membershipDao().getSync(memberId, groupId)?.isActive == true) { "Customer is not active in this chit" }
        val installments = db.installmentDao().getInstallmentsForGroupSync(groupId)
        require(installments.isNotEmpty()) { "No installment schedule found for this chit" }

        val receiptNo = "JVC-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}-${UUID.randomUUID().toString().take(4).uppercase()}"
        val paidAt = System.currentTimeMillis()
        var saved: SavedCollection? = null

        db.runInTransaction {
            val receipt = CollectionReceiptEntity().apply {
                id = UUID.randomUUID().toString()
                this.requestId = requestId
                this.receiptNo = receiptNo
                this.memberId = memberId
                this.groupId = groupId
                this.amountPaidPaise = amountPaise
                this.mode = mode
                this.referenceNo = referenceNo?.trim()?.ifBlank { null }
                this.notes = notes.trim()
                this.businessDate = businessDate
                this.paidAt = paidAt
                status = "SAVED"
            }
            db.collectionReceiptDao().insert(receipt)

            var remaining = amountPaise
            installments.forEach { installment ->
                if (remaining <= 0) return@forEach
                val membershipAmount = db.membershipDao().getSync(memberId, groupId)?.installmentAmountPaise ?: 0L
                val scheduled = membershipAmount.takeIf { it > 0 }
                    ?: (installment.baseAmount - (installment.kasaruAmount ?: 0)).toLong()
                val alreadyPaid = db.paymentDao()
                    .getPaymentsForInstallmentSync(memberId, groupId, installment.installmentNo.toString())
                    .sumOf { it.amountPaid }
                val pending = (scheduled - alreadyPaid).coerceAtLeast(0L)
                if (pending > 0) {
                    val allocation = minOf(remaining, pending)
                    db.paymentDao().insertPayment(paymentLine(memberId, groupId, installment.installmentNo.toString(), allocation, mode, referenceNo, receiptNo, paidAt, if (allocation == pending) "PAID" else "PARTIAL", collectedBy, collectedByAgentId))
                    remaining -= allocation
                }
            }
            if (remaining > 0) {
                db.paymentDao().insertPayment(paymentLine(memberId, groupId, "ADVANCE", remaining, mode, referenceNo, receiptNo, paidAt, "ADVANCE", collectedBy, collectedByAgentId))
            }
            db.activityLogDao().insertLog(
                ActivityLogEntity(
                    actionType = "PAYMENT_RECORDED",
                    title = "Collection received",
                    description = "$memberName • ${group.registerNo} • ₹${amountPaise / 100}",
                    timestamp = paidAt
                )
            )
            saved = SavedCollection(receiptNo, amountPaise, mode, businessDate, receipt.referenceNo)
        }
        return checkNotNull(saved)
    }

    private fun paymentLine(memberId: String, groupId: String, installmentId: String, amount: Long, mode: String, reference: String?, receipt: String, paidAt: Long, status: String, collectedBy: String? = null, collectedByAgentId: String? = null) =
        PaymentEntity().apply {
            id = UUID.randomUUID().toString()
            this.memberId = memberId
            this.groupId = groupId
            this.installmentId = installmentId
            amountPaid = amount
            this.mode = mode
            referenceNo = reference?.trim()
            receiptNo = receipt
            this.paidAt = paidAt
            this.status = status
            this.collectedBy = collectedBy
            this.collectedByAgentId = collectedByAgentId
        }

    private fun installmentsDue(start: String?, asOf: Date, duration: Int): Int {
        val startDate = parseDate(start) ?: return 1.coerceAtMost(duration)
        if (startDate.after(asOf)) return 0
        val from = Calendar.getInstance().apply { time = startDate }
        val to = Calendar.getInstance().apply { time = asOf }
        return ((to.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * 12 + to.get(Calendar.MONTH) - from.get(Calendar.MONTH) + 1)
            .coerceIn(0, duration)
    }

    private fun parseDate(value: String?): Date? {
        if (value.isNullOrBlank()) return null
        return listOf("dd-MMM-yyyy", "dd MMM yyyy", "yyyy-MM-dd", "dd-MM-yyyy").firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }.parse(value) }.getOrNull()
        }
    }

    private fun dayStart(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    private fun Long?.orZero(): Long = this ?: 0L
}
