@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.jothivel.chits.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.*
import com.jothivel.chits.ui.components.AshAnimatedSearchBar
import com.jothivel.chits.ui.components.DetailFindToolbar
import com.jothivel.chits.ui.components.CashCalendarDialog
import com.jothivel.chits.ui.components.CashCalendarEntry
import com.jothivel.chits.ui.components.CashCalendarStatus
import com.jothivel.chits.ui.components.PremiumInputField
import com.jothivel.chits.ui.ledger.ResizableLedgerSheet
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.entity.ActivityLogEntity
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.InstallmentEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.data.local.entity.ChitMembershipEntity
import com.jothivel.chits.data.local.entity.PaymentEntity
import com.jothivel.chits.data.local.entity.CollectionReceiptEntity
import com.jothivel.chits.data.local.entity.FinancialTransactionEntity
import com.jothivel.chits.data.local.CollectionService
import com.jothivel.chits.data.local.SavedCollection
import com.jothivel.chits.data.local.FinancialService
import com.jothivel.chits.utils.CsvDownloadHelper
import com.jothivel.chits.utils.ReceiptPdfHelper
import com.jothivel.chits.ui.agent.LocalAssistantEngine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private enum class AppDestination { HOME, COLLECT, LEDGER, PENDING, NEW_CHIT, ADD_MEMBER, SETTINGS, SETTLEMENT, DELIVERY, TODAY_WORK, CUSTOMER_PROFILE, DAILY_CLOSING, LABOUR }

private data class DueCustomer(
    val name: String,
    val code: String,
    val phone: String,
    val area: String,
    val filterArea: String,
    val chit: String,
    val chitValue: Long,
    val payable: Long,
    val paid: Long,
    val pending: Long,
    val lastPaid: String,
    val dueDate: String,
    val installments: List<String>,
    val agent: String,
    val overdueDays: Int
    ,val recentContactAt: Long = 0L
)

private data class CollectionLookupData(
    val members: List<MemberEntity> = emptyList(),
    val groups: List<ChitGroupEntity> = emptyList(),
    val memberships: List<ChitMembershipEntity> = emptyList()
)

private data class DashboardCollectionSnapshot(
    val todayAmountPaise: Long = 0,
    val todayCount: Int = 0,
    val calendarEntries: List<CashCalendarEntry> = emptyList(),
    val recentRows: List<Triple<String, String, String>> = emptyList(),
    val pendingPaise: Long = 0,
    val pendingCustomers: Int = 0,
    val settlementPaise: Long = 0,
    val deliveryPaise: Long = 0
)

// private val sampleDues = listOf(
//     DueCustomer("Gopal", "C-22", "8015286001", "Ponnampatti, Manapparai", "Cits-6", 100000, 19700, 0, 19700, "10-Jul-24", "15-Nov-24", listOf("17 TH", "18 TH", "19 TH"), "NR", 46),
//     DueCustomer("Chinnammal", "C-12", "8525286004", "Kannuthu, Manapparai", "Cits-12", 500000, 276500, 0, 276500, "12-Oct-25", "12-Mar-24", listOf("14 TH", "15 TH", "16 TH", "17 TH"), "BA", 132),
//     DueCustomer("Johnsi Rani", "C-8", "9943387896", "Purathakudi, Singampunari", "Cits-8", 200000, 194000, 0, 194000, "06-Sep-25", "06-Feb-24", listOf("15 TH", "16 TH", "17 TH"), "BA", 78)
// )

private fun money(value: Long): String = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)

