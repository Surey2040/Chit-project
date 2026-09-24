package com.jothivel.chits.ui.labour

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.data.firebase.AgentCollectionSync
import com.jothivel.chits.data.firebase.AgentDataSync
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.CollectionService
import com.jothivel.chits.data.local.DueBreakdown
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.data.local.entity.PaymentEntity
import com.jothivel.chits.ui.theme.AccentGold
import com.jothivel.chits.ui.theme.AccentGreen
import com.jothivel.chits.ui.theme.AccentOrange
import com.jothivel.chits.ui.theme.AccentRed
import com.jothivel.chits.ui.theme.DividerGray
import com.jothivel.chits.ui.theme.MaroonBackground
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.MaroonSurfaceLight
import com.jothivel.chits.ui.theme.TextGray
import com.jothivel.chits.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =============================================================================
// Data holders
// =============================================================================

private data class AgentChitCard(
    val group: ChitGroupEntity,
    val memberCount: Int,
    val pendingThisMonth: Int,
    val collectedThisMonth: Int,
    val syncFailed: Boolean = false
)

private data class AgentMemberDue(
    val member: MemberEntity,
    val ticketNo: String,
    val due: DueBreakdown,
    val collectedThisMonth: Boolean,
    val collectedAmountThisMonth: Long
)

private enum class MemberFilter { ALL, PENDING, COLLECTED }

// =============================================================================
// My Chits Screen (Labour home)
// =============================================================================

@Composable
fun AgentMyChitsScreen(onOpenGroup: (groupId: String, label: String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var syncText by remember { mutableStateOf("Ready") }
    var syncing by remember { mutableStateOf(false) }

    suspend fun refreshData() {
        syncing = true
        syncText = "Syncing all chits..."
        // Flush pending collected data first (don't lose anything)
        val flushed = withContext(Dispatchers.IO) { AgentCollectionSync.flushPending(context) }
        // Then pull latest assigned group data from cloud
        val pulled = withContext(Dispatchers.IO) { AgentDataSync.syncAssignedGroups(context) }
        syncText = pulled.fold(
            onSuccess = { count ->
                buildString {
                    append("Synced $count chit${if (count != 1) "s" else ""}")
                    if (flushed > 0) append(" \u2022 $flushed pending sent")
                }
            },
            onFailure = {
                val pending = AgentCollectionSync.pendingCount(context)
                "Offline mode${if (pending > 0) " \u2022 $pending pending sync" else ""}"
            }
        )
        syncing = false
        refreshKey++
    }

    LaunchedEffect(Unit) { refreshData() }

    val snapshot by produceState(initialValue = emptyList<AgentChitCard>(), refreshKey) {
        value = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val assigned = prefs.getAgentAssignedGroups().toSet()
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
            val allPayments = db.paymentDao().getAllPaymentsSync()

            db.groupDao().getAllGroupsSync()
                .filter { it.id in assigned }
                .map { group ->
                    val memberships = db.membershipDao().getActiveForGroupSync(group.id)
                    val pending = memberships.count {
                        CollectionService.calculateDueBreakdown(db, it.memberId, group.id).pendingPaise > 0
                    }
                    val collectedThisMonth = memberships.count { ms ->
                        allPayments.any { p ->
                            p.memberId == ms.memberId && p.groupId == group.id &&
                            SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date(p.paidAt)) == monthKey
                        }
                    }
                    AgentChitCard(group, memberships.size, pending, collectedThisMonth)
                }
                .sortedByDescending { it.pendingThisMonth }
        }
    }

    val todaysPayments by produceState(initialValue = emptyList<PaymentEntity>(), refreshKey) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).paymentDao().getAllPaymentsSync()
                .filter { it.collectedByAgentId == prefs.getAgentId() && sameDay(it.paidAt) }
        }
    }
    val todayTotal = todaysPayments.sumOf { it.amountPaid }
    val todayCount = todaysPayments.mapNotNull { it.receiptNo }.distinct().size
    val pendingSync = AgentCollectionSync.pendingCount(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaroonBackground),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            AgentWelcomeCard(
                name = prefs.getAgentName().ifBlank { "Labour" },
                syncText = syncText,
                syncing = syncing,
                pendingSync = pendingSync,
                onRefresh = { scope.launch { refreshData() } }
            )
        }
        item {
            Button(
                onClick = { scope.launch { refreshData() } },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !syncing
            ) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (syncing) "Syncing..." else "Sync Data from Cloud")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgentStat("Today", money(todayTotal / 100), "$todayCount receipts", AccentGreen, Modifier.weight(1f))
                AgentStat(
                    "Pending Sync",
                    pendingSync.toString(),
                    if (pendingSync > 0) "will retry" else "all synced",
                    if (pendingSync > 0) AccentRed else AccentGreen,
                    Modifier.weight(1f)
                )
            }
        }
        if (snapshot.isEmpty()) {
            item { EmptyAgentState("No assigned chits yet", "Ask admin to assign chit groups and sync data.") }
        } else {
            items(snapshot, key = { it.group.id }) { item ->
                AgentChitRow(item) {
                    onOpenGroup(
                        item.group.id,
                        "${item.group.registerNo ?: item.group.id} \u2022 ${item.group.name ?: "Chit"}"
                    )
                }
            }
        }
    }
}

