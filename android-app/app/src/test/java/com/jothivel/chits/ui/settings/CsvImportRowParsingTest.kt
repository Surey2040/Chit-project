package com.jothivel.chits.ui.settings

import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Plain-JUnit tests (no Robolectric needed — this logic touches no Android APIs) for the
 * CSV/XLSX import row parser [rowToMemberAndGroup] and [parseImportDate]. Pins the exact
 * "Member Name, Phone, Chit Group, Installment No, Base Amount, Amount Due, Amount Paid,
 * Payment Status, Payment Mode, Due Date" column contract the app's import screen documents,
 * so a future refactor can't silently change what a spreadsheet row is allowed to look like.
 */
class CsvImportRowParsingTest {

    private lateinit var members: MutableList<MemberEntity>
    private lateinit var groupMap: MutableMap<String, ChitGroupEntity>
    private lateinit var paymentRows: MutableList<ImportPaymentRow>
    private lateinit var seenPhones: MutableSet<String>
    private lateinit var errors: MutableList<String>

    @Before
    fun setUp() {
        members = mutableListOf()
        groupMap = mutableMapOf()
        paymentRows = mutableListOf()
        seenPhones = mutableSetOf()
        errors = mutableListOf()
    }

    private fun parseRow(
        name: String = "Ramesh",
        phone: String = "9876543210",
        group: String = "5L/20M",
        installmentNo: String = "3",
        baseAmount: String = "25000",
        amountDue: String = "22000",
        amountPaid: String = "22000",
        status: String = "PAID",
        mode: String = "CASH",
        dueDate: String = "2026-09-15"
    ): Int = rowToMemberAndGroup(
        name, phone, group, installmentNo, baseAmount, amountDue, amountPaid, status, mode, dueDate,
        members, groupMap, paymentRows, seenPhones, errors, "Row 1"
    )

    @Test
    fun `a valid row creates one member, one group, one payment row`() {
        parseRow()
        assertEquals(1, members.size)
        assertEquals("Ramesh", members[0].name)
        assertEquals("9876543210", members[0].phone)
        assertEquals(1, groupMap.size)
        assertEquals(1, paymentRows.size)
        assertEquals("PAID", paymentRows[0].paymentStatus)
    }

    @Test
    fun `blank name or phone skips the row and records an error`() {
        val skipped = parseRow(name = "")
        assertEquals(1, skipped)
        assertTrue(members.isEmpty())
        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun `the same phone on two rows only creates one member`() {
        parseRow(phone = "9876543210", installmentNo = "1")
        parseRow(phone = "9876543210", installmentNo = "2", name = "Ramesh Duplicate")
        assertEquals(1, members.size) // second row's name is ignored, not a new member
        assertEquals(2, paymentRows.size) // but both installment rows are still recorded
    }

    @Test
    fun `group duration is parsed from the NM pattern in the group name`() {
        parseRow(group = "10L/24M")
        assertEquals(24, groupMap["10L/24M"]!!.durationMonths)
    }

    @Test
    fun `group name without a duration pattern defaults to 20 months`() {
        parseRow(group = "MyChit")
        assertEquals(20, groupMap["MyChit"]!!.durationMonths)
    }

    @Test
    fun `amounts are converted from rupees to paise`() {
        parseRow(baseAmount = "25000", amountPaid = "22000")
        assertEquals(25000L * 100, paymentRows[0].baseAmountPaise)
        assertEquals(22000L * 100, paymentRows[0].amountPaidPaise)
    }

    @Test
    fun `an unrecognized payment status falls back to DUE and logs a warning`() {
        parseRow(status = "WEIRD_STATUS")
        assertEquals("DUE", paymentRows[0].paymentStatus)
        assertTrue(errors.any { it.contains("unknown Payment Status") })
    }

    @Test
    fun `payment mode variants all normalize to BANK_TRANSFER`() {
        listOf("BANK_TRANSFER", "BANK TRANSFER", "BANK", "NEFT", "IMPS", "RTGS").forEach { raw ->
            setUp()
            parseRow(mode = raw)
            assertEquals("Mode '$raw' should normalize to BANK_TRANSFER", "BANK_TRANSFER", paymentRows[0].paymentMode)
        }
    }

    @Test
    fun `an unrecognized payment mode defaults to CASH`() {
        parseRow(mode = "cheque")
        assertEquals("CASH", paymentRows[0].paymentMode)
    }

    @Test
    fun `zero or missing installment number skips payment data but still creates the member`() {
        parseRow(installmentNo = "0")
        assertEquals(1, members.size)
        assertTrue(paymentRows.isEmpty())
        assertTrue(errors.any { it.contains("invalid or missing Installment No") })

        setUp()
        parseRow(installmentNo = "not-a-number")
        assertTrue(paymentRows.isEmpty())
    }

    @Test
    fun `a blank group name skips group and payment creation but still creates the member`() {
        parseRow(group = "")
        assertEquals(1, members.size)
        assertTrue(groupMap.isEmpty())
        assertTrue(paymentRows.isEmpty())
    }

    // ─── parseImportDate ──────────────────────────────────────────────────────

    @Test
    fun `parseImportDate accepts every documented date format`() {
        assertTrue(parseImportDate("15-Sep-2026") != null)
        assertTrue(parseImportDate("15 Sep 2026") != null)
        assertTrue(parseImportDate("2026-09-15") != null)
        assertTrue(parseImportDate("15-09-2026") != null)
        assertTrue(parseImportDate("15/09/2026") != null)
    }

    @Test
    fun `parseImportDate returns null for blank or garbage input`() {
        assertNull(parseImportDate(""))
        assertNull(parseImportDate("not a date"))
    }
}
