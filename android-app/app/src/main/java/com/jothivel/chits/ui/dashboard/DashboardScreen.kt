package com.jothivel.chits.ui.dashboard

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R
import com.jothivel.chits.utils.CurrencyUtils
import com.jothivel.chits.ui.groups.AddGroupActivity
import com.jothivel.chits.ui.collections.CollectionEntryActivity
import com.jothivel.chits.ui.groups.AuctionEntryActivity
import com.jothivel.chits.ui.members.AddMemberActivity
import com.jothivel.chits.ui.components.SmoothTransitions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.jothivel.chits.ui.theme.*
import com.jothivel.chits.ui.components.AppBottomNavigationBar
import kotlinx.coroutines.delay
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel? = null,
    showBottomBar: Boolean = true
) {
    val context = LocalContext.current

    val activeChitsCount by viewModel?.activeChitsCount?.observeAsState(0) ?: remember { mutableIntStateOf(0) }
    val totalChitValue by viewModel?.totalChitValue?.observeAsState(0) ?: remember { mutableIntStateOf(0) }
    val totalMembers by viewModel?.totalMembers?.observeAsState(0) ?: remember { mutableIntStateOf(0) }
    val monthCollected by viewModel?.monthCollected?.observeAsState(0) ?: remember { mutableIntStateOf(0) }
    val pendingPaymentsCount by viewModel?.pendingPaymentsCount?.observeAsState(0) ?: remember { mutableIntStateOf(0) }
    val recentLogs by viewModel?.recentLogs?.observeAsState(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    // ── Staggered entry animations ───────────────────────────────────────
    var showGreeting by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var showStats1 by remember { mutableStateOf(false) }
    var showStats2 by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showActions2 by remember { mutableStateOf(false) }
    var showRecentLogs by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        viewModel?.loadDashboardStats()
        delay(150); showGreeting = true
        delay(100); showCalendar = true
        delay(100); showStats1 = true
        delay(100); showStats2 = true
        delay(100); showActions = true
        delay(100); showActions2 = true
        delay(100); showRecentLogs = true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(currentRoute = "Dashboard")
            }
        },
        containerColor = OffWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // ── Main Content (Background) ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(260.dp))

                // ── 2x2 Stat Cards (Row 1) ───────────────────────────────────
                AnimatedVisibility(
                visible = showStats1,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.active_chits),
                        targetValue = activeChitsCount,
                        isCurrency = false,
                        subtext = CurrencyUtils.formatPaiseToRupee(totalChitValue),
                        accentColor = MaroonLight
                    )
                    AnimatedStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.total_members_caps),
                        targetValue = totalMembers,
                        isCurrency = false,
                        subtext = stringResource(id = R.string.across_all_chits),
                        accentColor = AccentGold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── 2x2 Stat Cards (Row 2) ───────────────────────────────────
            AnimatedVisibility(
                visible = showStats2,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.this_month),
                        targetValue = monthCollected,
                        isCurrency = true,
                        subtext = stringResource(id = R.string.collected),
                        accentColor = AccentGreen
                    )
                    AnimatedStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.pending),
                        targetValue = pendingPaymentsCount,
                        isCurrency = false,
                        subtext = stringResource(id = R.string.members_overdue),
                        accentColor = AccentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showActions,
                enter = fadeIn(tween(400))
            ) {
                Text(stringResource(id = R.string.quick_actions), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── Quick Actions Row 1 ──────────────────────────────────────
            AnimatedVisibility(
                visible = showActions,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.members),
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color.Black) },
                        subtext = stringResource(id = R.string.members_count, totalMembers.toString()),
                        onClick = {
                            SmoothTransitions.startActivitySmooth(context, Intent(context, AddMemberActivity::class.java))
                        }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.new_chit),
                        icon = { Icon(painterResource(id = R.drawable.ic_add), contentDescription = null, modifier = Modifier.padding(8.dp).size(24.dp), tint = Color.Black) },
                        subtext = stringResource(id = R.string.active_groups_format, activeChitsCount.toString()),
                        onClick = {
                            SmoothTransitions.startActivitySmooth(context, Intent(context, AddGroupActivity::class.java))
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── Quick Actions Row 2 ──────────────────────────────────────
            AnimatedVisibility(
                visible = showActions2,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.auction),
                        icon = { Icon(painterResource(id = R.drawable.ic_add), contentDescription = null, modifier = Modifier.padding(8.dp).size(24.dp), tint = Color.Black) },
                        subtext = stringResource(id = R.string.record_auction),
                        onClick = {
                            SmoothTransitions.startActivitySmooth(context, Intent(context, AuctionEntryActivity::class.java))
                        }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.record_payment),
                        icon = { Icon(painterResource(id = R.drawable.ic_currency_rupee), contentDescription = null, modifier = Modifier.padding(8.dp).size(24.dp), tint = Color.Black) },
                        subtext = "$pendingPaymentsCount pending",
                        onClick = {
                            SmoothTransitions.startActivitySmooth(context, Intent(context, CollectionEntryActivity::class.java))
                        }
                    )
                }
            } // Closes AnimatedVisibility
            Spacer(modifier = Modifier.height(16.dp))

            // ── Recent Updates Feed ──────────────────────────────────────
            AnimatedVisibility(
                visible = showRecentLogs,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (recentLogs.isNotEmpty()) {
                        Text("Recent Updates", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        recentLogs.forEach { log ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(OffWhite, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = when(log.actionType) {
                                            "GROUP_CREATED" -> Icons.Default.Group
                                            "MEMBER_ADDED" -> Icons.Default.Person
                                            "PAYMENT_RECORDED" -> Icons.Default.CheckCircle
                                            else -> Icons.Default.Info
                                        }
                                        Icon(icon, contentDescription = null, tint = MaroonPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(log.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                        Text(log.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                                        Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(java.util.Date(log.timestamp)), fontSize = 10.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Extra space for floating nav bar
        } // Closes Main Content Column

        // ── Foreground Header ──────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp)
            ) {
                // ── Greeting ─────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = showGreeting,
                    enter = fadeIn(tween(500)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column {
                        val istZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                        val hour = Calendar.getInstance(istZone).get(Calendar.HOUR_OF_DAY)
                        val greeting = when (hour) {
                            in 5..11 -> stringResource(id = R.string.good_morning) + " ☀️"
                            in 12..16 -> stringResource(id = R.string.good_afternoon) + " 🌤️"
                            in 17..20 -> stringResource(id = R.string.good_evening) + " 🌅"
                            else -> stringResource(id = R.string.good_night) + " 🌙"
                        }
                        Text(
                            text = greeting,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Jothi Vell",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                // ── Horizontal Calendar ──────────────────────────────────────
                AnimatedVisibility(
                    visible = showCalendar,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    HorizontalCalendar()
                }
            }
        }
    } // Closes Box
} // Closes Scaffold Lambda
} // Closes DashboardScreen

// ═══════════════════════════════════════════════════════════════════════════════
// ANIMATED STAT CARD — with counting animation
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedStatCard(
    modifier: Modifier = Modifier,
    title: String,
    targetValue: Int,
    isCurrency: Boolean,
    subtext: String,
    accentColor: Color
) {
    // Counter animation
    var animatedValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetValue) {
        if (targetValue == 0) {
            animatedValue = 0
            return@LaunchedEffect
        }
        val steps = 20
        val stepDelay = 30L
        for (i in 1..steps) {
            animatedValue = (targetValue * i) / steps
            delay(stepDelay)
        }
        animatedValue = targetValue
    }

    val displayValue = if (isCurrency) {
        CurrencyUtils.formatPaiseToRupee(animatedValue)
    } else {
        animatedValue.toString()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.background(brush = LightMaroonGradient).padding(16.dp)) {
            Column {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(displayValue, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accentColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtext, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// QUICK ACTION CARD — with press scale effect
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    subtext: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.background(brush = LightMaroonGradient).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp)) {
                    icon()
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(subtext, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HORIZONTAL CALENDAR — with smooth selection animation
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun HorizontalCalendar() {
    val calendar = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    
    val currentMonthYear = remember(currentMonth, currentYear) {
        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.YEAR, currentYear)
        tempCal.set(Calendar.MONTH, currentMonth)
        SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(tempCal.time)
    }

    val dates = remember(currentMonth, currentYear) {
        val list = mutableListOf<Pair<String, String>>()
        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.YEAR, currentYear)
        tempCal.set(Calendar.MONTH, currentMonth)
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        repeat(maxDays) {
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(tempCal.time)
            val dateNum = SimpleDateFormat("dd", Locale.getDefault()).format(tempCal.time)
            list.add(Pair(dayName, dateNum))
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val todayCal = Calendar.getInstance()
    val isCurrentMonthAndYear = currentMonth == todayCal.get(Calendar.MONTH) && currentYear == todayCal.get(Calendar.YEAR)
    var selectedIndex by remember(currentMonth, currentYear) { 
        mutableIntStateOf(if (isCurrentMonthAndYear) todayCal.get(Calendar.DAY_OF_MONTH) - 1 else 0)
    }
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedIndex - 2))

    var expanded by remember { mutableStateOf(false) }
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.clickable { expanded = true }.padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = currentMonthYear,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 4.dp).size(28.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    months.forEachIndexed { index, monthName ->
                        DropdownMenuItem(
                            text = { Text("$monthName, $currentYear") },
                            onClick = {
                                currentMonth = index
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(dates) { index, pair ->
                val isSelected = index == selectedIndex
                CalendarDateItem(
                    dayName = pair.first,
                    dateNum = pair.second,
                    isSelected = isSelected,
                    onClick = { selectedIndex = index }
                )
            }
        }
    }
}

@Composable
fun CalendarDateItem(dayName: String, dateNum: String, isSelected: Boolean, onClick: () -> Unit) {
    // Smooth color crossfade on selection
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Transparent,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "calBg"
    )
    val dayColor by animateColorAsState(
        targetValue = if (isSelected) MaroonDark else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "calDay"
    )
    val dateColor by animateColorAsState(
        targetValue = if (isSelected) MaroonDark else Color.White,
        animationSpec = tween(300),
        label = "calDate"
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 0.dp,
        animationSpec = tween(300),
        label = "calElev"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "calScale"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        shadowElevation = elevation,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .width(65.dp)
            .height(85.dp)
            .scale(scale)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayName,
                fontSize = 13.sp,
                color = dayColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = dateNum,
                fontSize = 22.sp,
                color = dateColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
