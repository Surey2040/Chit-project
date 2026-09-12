package com.jothivel.chits.ui.collections

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R
import com.jothivel.chits.ui.components.AppBottomNavigationBar
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.ui.groups.GroupViewModel
import com.jothivel.chits.ui.members.MemberViewModel
import com.jothivel.chits.ui.theme.*
import com.jothivel.chits.ui.components.AppTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OffWhite = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionEntryScreen(
    paymentViewModel: PaymentViewModel,
    groupViewModel: GroupViewModel,
    memberViewModel: MemberViewModel,
    onBackClick: () -> Unit,
    onPaymentSaved: (String) -> Unit,
    onError: (String) -> Unit
) {
    var currentStep by remember { mutableStateOf(1) } // 1=Group, 2=Member, 3=Form
    var selectedGroup by remember { mutableStateOf<ChitGroupEntity?>(null) }
    var selectedMember by remember { mutableStateOf<MemberEntity?>(null) }

    val allGroups by groupViewModel.allGroups.observeAsState(emptyList<ChitGroupEntity>())

    val membersListState = remember(selectedGroup) {
        if (selectedGroup != null) memberViewModel.getMembersByChitId(selectedGroup!!.id)
        else null
    }
    val membersInGroup by (membersListState?.observeAsState(emptyList<MemberEntity>()) ?: remember { mutableStateOf(emptyList<MemberEntity>()) })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (currentStep) {
                            1 -> "Select Chit Group"
                            2 -> "Select Member"
                            else -> "Record Payment"
                        }, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentStep) {
                            3 -> currentStep = 2
                            2 -> {
                                currentStep = 1
                                selectedGroup = null
                            }
                            else -> onBackClick()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        containerColor = OffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            AnimatedContent<Int>(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(
                            initialOffsetX = { width -> width },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(350)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { width -> -width },
                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(350))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { width -> -width },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(350)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { width -> width },
                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(350))
                    }
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (allGroups.isEmpty()) {
                                item { Text("No active chit groups found.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                            }
                            items(allGroups) { group ->
                                GroupSelectionCard(group) {
                                    selectedGroup = group
                                    currentStep = 2
                                }
                            }
                        }
                    }
                    2 -> {
                        Column {
                            // Selected Group Header
                            selectedGroup?.let { group ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Recording for:", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        Text(group.registerNo ?: "Unknown", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Text("Members (${membersInGroup.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxHeight()) {
                                if (membersInGroup.isEmpty()) {
                                    item { Text("No members in this group yet.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                                }
                                items(membersInGroup) { member ->
                                    MemberSelectionCard(member) {
                                        selectedMember = member
                                        currentStep = 3
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        if (selectedMember != null && selectedGroup != null) {
                            PaymentForm(
                                member = selectedMember!!,
                                group = selectedGroup!!,
                                onSave = { amount, mode, _, _ ->
                                    // Auto-Allocation handles mapping payment to the correct installments
                                    paymentViewModel.recordPayment(
                                        selectedMember!!.id,
                                        selectedGroup!!.id,
                                        amount, 
                                        mode,
                                        object : com.jothivel.chits.data.repository.PaymentRepository.PaymentCallback {
                                            override fun onSuccess(receiptNo: String) {
                                                onPaymentSaved(receiptNo)
                                                // Reset and go back to member list
                                                currentStep = 2
                                                selectedMember = null
                                            }

                                            override fun onError(message: String) {
                                                onError(message)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSelectionCard(group: ChitGroupEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.background(brush = LightMaroonGradient).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(group.registerNo?.take(1)?.uppercase() ?: "G", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.registerNo ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Rs ${group.chitValue / 100} · ${group.subscriberCount} members", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MemberSelectionCard(member: MemberEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.background(brush = LightMaroonGradient).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFE0E0E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(member.name?.take(2)?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name ?: "Unknown", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Slot ${member.ticketNo ?: "—"} · ${member.phone ?: "—"}", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentForm(
    member: MemberEntity,
    group: ChitGroupEntity,
    onSave: (amount: Int, mode: String, date: String, refNo: String) -> Unit
) {
    val defaultAmount = if (group.durationMonths > 0) group.chitValue / group.durationMonths else 0
    var amountStr by remember { mutableStateOf(defaultAmount.toString()) }
    var mode by remember { mutableStateOf("CASH") }
    var refNo by remember { mutableStateOf("") }
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dateStr by remember { mutableStateOf(sdf.format(Date())) }

    val amountPaid = amountStr.toIntOrNull() ?: 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.background(brush = LightMaroonGradient).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(MaroonSurfaceLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(member.name?.take(2)?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(member.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${group.registerNo} · Slot ${member.ticketNo ?: "-"}", color = Color.Gray, fontSize = 13.sp)
                }
            }

            Divider(color = Color(0xFFEEEEEE))

            AppTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Amount Paid (₹)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            AppTextField(
                value = dateStr,
                onValueChange = { dateStr = it },
                label = "Payment Date",
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") }
            )

            // Payment Mode selector (Segmented Button style)
            Text("Payment Mode", fontSize = 14.sp, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("CASH", "UPI", "BANK", "DOOR").forEach { m ->
                    val isSelected = mode == m || (m == "DOOR" && mode == "DOOR_COLLECTION")
                    val label = if (m == "DOOR") "DOOR" else m
                    val actualMode = if (m == "DOOR") "DOOR_COLLECTION" else m
                    
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5),
                        animationSpec = tween(250, easing = FastOutSlowInEasing),
                        label = "modeBg_$m"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.Black,
                        animationSpec = tween(250),
                        label = "modeText_$m"
                    )
                    
                    Surface(
                        modifier = Modifier.weight(1f).clickable { mode = actualMode },
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
                    ) {
                        Text(
                            label,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (mode != "CASH" && mode != "DOOR_COLLECTION") {
                AppTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    label = "Reference No / UTR"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { 
                    onSave(amountPaid * 100, mode, dateStr, refNo) // Convert to paise
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                enabled = amountPaid > 0 && dateStr.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