@Composable
fun ApprovedAppFlow(
    onLogout: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit = {}
) {
    val roleCheckContext = LocalContext.current
    val userRole = remember { com.jothivel.chits.utils.AppPreferences(roleCheckContext).getUserRole() }
    if (userRole == com.jothivel.chits.utils.AppPreferences.ROLE_AGENT) {
        AgentAppFlow(onLogout = onLogout)
        return
    }

    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var previousDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var collectionCustomer by rememberSaveable { mutableStateOf("Thilban") }
    var collectionChit by rememberSaveable { mutableStateOf("Cits-39") }
    var ledgerCustomer by remember { mutableStateOf<DueCustomer?>(null) }
    var profileCustomerId by rememberSaveable { mutableStateOf("") }

    fun open(next: AppDestination) {
        if (next == destination) return
        previousDestination = destination
        destination = next
    }

    BackHandler(enabled = destination != AppDestination.HOME) {
        destination = when (destination) {
            AppDestination.NEW_CHIT,
            AppDestination.ADD_MEMBER,
            AppDestination.SETTINGS,
            AppDestination.SETTLEMENT,
            AppDestination.DELIVERY -> AppDestination.HOME
            AppDestination.TODAY_WORK,
            AppDestination.DAILY_CLOSING -> AppDestination.HOME
            AppDestination.CUSTOMER_PROFILE -> previousDestination.takeIf { it != AppDestination.CUSTOMER_PROFILE } ?: AppDestination.HOME
            else -> previousDestination.takeIf { it != destination } ?: AppDestination.HOME
        }
        if (destination == AppDestination.HOME) previousDestination = AppDestination.HOME
    }

    Scaffold(
        containerColor = MaroonBackground,
        bottomBar = {
            val showBottomBar = destination !in setOf(AppDestination.LEDGER, AppDestination.NEW_CHIT, AppDestination.ADD_MEMBER, AppDestination.SETTINGS, AppDestination.SETTLEMENT, AppDestination.DELIVERY, AppDestination.LABOUR)
            if (showBottomBar) ApprovedBottomBar(destination) { open(it) }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.animation.AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    androidx.compose.animation.ContentTransform(
                        targetContentEnter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(140)),
                        initialContentExit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90))
                    )
                },
                label = "appPageTransition"
            ) { activeDestination ->
            when (activeDestination) {
                AppDestination.HOME -> ApprovedDashboard(
                    onLedger = { ledgerCustomer = null; open(AppDestination.LEDGER) },
                    onCollect = { collectionCustomer = ""; collectionChit = ""; open(AppDestination.COLLECT) },
                    onPending = { open(AppDestination.PENDING) },
                    onNewChit = { open(AppDestination.NEW_CHIT) },
                    onAddMember = { open(AppDestination.ADD_MEMBER) },
                    onSettlement = { open(AppDestination.SETTLEMENT) },
                    onDelivery = { open(AppDestination.DELIVERY) },
                    onTodayWork = { open(AppDestination.TODAY_WORK) },
                    onClosing = { open(AppDestination.DAILY_CLOSING) },
                    onSettings = { open(AppDestination.SETTINGS) }
                )
                AppDestination.COLLECT -> CollectionScreen(
                    initialCustomer = collectionCustomer,
                    initialChit = collectionChit,
                    onBack = { destination = previousDestination }
                )
                AppDestination.LEDGER -> LedgerScreenApproved(
                    selectedCustomer = ledgerCustomer,
                    onBack = { destination = previousDestination },
                    onCollect = { customer, chit -> collectionCustomer = customer; collectionChit = chit; open(AppDestination.COLLECT) }
                )
                AppDestination.SETTINGS -> SettingsScreenApproved(
                    onBack = { destination = AppDestination.HOME },
                    onBackup = onBackupDatabase,
                    onRestore = onRestoreDatabase,
                    onExport = onExportCsv,
                    onImport = onImportCsv,
                    onLabour = { open(AppDestination.LABOUR) },
                    onLogout = onLogout
                )
                AppDestination.LABOUR -> com.jothivel.chits.ui.labour.LabourManagementScreen(
                    onBack = { destination = AppDestination.SETTINGS }
                )
                AppDestination.PENDING -> PendingScreen(
                    onBack = { destination = previousDestination },
                    onOpenLedger = { customer -> profileCustomerId=customer.code; open(AppDestination.CUSTOMER_PROFILE) },
                    onCollect = { customer -> collectionCustomer = customer.name; collectionChit = customer.chit; open(AppDestination.COLLECT) }
                )
                AppDestination.NEW_CHIT -> CompactNewChitScreen(onBack = { destination = AppDestination.HOME })
                AppDestination.ADD_MEMBER -> CompactAddMemberScreen(onBack = { destination = AppDestination.HOME })
                AppDestination.SETTLEMENT -> FinancialEntryScreen("SETTLEMENT", onBack = { destination = AppDestination.HOME })
                AppDestination.DELIVERY -> FinancialEntryScreen("DELIVERY", onBack = { destination = AppDestination.HOME })
                AppDestination.TODAY_WORK -> TodayWorkScreen(onBack={destination=AppDestination.HOME}, onProfile={profileCustomerId=it;open(AppDestination.CUSTOMER_PROFILE)}, onCollect={customer,chit->collectionCustomer=customer;collectionChit=chit;open(AppDestination.COLLECT)})
                AppDestination.CUSTOMER_PROFILE -> CustomerProfileScreen(profileCustomerId, onBack={destination=previousDestination}, onCollect={customer,chit->collectionCustomer=customer;collectionChit=chit;open(AppDestination.COLLECT)})
                AppDestination.DAILY_CLOSING -> DailyClosingScreen(onBack={destination=AppDestination.HOME})
            }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LABOUR (FIELD AGENT) APP SHELL — separate nav for the AGENT role, admin flow untouched above
// ═══════════════════════════════════════════════════════════════════════════════

private enum class AgentDestination { MY_CHITS, MEMBER_LIST, COLLECT, TODAY_SUMMARY }

@Composable
private fun AgentAppFlow(onLogout: () -> Unit) {
    var destination by rememberSaveable { mutableStateOf(AgentDestination.MY_CHITS) }
    var previousDestination by rememberSaveable { mutableStateOf(AgentDestination.MY_CHITS) }
    var selectedGroupId by rememberSaveable { mutableStateOf("") }
    var selectedGroupLabel by rememberSaveable { mutableStateOf("") }
    var collectCustomer by rememberSaveable { mutableStateOf("") }
    var collectChit by rememberSaveable { mutableStateOf("") }

    fun open(next: AgentDestination) {
        if (next == destination) return
        previousDestination = destination
        destination = next
    }

    BackHandler(enabled = destination != AgentDestination.MY_CHITS) {
        destination = if (destination == AgentDestination.MEMBER_LIST) AgentDestination.MY_CHITS
        else previousDestination.takeIf { it != destination } ?: AgentDestination.MY_CHITS
    }

    Scaffold(
        containerColor = MaroonBackground,
        bottomBar = {
            if (destination != AgentDestination.MEMBER_LIST) AgentBottomBar(destination) { open(it) }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.animation.AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    androidx.compose.animation.ContentTransform(
                        targetContentEnter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(140)),
                        initialContentExit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90))
                    )
                },
                label = "agentPageTransition"
            ) { activeDestination ->
                when (activeDestination) {
                    AgentDestination.MY_CHITS -> Column(Modifier.fillMaxSize()) {
                        BrandTopBar("My Chits", action = Icons.Default.Logout, onAction = onLogout)
                        com.jothivel.chits.ui.labour.AgentMyChitsScreen(
                            onOpenGroup = { groupId, label -> selectedGroupId = groupId; selectedGroupLabel = label; open(AgentDestination.MEMBER_LIST) }
                        )
                    }
                    AgentDestination.MEMBER_LIST -> Column(Modifier.fillMaxSize()) {
                        BrandTopBar(selectedGroupLabel.ifBlank { "Members" }, back = { destination = AgentDestination.MY_CHITS })
                        com.jothivel.chits.ui.labour.AgentMemberListScreen(
                            groupId = selectedGroupId,
                            onCollect = { memberName, chitLabel -> collectCustomer = memberName; collectChit = chitLabel; open(AgentDestination.COLLECT) }
                        )
                    }
                    AgentDestination.COLLECT -> CollectionScreen(
                        initialCustomer = collectCustomer,
                        initialChit = collectChit,
                        onBack = { destination = previousDestination }
                    )
                    AgentDestination.TODAY_SUMMARY -> Column(Modifier.fillMaxSize()) {
                        BrandTopBar("Today", action = Icons.Default.Logout, onAction = onLogout)
                        com.jothivel.chits.ui.labour.AgentTodaySummaryScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentBottomBar(selected: AgentDestination, onSelect: (AgentDestination) -> Unit) {
    val entries = listOf(
        Triple(AgentDestination.MY_CHITS, Icons.Default.ListAlt, "My Chits"),
        Triple(AgentDestination.COLLECT, Icons.Default.AddCircle, "Collect"),
        Triple(AgentDestination.TODAY_SUMMARY, Icons.Default.Today, "Today")
    )
    Box(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical = 5.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaroonPrimary,
            border = BorderStroke(1.dp, MaroonDark.copy(alpha = .35f)),
            shadowElevation = 3.dp
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            entries.forEach { (route, icon, label) ->
                val isSelected = selected == route
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelect(route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 44.dp else 32.dp)
                            .height(24.dp)
                            .background(
                                if (isSelected) Color.White.copy(alpha=.16f) else Color.Transparent,
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            label,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha=.60f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha=.62f),
                        fontSize = 8.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ApprovedBottomBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    val entries = listOf(
        Triple(AppDestination.HOME, Icons.Default.Home, "Home"),
        Triple(AppDestination.LEDGER, Icons.Default.MenuBook, "Ledger"),
        Triple(AppDestination.COLLECT, Icons.Default.AddCircle, "Collection"),
        Triple(AppDestination.PENDING, Icons.Default.PendingActions, "Pending")
    )
    Box(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical = 5.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaroonPrimary,
            border = BorderStroke(1.dp, MaroonDark.copy(alpha = .35f)),
            shadowElevation = 3.dp
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            entries.forEach { (route, icon, label) ->
                val isSelected = selected == route
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelect(route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 44.dp else 32.dp)
                            .height(24.dp)
                            .background(
                                if (isSelected) Color.White.copy(alpha=.16f) else Color.Transparent,
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            label,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha=.60f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha=.62f),
                        fontSize = 8.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun BrandTopBar(
    title: String,
    back: (() -> Unit)? = null,
    action: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    secondAction: ImageVector? = null,
    onSecondAction: (() -> Unit)? = null
) {
    Surface(color = MaroonPrimary, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (back != null) {
                IconButton(onClick = back) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing=.1.sp, modifier = Modifier.weight(1f))
            if (action != null) IconButton(onClick = { onAction?.invoke() }, enabled = onAction != null) { Icon(action, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            if (secondAction != null) IconButton(onClick = { onSecondAction?.invoke() }, enabled = onSecondAction != null) { Icon(secondAction, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun ApprovedDashboard(onLedger: () -> Unit, onCollect: () -> Unit, onPending: () -> Unit, onNewChit: () -> Unit, onAddMember: () -> Unit, onSettlement: () -> Unit, onDelivery: () -> Unit, onTodayWork: () -> Unit, onClosing: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    var showCalendar by remember { mutableStateOf(false) }
    val dashboardListState = rememberLazyListState()
    val lastBackupAt = remember { com.jothivel.chits.utils.AppPreferences(context).getLastBackupAt() }
    val backupAgeDays = if(lastBackupAt==0L) Long.MAX_VALUE else ((System.currentTimeMillis()-lastBackupAt)/86_400_000L).coerceAtLeast(0)
    val collectionSnapshot by produceState(initialValue = DashboardCollectionSnapshot(), context) {
        value = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val today = CollectionService.todayKey()
            val members = db.memberDao().getAllMembersSync().associateBy { it.id }
            val groups = db.groupDao().getAllGroupsSync().associateBy { it.id }
            val receipts = db.collectionReceiptDao().getRecentSync(Int.MAX_VALUE)
            val scheduledDues = CollectionService.buildCalendarSchedule(db)
            val dueAmounts = db.membershipDao().getAllActiveSync().map { CollectionService.calculateDuePaise(db, it.memberId, it.groupId) }.filter { it > 0 }
            DashboardCollectionSnapshot(
                todayAmountPaise = db.collectionReceiptDao().getTotalForDateSync(today),
                todayCount = db.collectionReceiptDao().getCountForDateSync(today),
                calendarEntries = receipts.map { receipt ->
                    val member = members[receipt.memberId]
                    val group = groups[receipt.groupId]
                    CashCalendarEntry(receipt.businessDate, member?.name ?: receipt.memberId, member?.id ?: receipt.memberId, group?.registerNo ?: receipt.groupId, receipt.amountPaidPaise / 100, CashCalendarStatus.PAID, receipt.mode, receipt.receiptNo)
                } + scheduledDues.map { due ->
                    CashCalendarEntry(due.dateKey, due.memberName, due.memberId, due.chitNo, due.remainingPaise / 100, CashCalendarStatus.DUE, "${due.installmentNo} TH")
                },
                recentRows = receipts.take(4).map { receipt ->
                    val member = members[receipt.memberId]
                    val group = groups[receipt.groupId]
                    Triple("Collection received from ${member?.name ?: receipt.memberId} (${group?.registerNo ?: receipt.groupId})", SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(receipt.paidAt)), money(receipt.amountPaidPaise / 100))
                },
                pendingPaise = dueAmounts.sum(),
                pendingCustomers = dueAmounts.size,
                settlementPaise = db.financialTransactionDao().getPostedTotalByTypeSync("SETTLEMENT"),
                deliveryPaise = db.financialTransactionDao().getPostedTotalByTypeSync("DELIVERY")
            )
        }
    }
    val calendarEntries = collectionSnapshot.calendarEntries
    Column(Modifier.fillMaxSize()) {
        BrandTopBar(
            "Jothi Vel Chits",
            action = Icons.Default.CalendarMonth,
            onAction = { showCalendar = true },
            secondAction = Icons.Default.Settings,
            onSecondAction = onSettings
        )
        LazyColumn(
            state = dashboardListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)).background(MaroonPrimary)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) {
                        Text("Welcome", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH).format(Date()), color = Color.White.copy(alpha = .70f), fontSize = 10.sp)
                    }
                    AutoSlidingMetricCarousel(
                        autoPlayEnabled = !dashboardListState.isScrollInProgress,
                        cards = listOf(
                            DashboardMetricData("Today Collection", money(collectionSnapshot.todayAmountPaise / 100), "${collectionSnapshot.todayCount} Collections", MaroonPrimary, onCollect),
                            DashboardMetricData("Pending", money(collectionSnapshot.pendingPaise / 100), "${collectionSnapshot.pendingCustomers} Pending Dues", AccentGold, onPending),
                            DashboardMetricData("Delivery", money(collectionSnapshot.deliveryPaise / 100), "Recorded delivery", AccentGreen, onDelivery),
                            DashboardMetricData("Settlement", money(collectionSnapshot.settlementPaise / 100), "Recorded settlement", Color(0xFF285A9B), onSettlement)
                        )
                    )
                }
            }
            item {
                Row(Modifier.padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    QuickCreateAction("Today's Work", "Priority daily tasks", Icons.Default.Today, onTodayWork, Modifier.weight(1f))
                    QuickCreateAction("Day Closing", "Cash & bank summary", Icons.Default.Assessment, onClosing, Modifier.weight(1f))
                }
            }
            if(backupAgeDays >= 7) item {
                Surface(Modifier.padding(horizontal=12.dp).fillMaxWidth().clickable(onClick=onSettings), shape=RoundedCornerShape(11.dp), color=Color(0xFFFFF5DF), border=BorderStroke(1.dp,AccentGold.copy(alpha=.45f))) {
                    Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.Backup,null,tint=AccentGold); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)){Text(if(lastBackupAt==0L) "Backup not created" else "Last backup: $backupAgeDays days ago",fontSize=11.sp,fontWeight=FontWeight.SemiBold);Text("Tap Settings and save a backup",fontSize=9.sp,color=TextGray)};Icon(Icons.Default.ChevronRight,null,tint=TextGray) }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickCreateAction("New Chit", "Create chit group", Icons.Default.AddCard, onNewChit, Modifier.weight(1f))
                    QuickCreateAction("Add Member", "Join customer to chit", Icons.Default.PersonAdd, onAddMember, Modifier.weight(1f))
                }
            }
            item { Box(Modifier.padding(horizontal = 12.dp)) { SectionTitle("Recent Activity") } }
            if (collectionSnapshot.recentRows.isEmpty()) item { Text("No collection activity yet", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
            items(collectionSnapshot.recentRows) { item ->
                Box(Modifier.padding(horizontal = 12.dp)) {
                    ActivityRow(item.first, item.second, item.third)
                }
            }
        }
    }
    if (showCalendar) CashCalendarDialog(calendarEntries, onDismiss = { showCalendar = false }, onCustomerClick = { showCalendar = false; onLedger() })
}

private data class DashboardMetricData(
    val title: String,
    val value: String,
    val subtitle: String,
    val color: Color,
    val onClick: () -> Unit = {}
)

@Composable
private fun AutoSlidingMetricCarousel(cards: List<DashboardMetricData>, autoPlayEnabled: Boolean) {
    if (cards.isEmpty()) return
    var activeIndex by rememberSaveable(cards.size) { mutableIntStateOf(0) }
    var dragDistance by remember { mutableFloatStateOf(0f) }

    fun moveBy(step: Int) {
        activeIndex = ((activeIndex + step) % cards.size + cards.size) % cards.size
    }

    LaunchedEffect(cards.size, activeIndex, autoPlayEnabled) {
        if (!autoPlayEnabled) return@LaunchedEffect
        delay(4_500L)
        moveBy(1)
    }

    val panelShape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = panelShape,
        color = MaroonPrimary,
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clipToBounds()
                    .pointerInput(cards.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onHorizontalDrag = { change, amount -> change.consume(); dragDistance += amount },
                        onDragEnd = {
                            if (dragDistance <= -45f) moveBy(1)
                            if (dragDistance >= 45f) moveBy(-1)
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f }
                    )
                    },
                contentAlignment = Alignment.Center
            ) {
                val responsiveCardWidth = (maxWidth * .70f).coerceIn(220.dp, 292.dp)
                val responsiveSideOffset = maxWidth * .36f
                val density = LocalDensity.current
                cards.forEachIndexed { index, card ->
                var relative = index - activeIndex
                val half = cards.size / 2
                if (relative > half) relative -= cards.size
                if (relative < -half) relative += cards.size
                if (kotlin.math.abs(relative) > 1) return@forEachIndexed

                val targetX = when {
                    relative == 0 -> 0.dp
                    relative < 0 -> -responsiveSideOffset
                    relative == 1 -> responsiveSideOffset
                    else -> 0.dp
                }
                val targetY = when {
                    relative == 0 -> (-12).dp
                    kotlin.math.abs(relative) == 1 -> 31.dp
                    else -> 78.dp
                }
                val targetScale = when (kotlin.math.abs(relative)) { 0 -> 1f; 1 -> .82f; else -> .68f }
                val targetAlpha = when (kotlin.math.abs(relative)) { 0 -> 1f; 1 -> .40f; else -> .14f }
                val targetZ = when (kotlin.math.abs(relative)) { 0 -> 4f; 1 -> 3f; else -> 1f }
                val targetXPx = with(density) { targetX.toPx() }
                val targetYPx = with(density) { targetY.toPx() }
                val x by androidx.compose.animation.core.animateFloatAsState(targetXPx, androidx.compose.animation.core.tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "carouselX")
                val y by androidx.compose.animation.core.animateFloatAsState(targetYPx, androidx.compose.animation.core.tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "carouselY")
                val scale by androidx.compose.animation.core.animateFloatAsState(targetScale, androidx.compose.animation.core.tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "carouselScale")
                val alpha by androidx.compose.animation.core.animateFloatAsState(targetAlpha, androidx.compose.animation.core.tween(300), label = "carouselAlpha")

                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .zIndex(targetZ)
                            .graphicsLayer {
                                translationX = x
                                translationY = y
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        DashboardMetric(
                            card.title,
                            card.value,
                            card.subtitle,
                            card.color,
                            Modifier.width(responsiveCardWidth),
                            elevated = relative == 0,
                            onClick = { if (index == activeIndex) card.onClick() else activeIndex = index }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { moveBy(-1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, "Previous card", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    cards.indices.forEach { index ->
                        val selected = index == activeIndex
                        Box(
                            Modifier
                                .width(if (selected) 20.dp else 6.dp)
                                .height(6.dp)
                                .background(if (selected) Color.White else Color.White.copy(alpha = .28f), RoundedCornerShape(6.dp))
                                .clickable { activeIndex = index }
                        )
                    }
                }
                IconButton(onClick = { moveBy(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, "Next card", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickCreateAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Surface(modifier.height(62.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(MaroonSurfaceLight, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaroonPrimary, modifier = Modifier.size(18.dp)) }
            Column(Modifier.padding(start = 9.dp), verticalArrangement = Arrangement.Center) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

private fun relativeDateKey(offsetDays: Int): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offsetDays) }.time)

@Composable
private fun DashboardMetric(title: String, value: String, subtitle: String, color: Color, modifier: Modifier, elevated: Boolean, onClick: () -> Unit = {}) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .wrapContentHeight()
            .then(if (elevated) Modifier.shadow(2.dp, shape, clip = false) else Modifier)
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFCF4F4))),
                shape
            )
            .border(.7.dp, Color(0xFFF1DCDD), shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title.uppercase(Locale.ENGLISH), fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.SemiBold, letterSpacing = .6.sp)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, fontSize = 10.sp, color = TextGray, maxLines = 1)
        }
    }
}

@Composable private fun SectionTitle(title: String, action: String? = null) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); if (action != null) Text(action, color = Color(0xFF285A9B), fontSize = 10.sp) }

