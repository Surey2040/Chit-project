package com.jothivel.chits.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.ChitMembershipEntity
import com.jothivel.chits.data.local.entity.InstallmentEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Regression tests for the core due/paid/pending money math in [CollectionService].
 * Runs against a real in-memory Room database (via Robolectric) rather than a fake, so
 * these exercise the exact same DAOs/queries the app uses at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionServiceTest {

    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)

    // Group starts 01-Jan-2026; all "asOf" dates below are chosen relative to this so the
    // number of installments considered "due by now" is explicit and easy to reason about.
    private val groupStart = "01-Jan-2026"
    private val allThreeDueAsOf = dateFormat.parse("15-Mar-2026")!! // 3 months in -> all 3 installments due
    private val onlyFirstDueAsOf = dateFormat.parse("15-Jan-2026")!! // 2 weeks in -> only installment 1 due

    // NOTE: deliberately NOT named groupId/memberId — inside `SomeJavaEntity().apply { ... }`,
    // an unqualified reference on the right-hand side resolves to the entity's OWN field of the
    // same name (the apply-lambda receiver) before it resolves to an outer-scope property, so
    // `this.groupId = groupId` would silently self-assign null instead of reading this value.
    private lateinit var mainGroupId: String
    private lateinit var mainMemberId: String

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        mainGroupId = UUID.randomUUID().toString()
        mainMemberId = UUID.randomUUID().toString()

        db.groupDao().insertAll(listOf(ChitGroupEntity().apply {
            id = mainGroupId
            name = "Test Chit"
            registerNo = "TEST-G1"
            chitValue = 300_00 // 300 rupees in paise, 3 installments of 100 each
            durationMonths = 3
            subscriberCount = 1
            branch = "Main"
            startDate = groupStart
            status = "ACTIVE"
        }))

        db.installmentDao().insertAll((1..3).map { no ->
            InstallmentEntity().apply {
                id = UUID.randomUUID().toString()
                groupId = mainGroupId
                installmentNo = no
                baseAmount = 100_00 // 100 rupees per installment
                kasaruAmount = null
                status = "UPCOMING"
            }
        })

        db.memberDao().insertAll(listOf(MemberEntity().apply {
            id = mainMemberId
            name = "Test Member"
            phone = "9000000000"
            role = "MEMBER"
            isActive = true
        }))

        db.membershipDao().upsertAll(listOf(ChitMembershipEntity().apply {
            id = UUID.randomUUID().toString()
            memberId = mainMemberId
            groupId = mainGroupId
            ticketNo = "1"
            installmentAmountPaise = 0L // falls back to baseAmount - kasaru
            isActive = true
        }))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `no payments means everything due is pending`() {
        val breakdown = CollectionService.calculateDueBreakdown(db, mainMemberId, mainGroupId, allThreeDueAsOf)
        assertEquals(300_00L, breakdown.payablePaise)
        assertEquals(0L, breakdown.paidPaise)
        assertEquals(300_00L, breakdown.pendingPaise)
        assertEquals(listOf(1, 2, 3), breakdown.pendingInstallments)
    }

    @Test
    fun `only installments due so far count toward payable`() {
        val breakdown = CollectionService.calculateDueBreakdown(db, mainMemberId, mainGroupId, onlyFirstDueAsOf)
        assertEquals(100_00L, breakdown.payablePaise)
        assertEquals(listOf(1), breakdown.pendingInstallments)
    }

    @Test
    fun `recording a full payment for installment 1 clears it from pending`() {
        CollectionService.record(
            db, requestId = UUID.randomUUID().toString(), memberId = mainMemberId, memberName = "Test Member",
            groupId = mainGroupId, amountPaise = 100_00, mode = "Cash", referenceNo = null, notes = "",
            businessDate = "2026-01-15"
        )
        val breakdown = CollectionService.calculateDueBreakdown(db, mainMemberId, mainGroupId, allThreeDueAsOf)
        assertEquals(300_00L, breakdown.payablePaise)
        assertEquals(100_00L, breakdown.paidPaise)
        assertEquals(200_00L, breakdown.pendingPaise)
        assertEquals(listOf(2, 3), breakdown.pendingInstallments)
    }

    @Test
    fun `a payment larger than one installment overflows into the next as advance credit`() {
        // Pay 150 in one go: should fully cover installment 1 (100) and half of installment 2 (50).
        CollectionService.record(
            db, requestId = UUID.randomUUID().toString(), memberId = mainMemberId, memberName = "Test Member",
            groupId = mainGroupId, amountPaise = 150_00, mode = "Cash", referenceNo = null, notes = "",
            businessDate = "2026-01-15"
        )
        val breakdown = CollectionService.calculateDueBreakdown(db, mainMemberId, mainGroupId, allThreeDueAsOf)
        assertEquals(150_00L, breakdown.paidPaise)
        assertEquals(150_00L, breakdown.pendingPaise)
        // Installment 1 fully paid, installment 2 half-paid (still pending), installment 3 untouched.
        assertEquals(listOf(2, 3), breakdown.pendingInstallments)
    }

    @Test
    fun `recording twice with the same requestId does not double-charge`() {
        val requestId = UUID.randomUUID().toString()
        val first = CollectionService.record(
            db, requestId = requestId, memberId = mainMemberId, memberName = "Test Member",
            groupId = mainGroupId, amountPaise = 100_00, mode = "Cash", referenceNo = null, notes = "",
            businessDate = "2026-01-15"
        )
        val second = CollectionService.record(
            db, requestId = requestId, memberId = mainMemberId, memberName = "Test Member",
            groupId = mainGroupId, amountPaise = 100_00, mode = "Cash", referenceNo = null, notes = "",
            businessDate = "2026-01-15"
        )
        assertEquals(first.receiptNo, second.receiptNo)
        val breakdown = CollectionService.calculateDueBreakdown(db, mainMemberId, mainGroupId, allThreeDueAsOf)
        assertEquals(100_00L, breakdown.paidPaise) // not 200 — the retry must not double-charge
    }

    @Test
    fun `a payment in an unrelated group never counts toward this group's dues`() {
        // Regression guard for the "payments summed across ALL of a member's groups" bug
        // found and fixed earlier (LedgerViewModel/ApprovedAppFlow/LocalAssistantEngine were
        // calling the unscoped payment query). CollectionService already scopes correctly
        // (line: payments.filter { it.groupId == groupId }) — this test pins that behavior.
        val otherGroupId = UUID.randomUUID().toString()
        db.groupDao().insertAll(listOf(ChitGroupEntity().apply {
            id = otherGroupId
            name = "Other Chit"
            registerNo = "TEST-G2"
            chitValue = 500_00
            durationMonths = 1
            subscriberCount = 1
            branch = "Main"
            startDate = groupStart
            status = "ACTIVE"
        }))
        db.installmentDao().insertAll(listOf(InstallmentEntity().apply {
            id = UUID.randomUUID().toString()
            groupId = otherGroupId
            installmentNo = 1
            baseAmount = 500_00
            status = "UPCOMING"
        }))
        db.membershipDao().upsertAll(listOf(ChitMembershipEntity().apply {
            id = UUID.randomUUID().toString()
            memberId = mainMemberId
            groupId = otherGroupId
            ticketNo = "1"
            installmentAmountPaise = 0L
            isActive = true
        }))
        CollectionService.record(
            db, requestId = UUID.randomUUID().toString(), memberId = mainMemberId, memberName = "Test Member",
            groupId = otherGroupId, amountPaise = 500_00, mode = "Cash", referenceNo = null, notes = "",
            businessDate = "2026-01-15"
        )

        val breakdown = CollectionService.calculateDueBreakdown(db, mainMemberId, mainGroupId, allThreeDueAsOf)
        assertEquals(0L, breakdown.paidPaise) // the other group's payment must not leak in here
        assertEquals(300_00L, breakdown.pendingPaise)
    }

    @Test
    fun `PaymentDao group-scoped query only returns that group's payments`() {
        // Direct regression test for PaymentDao.getPaymentsByMemberAndGroupSync, added when
        // fixing the cross-group payment-leak bug in LedgerViewModel/ApprovedAppFlow.
        CollectionService.record(
            db, requestId = UUID.randomUUID().toString(), memberId = mainMemberId, memberName = "Test Member",
            groupId = mainGroupId, amountPaise = 100_00, mode = "Cash", referenceNo = null, notes = "",
            businessDate = "2026-01-15"
        )
        val otherGroupId = UUID.randomUUID().toString()
        db.groupDao().insertAll(listOf(ChitGroupEntity().apply {
            id = otherGroupId; name = "Other"; registerNo = "G2"; chitValue = 100_00; durationMonths = 1
            subscriberCount = 1; branch = "Main"; startDate = groupStart; status = "ACTIVE"
        }))
        db.installmentDao().insertAll(listOf(InstallmentEntity().apply {
            id = UUID.randomUUID().toString(); groupId = otherGroupId; installmentNo = 1; baseAmount = 100_00; status = "UPCOMING"
        }))
        db.membershipDao().upsertAll(listOf(ChitMembershipEntity().apply {
            id = UUID.randomUUID().toString(); memberId = mainMemberId; groupId = otherGroupId
            ticketNo = "1"; installmentAmountPaise = 0L; isActive = true
        }))
        CollectionService.record(
            db, requestId = UUID.randomUUID().toString(), memberId = mainMemberId, memberName = "Test Member",
            groupId = otherGroupId, amountPaise = 100_00, mode = "Cash", referenceNo = null, notes = "",
            businessDate = "2026-01-15"
        )

        val scoped = db.paymentDao().getPaymentsByMemberAndGroupSync(mainMemberId, mainGroupId)
        assertTrue(scoped.isNotEmpty())
        assertTrue(scoped.all { it.groupId == mainGroupId })
        assertFalse(scoped.any { it.groupId == otherGroupId })
    }
}