// =============================================================================
// Member List Screen (inside a chit) — with filter tabs
// =============================================================================

@Composable
fun AgentMemberListScreen(groupId: String, onCollect: (memberName: String, chitLabel: String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var activeFilter by remember { mutableStateOf(MemberFilter.PENDING) }

    val data by produceState(initialValue = Pair<ChitGroupEntity?, List<AgentMemberDue>>(null, emptyList()), groupId) {
        value = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val group = db.groupDao().getGroupByIdSync(groupId)
            val allPayments = db.paymentDao().getAllPaymentsSync()
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())

            val rows = db.membershipDao().getActiveForGroupSync(groupId).mapNotNull { membership ->
                val member = db.memberDao().getMemberByIdSync(membership.memberId) ?: return@mapNotNull null
                val due = CollectionService.calculateDueBreakdown(db, member.id, groupId)
                val paymentsThisMonth = allPayments.filter { p ->
                    p.memberId == membership.memberId && p.groupId == groupId &&
                    SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date(p.paidAt)) == monthKey
                }
                AgentMemberDue(
                    member = member,
                    ticketNo = membership.ticketNo.orEmpty(),
                    due = due,
                    collectedThisMonth = paymentsThisMonth.isNotEmpty(),
                    collectedAmountThisMonth = paymentsThisMonth.sumOf { it.amountPaid }
                )
            }.sortedWith(
                compareByDescending<AgentMemberDue> { it.due.pendingPaise > 0 }
                    .thenByDescending { it.due.overdueDays }
                    .thenBy { it.member.name ?: "" }
            )
            group to rows
        }
    }
    val group = data.first
    val allRows = data.second
    val pendingCount = allRows.count { it.due.pendingPaise > 0 }
    val collectedCount = allRows.count { it.collectedThisMonth }
    val totalCount = allRows.size

    val filteredRows = when (activeFilter) {
        MemberFilter.ALL -> allRows
        MemberFilter.PENDING -> allRows.filter { it.due.pendingPaise > 0 }
        MemberFilter.COLLECTED -> allRows.filter { it.collectedThisMonth }
    }

    Column(Modifier.fillMaxSize().background(MaroonBackground)) {
        // Header summary card
        group?.let { g ->
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${g.registerNo} \u2022 ${g.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Collection fraction pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (pendingCount == 0) AccentGreen.copy(.12f) else AccentRed.copy(.10f)
                        ) {
                            Text(
                                "$collectedCount/$totalCount collected",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingCount == 0) AccentGreen else AccentRed,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Progress bar
                    val fraction = if (totalCount > 0) collectedCount.toFloat() / totalCount else 0f
                    LinearProgressIndicator(
                        progress = fraction,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (fraction >= 1f) AccentGreen else MaroonPrimary,
                        trackColor = MaroonSurfaceLight
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${money(g.chitValue.toLong() / 100)} \u2022 ${g.durationMonths}m \u2022 ${g.branch ?: "-"}", color = TextGray, fontSize = 9.sp)
                        if (pendingCount > 0)
                            Text("$pendingCount pending", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        else
                            Text("\u2705 All collected", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Filter tabs
        FilterTabRow(activeFilter, pendingCount, collectedCount, totalCount) { activeFilter = it }

        // Member list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredRows.isEmpty()) {
                item {
                    EmptyAgentState(
                        when (activeFilter) {
                            MemberFilter.PENDING -> "No pending members \uD83C\uDF89"
                            MemberFilter.COLLECTED -> "No collected members yet"
                            MemberFilter.ALL -> "No members in this chit"
                        },
                        when (activeFilter) {
                            MemberFilter.PENDING -> "All members have paid this month!"
                            MemberFilter.COLLECTED -> "Collect from members to see them here."
                            MemberFilter.ALL -> "Sync data from admin or add members to this chit."
                        }
                    )
                }
            } else {
                items(filteredRows, key = { it.member.id }) { row ->
                    AgentMemberRow(row, group, onCollect)
                }
            }
        }
    }
}

// =============================================================================
// Today Summary Screen
// =============================================================================

@Composable
fun AgentTodaySummaryScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { AgentCollectionSync.flushPending(context) }
        refreshKey++
    }

    val payments by produceState(initialValue = emptyList<PaymentEntity>(), refreshKey) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).paymentDao().getAllPaymentsSync()
                .filter { it.collectedByAgentId == prefs.getAgentId() && sameDay(it.paidAt) }
                .sortedByDescending { it.paidAt }
        }
    }
    val byReceipt = payments.groupBy { it.receiptNo ?: it.id }.values.map { rows -> rows.maxBy { it.paidAt } to rows.sumOf { it.amountPaid } }
    val cash = payments.filter { it.mode.equals("Cash", ignoreCase = true) }.sumOf { it.amountPaid }
    val upi = payments.filter { it.mode.equals("UPI", ignoreCase = true) }.sumOf { it.amountPaid }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaroonBackground),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgentStat("Collected", money((cash + upi) / 100), "${byReceipt.size} receipts", AccentGreen, Modifier.weight(1f))
                AgentStat("Pending Sync", AgentCollectionSync.pendingCount(context).toString(), "will retry", AccentGold, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgentStat("Cash", money(cash / 100), "today", MaroonPrimary, Modifier.weight(1f))
                AgentStat("UPI", money(upi / 100), "today", Color(0xFF285A9B), Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = { scope.launch { withContext(Dispatchers.IO) { AgentCollectionSync.flushPending(context) }; refreshKey++ } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Retry pending sync")
            }
        }
        if (byReceipt.isEmpty()) {
            item { EmptyAgentState("No collections today", "Saved collections will appear here immediately.") }
        } else {
            items(byReceipt, key = { it.first.receiptNo ?: it.first.id }) { (payment, total) ->
                TodayPaymentRow(payment, total)
            }
        }
    }
}