@Composable
private fun ActivityRow(title: String, time: String, amount: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation=.5.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(27.dp).background(AccentGreen.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.SouthWest, null, tint = AccentGreen, modifier = Modifier.size(15.dp)) }
            Column(Modifier.padding(start = 8.dp).weight(1f)) { Text(title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(time, fontSize = 9.sp, color = TextGray) }
            Text(amount, color = if (title.contains("Pending") || title.contains("Delivery")) AccentRed else AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LedgerScreenApproved(
    selectedCustomer: DueCustomer?,
    onBack: () -> Unit,
    onCollect: (String, String) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showRotatePrompt by remember { mutableStateOf(true) }
    if (isLandscape) {
        ResizableLedgerSheet(onBack)
        return
    }
    // Portrait also shows the same Room-backed sheet; rotation only expands the workspace.
    ResizableLedgerSheet(onBack)
    if (showRotatePrompt) {
        AlertDialog(
            onDismissRequest = { showRotatePrompt = false },
            icon = { Icon(Icons.Default.ScreenRotation, null, tint = MaroonPrimary, modifier = Modifier.size(38.dp)) },
            title = { Text("Rotate for wide Ledger") },
            text = { Text("100 customer rows are loaded. Rotate to landscape to see more columns at once.") },
            confirmButton = { Button(onClick = { showRotatePrompt = false }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text("View table") } }
        )
    }
    return
    var query by remember { mutableStateOf("") }
    var detailed by remember { mutableStateOf(false) }
    var activeCustomer by remember(selectedCustomer) { mutableStateOf(selectedCustomer) }
    var activeResultIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val name = activeCustomer?.name ?: "Thilban"
    val code = activeCustomer?.code ?: "C-39"
    val phone = activeCustomer?.phone ?: "8220196686"
    val area = activeCustomer?.area ?: "Manapparai"
    val chit = activeCustomer?.chit ?: "Cits-39"
    val chitValue = activeCustomer?.chitValue ?: 300000L
    val collection = activeCustomer?.paid ?: 253650L
    val settlement = if (activeCustomer == null) 253650L else activeCustomer!!.paid
    val balance = collection - settlement
    val delivery = 0L
    val ledgerRows = remember { listOf("Jun 2024 (6/12)" to 13050L, "Jul 2024 (7/12)" to 10550L, "Aug 2024 (8/12)" to 10650L, "Sep 2024 (9/12)" to 10800L, "Oct 2024 (10/12)" to 10950L, "Nov 2024 (11/12)" to 11100L, "Dec 2024 (12/12)" to 11250L, "Jan 2025 (1/12)" to 11550L) }
    val normalizedQuery = query.trim().lowercase()
    val ledgerMatches = buildList {
        if (normalizedQuery.isNotBlank()) {
            val summaryText = "$name $code $phone $area $chit collection settlement balance delivery ${money(chitValue)}".lowercase()
            if (summaryText.contains(normalizedQuery)) add(1)
            ledgerRows.forEachIndexed { index, row ->
                if ("${row.first} ${money(row.second)} collection settlement".lowercase().contains(normalizedQuery)) add(index + 4)
            }
        }
    }
    val activeLedgerTarget = ledgerMatches.getOrNull(activeResultIndex)

    fun goToLedgerResult(direction: Int) {
        if (ledgerMatches.isEmpty()) return
        activeResultIndex = (activeResultIndex + direction + ledgerMatches.size) % ledgerMatches.size
        scope.launch { listState.animateScrollToItem(ledgerMatches[activeResultIndex]) }
    }

    LaunchedEffect(query) {
        activeResultIndex = 0
        ledgerMatches.firstOrNull()?.let { listState.animateScrollToItem(it) }
    }

    fun shareStatement() {
        val statement = "Jothi Vel Chits\nCustomer: $name ($code)\nPhone: $phone\nChit: $chit\nCollection: ${money(collection)}\nSettlement: ${money(settlement)}\nBalance: ${money(balance)}\nDelivery: ${money(delivery)}"
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Jothi Vel Chits - $name Ledger")
            putExtra(Intent.EXTRA_TEXT, statement)
        }, "Share statement"))
    }

    Column(Modifier.fillMaxSize()) {
        BrandTopBar("Ledger", onBack, Icons.Default.Share)
        DetailFindToolbar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Find customer, amount or month",
            resultCount = ledgerMatches.size,
            activeResultIndex = activeResultIndex.coerceAtMost((ledgerMatches.size - 1).coerceAtLeast(0)),
            onPrevious = { goToLedgerResult(-1) },
            onNext = { goToLedgerResult(1) },
            onDownload = { shareStatement() },
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
        LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(!detailed, { detailed = false }, { Text("Consolidated") }); FilterChip(detailed, { detailed = true }, { Text("Details") }) } }
            item { CustomerSummaryCard(name, code, phone, area, chit, chitValue, collection, settlement, balance, delivery, activeLedgerTarget == 1, onCollect) }
            item {
                OutlinedButton(onClick = { shareStatement() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.IosShare, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Export / Share Statement")
                }
            }
            item { SectionTitle(if (detailed) "Monthly Transaction Details" else "Monthly Ledger") }
            items(ledgerRows, key = { it.first }, contentType = { "ledger_month" }) { (month, amount) ->
                val target = ledgerRows.indexOfFirst { it.first == month } + 4
                if (detailed) DetailedInstallmentRow(month, amount, !month.startsWith("Jan"), activeLedgerTarget == target) else InstallmentRow(month, amount, !month.startsWith("Jan"), activeLedgerTarget == target)
            }
        }
    }
    if (showRotatePrompt) {
        AlertDialog(
            onDismissRequest = { showRotatePrompt = false },
            icon = { Icon(Icons.Default.ScreenRotation, null, tint = MaroonPrimary, modifier = Modifier.size(42.dp)) },
            title = { Text("Rotate for Ledger Sheet") },
            text = { Text("For the full Excel-style ledger, rotate your phone to landscape. You can drag column lines to resize and scroll in both directions.") },
            confirmButton = { Button(onClick = { showRotatePrompt = false }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text("Got it") } },
            dismissButton = { TextButton(onClick = { showRotatePrompt = false }) { Text("Continue portrait") } }
        )
    }
}

@Composable
private fun CustomerSummaryCard(
    name: String, code: String, phone: String, area: String, chit: String,
    chitValue: Long, collection: Long, settlement: Long, balance: Long, delivery: Long,
    highlighted: Boolean,
    onCollect: (String, String) -> Unit
) {
    val cardColor by androidx.compose.animation.animateColorAsState(if (highlighted) Color(0xFFF1E8EA) else Color.White, label = "ledgerHighlight")
    Surface(shape = RoundedCornerShape(16.dp), color = cardColor, border = BorderStroke(1.dp, if (highlighted) MaroonPrimary.copy(alpha = .45f) else DividerGray)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(MaroonLight, CircleShape), contentAlignment = Alignment.Center) { Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Column(Modifier.padding(start = 9.dp).weight(1f)) { Text("$name • $code", fontWeight = FontWeight.SemiBold, fontSize = 16.sp); Text("$phone • $area", color = TextGray, fontSize = 11.sp, maxLines = 1) }
                AssistChip(onClick = {}, label = { Text("Active", color = AccentGreen) })
            }
            Spacer(Modifier.height(8.dp)); Text("$chit • Chit Value", fontSize = 10.sp); Text(money(chitValue), fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("15 Jun 2024 – 15 Jan 2026", color = TextGray, fontSize = 10.sp)
            Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MiniMetric("Collection", money(collection), AccentGreen, Modifier.weight(1f)); MiniMetric("Settlement", money(settlement), AccentGreen, Modifier.weight(1f)); MiniMetric("Balance", money(balance), if (balance < 0) AccentRed else Color.Black, Modifier.weight(1f)); MiniMetric("Delivery", money(delivery), Color(0xFF285A9B), Modifier.weight(1f))
            }
            TextButton(onClick = { onCollect(name, chit) }, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.Add, null); Text("Add collection") }
        }
    }
}

@Composable private fun MiniMetric(label: String, value: String, color: Color, modifier: Modifier) = Surface(modifier, shape = RoundedCornerShape(8.dp), color = MaroonBackground, border = BorderStroke(1.dp, DividerGray)) { Column(Modifier.padding(7.dp)) { Text(label, fontSize = 9.sp); Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1) } }

