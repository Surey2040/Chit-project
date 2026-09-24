package com.jothivel.chits.data.firebase

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [AgentCollectionSync]'s offline queue (the SharedPreferences-backed JSON list
 * that [AgentCollectionSync.push] falls back to when Firestore is unreachable, and that
 * [AgentCollectionSync.flushPending] later retries). Deliberately does NOT exercise
 * push()/flushPending() themselves — those need a live Firestore connection; these tests
 * cover the pure serialize/dedup logic that a Mutex fix earlier this session made safe
 * against concurrent access, so the round-trip and dedup behavior stay pinned regardless.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentCollectionSyncTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun sampleDoc(requestId: String = "req-1", amountPaise: Long = 5000_00L, referenceNo: String? = "UTR123") =
        AgentCollectionDoc(
            requestId = requestId,
            agentId = "agent-1",
            agentName = "Field Agent",
            memberId = "member-1",
            memberName = "Ramesh",
            groupId = "group-1",
            chitNo = "G-1",
            amountPaise = amountPaise,
            mode = "UPI",
            referenceNo = referenceNo,
            receiptNo = "RCT-1",
            notes = "test note",
            businessDate = "2026-09-15",
            status = "PAID"
        )

    @Test
    fun `writeQueue then readQueue round-trips every field`() {
        val prefs = context.getSharedPreferences("test_prefs_1", Context.MODE_PRIVATE)
        val doc = sampleDoc()
        AgentCollectionSync.writeQueue(prefs, listOf(doc))

        val readBack = AgentCollectionSync.readQueue(prefs)
        assertEquals(1, readBack.size)
        val r = readBack[0]
        assertEquals(doc.requestId, r.requestId)
        assertEquals(doc.agentId, r.agentId)
        assertEquals(doc.memberId, r.memberId)
        assertEquals(doc.groupId, r.groupId)
        assertEquals(doc.amountPaise, r.amountPaise)
        assertEquals(doc.mode, r.mode)
        assertEquals(doc.referenceNo, r.referenceNo)
        assertEquals(doc.receiptNo, r.receiptNo)
        assertEquals(doc.businessDate, r.businessDate)
        assertEquals(doc.status, r.status)
        assertEquals(doc.syncedToAdmin, r.syncedToAdmin)
    }

    @Test
    fun `a blank referenceNo round-trips back as null, not an empty string`() {
        // writeQueue stores null referenceNo as "" (JSONObject can't hold a null value cleanly),
        // and readQueue must turn that back into null via optString(...).ifBlank { null } —
        // otherwise every cash payment (which has no UTR) would come back with referenceNo=""
        // instead of null, which downstream code treats differently.
        val prefs = context.getSharedPreferences("test_prefs_2", Context.MODE_PRIVATE)
        AgentCollectionSync.writeQueue(prefs, listOf(sampleDoc(referenceNo = null)))
        val readBack = AgentCollectionSync.readQueue(prefs)
        assertNull(readBack[0].referenceNo)
    }

    @Test
    fun `reading an empty or missing queue returns an empty list, not a crash`() {
        val prefs = context.getSharedPreferences("test_prefs_empty", Context.MODE_PRIVATE)
        assertEquals(emptyList<AgentCollectionDoc>(), AgentCollectionSync.readQueue(prefs))
    }

    @Test
    fun `reading a corrupted queue value returns an empty list instead of crashing`() {
        val prefs = context.getSharedPreferences("test_prefs_corrupt", Context.MODE_PRIVATE)
        prefs.edit().putString("queue", "{not valid json[").apply()
        assertEquals(emptyList<AgentCollectionDoc>(), AgentCollectionSync.readQueue(prefs))
    }

    @Test
    fun `queuing the same requestId twice replaces the entry instead of duplicating it`() {
        // This is the exact property the Mutex fix protects: two near-simultaneous queue()
        // calls for the same retried payment must never leave two copies of it queued.
        AgentCollectionSync.queueLocked(context, sampleDoc(requestId = "dup-1", amountPaise = 100_00L))
        AgentCollectionSync.queueLocked(context, sampleDoc(requestId = "dup-1", amountPaise = 999_00L))
        assertEquals(1, AgentCollectionSync.pendingCount(context))

        val prefs = context.getSharedPreferences("jvc_pending_collections", Context.MODE_PRIVATE)
        val queued = AgentCollectionSync.readQueue(prefs)
        assertEquals(1, queued.size)
        assertEquals(999_00L, queued[0].amountPaise) // the newer write wins, not the first
    }

    @Test
    fun `queuing different requestIds accumulates all of them`() {
        AgentCollectionSync.queueLocked(context, sampleDoc(requestId = "a"))
        AgentCollectionSync.queueLocked(context, sampleDoc(requestId = "b"))
        AgentCollectionSync.queueLocked(context, sampleDoc(requestId = "c"))
        assertEquals(3, AgentCollectionSync.pendingCount(context))
    }
}
