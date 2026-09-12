package com.jothivel.chits.ui.ledger

import android.os.Bundle
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jothivel.chits.ui.components.AppBottomNavigationBar
import com.jothivel.chits.ui.components.AshAnimatedSearchBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.ui.collections.PaymentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.jothivel.chits.ui.theme.*
import com.jothivel.chits.ui.components.SmoothTransitions

class LedgerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[LedgerViewModel::class.java]
        val paymentViewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

        setContent {
            JothiVelChitsTheme {
                var selectedMember by remember { mutableStateOf<MemberEntity?>(null) }
                
                if (selectedMember == null) {
                    // Level 1 & 2
                    LedgerScreen(
                        viewModel = viewModel,
                        onMemberClick = { member -> selectedMember = member },
                        onBack = { SmoothTransitions.finishSmooth(this) }
                    )
                } else {
                    // Level 3
                    val group by viewModel.selectedGroup.observeAsState()
                    MemberLedgerDetailScreen(
                        member = selectedMember!!,
                        group = group,
                        paymentViewModel = paymentViewModel,
                        onBack = { selectedMember = null }
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        SmoothTransitions.applyExitTransition(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(showBottomBar: Boolean = true,
    viewModel: LedgerViewModel,
    onMemberClick: (MemberEntity) -> Unit,
    onBack: () -> Unit
) {
    val allGroups by viewModel.allGroups.observeAsState(emptyList<ChitGroupEntity>())
    val selectedGroup by viewModel.selectedGroup.observeAsState()
    val ledgerItems by viewModel.ledgerMemberItems.observeAsState(emptyList<LedgerMemberItem>())
    val summary by viewModel.ledgerSummary.observeAsState(LedgerSummary())

    var showBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = ledgerItems.filter {
        it.member.name?.contains(searchQuery, ignoreCase = true) == true ||
        it.member.ticketNo?.contains(searchQuery, ignoreCase = true) == true
    }

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text("Select a Chit Group", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                }
                if (allGroups.isEmpty()) {
                    item { Text("No active chit groups available.", color = Color.Gray) }
                } else {
                    items(allGroups) { group ->
                        ChitGroupItem(group, isSelected = group.id == selectedGroup?.id) {
                            viewModel.selectGroup(group.id)
                            showBottomSheet = false
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ledger", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        bottomBar = {
            if (showBottomBar) AppBottomNavigationBar(currentRoute = "Ledger")
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFCF9F6))
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // LEVEL 1: Chit Selector Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBottomSheet = true },
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.background(brush = LightMaroonGradient).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(MaroonSurfaceLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedGroup?.registerNo?.take(1)?.uppercase() ?: "C", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedGroup?.registerNo ?: "Select Chit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val valStr = selectedGroup?.chitValue?.let { it / 100 } ?: 0
                        Text("Reg. ${selectedGroup?.registerNo ?: "-"} · Rs $valStr · ${selectedGroup?.subscriberCount ?: 0} members", color = Color.Gray, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Tap to switch chit", color = Color.Gray, fontSize = 10.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LEVEL 2: Summary Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Row(modifier = Modifier.background(brush = LightMaroonGradient).padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total collected", color = Color.Gray, fontSize = 12.sp)
                        Text("Rs ${summary.totalCollected}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column {
                        Text("Total pending", color = Color.Gray, fontSize = 12.sp)
                        Text("Rs ${summary.totalPending}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column {
                        Text("Overdue members", color = Color.Gray, fontSize = 12.sp)
                        Text("${summary.overdueMembers}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AshAnimatedSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search member or slot no.",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Filter", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("M E M B E R S", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

            // LEVEL 2: Member List
            LazyColumn {
                items(filteredItems) { item ->
                    MemberLedgerRow(item) { onMemberClick(item.member) }
                    Divider(color = Color(0xFFEEEEEE))
                }
                item { Spacer(modifier = Modifier.height(100.dp)) } // Extra space for floating nav bar
            }
        }
    }
}

@Composable
fun ChitGroupItem(group: ChitGroupEntity, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFFF5F5F5) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(group.registerNo ?: "Unknown", fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black)
            Text("Rs ${group.chitValue / 100} · ${group.subscriberCount} members", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun MemberLedgerRow(item: LedgerMemberItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.member.ticketNo ?: "-", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFE0E0E0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(item.member.name?.take(2)?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(item.member.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Slot ${item.member.ticketNo ?: "-"}", color = Color.Gray, fontSize = 12.sp)
            Text("Paid Rs ${item.totalPaid}", color = AccentGreen, fontSize = 12.sp)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text("Due", color = Color.Gray, fontSize = 10.sp)
            Text("Rs ${item.totalDue}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(4.dp))
            val badgeColor = if (item.status == "PAID") Color(0xFFE8F5E9) else if (item.status == "OVERDUE") MaroonSurfaceLight else Color(0xFFFFF3E0)
            val textColor = if (item.status == "PAID") AccentGreen else if (item.status == "OVERDUE") MaterialTheme.colorScheme.primary else Color(0xFFE65100)
            Box(modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(item.status, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberLedgerDetailScreen(
    member: MemberEntity,
    group: ChitGroupEntity?,
    paymentViewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val payments by paymentViewModel.getPaymentsByMember(member.id).observeAsState(emptyList())
    val totalPaid = payments.sumOf { it.amountPaid } / 100 // Convert paise to rupees
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("${group?.registerNo ?: "Chit"} — Ledger", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Register No. ${group?.registerNo} · Ticket ${member.ticketNo} · ${group?.durationMonths} months", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFCF9F6))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Profile Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Row(modifier = Modifier.background(brush = LightMaroonGradient).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                        Text(member.name?.take(2)?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.name ?: "", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Slot ${member.ticketNo} · +91 ${member.phone}", color = Color.Gray, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total due", color = Color.Gray, fontSize = 12.sp)
                        Text("Rs ${if (group != null) group.chitValue / 100 else 0}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Paid so far", color = Color.Gray, fontSize = 12.sp)
                        Text("Rs $totalPaid", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ledger Table (Mock UI mapping the image)
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.background(brush = LightMaroonGradient)) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("No.", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.5f))
                        Text("Date", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.5f))
                        Text("Kasaru", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                        Text("Paid", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                        Text("Status", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                    }
                    Divider()
                    
                    if (payments.isEmpty()) {
                        Text(
                            "No payments recorded yet.",
                            color = Color.Gray,
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        val sdf = java.text.SimpleDateFormat("dd MMM yy", java.util.Locale.getDefault())
                        payments.forEachIndexed { i, payment ->
                            val dateStr = sdf.format(java.util.Date(payment.paidAt))
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(payment.installmentId ?: "-", color = Color.Gray, modifier = Modifier.weight(0.5f))
                                Text(dateStr, color = Color.Gray, modifier = Modifier.weight(1.5f))
                                Text("—", color = Color.Gray, modifier = Modifier.weight(1f)) // Kasaru placeholder
                                Text("${payment.amountPaid / 100}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                Text(
                                    payment.status ?: "PAID",
                                    fontWeight = FontWeight.Bold,
                                    color = if (payment.status == "PARTIAL") Color(0xFFF57C00) else AccentGreen,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp
                                )
                            }
                            Divider(color = Color(0xFFF0F0F0))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(color = Color.Blue)
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Paid so far", color = Color.Gray, fontSize = 12.sp); Text("Rs 78,250", color = AccentGreen, fontWeight = FontWeight.Bold) }
                        Column { Text("Remaining", color = Color.Gray, fontSize = 12.sp); Text("Rs 3,15,000", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        Column { Text("Next due", color = Color.Gray, fontSize = 12.sp); Text("03 Jun 2026", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            val context = androidx.compose.ui.platform.LocalContext.current

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Record payment") }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { generateAndSharePdf(context, member, group) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Share ledger PDF", color = Color.Black) }
            }
        }
    }
}

fun generateAndSharePdf(context: android.content.Context, member: MemberEntity, group: ChitGroupEntity?) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint()

    paint.color = android.graphics.Color.BLACK
    paint.textSize = 24f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText("Ledger Statement", 50f, 50f, paint)

    paint.textSize = 14f
    paint.typeface = android.graphics.Typeface.DEFAULT
    canvas.drawText("Member: ${member.name}", 50f, 90f, paint)
    canvas.drawText("Ticket No: ${member.ticketNo}", 50f, 110f, paint)
    canvas.drawText("Group: ${group?.registerNo}", 50f, 130f, paint)

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText("No.       Date            Kasaru      Paid          Status", 50f, 180f, paint)
    paint.typeface = android.graphics.Typeface.DEFAULT

    var y = 210f
    for (i in 1..5) {
        val status = if (i < 4) "PAID" else if (i == 4) "PARTIAL" else "OVERDUE"
        canvas.drawText("$i         02 Jan 2026     7,500       Rs 25,000     $status", 50f, y, paint)
        y += 30f
    }

    pdfDocument.finishPage(page)

    val file = java.io.File(context.cacheDir, "pdfs")
    file.mkdirs()
    val pdfFile = java.io.File(file, "Ledger_${member.name}.pdf")

    try {
        pdfDocument.writeTo(java.io.FileOutputStream(pdfFile))
    } catch (e: java.io.IOException) {
        e.printStackTrace()
    }
    pdfDocument.close()

    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Ledger PDF"))
}