@Composable
private fun InstallmentRow(month: String, amount: Long, paid: Boolean, highlighted: Boolean = false) {
    val rowColor by androidx.compose.animation.animateColorAsState(if (highlighted) Color(0xFFE3E5E8) else Color.White, label = "installmentHighlight")
    Row(Modifier.fillMaxWidth().background(rowColor, RoundedCornerShape(9.dp)).border(if (highlighted) 1.dp else 0.dp, MaroonPrimary.copy(alpha = .4f), RoundedCornerShape(9.dp)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (paid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (paid) AccentGreen else TextLightGray)
        Text(month, Modifier.padding(start = 8.dp).weight(1f), fontSize = 12.sp)
        Column(horizontalAlignment = Alignment.End) { Text(money(amount), color = if (paid) AccentGreen else Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(if (paid) "Paid" else "Due", color = if (paid) TextGray else AccentRed, fontSize = 9.sp) }
    }
}

@Composable
private fun DetailedInstallmentRow(month: String, amount: Long, paid: Boolean, highlighted: Boolean = false) {
    val rowColor by androidx.compose.animation.animateColorAsState(if (highlighted) Color(0xFFE3E5E8) else Color.White, label = "detailHighlight")
    Surface(shape = RoundedCornerShape(9.dp), color = rowColor, border = BorderStroke(1.dp, if (highlighted) MaroonPrimary.copy(alpha = .45f) else DividerGray)) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(month, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (paid) "Settled" else "Pending", color = if (paid) AccentGreen else AccentRed, fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row {
                MiniDue("Collection", if (paid) amount else 0, AccentGreen, Modifier.weight(1f))
                MiniDue("Settlement", if (paid) amount else 0, Color(0xFF285A9B), Modifier.weight(1f))
                MiniDue("Balance", if (paid) 0 else -amount, if (paid) Color.Black else AccentRed, Modifier.weight(1f))
                MiniDue("Delivery", 0, Color.Black, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionScreen(initialCustomer: String, initialChit: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val agentPrefs = remember { com.jothivel.chits.utils.AppPreferences(context) }
    val isAgentMode = remember { agentPrefs.isAgent() }
    val lookup by produceState(initialValue = CollectionLookupData(), context) {
        value = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            CollectionLookupData(db.memberDao().getAllMembersSync().filter { it.isActive }, db.groupDao().getAllGroupsSync().filter { it.status == "ACTIVE" }, db.membershipDao().getAllActiveSync())
        }
    }
    val members = lookup.members
    val groups = lookup.groups
    var selectedMember by remember { mutableStateOf<MemberEntity?>(null) }
    var selectedGroup by remember { mutableStateOf<ChitGroupEntity?>(null) }
    var dueAmountPaise by remember { mutableLongStateOf(0L) }
    var amount by remember { mutableStateOf("") }
    var businessDate by remember { mutableStateOf(CollectionService.todayKey()) }
    var mode by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Monthly collection received.") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmExcess by remember { mutableStateOf(false) }
    var savedReceipt by remember { mutableStateOf<SavedCollection?>(null) }
    val eligibleGroups = remember(selectedMember, lookup) {
        val member = selectedMember ?: return@remember emptyList()
        val linkedIds = lookup.memberships.filter { it.memberId == member.id }.mapTo(hashSetOf()) { it.groupId }
        if (linkedIds.isEmpty()) member.selectedChitId?.let(linkedIds::add)
        groups.filter { it.id in linkedIds }
    }
    LaunchedEffect(members, initialCustomer) {
        if (selectedMember == null) selectedMember = members.firstOrNull { it.name.equals(initialCustomer, true) || it.id.equals(initialCustomer, true) }
    }
    LaunchedEffect(selectedMember, eligibleGroups, initialChit) {
        if (selectedGroup?.id !in eligibleGroups.map { it.id }) selectedGroup = eligibleGroups.firstOrNull { it.registerNo.equals(initialChit, true) || it.id.equals(initialChit, true) } ?: eligibleGroups.firstOrNull()
    }
    LaunchedEffect(selectedMember?.id, selectedGroup?.id) {
        val member = selectedMember
        val group = selectedGroup
        dueAmountPaise = if (member != null && group != null) withContext(Dispatchers.IO) { CollectionService.calculateDuePaise(AppDatabase.getDatabase(context), member.id, group.id) } else 0L
        amount = if (dueAmountPaise > 0) ((dueAmountPaise + 99) / 100).toString() else ""
        error = null
    }
    val customer = selectedMember?.name.orEmpty()
    val enteredRupees = amount.toLongOrNull() ?: 0L
    val enteredPaise = enteredRupees.coerceAtMost(Long.MAX_VALUE / 100) * 100
    val paymentState = when {
        enteredPaise <= 0 -> null
        dueAmountPaise <= 0 -> "Advance payment"
        enteredPaise < dueAmountPaise -> "Partial payment"
        enteredPaise == dueAmountPaise -> "Full payment"
        else -> "Excess by ${money((enteredPaise - dueAmountPaise) / 100)}"
    }
    fun saveCollection() {
        val member = selectedMember
        val group = selectedGroup
        val requestId = UUID.randomUUID().toString()
        error = when {
            member == null -> "Select a customer"
            group == null -> "Select a chit linked to this customer"
            enteredRupees <= 0 -> "Enter a valid received amount"
            enteredRupees > Long.MAX_VALUE / 100 -> "Amount is too large"
            mode != "Cash" && reference.isBlank() -> "Reference / UTR is required for $mode"
            else -> null
        }
        if (error != null || member == null || group == null || saving) return
        saving = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    CollectionService.record(
                        AppDatabase.getDatabase(context),
                        requestId,
                        member.id,
                        member.name,
                        group.id,
                        enteredPaise,
                        mode,
                        reference,
                        notes,
                        businessDate,
                        collectedBy = if (isAgentMode) agentPrefs.getAgentName() else null,
                        collectedByAgentId = if (isAgentMode) agentPrefs.getAgentId() else null
                    )
                }
            }
            saving = false
            result.onSuccess { receipt ->
                savedReceipt = receipt
                dueAmountPaise = withContext(Dispatchers.IO) { CollectionService.calculateDuePaise(AppDatabase.getDatabase(context), member.id, group.id) }
                amount = if (dueAmountPaise > 0) ((dueAmountPaise + 99) / 100).toString() else ""
                // Back up every collection to Firestore - not just agent-collected ones - so
                // "Restore Data from Cloud" can recover full payment history after an
                // uninstall/reinstall, not just chit/member structure. Admin-recorded
                // collections are already in this device's own Room (written above), so they're
                // tagged syncedToAdmin=true - FirebaseSyncService must not re-apply them; agent
                // collections leave it false so the admin's listener mirrors them in as before.
                withContext(Dispatchers.IO) {
                    com.jothivel.chits.data.firebase.AgentCollectionSync.push(
                        context,
                        com.jothivel.chits.data.firebase.AgentCollectionDoc(
                            requestId = requestId,
                            agentId = if (isAgentMode) agentPrefs.getAgentId() else "",
                            agentName = if (isAgentMode) agentPrefs.getAgentName() else "Admin",
                            memberId = member.id,
                            memberName = member.name,
                            groupId = group.id,
                            chitNo = group.registerNo ?: group.id,
                            amountPaise = receipt.amountPaise,
                            mode = receipt.mode,
                            referenceNo = receipt.referenceNo,
                            receiptNo = receipt.receiptNo,
                            notes = notes,
                            businessDate = receipt.businessDate,
                            status = "PAID",
                            syncedToAdmin = !isAgentMode
                        )
                    )
                }
            }.onFailure { error = it.message ?: "Collection could not be saved" }
        }
    }
    Column(Modifier.fillMaxSize()) {
        BrandTopBar("Add Collection", onBack)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { SearchableDropdownField("Customer *", selectedMember?.let { "${it.name} • ${it.id}" }.orEmpty(), members, { "${it.name} ${it.id} ${it.phone.orEmpty()} ${it.city.orEmpty()}" }, { it.name ?: "Unnamed" }, { "${it.id} • ${it.phone ?: "No mobile"} • ${it.city ?: "No area"}" }, Icons.Default.Person) { member -> selectedMember = member; selectedGroup = null } }
            item { SearchableDropdownField("Chit *", selectedGroup?.let { "${it.registerNo} • ${it.name}" }.orEmpty(), eligibleGroups, { "${it.registerNo.orEmpty()} ${it.name.orEmpty()} ${it.branch.orEmpty()}" }, { "${it.registerNo ?: it.id} • ${it.name ?: "Chit"}" }, { "${money(it.chitValue.toLong() / 100)} • ${it.durationMonths} months • ${it.branch ?: "No branch"}" }, Icons.Default.Description) { group -> selectedGroup = group } }
            if (selectedMember != null && eligibleGroups.isEmpty()) item { Text("This customer is not linked to an active chit", color = AccentRed, fontSize = 10.sp) }
            item { CompactDateField("Collection Date *", businessDate, { businessDate=it }, Modifier.fillMaxWidth(), "yyyy-MM-dd") }
            item { Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF7E2), border = BorderStroke(1.dp, AccentGold.copy(alpha = .45f))) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Current Due", fontWeight = FontWeight.Bold); Text(money(dueAmountPaise / 100), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) } } }
            item { PremiumInputField(amount, { amount = it.filter(Char::isDigit).take(10); error = null }, "Amount Received *", Modifier.fillMaxWidth(), leadingIcon = Icons.Default.CurrencyRupee, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
            paymentState?.let { state -> item { Text(state, color = if (state.startsWith("Excess")) AccentRed else if (state == "Full payment") AccentGreen else AccentGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) } }
            item { Text("Payment Mode *", fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Cash", "UPI").forEach { item -> FilterChip(mode == item, { mode = item }, { Text(item) }, modifier = Modifier.weight(1f)) } } }
            if (mode != "Cash") item { FormField("Reference / UTR *", reference, { reference = it; error = null }, Icons.Default.Tag) }
            item { PremiumInputField(notes, { notes = it.take(150) }, "Notes (Optional)", Modifier.fillMaxWidth().height(86.dp), leadingIcon = Icons.Default.Notes, singleLine = false, minLines = 3) }
            error?.let { message -> item { Text(message, color = AccentRed, fontSize = 10.sp) } }
            item { Button(onClick = { if (dueAmountPaise > 0 && enteredPaise > dueAmountPaise) confirmExcess = true else saveCollection() }, enabled = !saving && selectedMember != null && selectedGroup != null && enteredRupees > 0, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary), shape = RoundedCornerShape(10.dp)) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else { Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Save & Send Receipt", fontWeight = FontWeight.SemiBold) } } }
        }
    }
    if (confirmExcess) AlertDialog(onDismissRequest = { confirmExcess = false }, title = { Text("Amount is higher than due") }, text = { Text("Received ${money(enteredPaise / 100)}. Current due is ${money(dueAmountPaise / 100)}. Extra amount will be saved as advance.") }, confirmButton = { Button(onClick = { confirmExcess = false; saveCollection() }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text("Save as advance") } }, dismissButton = { TextButton(onClick = { confirmExcess = false }) { Text("Check amount") } })
    savedReceipt?.let { receipt -> ReceiptDialog(customer, selectedGroup?.registerNo.orEmpty(), receipt, { savedReceipt = null }, { savedReceipt = null; onBack() }) }
}

@Composable
private fun FinancialEntryScreen(type: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    val lookup by produceState(initialValue = CollectionLookupData(), context, refreshKey) {
        value = withContext(Dispatchers.IO) { val db=AppDatabase.getDatabase(context); CollectionLookupData(db.memberDao().getAllMembersSync().filter { it.isActive }, db.groupDao().getAllGroupsSync().filter { it.status=="ACTIVE" }, db.membershipDao().getAllActiveSync()) }
    }
    val entries by produceState(initialValue = emptyList<FinancialTransactionEntity>(), context, refreshKey) {
        value = withContext(Dispatchers.IO) { AppDatabase.getDatabase(context).financialTransactionDao().getAllSync().filter { it.type==type }.take(30) }
    }
    var member by remember { mutableStateOf<MemberEntity?>(null) }
    var group by remember { mutableStateOf<ChitGroupEntity?>(null) }
    var amount by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var reverseEntry by remember { mutableStateOf<FinancialTransactionEntity?>(null) }
    var reverseReason by remember { mutableStateOf("") }
    val groups = remember(member, lookup) { val ids=lookup.memberships.filter { it.memberId==member?.id }.mapTo(hashSetOf()) { it.groupId }; lookup.groups.filter { it.id in ids } }
    val title = if (type=="SETTLEMENT") "Add Settlement" else "Add Delivery"
    fun save() {
        val paise=(amount.toLongOrNull() ?: 0L)*100
        error=when { member==null -> "Select a customer"; group==null -> "Select a linked chit"; paise<=0 -> "Enter a valid amount"; mode!="Cash" && reference.isBlank() -> "Reference / UTR is required"; else -> null }
        if(error!=null || saving) return
        saving=true
        scope.launch { val result=withContext(Dispatchers.IO) { runCatching { FinancialService.record(AppDatabase.getDatabase(context), UUID.randomUUID().toString(), type, member!!.id, group!!.id, paise, mode, reference, notes) } }; saving=false; result.onSuccess { amount=""; reference=""; notes=""; error=null; refreshKey++; android.widget.Toast.makeText(context, "$title saved", android.widget.Toast.LENGTH_SHORT).show() }.onFailure { error=it.message } }
    }
    Column(Modifier.fillMaxSize().background(MaroonBackground)) {
        BrandTopBar(title, back=onBack)
        LazyColumn(contentPadding=PaddingValues(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item { SearchableDropdownField("Customer *", member?.let { "${it.name} • ${it.id}" }.orEmpty(), lookup.members, { "${it.name} ${it.id} ${it.phone}" }, { it.name ?: it.id }, { "${it.id} • ${it.phone.orEmpty()}" }, Icons.Default.Person) { member=it; group=null } }
            item { SearchableDropdownField("Chit *", group?.let { "${it.registerNo} • ${it.name}" }.orEmpty(), groups, { "${it.registerNo} ${it.name}" }, { it.registerNo ?: it.id }, { it.name ?: "Chit" }, Icons.Default.Savings) { group=it } }
            item { PremiumInputField(amount, { amount=it.filter(Char::isDigit).take(10); error=null }, "Amount *", Modifier.fillMaxWidth(), leadingIcon=Icons.Default.CurrencyRupee, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)) }
            item { Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("Cash","UPI","Bank").forEach { value -> FilterChip(mode==value, { mode=value }, { Text(value) }, Modifier.weight(1f)) } } }
            if(mode!="Cash") item { PremiumInputField(reference, { reference=it }, "Reference / UTR *", Modifier.fillMaxWidth(), leadingIcon=Icons.Default.Tag) }
            item { PremiumInputField(notes, { notes=it.take(150) }, "Notes", Modifier.fillMaxWidth().height(78.dp), leadingIcon=Icons.Default.Notes, singleLine=false, minLines=2) }
            error?.let { item { Text(it, color=AccentRed, fontSize=10.sp) } }
            item { Button(::save, enabled=!saving, modifier=Modifier.fillMaxWidth().height(46.dp), colors=ButtonDefaults.buttonColors(containerColor=MaroonPrimary)) { Text(if(saving) "Saving…" else "Save ${type.lowercase().replaceFirstChar(Char::uppercase)}") } }
            item { Text("Recent entries", fontWeight=FontWeight.Bold, fontSize=13.sp, modifier=Modifier.padding(top=6.dp)) }
            if(entries.isEmpty()) item { EmptyCreatedList("No ${type.lowercase()} entries yet") }
            items(entries, key={it.id}) { entry -> Surface(shape=RoundedCornerShape(11.dp), color=Color.White, border=BorderStroke(1.dp, DividerGray)) { Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${entry.memberId} • ${entry.groupId}", fontSize=11.sp, fontWeight=FontWeight.SemiBold); Text(money(entry.amountPaise/100), fontSize=13.sp, color=if(entry.status=="POSTED") AccentGreen else TextGray, fontWeight=FontWeight.Bold); Text("${entry.mode} • ${if(entry.status=="POSTED") "Posted" else "Reversed"}", fontSize=8.sp, color=TextGray) }; if(entry.status=="POSTED") TextButton({ reverseEntry=entry }) { Text("Reverse", color=AccentRed, fontSize=9.sp) } } } }
        }
    }
    reverseEntry?.let { entry -> AlertDialog(onDismissRequest={reverseEntry=null}, title={Text("Reverse entry?")}, text={PremiumInputField(reverseReason,{reverseReason=it.take(100)},"Reason *",Modifier.fillMaxWidth(),leadingIcon=Icons.Default.Notes)}, confirmButton={Button(onClick={ scope.launch { val result=withContext(Dispatchers.IO){runCatching{FinancialService.reverse(AppDatabase.getDatabase(context),entry.id,reverseReason)}}; result.onSuccess{reverseEntry=null;reverseReason="";refreshKey++}.onFailure{error=it.message} } }, colors=ButtonDefaults.buttonColors(containerColor=AccentRed)){Text("Reverse")}}, dismissButton={TextButton({reverseEntry=null}){Text("Cancel")}}) }
}

@Composable
private fun <T> SearchableDropdownField(
    label: String,
    selectedText: String,
    items: List<T>,
    searchText: (T) -> String,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    leadingIcon: ImageVector,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) { if (query.isBlank()) items else items.filter { searchText(it).contains(query.trim(), true) } }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it; if (!it) query = "" }) {
        PremiumInputField(
            value = selectedText,
            onValueChange = {},
            label = label,
            readOnly = true,
            leadingIcon = leadingIcon,
            trailingContent = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; query = "" },
            modifier = Modifier.background(Color.White).heightIn(max = 300.dp)
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp)) {
                PremiumInputField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search",
                    placeholder = "Search ${label.removeSuffix(" *")}",
                    leadingIcon = Icons.Default.Search,
                    trailingContent = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(15.dp)) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("No matching results", color = TextGray, fontSize = 11.sp) }, onClick = {}, enabled = false)
            } else {
                filtered.forEach { item ->
                    val selected = selectedText.isNotBlank() && selectedText.contains(itemTitle(item).substringBefore(" • "), true)
                    DropdownMenuItem(
                        text = { Column { Text(itemTitle(item), fontWeight = FontWeight.Medium, fontSize = 11.sp, maxLines = 1); Text(itemSubtitle(item), color = TextGray, fontSize = 9.sp, maxLines = 1) } },
                        leadingIcon = { if (selected) Icon(Icons.Default.Check, null, tint = MaroonPrimary, modifier = Modifier.size(17.dp)) else Spacer(Modifier.size(17.dp)) },
                        onClick = { onSelected(item); expanded = false; query = "" },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable private fun FormField(label: String, value: String, onChange: (String) -> Unit, icon: ImageVector) = PremiumInputField(value, onChange, label, Modifier.fillMaxWidth(), leadingIcon = icon, trailingContent = { Icon(Icons.Default.ArrowDropDown, null) })

@Composable
private fun ReceiptDialog(customer: String, chitNo: String, receipt: SavedCollection, dismiss: () -> Unit, done: () -> Unit) {
    val context = LocalContext.current
    fun share() {
        ReceiptPdfHelper.share(context,customer,chitNo,receipt).onFailure { android.widget.Toast.makeText(context,"Invoice could not be created: ${it.message}",android.widget.Toast.LENGTH_LONG).show() }
    }
    AlertDialog(onDismissRequest = dismiss, icon = { Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(48.dp)) }, title = { Text("Collection saved") }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(customer, fontWeight = FontWeight.Bold); Text(money(receipt.amountPaise / 100), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaroonPrimary); Text("${receipt.mode} • ${receipt.receiptNo}", color = TextGray, fontSize = 11.sp) } }, confirmButton = { Button(onClick = done, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text("Done") } }, dismissButton = { TextButton(onClick = ::share) { Text("Send PDF Invoice") } })
}

