package com.jothivel.chits.ui.labour

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private data class AgentChitCard(
    val group: ChitGroupEntity,
    val memberCount: Int,
    val pendingThisMonth: Int
)

private data class AgentMemberDue(
    val member: MemberEntity,
    val ticketNo: String,
    val due: DueBreakdown
)

@Composable
fun AgentMyChitsScreen(onOpenGroup: (groupId: String, label: String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var syncText by remember { mutableStateOf("Ready") }

    suspend fun refreshData() {
        syncText = "Refreshing..."
        val pulled = withContext(Dispatchers.IO) { AgentDataSync.syncAssignedGroups(context) }
        val flushed = withContext(Dispatchers.IO) { AgentCollectionSync.flushPending(context) }
        syncText = pulled.fold(
            onSuccess = { "Synced $it chits" + if (flushed > 0) " • sent $flushed pending" else "" },
            onFailure = { "Offline mode • ${AgentCollectionSync.pendingCount(context)} pending sync" }
        )
        refreshKey++
    }

    LaunchedEffect(Unit) { refreshData() }

    val snapshot by produceState(initialValue = emptyList<AgentChitCard>(), refreshKey) {
        value = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val assigned = prefs.getAgentAssignedGroups().toSet()
            db.groupDao().getAllGroupsSync()
                .filter { assigned.isEmpty() || it.id in assigned }
                .map { group ->
                    val memberships = db.membershipDao().getActiveForGroupSync(group.id)
                    val pending = memberships.count { CollectionService.calculateDueBreakdown(db, it.memberId, group.id).pendingPaise > 0 }
                    AgentChitCard(group, memberships.size, pending)
                }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaroonBackground),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            AgentWelcomeCard(
                name = prefs.getAgentName().ifBlank { "Labour" },
                syncText = syncText,
                onRefresh = { scope.launch { refreshData() } }
            )
        }
        item {
            Button(
                onClick = { scope.launch { refreshData() } },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Sync Data from Cloud")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgentStat("Today", money(todayTotal / 100), "$todayCount receipts", AccentGreen, Modifier.weight(1f))
                AgentStat("Pending Sync", AgentCollectionSync.pendingCount(context).toString(), "offline entries", AccentGold, Modifier.weight(1f))
            }
        }
        if (snapshot.isEmpty()) {
            item { EmptyAgentState("No assigned chits yet", "Ask admin to assign chit groups and sync data.") }
        } else {
            items(snapshot, key = { it.group.id }) { item ->
                AgentChitRow(item) {
                    onOpenGroup(item.group.id, "${item.group.registerNo ?: item.group.id} • ${item.group.name ?: "Chit"}")
                }
            }
        }
    }
}

@Composable
fun AgentMemberListScreen(groupId: String, onCollect: (memberName: String, chitLabel: String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val data by produceState(initialValue = Pair<ChitGroupEntity?, List<AgentMemberDue>>(null, emptyList()), groupId) {
        value = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val group = db.groupDao().getGroupByIdSync(groupId)
            val rows = db.membershipDao().getActiveForGroupSync(groupId).mapNotNull { membership ->
                val member = db.memberDao().getMemberByIdSync(membership.memberId) ?: return@mapNotNull null
                AgentMemberDue(member, membership.ticketNo.orEmpty(), CollectionService.calculateDueBreakdown(db, member.id, groupId))
            }
            group to rows
        }
    }
    val group = data.first
    val rows = data.second

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaroonBackground),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (rows.isEmpty()) {
            item { EmptyAgentState("No members in this chit", "Sync data from admin or assign members to this chit.") }
        } else {
            items(rows, key = { it.member.id }) { row ->
                AgentMemberRow(row, group, onCollect)
            }
        }
    }
}

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
    val cash = payments.filter { it.mode == "Cash" }.sumOf { it.amountPaid }
    val upi = payments.filter { it.mode == "UPI" }.sumOf { it.amountPaid }

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

@Composable
private fun AgentWelcomeCard(name: String, syncText: String, onRefresh: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountCircle, null, tint = MaroonPrimary)
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text("Vanakkam, $name", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH).format(Date()), color = TextGray, fontSize = 10.sp)
                Text(syncText, color = TextGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh", tint = MaroonPrimary) }
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
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation = 1.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Savings, null, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text("${group.registerNo ?: group.id} • ${group.name ?: "Chit"}", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${money(group.chitValue.toLong() / 100)} • ${group.durationMonths} months • ${group.branch ?: "-"}", color = TextGray, fontSize = 10.sp, maxLines = 1)
                Text("${item.memberCount} members • ${item.pendingThisMonth} pending", color = TextGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AgentMemberRow(row: AgentMemberDue, group: ChitGroupEntity?, onCollect: (String, String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val phone = row.member.phone.orEmpty().filter(Char::isDigit).takeLast(10)
    val statusColor = when {
        row.due.pendingPaise <= 0 -> AccentGreen
        row.due.overdueDays > 0 -> AccentRed
        else -> AccentGold
    }
    val statusText = when {
        row.due.pendingPaise <= 0 -> "Paid"
        row.due.overdueDays > 0 -> "Overdue"
        else -> "Pending"
    }
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).background(statusColor.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(row.member.name.orEmpty().take(1).ifBlank { "?" }, color = statusColor, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 9.dp).weight(1f)) {
                    Text(row.member.name ?: row.member.id, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${row.member.phone.orEmpty()} • Ticket ${row.ticketNo.ifBlank { "-" }}", color = TextGray, fontSize = 10.sp)
                }
                Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Due ${money(row.due.pendingPaise / 100)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (row.due.pendingPaise > 0) AccentRed else AccentGreen)
                Text(row.due.pendingInstallments.joinToString { "$it TH" }.ifBlank { "No pending" }, color = TextGray, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("Call", Icons.Default.Call, MaroonPrimary, Modifier.weight(1f)) {
                    if (phone.isNotBlank()) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }
                SmallAction("WhatsApp", Icons.Default.Send, AccentGreen, Modifier.weight(1f)) {
                    if (phone.isNotBlank()) {
                        val text = "Vanakkam ${row.member.name}, ${group?.registerNo ?: group?.id ?: "chit"} pending ${money(row.due.pendingPaise / 100)}. Kindly make the payment. - Jothi Vel Chits"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$phone?text=${Uri.encode(text)}")))
                    }
                }
                SmallAction("Collect", Icons.Default.PendingActions, MaroonPrimary, Modifier.weight(1f)) {
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
                Text("${payment.mode} • ${SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(payment.paidAt))}", color = TextGray, fontSize = 9.sp)
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