// =============================================================================
// Sub-composables
// =============================================================================

@Composable
private fun FilterTabRow(
    active: MemberFilter,
    pendingCount: Int,
    collectedCount: Int,
    totalCount: Int,
    onSelect: (MemberFilter) -> Unit
) {
    Surface(color = Color.White, shadowElevation = 0.5.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTab(
                label = "Pending",
                count = pendingCount,
                selected = active == MemberFilter.PENDING,
                selectedColor = AccentRed,
                modifier = Modifier.weight(1f)
            ) { onSelect(MemberFilter.PENDING) }
            FilterTab(
                label = "All",
                count = totalCount,
                selected = active == MemberFilter.ALL,
                selectedColor = MaroonPrimary,
                modifier = Modifier.weight(1f)
            ) { onSelect(MemberFilter.ALL) }
            FilterTab(
                label = "Collected",
                count = collectedCount,
                selected = active == MemberFilter.COLLECTED,
                selectedColor = AccentGreen,
                modifier = Modifier.weight(1f)
            ) { onSelect(MemberFilter.COLLECTED) }
        }
    }
}

@Composable
private fun FilterTab(
    label: String,
    count: Int,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) selectedColor.copy(.12f) else Color.Transparent,
        animationSpec = tween(160),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) selectedColor else TextGray,
        animationSpec = tween(160),
        label = "tabText"
    )
    Surface(
        modifier = modifier.height(34.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = if (selected) BorderStroke(1.dp, selectedColor.copy(.30f)) else BorderStroke(1.dp, DividerGray)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = textColor, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            Spacer(Modifier.width(4.dp))
            Surface(shape = CircleShape, color = textColor.copy(.15f)) {
                Text(
                    count.toString(),
                    color = textColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AgentWelcomeCard(
    name: String,
    syncText: String,
    syncing: Boolean,
    pendingSync: Int,
    onRefresh: () -> Unit
) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountCircle, null, tint = MaroonPrimary)
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("Vanakkam, $name", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH).format(Date()), color = TextGray, fontSize = 10.sp)
                    Text(syncText, color = if (syncText.startsWith("Offline")) AccentOrange else TextGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onRefresh, enabled = !syncing) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = if (syncing) TextGray else MaroonPrimary)
                }
            }
            // Offline pending warning banner
            if (pendingSync > 0) {
                Divider(color = DividerGray, thickness = 0.5.dp)
                Row(
                    Modifier.fillMaxWidth().background(AccentOrange.copy(.08f)).padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = AccentOrange, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$pendingSync collection${if (pendingSync > 1) "s" else ""} waiting to sync to cloud",
                        color = AccentOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentStat(title: String, value: String, subtitle: String, color: Color, modifier: Modifier) {
    Surface(modifier.height(76.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation = 1.dp) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title.uppercase(Locale.ENGLISH), color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = TextGray, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AgentChitRow(item: AgentChitCard, onClick: () -> Unit) {
    val group = item.group
    val allCollected = item.pendingThisMonth == 0 && item.memberCount > 0
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (item.pendingThisMonth > 0) AccentRed.copy(.18f) else DividerGray),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Savings, null, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        "${group.registerNo ?: group.id} \u2022 ${group.name ?: "Chit"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${money(group.chitValue.toLong() / 100)} \u2022 ${group.durationMonths}m \u2022 ${group.branch ?: "-"}",
                        color = TextGray,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                // Status badge
                if (allCollected) {
                    Surface(shape = RoundedCornerShape(20.dp), color = AccentGreen.copy(.12f)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("All done", fontSize = 9.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (item.pendingThisMonth > 0) {
                    Surface(shape = RoundedCornerShape(20.dp), color = AccentRed.copy(.10f)) {
                        Text(
                            "${item.pendingThisMonth} PENDING",
                            fontSize = 9.sp,
                            color = AccentRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            // Mini progress bar
            if (item.memberCount > 0) {
                val fraction = item.collectedThisMonth.toFloat() / item.memberCount
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(4.dp).background(DividerGray, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(fraction).height(4.dp).background(
                        if (fraction >= 1f) AccentGreen else MaroonPrimary, RoundedCornerShape(2.dp)))
                }
                Text(
                    "${item.collectedThisMonth}/${item.memberCount} collected this month",
                    fontSize = 9.sp,
                    color = TextGray,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AgentMemberRow(row: AgentMemberDue, group: ChitGroupEntity?, onCollect: (String, String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val phone = row.member.phone.orEmpty().filter(Char::isDigit).takeLast(10)
    val isPending = row.due.pendingPaise > 0
    val isOverdue = row.due.overdueDays > 0
    val statusColor = when {
        !isPending -> AccentGreen
        isOverdue -> AccentRed
        else -> AccentOrange
    }
    val statusText = when {
        !isPending -> "Paid"
        isOverdue -> "Overdue ${row.due.overdueDays}d"
        else -> "Pending"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isOverdue) AccentRed.copy(.25f) else DividerGray),
        shadowElevation = if (isPending) 2.dp else 1.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(statusColor.copy(.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(row.member.name.orEmpty().take(1).ifBlank { "?" }, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(Modifier.padding(start = 9.dp).weight(1f)) {
                    Text(row.member.name ?: row.member.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(row.member.phone.orEmpty(), color = TextGray, fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(.10f)) {
                    Text(statusText, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            // Amount row
            if (row.collectedThisMonth) {
                // Collected this month
                Surface(shape = RoundedCornerShape(10.dp), color = AccentGreen.copy(.07f)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Collected", tint = AccentGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Collected this month", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(money(row.collectedAmountThisMonth / 100), color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else if (isPending) {
                // Pending amount
                Surface(shape = RoundedCornerShape(10.dp), color = AccentRed.copy(.07f)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PendingActions, "Pending", tint = AccentRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Due now", color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            if (row.due.pendingInstallments.isNotEmpty())
                                Text(row.due.pendingInstallments.joinToString { "${it}th" }, color = TextGray, fontSize = 9.sp)
                        }
                        Text(money(row.due.pendingPaise / 100), color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("Call", Icons.Default.Call, MaroonPrimary, Modifier.weight(1f)) {
                    if (phone.isNotBlank()) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }
                SmallAction("WhatsApp", Icons.Default.Send, AccentGreen, Modifier.weight(1f)) {
                    if (phone.isNotBlank()) {
                        val text = "Vanakkam ${row.member.name}, ${group?.registerNo ?: "chit"} pending ${money(row.due.pendingPaise / 100)}. Kindly make the payment. - Jothi Vel Chits"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$phone?text=${Uri.encode(text)}")))
                    }
                }
                SmallAction(
                    if (isPending) "Collect" else "Collect Again",
                    Icons.Default.PendingActions,
                    if (isPending) MaroonPrimary else TextGray,
                    Modifier.weight(1f)
                ) {
                    onCollect(row.member.name ?: row.member.id, group?.registerNo ?: group?.id ?: "")
                }
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.height(36.dp).clickable(onClick = onClick), shape = RoundedCornerShape(11.dp), color = tint.copy(alpha = .08f), border = BorderStroke(1.dp, tint.copy(alpha = .12f))) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TodayPaymentRow(payment: PaymentEntity, total: Long) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).background(AccentGreen.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.padding(start = 9.dp).weight(1f)) {
                Text(payment.receiptNo ?: payment.id, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
                Text("${payment.mode} \u2022 ${SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(payment.paidAt))}", color = TextGray, fontSize = 9.sp)
            }
            Text(money(total / 100), color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyAgentState(title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Group, null, tint = MaroonPrimary.copy(alpha = .7f))
            Spacer(Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = TextGray, fontSize = 10.sp)
        }
    }
}

private fun sameDay(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val other = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun money(value: Long): String = "Rs " + NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