@Composable
private fun CompactNewChitScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    val groups by produceState(initialValue = emptyList<ChitGroupEntity>(), context, refreshKey) {
        value = withContext(Dispatchers.IO) { AppDatabase.getDatabase(context).groupDao().getAllGroupsSync().asReversed() }
    }
    val progress by produceState(initialValue=emptyMap<String,GroupProgress>(),groups,refreshKey){value=withContext(Dispatchers.IO){val db=AppDatabase.getDatabase(context);val payments=db.paymentDao().getAllPaymentsSync();groups.associate{g->val start=listOf("dd-MMM-yyyy","yyyy-MM-dd").firstNotNullOfOrNull{p->runCatching{SimpleDateFormat(p,Locale.ENGLISH).parse(g.startDate)}.getOrNull()};val elapsed=start?.let{s->val a=Calendar.getInstance().apply{time=s};val b=Calendar.getInstance();((b.get(Calendar.YEAR)-a.get(Calendar.YEAR))*12+b.get(Calendar.MONTH)-a.get(Calendar.MONTH)+1).coerceIn(0,g.durationMonths)}?:0;val next=start?.let{s->SimpleDateFormat("dd MMM yy",Locale.ENGLISH).format(Calendar.getInstance().apply{time=s;add(Calendar.MONTH,elapsed.coerceAtMost((g.durationMonths-1).coerceAtLeast(0)))}.time)}?:"-";g.id to GroupProgress(db.membershipDao().countActiveForGroupSync(g.id),payments.filter{it.groupId==g.id}.sumOf{it.amountPaid}/100,elapsed,next)}}}
    var chitNo by remember { mutableStateOf("") }
    var chitName by remember { mutableStateOf("") }
    var valueText by remember { mutableStateOf("") }
    var monthsText by remember { mutableStateOf("20") }
    var branch by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(Date())) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val chitValue = valueText.replace(",", "").toLongOrNull() ?: 0L
    val months = monthsText.toIntOrNull() ?: 0

    fun save() {
        val validStartDate = runCatching { SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).apply { isLenient = false }.parse(startDate.trim()) }.getOrNull()
        error = when {
            chitNo.isBlank() -> "Chit number is required"
            chitName.isBlank() -> "Chit name is required"
            chitValue <= 0 -> "Enter a valid chit value"
            chitValue > Int.MAX_VALUE / 100L -> "Chit value is too large"
            months !in 1..100 -> "Months must be between 1 and 100"
            branch.isBlank() -> "Branch is required"
            validStartDate == null -> "Start date must be like 01-Sep-2026"
            else -> null
        }
        if (error != null) return
        saving = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val db = AppDatabase.getDatabase(context)
                    if (db.groupDao().getAllGroupsSync().any { it.registerNo.equals(chitNo.trim(), true) }) throw IllegalArgumentException("Chit number already exists")
                    val id = "G-${UUID.randomUUID()}"
                    val valuePaise = (chitValue * 100).toInt()
                    val group = ChitGroupEntity().apply {
                        this.id = id; name = chitName.trim(); registerNo = chitNo.trim(); this.chitValue = valuePaise
                        durationMonths = months; subscriberCount = months; this.branch = branch.trim(); this.startDate = startDate.trim(); status = "ACTIVE"
                    }
                    val installmentAmount = valuePaise / months
                    val installments = (1..months).map { number -> InstallmentEntity().apply {
                        this.id = "$id-I$number"; groupId = id; installmentNo = number; baseAmount = installmentAmount
                        kasaruAmount = 0; payoutAmount = null; auctionDate = null; status = "UPCOMING"; winningMemberId = null
                    } }
                    db.runInTransaction {
                        db.groupDao().insertGroup(group)
                        db.installmentDao().insertAll(installments)
                        db.activityLogDao().insertLog(ActivityLogEntity(actionType = "GROUP_CREATED", title = "New Chit Created", description = "${group.registerNo} - ${group.name}"))
                    }
                }
            }
            saving = false
            result.onSuccess {
                android.widget.Toast.makeText(context, "Chit created", android.widget.Toast.LENGTH_SHORT).show()
                chitNo = ""; chitName = ""; valueText = ""; branch = ""; monthsText = "20"; error = null
                refreshKey++
            }
                .onFailure { error = it.message ?: "Unable to save chit" }
        }
    }

    Column(Modifier.fillMaxSize().background(MaroonBackground)) {
        BrandTopBar("New Chit", back = onBack)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactFormField("Chit No *", chitNo, { chitNo = it }, Modifier.weight(1f))
                CompactFormField("Chit Name *", chitName, { chitName = it }, Modifier.weight(1.35f))
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactFormField("Chit Value ₹ *", valueText, { valueText = it.filter { c -> c.isDigit() || c == ',' } }, Modifier.weight(1.35f), KeyboardType.Number)
                CompactFormField("Months / Members *", monthsText, { monthsText = it.filter(Char::isDigit) }, Modifier.weight(1f), KeyboardType.Number)
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactFormField("Branch *", branch, { branch = it }, Modifier.weight(1f))
                CompactDateField("Start Date *", startDate, { startDate = it }, Modifier.weight(1f))
            } }
            if (chitValue > 0 && months > 0) item {
                Text("Installment: ${money(chitValue / months)} × $months members", color = TextGray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 2.dp))
            }
            error?.let { message -> item { Text(message, color = AccentRed, fontSize = 10.sp) } }
            item { Button(onClick = ::save, enabled = !saving, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) else Text("Create Chit") } }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Chit Groups", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${groups.size} groups", fontSize = 10.sp, color = TextGray)
                }
            }
            if (groups.isEmpty()) item { EmptyCreatedList("No chit groups created yet") }
            items(groups, key = { it.id }) { group -> CompactGroupCard(group,progress[group.id]?:GroupProgress()) }
        }
    }
}

@Composable
private fun CompactAddMemberScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    val groups by produceState(initialValue = emptyList<ChitGroupEntity>(), context, refreshKey) { value = withContext(Dispatchers.IO) { AppDatabase.getDatabase(context).groupDao().getAllGroupsSync().filter { it.status == "ACTIVE" } } }
    val members by produceState(initialValue = emptyList<MemberEntity>(), context, refreshKey) { value = withContext(Dispatchers.IO) { AppDatabase.getDatabase(context).memberDao().getAllMembersSync().asReversed() } }
    val groupsById = remember(groups) { groups.associateBy { it.id } }
    var selectedGroup by remember { mutableStateOf<ChitGroupEntity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var customerCode by remember { mutableStateOf("C-${System.currentTimeMillis().toString().takeLast(5)}") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ticket by remember { mutableStateOf("") }
    var installment by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf(SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(Date())) }
    var dueDate by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun selectGroup(group: ChitGroupEntity) {
        selectedGroup = group
        installment = if (group.durationMonths > 0) ((group.chitValue.toLong() / 100) / group.durationMonths).toString() else ""
        dueDate = runCatching {
            val format = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            format.format(Calendar.getInstance().apply { time = format.parse(joiningDate)!!; add(Calendar.MONTH, group.durationMonths) }.time)
        }.getOrDefault("")
        menuOpen = false
    }

    fun save() {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).apply { isLenient = false }
        val parsedJoining = runCatching { dateFormat.parse(joiningDate.trim()) }.getOrNull()
        val parsedDue = runCatching { dateFormat.parse(dueDate.trim()) }.getOrNull()
        val installmentValue = installment.toLongOrNull() ?: 0L
        error = when {
            selectedGroup == null -> "Select a chit"
            customerCode.isBlank() -> "Customer code is required"
            name.isBlank() -> "Customer name is required"
            phone.length != 10 -> "Enter a valid 10-digit mobile"
            ticket.isBlank() -> "Ticket / serial is required"
            installmentValue <= 0 -> "Enter a valid installment"
            parsedJoining == null || parsedDue == null -> "Dates must be like 01-Sep-2026"
            parsedDue.before(parsedJoining) -> "Due date cannot be before joining date"
            else -> null
        }
        if (error != null) return
        saving = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                val db = AppDatabase.getDatabase(context)
                val existingMember = db.memberDao().getAllMembersSync().firstOrNull { it.id.equals(customerCode.trim(), true) }
                val samePhone = db.memberDao().getAllMembersSync().firstOrNull { it.phone == phone && !it.id.equals(customerCode.trim(), true) }
                if (samePhone != null) throw IllegalArgumentException("Mobile number already belongs to ${samePhone.name} (${samePhone.id})")
                if (db.membershipDao().countActiveForGroupSync(selectedGroup!!.id) >= selectedGroup!!.subscriberCount) throw IllegalArgumentException("This chit is full (${selectedGroup!!.subscriberCount} members)")
                if (db.membershipDao().countActiveTicketSync(selectedGroup!!.id, ticket.trim()) > 0) throw IllegalArgumentException("Ticket / serial already exists in this chit")
                val member = existingMember ?: MemberEntity().apply {
                    id = customerCode.trim(); this.name = name.trim(); this.phone = phone; photoUrl = null
                    nomineeName = null; nomineePhone = null; role = "MEMBER"; isActive = true; dob = null; gender = null
                    addressLine = address.trim(); this.city = city.trim(); state = "Tamil Nadu"; pincode = null
                    aadhaarNoEncrypted = null; panNo = null; aadhaarDocumentPath = null; panDocumentPath = null
                    selectedChitId = selectedGroup!!.id; ticketNo = ticket.trim(); installmentAmount = installment.trim()
                    this.joiningDate = joiningDate.trim(); this.dueDate = dueDate.trim(); nomineeRelationship = null
                }
                if (db.membershipDao().getSync(member.id, selectedGroup!!.id) != null) throw IllegalArgumentException("Customer is already added to this chit")
                val joiningDateValue = joiningDate.trim()
                val dueDateValue = dueDate.trim()
                val membership = ChitMembershipEntity().apply {
                    id = "${member.id}:${selectedGroup!!.id}"
                    memberId = member.id
                    groupId = selectedGroup!!.id
                    ticketNo = ticket.trim()
                    installmentAmountPaise = installmentValue * 100
                    joiningDate = joiningDateValue
                    dueDate = dueDateValue
                    isActive = true
                }
                db.runInTransaction {
                    if (existingMember == null) {
                        db.memberDao().insertMember(member)
                    } else {
                        db.memberDao().updateAddress(member.id, member.addressLine)
                        db.memberDao().updateCity(member.id, member.city)
                    }
                    db.membershipDao().insert(membership)
                    db.activityLogDao().insertLog(ActivityLogEntity(actionType = "MEMBER_ADDED", title = "Member Added", description = "${member.name} joined ${selectedGroup!!.registerNo}"))
                }
            } }
            saving = false
            result.onSuccess {
                android.widget.Toast.makeText(context, "Member added", android.widget.Toast.LENGTH_SHORT).show()
                customerCode = "C-${System.currentTimeMillis().toString().takeLast(5)}"
                name = ""; phone = ""; ticket = ""; address = ""; city = ""; error = null
                selectedGroup = null; installment = ""; dueDate = ""
                refreshKey++
            }
                .onFailure { error = it.message ?: "Unable to save member" }
        }
    }

    Column(Modifier.fillMaxSize().background(MaroonBackground)) {
        BrandTopBar("Add Member", back = onBack)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(9.dp)) {
                        Text(selectedGroup?.let { "${it.registerNo} • ${it.name}" } ?: "Select Chit *", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start, fontSize = 11.sp)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        groups.forEach { group -> DropdownMenuItem(text = { Text("${group.registerNo} • ${group.name}", fontSize = 11.sp) }, onClick = { selectGroup(group) }) }
                    }
                }
            }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactFormField("Customer Code *", customerCode, { customerCode = it }, Modifier.weight(1f))
                CompactFormField("Ticket / Serial *", ticket, { ticket = it }, Modifier.weight(1f), KeyboardType.Number)
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactFormField("Customer Name *", name, { name = it }, Modifier.weight(1.25f))
                CompactFormField("Mobile *", phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.weight(1f), KeyboardType.Phone)
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactFormField("Installment ₹ *", installment, { installment = it.filter(Char::isDigit) }, Modifier.weight(1f), KeyboardType.Number)
                CompactFormField("City", city, { city = it }, Modifier.weight(1f))
            } }
            item { CompactFormField("Address", address, { address = it }, Modifier.fillMaxWidth()) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactDateField("Joining Date *", joiningDate, { joiningDate = it; selectedGroup?.let(::selectGroup) }, Modifier.weight(1f))
                CompactDateField("Due Date *", dueDate, { dueDate = it }, Modifier.weight(1f))
            } }
            error?.let { message -> item { Text(message, color = AccentRed, fontSize = 10.sp) } }
            item { Button(onClick = ::save, enabled = !saving && groups.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) else Text("Add Member") } }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Members", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${members.size} members", fontSize = 10.sp, color = TextGray)
                }
            }
            if (members.isEmpty()) item { EmptyCreatedList("No members added yet") }
            items(members, key = { it.id }, contentType = { "member_card" }) { member ->
                CompactMemberCard(member, member.selectedChitId?.let(groupsById::get))
            }
        }
    }
}

@Composable
private fun EmptyCreatedList(message: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
        Text(message, modifier = Modifier.padding(18.dp), color = TextGray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun CompactGroupCard(group: ChitGroupEntity, progress: GroupProgress) {
    var expanded by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth().clickable { expanded = !expanded }, shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, MaroonPrimary.copy(alpha = .12f))) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).background(MaroonSurfaceLight, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Savings, null, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.padding(start = 9.dp).weight(1f)) {
                    Text("${group.registerNo} • ${group.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(group.status ?: "ACTIVE", fontSize = 8.sp, color = AccentGreen, modifier = Modifier.background(AccentGreen.copy(alpha = .10f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp))
                Spacer(Modifier.width(8.dp))
                Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, tint = MaroonPrimary)
            }
            if (expanded) {
                Column(Modifier.padding(start = 43.dp, top = 4.dp)) {
                    Text("${money(group.chitValue.toLong() / 100)} • ${group.durationMonths} months • ${group.branch.orEmpty()}", fontSize = 9.sp, color = TextGray, maxLines = 1)
                    Text("Started ${group.startDate.orEmpty()}", fontSize = 8.sp, color = TextGray)
                    Text("${progress.members}/${group.subscriberCount} members • ${money(progress.collected)} collected",fontSize=8.sp,color=MaroonPrimary,maxLines=1)
                    Text("${progress.elapsed}/${group.durationMonths} months • Next due ${progress.nextDue}",fontSize=8.sp,color=TextGray,maxLines=1)
                }
            }
        }
    }
}

@Composable
private fun CompactMemberCard(member: MemberEntity, group: ChitGroupEntity?) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxWidth().clickable { android.widget.Toast.makeText(context, "Member details can be viewed from Dashboard", android.widget.Toast.LENGTH_SHORT).show() }, shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, MaroonPrimary.copy(alpha = .12f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                Text(member.name?.firstOrNull()?.uppercase() ?: "M", color = MaroonPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Column(Modifier.padding(start = 9.dp).weight(1f)) {
                Text(member.name ?: "Unnamed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${member.phone.orEmpty()} • ${member.city.orEmpty()}", fontSize = 9.sp, color = TextGray, maxLines = 1)
                val chitLine = buildString {
                    append(group?.registerNo ?: "No chit assigned")
                    member.ticketNo?.takeIf { it.isNotBlank() }?.let { append(" • Ticket $it") }
                    member.installmentAmount?.takeIf { it.isNotBlank() }?.let { append(" • ₹$it") }
                }
                Text(chitLine, fontSize = 8.sp, color = TextGray, maxLines = 1)
            }
            if (member.isActive) Text("Active", fontSize = 8.sp, color = AccentGreen)
        }
    }
}

@Composable
private fun CompactFormField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    PremiumInputField(
        value = value,
        onValueChange = onChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.height(54.dp)
    )
}

@Composable
private fun CompactDateField(label:String,value:String,onChange:(String)->Unit,modifier:Modifier,pattern:String="dd-MMM-yyyy") {
    val context=LocalContext.current
    fun openPicker(){
        val calendar=Calendar.getInstance()
        listOf(pattern,"dd-MMM-yyyy","yyyy-MM-dd").firstNotNullOfOrNull { p->runCatching{SimpleDateFormat(p,Locale.ENGLISH).apply{isLenient=false}.parse(value)}.getOrNull() }?.let{calendar.time=it}
        android.app.DatePickerDialog(context,{_,year,month,day->calendar.set(year,month,day);onChange(SimpleDateFormat(pattern,Locale.ENGLISH).format(calendar.time))},calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    PremiumInputField(value,{},label,modifier.height(54.dp),leadingIcon=Icons.Default.CalendarMonth,trailingContent={IconButton(::openPicker,modifier=Modifier.size(36.dp)){Icon(Icons.Default.EditCalendar,"Change date",tint=MaroonPrimary,modifier=Modifier.size(18.dp))}},readOnly=true)
}

@Composable
private fun PendingScreen(
    onBack: () -> Unit,
    onOpenLedger: (DueCustomer) -> Unit,
    onCollect: (DueCustomer) -> Unit
) {
    var selectedArea by remember { mutableStateOf<String?>(null) }
    var selectedChit by remember { mutableStateOf<String?>(null) }
    var selectedAging by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var activeResultIndex by remember { mutableIntStateOf(0) }
    var expandedDueKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val roomDues by produceState(initialValue = emptyList<DueCustomer>(), context) {
        value = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val membersById = db.memberDao().getAllMembersSync().associateBy { it.id }
            val groupsById = db.groupDao().getAllGroupsSync().associateBy { it.id }
            val contacts = db.activityLogDao().getAllSync().filter { it.actionType=="CUSTOMER_CONTACTED" }.groupBy { it.description.substringBefore('|') }
            db.membershipDao().getAllActiveSync().mapNotNull { membership ->
                val member = membersById[membership.memberId] ?: return@mapNotNull null
                val group = groupsById[membership.groupId] ?: return@mapNotNull null
                val breakdown = CollectionService.calculateDueBreakdown(db, member.id, group.id)
                val paid = breakdown.paidPaise / 100
                val pending = breakdown.pendingPaise / 100
                val payable = breakdown.payablePaise / 100
                if (pending == 0L) null else {
                    DueCustomer(
                        name = member.name ?: "Unnamed",
                        code = member.id,
                        phone = member.phone.orEmpty(),
                        area = listOfNotNull(member.addressLine?.takeIf { it.isNotBlank() }, member.city?.takeIf { it.isNotBlank() }).joinToString(", "),
                        filterArea = member.city?.trim()?.takeIf { it.isNotBlank() } ?: member.addressLine?.trim().orEmpty(),
                        chit = listOfNotNull(group.registerNo?.takeIf { it.isNotBlank() }, group.name?.takeIf { it.isNotBlank() }).joinToString(" - "),
                        chitValue = group.chitValue.toLong() / 100,
                        payable = payable,
                        paid = paid,
                        pending = pending,
                        lastPaid = breakdown.lastPaidAt?.let { SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH).format(Date(it)) } ?: "Not paid",
                        dueDate = breakdown.earliestDueDate,
                        installments = breakdown.pendingInstallments.map { "$it TH" },
                        agent = member.role ?: "MEMBER",
                        overdueDays = breakdown.overdueDays,
                        recentContactAt = contacts[member.id]?.maxOfOrNull { it.timestamp } ?: 0L
                    )
                }
            }
        }
    }
    val areaOptions = remember(roomDues) { roomDues.map { it.filterArea }.filter { it.isNotBlank() }.distinct().sorted() }
    val chitOptions = remember(roomDues) { roomDues.map { it.chit }.filter { it.isNotBlank() }.distinct().sorted() }
    val agingOptions = listOf("1–30 days", "31–60 days", "61–90 days", "90+ days")
    val hasFilters = selectedArea != null || selectedChit != null || selectedAging != null
    val filtered = remember(roomDues, query, selectedArea, selectedChit, selectedAging) {
        roomDues.filter {
            (it.name.contains(query, true) || it.phone.contains(query) || it.chit.contains(query, true) || it.area.contains(query, true) || it.agent.contains(query, true) || it.installments.any { month -> month.contains(query, true) }) &&
                (selectedArea == null || it.filterArea == selectedArea) &&
                (selectedChit == null || it.chit == selectedChit) &&
                when (selectedAging) {
                    "1–30 days" -> it.overdueDays in 1..30
                    "31–60 days" -> it.overdueDays in 31..60
                    "61–90 days" -> it.overdueDays in 61..90
                    "90+ days" -> it.overdueDays > 90
                    else -> true
                }
            }.sortedWith(compareByDescending<DueCustomer> { it.overdueDays >= 90 }.thenByDescending { it.overdueDays }.thenByDescending { it.pending }.thenBy { if(it.recentContactAt==0L) 0 else 1 })
        }
    val activeDue = filtered.getOrNull(activeResultIndex)

    fun goToPendingResult(direction: Int) {
        if (filtered.isEmpty()) return
        activeResultIndex = (activeResultIndex + direction + filtered.size) % filtered.size
        scope.launch { listState.animateScrollToItem(activeResultIndex + 2) }
    }

    fun downloadPending() {
        val csv = buildString {
            appendLine("Customer,Code,Phone,Area,Agent,Chit,Payable,Paid,Pending,Pending Months")
            filtered.forEach { due -> appendLine("${due.name},${due.code},${due.phone},${due.area},${due.agent},${due.chit},${due.payable},${due.paid},${due.pending},${due.installments.joinToString("|")}") }
        }
        scope.launch {
            val fileName="Pending-${SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(Date())}.csv"
            val result=withContext(Dispatchers.IO){CsvDownloadHelper.save(context,fileName,csv)}
            android.widget.Toast.makeText(context,result.fold({"Downloaded to $it"},{"Download failed: ${it.message}"}),android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(query, selectedArea, selectedChit, selectedAging) {
        activeResultIndex = 0
        if (filtered.isNotEmpty() && query.isNotBlank()) listState.animateScrollToItem(2)
    }
    Column(Modifier.fillMaxSize()) {
        BrandTopBar("Pending", onBack, Icons.Default.FilterAlt, onAction = { showFilters = !showFilters })
        DetailFindToolbar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Find name, area, chit or pending month",
            resultCount = if (query.isBlank()) 0 else filtered.size,
            activeResultIndex = activeResultIndex.coerceAtMost((filtered.size - 1).coerceAtLeast(0)),
            onPrevious = { goToPendingResult(-1) },
            onNext = { goToPendingResult(1) },
            onDownload = { downloadPending() },
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
        LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = MaroonPrimary.copy(alpha = .08f), spotColor = MaroonPrimary.copy(alpha = .13f))
                        .background(Brush.linearGradient(listOf(Color.White, Color(0xFFFFECEE))), RoundedCornerShape(16.dp))
                        .border(1.dp, MaroonPrimary.copy(alpha = .12f), RoundedCornerShape(16.dp))
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(MaroonPrimary, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text("LIVE DUES", color = MaroonPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                            Text(money(filtered.sumOf { it.pending }), color = AccentRed, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = .8f), border = BorderStroke(1.dp, MaroonPrimary.copy(alpha = .10f))) {
                            Text("${filtered.size} customers", color = TextGray, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                        }
                    }
                }
            }
            item {
                androidx.compose.animation.AnimatedVisibility(visible = showFilters || hasFilters) {
                    Surface(shape = RoundedCornerShape(11.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
                        Column(Modifier.padding(horizontal = 7.dp, vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Filter pending", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (hasFilters) TextButton(onClick = { selectedArea = null; selectedChit = null; selectedAging = null }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 7.dp)) { Text("Clear", fontSize = 9.sp) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                PendingFilterMenu("Area", selectedArea, areaOptions, Icons.Default.LocationOn, { selectedArea = it }, Modifier.weight(1f))
                                PendingFilterMenu("Chit", selectedChit, chitOptions, Icons.Default.Savings, { selectedChit = it }, Modifier.weight(1f))
                                PendingFilterMenu("Aging", selectedAging, agingOptions, Icons.Default.Schedule, { selectedAging = it }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) item { Text("No customers match this filter", modifier = Modifier.fillMaxWidth().padding(28.dp), color = TextGray) }
            items(filtered, key = { "${it.code}-${it.chit}" }, contentType = { "pending_card" }) { due ->
                val dueKey = "${due.code}-${due.chit}"
                DueCard(
                    due = due,
                    highlighted = query.isNotBlank() && due == activeDue,
                    expanded = expandedDueKey == dueKey,
                    onToggle = { expandedDueKey = if (expandedDueKey == dueKey) null else dueKey },
                    onOpenLedger = { onOpenLedger(due) },
                    onCollect = { onCollect(due) },
                    onCall = { recordCustomerContact(context,due,"CALL"); context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${due.phone}"))) },
                    onWhatsApp = { recordCustomerContact(context,due,"WHATSAPP"); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91${due.phone.filter(Char::isDigit).takeLast(10)}?text=${Uri.encode("Vanakkam ${due.name}, ${due.chit} pending ${money(due.pending)}. Pending installments: ${due.installments.joinToString()}. Kindly make the payment. - Jothi Vel Chits")}"))) }
                )
            }
        }
    }
}

@Composable
private fun DueCard(
    due: DueCustomer,
    highlighted: Boolean = false,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenLedger: () -> Unit,
    onCollect: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val cardColor by androidx.compose.animation.animateColorAsState(if (highlighted) Color(0xFFE3E5E8) else Color.White, label = "dueHighlight")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        border = BorderStroke(1.dp, if (highlighted) MaroonPrimary.copy(alpha = .40f) else MaroonPrimary.copy(alpha = .12f))
    ) {
        Column(Modifier.padding(horizontal=11.dp,vertical=10.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${due.name} (${due.code})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onOpenLedger))
                    Text("☎ ${due.phone}", fontSize = 10.sp)
                    Text("⌖ ${due.area} • Agent ${due.agent}", fontSize = 10.sp, color = TextGray)
                    Text("${due.chit} • ${money(due.chitValue)}", fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    OverdueBadge(due.overdueDays)
                    Box(Modifier.size(27.dp).background(if (expanded) MaroonSurfaceLight else Color(0xFFF4F1F1), CircleShape).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            if (expanded) "Hide pending details" else "Show pending details",
                            tint = if (expanded) MaroonPrimary else TextGray,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column {
                    Divider(Modifier.padding(vertical = 9.dp), color = DividerGray)
                    Row { MiniDue("Payable", due.payable, Color.Black, Modifier.weight(1f)); MiniDue("Paid", due.paid, Color.Black, Modifier.weight(1f)); MiniDue("Pending", due.pending, AccentRed, Modifier.weight(1f)) }
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Last Paid: ${due.lastPaid}", color = TextGray, fontSize = 10.sp); Text("Due: ${due.dueDate}", fontSize = 10.sp) }
                    Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { due.installments.forEach { Text(it, color = AccentRed, fontSize = 9.sp, modifier = Modifier.background(AccentRedLight, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) } }
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) { TextButton(onClick = onCall) { Icon(Icons.Default.Call, null); Text("Call") }; TextButton(onClick = onWhatsApp) { Icon(Icons.Default.Chat, null, tint = AccentGreen); Text("WhatsApp", color = AccentGreen) }; TextButton(onClick = onCollect) { Icon(Icons.Default.Add, null); Text("Collect") } }
                }
            }
        }
    }
}

@Composable
private fun PendingFilterMenu(
    label: String,
    selectedValue: String?,
    options: List<String>,
    icon: ImageVector,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (selectedValue != null) MaroonPrimary else MaroonSurfaceLight.copy(alpha = .55f), shape)
                .border(1.dp, if (selectedValue != null) MaroonPrimary else MaroonPrimary.copy(alpha = .10f), shape)
                .clickable { expanded = true }
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (selectedValue != null) Color.White else MaroonPrimary.copy(alpha = .72f), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(selectedValue ?: label, color = if (selectedValue != null) Color.White else TextGray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, null, tint = if (selectedValue != null) Color.White else TextGray, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (selectedValue != null) DropdownMenuItem(
                text = { Text("All $label", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                onClick = { onSelect(null); expanded = false },
                leadingIcon = { Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp)) }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 11.sp, maxLines = 1) },
                    onClick = { onSelect(option); expanded = false },
                    trailingIcon = if (option == selectedValue) ({ Icon(Icons.Default.Check, null, tint = AccentGreen, modifier = Modifier.size(16.dp)) }) else null
                )
            }
        }
    }
}

@Composable
private fun OverdueBadge(days: Int) {
    val badgeColor = when {
        days >= 180 -> Color(0xFFB20D24)
        days >= 90 -> Color(0xFFD53848)
        else -> Color(0xFFE85A66)
    }
    Row(
        Modifier
            .background(Brush.horizontalGradient(listOf(badgeColor.copy(alpha = .10f), badgeColor.copy(alpha = .18f))), RoundedCornerShape(10.dp))
            .border(1.dp, badgeColor.copy(alpha = .30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Default.Schedule, null, tint = badgeColor, modifier = Modifier.size(12.dp))
        Text("${days}d overdue", color = badgeColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun MiniDue(label: String, value: Long, color: Color, modifier: Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Text(label, fontSize = 10.sp, color = TextGray); Text(money(value), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color) }

private fun recordCustomerContact(context: android.content.Context, due: DueCustomer, channel: String) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        AppDatabase.getDatabase(context).activityLogDao().insertLog(ActivityLogEntity(actionType="CUSTOMER_CONTACTED",title="$channel reminder",description="${due.code}|${due.chit}"))
    }
}

private fun loadOperationalDues(db: AppDatabase): List<DueCustomer> {
    val members=db.memberDao().getAllMembersSync().associateBy { it.id }
    val groups=db.groupDao().getAllGroupsSync().associateBy { it.id }
    val contacts=db.activityLogDao().getAllSync().filter { it.actionType=="CUSTOMER_CONTACTED" }.groupBy { it.description.substringBefore('|') }
    return db.membershipDao().getAllActiveSync().mapNotNull { link ->
        val member=members[link.memberId] ?: return@mapNotNull null
        val group=groups[link.groupId] ?: return@mapNotNull null
        val due=CollectionService.calculateDueBreakdown(db,member.id,group.id)
        if(due.pendingPaise<=0) return@mapNotNull null
        DueCustomer(member.name?:"Unnamed",member.id,member.phone.orEmpty(),listOfNotNull(member.addressLine,member.city).joinToString(", "),member.city.orEmpty(),group.registerNo?:group.id,group.chitValue.toLong()/100,due.payablePaise/100,due.paidPaise/100,due.pendingPaise/100,due.lastPaidAt?.let{SimpleDateFormat("dd-MMM-yy",Locale.ENGLISH).format(Date(it))}?:"Not paid",due.earliestDueDate,due.pendingInstallments.map{"$it TH"},member.role?:"MEMBER",due.overdueDays,contacts[member.id]?.maxOfOrNull{it.timestamp}?:0L)
    }.sortedWith(compareByDescending<DueCustomer>{it.overdueDays>=90}.thenByDescending{it.overdueDays}.thenByDescending{it.pending}.thenBy{if(it.recentContactAt==0L)0 else 1})
}

private data class TodayWorkSnapshot(val dues:List<DueCustomer> = emptyList(), val completed:List<CollectionReceiptEntity> = emptyList(), val settlements:Long=0, val deliveries:Long=0)
private data class GroupProgress(val members:Int=0,val collected:Long=0,val elapsed:Int=0,val nextDue:String="-")

@Composable
private fun TodayWorkScreen(onBack:()->Unit,onProfile:(String)->Unit,onCollect:(String,String)->Unit) {
    val context=LocalContext.current
    val snapshot by produceState(initialValue=TodayWorkSnapshot(),context) { value=withContext(Dispatchers.IO){ val db=AppDatabase.getDatabase(context); val today=CollectionService.todayKey(); val financial=db.financialTransactionDao().getAllSync().filter{it.status=="POSTED" && SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date(it.occurredAt))==today}; TodayWorkSnapshot(loadOperationalDues(db),db.collectionReceiptDao().getRecentSync(Int.MAX_VALUE).filter{it.businessDate==today},financial.filter{it.type=="SETTLEMENT"}.sumOf{it.amountPaise}/100,financial.filter{it.type=="DELIVERY"}.sumOf{it.amountPaise}/100) } }
    val todayLabel=SimpleDateFormat("dd-MMM-yy",Locale.ENGLISH).format(Date())
    val dueToday=snapshot.dues.filter{it.dueDate.equals(todayLabel,true)}
    val overdue=snapshot.dues.filter{it.overdueDays>0}
    Column(Modifier.fillMaxSize().background(MaroonBackground)){ BrandTopBar("Today's Work",back=onBack); LazyColumn(contentPadding=PaddingValues(11.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){WorkMetric("Due Today",dueToday.size.toString(),AccentGold,Modifier.weight(1f));WorkMetric("Overdue",overdue.size.toString(),AccentRed,Modifier.weight(1f));WorkMetric("Completed",snapshot.completed.size.toString(),AccentGreen,Modifier.weight(1f))}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){WorkMetric("Settlement",money(snapshot.settlements),Color(0xFF285A9B),Modifier.weight(1f));WorkMetric("Delivery",money(snapshot.deliveries),AccentGreen,Modifier.weight(1f))}}
        item{Text("Priority customers",fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=5.dp))}
        if(snapshot.dues.isEmpty()) item{ActionEmptyState("No pending dues","All customer collections are up to date",Icons.Default.CheckCircle,null)}
        items(snapshot.dues.take(30),key={"${it.code}-${it.chit}"}){due-> Surface(Modifier.fillMaxWidth().clickable{onProfile(due.code)},shape=RoundedCornerShape(12.dp),color=Color.White,border=BorderStroke(1.dp,if(due.overdueDays>=90)AccentRed.copy(alpha=.35f) else DividerGray)){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${due.name} • ${due.chit}",fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text("${money(due.pending)} pending • ${if(due.overdueDays>0)"${due.overdueDays}d overdue" else "Due today"}",fontSize=9.sp,color=if(due.overdueDays>0)AccentRed else AccentGold);if(due.recentContactAt>0)Text("Contacted ${relativeTime(due.recentContactAt)}",fontSize=8.sp,color=TextGray)};TextButton({onCollect(due.name,due.chit)}){Text("Collect",fontSize=9.sp)}}}}
        item{Text("Today completed",fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=5.dp))}
        if(snapshot.completed.isEmpty()) item{ActionEmptyState("No collections completed yet","Saved collections will appear here",Icons.Default.ReceiptLong,null)}
        items(snapshot.completed.take(15),key={it.id}){r->Surface(Modifier.fillMaxWidth().clickable{onProfile(r.memberId)},shape=RoundedCornerShape(10.dp),color=Color.White,border=BorderStroke(1.dp,DividerGray)){Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("${r.memberId} • ${r.groupId}",fontSize=10.sp);Text(money(r.amountPaidPaise/100),fontSize=11.sp,fontWeight=FontWeight.Bold,color=AccentGreen)}}}
    }}
}

@Composable private fun WorkMetric(label:String,value:String,color:Color,modifier:Modifier)=Surface(modifier,shape=RoundedCornerShape(14.dp),color=Color.White,border=BorderStroke(1.dp,DividerGray),shadowElevation=.5.dp){Column(Modifier.padding(horizontal=10.dp,vertical=9.dp)){Text(label.uppercase(),fontSize=7.sp,color=TextGray,letterSpacing=.4.sp);Spacer(Modifier.height(3.dp));Text(value,fontSize=14.sp,fontWeight=FontWeight.Bold,color=color,maxLines=1)}}
private fun relativeTime(at:Long):String { val hours=((System.currentTimeMillis()-at)/3_600_000L).coerceAtLeast(0); return if(hours<24)"${hours}h ago" else "${hours/24}d ago" }

private data class ProfileSnapshot(val member:MemberEntity?=null,val groups:List<Triple<ChitGroupEntity,ChitMembershipEntity,Long>> = emptyList(),val payments:List<com.jothivel.chits.data.local.entity.PaymentEntity> = emptyList(),val financial:List<FinancialTransactionEntity> = emptyList())

@Composable
private fun CustomerProfileScreen(customerId:String,onBack:()->Unit,onCollect:(String,String)->Unit){
    val context=LocalContext.current
    val data by produceState(initialValue=ProfileSnapshot(),customerId){value=withContext(Dispatchers.IO){val db=AppDatabase.getDatabase(context);val member=db.memberDao().getAllMembersSync().firstOrNull{it.id==customerId};val links=db.membershipDao().getForMemberSync(customerId);ProfileSnapshot(member,links.mapNotNull{l->db.groupDao().getGroupByIdSync(l.groupId)?.let{Triple(it,l,CollectionService.calculateDuePaise(db,customerId,it.id)/100)}},db.paymentDao().getPaymentsByMemberSync(customerId),db.financialTransactionDao().getAllSync().filter{it.memberId==customerId})}}
    val member=data.member
    Column(Modifier.fillMaxSize().background(MaroonBackground)){
        BrandTopBar("Customer Profile",back=onBack)
        if(member==null){
            ActionEmptyState("Customer not found","The customer may be inactive or removed",Icons.Default.PersonOff,onBack)
        } else {
            LazyColumn(contentPadding=PaddingValues(11.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Surface(shape=RoundedCornerShape(16.dp),color=Color.White,border=BorderStroke(1.dp,DividerGray),shadowElevation=1.dp){Column(Modifier.padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).background(MaroonSurfaceLight,CircleShape),contentAlignment=Alignment.Center){Text(member.name?.take(1)?.uppercase()?:"C",color=MaroonPrimary,fontWeight=FontWeight.Bold,fontSize=16.sp)};Column(Modifier.padding(start=10.dp).weight(1f)){Text(member.name ?: "Unnamed",fontSize=15.sp,fontWeight=FontWeight.Bold);Text("${member.phone.orEmpty()} • ${member.city.orEmpty()}",fontSize=9.sp,color=TextGray);Text(member.addressLine.orEmpty(),fontSize=8.sp,color=TextGray,maxLines=1)}};Divider(Modifier.padding(vertical=7.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){TextButton({context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${member.phone}")))}){Icon(Icons.Default.Call,null,modifier=Modifier.size(17.dp));Text("Call",fontSize=10.sp)};TextButton({context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/91${member.phone.orEmpty().filter(Char::isDigit).takeLast(10)}")))}){Icon(Icons.Default.Chat,null,tint=AccentGreen,modifier=Modifier.size(17.dp));Text("WhatsApp",color=AccentGreen,fontSize=10.sp)}}}}}
        item{Text("Joined chits",fontSize=13.sp,fontWeight=FontWeight.Bold)}
        if(data.groups.isEmpty())item{ActionEmptyState("No active chit","Add this customer to a chit to start collection",Icons.Default.GroupAdd,null)}
        items(data.groups,key={it.first.id}){(g,l,due)->Surface(shape=RoundedCornerShape(12.dp),color=Color.White,border=BorderStroke(1.dp,DividerGray)){Column(Modifier.padding(10.dp)){Row{Column(Modifier.weight(1f)){Text("${g.registerNo} • ${g.name}",fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text("Ticket ${l.ticketNo} • ${money(g.chitValue.toLong()/100)}",fontSize=9.sp,color=TextGray)};Text(money(due),color=if(due>0)AccentRed else AccentGreen,fontWeight=FontWeight.Bold,fontSize=12.sp)};Button({onCollect(member.name,g.registerNo?:g.id)},enabled=due>0,modifier=Modifier.fillMaxWidth().height(36.dp),colors=ButtonDefaults.buttonColors(containerColor=MaroonPrimary)){Text(if(due>0)"Collect" else "Paid up",fontSize=10.sp)}}}}
        item{Text("Chit Passbook",fontSize=13.sp,fontWeight=FontWeight.Bold)}
        if(data.payments.isEmpty())item{ActionEmptyState("No payment history","Collections saved for this customer will appear here",Icons.Default.ReceiptLong,null)}
        else item{ChitPassbook(member.name ?: "Unnamed", data.payments)}
        item{Text("Settlement & Delivery",fontSize=13.sp,fontWeight=FontWeight.Bold)}
        if(data.financial.isEmpty())item{ActionEmptyState("No settlement or delivery","Financial entries will appear here",Icons.Default.AccountBalanceWallet,null)}
        items(data.financial.take(30),key={it.id}){f->HistoryRow("${f.type.lowercase().replaceFirstChar(Char::uppercase)} • ${f.groupId}",f.amountPaise,f.mode,f.occurredAt,f.status)}
    }
        }
    }
}

@Composable private fun HistoryRow(title:String,paise:Long,mode:String,at:Long,status:String)=Surface(shape=RoundedCornerShape(10.dp),color=Color.White,border=BorderStroke(1.dp,DividerGray)){Row(Modifier.fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontSize=10.sp,fontWeight=FontWeight.SemiBold);Text("$mode • ${SimpleDateFormat("dd MMM yy, hh:mm a",Locale.ENGLISH).format(Date(at))} • $status",fontSize=8.sp,color=TextGray)};Text(money(paise/100),fontSize=11.sp,fontWeight=FontWeight.Bold)}}

@Composable private fun ActionEmptyState(title:String,subtitle:String,icon:ImageVector,onAction:(()->Unit)?){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=Color.White,border=BorderStroke(1.dp,DividerGray)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=AccentGreen,modifier=Modifier.size(25.dp));Column(Modifier.padding(start=9.dp).weight(1f)){Text(title,fontSize=11.sp,fontWeight=FontWeight.SemiBold);Text(subtitle,fontSize=9.sp,color=TextGray)};onAction?.let{TextButton(it){Text("Open",fontSize=9.sp)}}}}}

// ═══════════════════════════════════════════════════════════════════════════════
// CHIT PASSBOOK — a customer's payment history rendered as a flip-through paper
// passbook (the physical books this business's customers grew up using) instead
// of a flat list, with a 3D page-turn swipe between pages.
// ═══════════════════════════════════════════════════════════════════════════════

private val PassbookPaper = Color(0xFFFBF3E1)
private val PassbookRule = Color(0xFFD8C9A3)
private val PassbookInk = Color(0xFF3A2E22)

@Composable
private fun ChitPassbook(customerName: String, payments: List<PaymentEntity>) {
    val rowsPerPage = 8
    val sorted = remember(payments) { payments.sortedByDescending { it.paidAt } }
    val pages = remember(sorted) { sorted.chunked(rowsPerPage) }
    var pageIndex by remember(pages) { mutableIntStateOf(0) }
    val current = pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "passbookPage"
        ) { page ->
            PassbookPage(customerName, pages[page], page + 1, pages.size, rowsPerPage)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconButton(onClick = { pageIndex = (current - 1).coerceAtLeast(0) }, enabled = current > 0, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ChevronLeft, "Previous page", tint = if (current > 0) MaroonPrimary else DividerGray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                pages.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(if (index == current) 7.dp else 5.dp)
                            .background(if (index == current) MaroonPrimary else DividerGray, CircleShape)
                    )
                }
            }
            IconButton(onClick = { pageIndex = (current + 1).coerceAtMost(pages.size - 1) }, enabled = current < pages.size - 1, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ChevronRight, "Next page", tint = if (current < pages.size - 1) MaroonPrimary else DividerGray)
            }
        }
    }
}

@Composable
private fun PassbookPage(customerName: String, rows: List<PaymentEntity>, pageNo: Int, pageCount: Int, rowsPerPage: Int) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = PassbookPaper,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, PassbookRule)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Red margin line, like a ruled ledger page
                    drawLine(Color(0xFFB33), Offset(34f, 0f), Offset(34f, size.height), strokeWidth = 1.5f)
                }
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
        ) {
            Text("JOTHI VEL CHITS", color = MaroonPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(customerName, color = PassbookInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().drawBehind {
                drawLine(PassbookRule, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.5f)
            }.padding(bottom = 5.dp)) {
                Text("DATE", color = PassbookInk.copy(alpha = .65f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
                Text("MODE", color = PassbookInk.copy(alpha = .65f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                Text("STATUS", color = PassbookInk.copy(alpha = .65f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                Text("AMOUNT", color = PassbookInk.copy(alpha = .65f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            rows.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().drawBehind {
                        drawLine(PassbookRule.copy(alpha = .6f), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                    }.padding(vertical = 7.dp)
                ) {
                    Text(SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH).format(Date(p.paidAt)), color = PassbookInk, fontSize = 10.sp, modifier = Modifier.weight(1.1f))
                    Text(p.mode, color = PassbookInk, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
                    Text(p.status, color = when (p.status) { "PAID", "ADVANCE" -> AccentGreen; "PARTIAL" -> AccentGold; else -> AccentRed }, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f))
                    Text(money(p.amountPaid / 100), color = PassbookInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }
            repeat((rowsPerPage - rows.size).coerceAtLeast(0)) {
                Spacer(Modifier.fillMaxWidth().height(24.dp).drawBehind {
                    drawLine(PassbookRule.copy(alpha = .35f), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                })
            }
            Spacer(Modifier.height(8.dp))
            Text("Page $pageNo of $pageCount", color = PassbookInk.copy(alpha = .5f), fontSize = 8.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun DailyClosingScreen(onBack:()->Unit){val context=LocalContext.current;val today=CollectionService.todayKey();val snapshot by produceState(initialValue=emptyMap<String,Long>(),context){value=withContext(Dispatchers.IO){val db=AppDatabase.getDatabase(context);val receipts=db.collectionReceiptDao().getRecentSync(Int.MAX_VALUE).filter{it.businessDate==today&&it.status=="SAVED"};val finance=db.financialTransactionDao().getAllSync().filter{it.status=="POSTED"&&SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date(it.occurredAt))==today};val dues=loadOperationalDues(db).filter{it.dueDate.equals(SimpleDateFormat("dd-MMM-yy",Locale.ENGLISH).format(Date()),true)}.sumOf{it.pending};mapOf("Cash" to receipts.filter{it.mode=="Cash"}.sumOf{it.amountPaidPaise}/100,"UPI" to receipts.filter{it.mode=="UPI"}.sumOf{it.amountPaidPaise}/100,"Bank" to receipts.filter{it.mode=="Bank"}.sumOf{it.amountPaidPaise}/100,"Settlement" to finance.filter{it.type=="SETTLEMENT"}.sumOf{it.amountPaise}/100,"Delivery" to finance.filter{it.type=="DELIVERY"}.sumOf{it.amountPaise}/100,"Due" to dues)}};val received=(snapshot["Cash"]?:0)+(snapshot["UPI"]?:0)+(snapshot["Bank"]?:0);val expected=received+(snapshot["Due"]?:0);val difference=received-expected;fun share(){val text="Jothi Vel Chits - Daily Closing ($today)\nCash: ${money(snapshot["Cash"]?:0)}\nUPI: ${money(snapshot["UPI"]?:0)}\nBank: ${money(snapshot["Bank"]?:0)}\nSettlement: ${money(snapshot["Settlement"]?:0)}\nDelivery: ${money(snapshot["Delivery"]?:0)}\nExpected: ${money(expected)}\nReceived: ${money(received)}\nDifference: ${money(difference)}";context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"Share closing summary"))};Column(Modifier.fillMaxSize().background(MaroonBackground)){BrandTopBar("Daily Closing",back=onBack,action=Icons.Default.Share,onAction=::share);LazyColumn(contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text(SimpleDateFormat("EEEE, dd MMM yyyy",Locale.ENGLISH).format(Date()),fontSize=11.sp,color=TextGray)};listOf("Cash" to AccentGreen,"UPI" to Color(0xFF285A9B),"Bank" to MaroonPrimary,"Settlement" to Color(0xFF285A9B),"Delivery" to AccentGreen).forEach{(key,color)->item{ClosingRow(key,snapshot[key]?:0,color)}};item{Divider()};item{ClosingRow("Expected",expected,Color.Black)};item{ClosingRow("Received",received,AccentGreen)};item{ClosingRow("Difference",difference,if(difference<0)AccentRed else AccentGreen)};item{Button(::share,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=MaroonPrimary)){Icon(Icons.Default.Share,null);Spacer(Modifier.width(6.dp));Text("Share Closing Summary")}}}}}
@Composable private fun ClosingRow(label:String,value:Long,color:Color)=Surface(shape=RoundedCornerShape(13.dp),color=Color.White,border=BorderStroke(1.dp,DividerGray)){Row(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=11.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontSize=10.sp,color=TextGray);Text(money(value),fontSize=13.sp,fontWeight=FontWeight.Bold,color=color)}}

@Composable
private fun PlaceholderListScreen(title: String, icon: ImageVector) = Column(Modifier.fillMaxSize()) { BrandTopBar(title); Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = MaroonPrimary, modifier = Modifier.size(60.dp)); Text("Group management continues here", fontWeight = FontWeight.Bold); Text("Existing group flow is preserved", color = TextGray) } } }

private data class AgentMessage(val text: String, val fromUser: Boolean)

@Composable
private fun AgentScreen() {
    val context = LocalContext.current
    val engine = remember(context) { LocalAssistantEngine(context) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(AgentMessage("Vanakkam! Customer ledger, pending, details அல்லது today collection கேளுங்கள்.", false))
    }
    val listState = rememberLazyListState()

    fun send(message: String) {
        val command = message.trim()
        if (command.isBlank() || loading) return
        messages += AgentMessage(command, true)
        input = ""
        loading = true
        scope.launch {
            val reply = withContext(Dispatchers.IO) { engine.answer(command) }
            messages += AgentMessage(reply, false)
            loading = false
        }
    }

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    Column(Modifier.fillMaxSize().background(MaroonBackground)) {
        BrandTopBar("Agent")
        Text("Ledger, pending and collection assistant", color = TextGray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Customer details", "Pending details", "Today collection").forEach { suggestion ->
                AssistChip(onClick = { input = suggestion }, label = { Text(suggestion, fontSize = 8.sp) })
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (message.fromUser) MaroonPrimary else Color(0xFFE9EAEC),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) { Text(message.text, color = if (message.fromUser) Color.White else Color(0xFF242528), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) }
                }
            }
            if (loading) item { LinearProgressIndicator(Modifier.width(70.dp), color = MaroonPrimary) }
        }
        Surface(color = Color.White, shadowElevation = 5.dp) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, placeholder = { Text("Ravi pending evlo?", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = { send(input) }, enabled = input.isNotBlank() && !loading) { Icon(Icons.Default.Send, "Send", tint = MaroonPrimary) }
            }
        }
    }
}

private fun answerAgentCommand(context: android.content.Context, raw: String): String {
    val query = raw.lowercase(Locale.ROOT).replace(Regex("(.)\\1{2,}"), "$1$1")
    val db = AppDatabase.getDatabase(context)
    if (listOf("today collection", "innaiku collection", "இன்று collection").any { query.contains(it) }) {
        val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val totalPaise = db.paymentDao().getAllPaymentsSync().filter { it.paidAt >= start }.sumOf { it.amountPaid }
        return "Innaiku total collection ${money(totalPaise / 100)}. Room DB payment entries-la irundhu calculate pannathu."
    }
    val members = db.memberDao().getAllMembersSync()
    if (query.trim() in setOf("pending", "pending details", "due details", "total pending")) {
        val pendingRows = members.mapNotNull { member ->
            val paid = db.paymentDao().getPaymentsByMemberSync(member.id).sumOf { it.amountPaid } / 100
            val group = member.selectedChitId?.let { db.groupDao().getGroupByIdSync(it) }
            val expected = group?.let { it.chitValue.toLong() / 100 }
                ?: ((member.installmentAmount?.toLongOrNull() ?: 0L) * 20)
            val due = (expected - paid).coerceAtLeast(0)
            if (due > 0) Triple(member.name ?: "Unnamed", member.id, due) else null
        }.sortedByDescending { it.third }
        if (pendingRows.isEmpty()) return "Room DB-la pending customer entries இல்லை."
        return buildString {
            append("Total pending ${money(pendingRows.sumOf { it.third })} • ${pendingRows.size} customers")
            pendingRows.take(5).forEach { (name, code, due) -> append("\n$name ($code) • ${money(due)}") }
            if (pendingRows.size > 5) append("\nமேலும் ${pendingRows.size - 5} customers இருக்காங்க.")
        }
    }
    if (query.trim() in setOf("customer", "customer details", "member details", "details")) {
        return "எந்த customer details வேணும்? Name, customer code அல்லது mobile number சொல்லுங்க. Example: Ravi details / C102 details."
    }
    val direct = members.filter { member ->
        query.contains(member.id.lowercase(Locale.ROOT)) ||
            (!member.phone.isNullOrBlank() && query.contains(member.phone)) ||
            (!member.name.isNullOrBlank() && query.contains(member.name.lowercase(Locale.ROOT)))
    }
    val ignored = setOf("customer", "member", "details", "detail", "pending", "ledger", "oda", "evlo", "sollu", "kaatu", "kudu", "amount", "full")
    val tokens = query.split(Regex("[^a-z0-9]+" )).filter { it.length >= 3 && it !in ignored }
    val matches = if (direct.isNotEmpty()) direct else members.filter { member -> tokens.any { token -> member.name?.lowercase(Locale.ROOT)?.contains(token) == true } }
    if (matches.isEmpty()) return "Matching customer kidaikala. Customer name, code அல்லது mobile number use panni try pannunga."
    if (matches.size > 1) return "${matches.size} customers match ஆகுறாங்க:\n" + matches.take(5).joinToString("\n") { "${it.name} • ${it.id} • ${it.phone}" } + "\nCorrect code/mobile சொல்லுங்க."
    val member = matches.first()
    val paid = db.paymentDao().getPaymentsByMemberSync(member.id).sumOf { it.amountPaid } / 100
    val group = member.selectedChitId?.let { db.groupDao().getGroupByIdSync(it) }
    val expected = group?.let { it.chitValue.toLong() / 100 } ?: ((member.installmentAmount?.toLongOrNull() ?: 0L) * 20)
    val pending = (expected - paid).coerceAtLeast(0)
    return when {
        query.contains("pending") || query.contains("due") || query.contains("baaki") -> "${member.name} (${member.id}) pending ${money(pending)}. Paid ${money(paid)}."
        query.contains("ledger") -> "${member.name} (${member.id}) ledger summary: Chit ${group?.registerNo ?: member.selectedChitId ?: "Unavailable"}, paid ${money(paid)}, pending ${money(pending)}."
        else -> "${member.name} (${member.id})\nMobile: ${member.phone ?: "Unavailable"}\nArea: ${listOfNotNull(member.addressLine, member.city).joinToString(", ").ifBlank { "Unavailable" }}\nChit: ${group?.registerNo ?: member.selectedChitId ?: "Unavailable"}"
    }
}

@Composable
private fun SettingsScreenApproved(onBack: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, onExport: () -> Unit, onImport: () -> Unit, onLabour: () -> Unit, onLogout: () -> Unit) {
    val context=LocalContext.current;val last=remember{com.jothivel.chits.utils.AppPreferences(context).getLastBackupAt()};val backupText=if(last==0L)"No backup created yet" else "Last backup ${relativeTime(last)}"
    Column(Modifier.fillMaxSize()) { BrandTopBar("Settings", back = onBack); LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { item{Text(backupText,fontSize=10.sp,color=if(last==0L)AccentRed else AccentGreen,modifier=Modifier.padding(horizontal=5.dp,vertical=3.dp))}; item { MoreRow(Icons.Default.Backup, "Backup database", onBackup) }; item { MoreRow(Icons.Default.Restore, "Restore database", onRestore) }; item { MoreRow(Icons.Default.FileDownload, "Export CSV", onExport) }; item { MoreRow(Icons.Default.FileUpload, "Import CSV / XLS / XLSX", onImport) }; item { MoreRow(Icons.Default.Groups, "Labour (field agents)", onLabour) }; item { MoreRow(Icons.Default.Logout, "Log out", onLogout) } } }
}

@Composable
private fun MoreRow(icon: ImageVector, label: String, onClick: () -> Unit) = Surface(
    Modifier.fillMaxWidth().height(46.dp).clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color = Color.White,
    border = BorderStroke(1.dp, DividerGray.copy(alpha = .8f))
) {
    Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
        Text(label, Modifier.padding(start = 10.dp).weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, null, tint = TextGray, modifier = Modifier.size(17.dp))
    }
}
