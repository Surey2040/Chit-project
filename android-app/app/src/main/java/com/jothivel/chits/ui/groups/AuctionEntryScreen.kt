package com.jothivel.chits.ui.groups

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.*
import com.jothivel.chits.ui.components.AppTextField
import com.jothivel.chits.utils.CurrencyUtils
import kotlinx.coroutines.delay
import kotlin.math.max

val GreenText = Color(0xFF1E6B3B)
val LightGreen = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionEntryScreen(
    chitValue: Long, // in paise
    subscriberCount: Int,
    onBackClick: () -> Unit,
    onSaveClick: (winningBid: Long, winningMemberId: String) -> Unit
) {
    var winningMemberId by remember { mutableStateOf("") }
    var winningBidStr by remember { mutableStateOf("") }
    
    val winningBid = winningBidStr.toLongOrNull() ?: 0L
    
    // Live Kasaru Engine Calculations
    val baseAmount = if (subscriberCount > 0) chitValue / subscriberCount else 0L
    val commission = chitValue * 5 / 100
    val winningBidPaise = winningBid * 100
    val totalKasaruPaise = max(0L, winningBidPaise - commission)
    
    val kasaruPerMember = if (subscriberCount > 0) totalKasaruPaise / subscriberCount else 0L
    val nextMonthDue = max(0L, baseAmount - kasaruPerMember)
    val winnerPayout = max(0L, chitValue - winningBidPaise)

    // Staggered entry
    var showForm by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100); showForm = true
        delay(250); showPreview = true
        delay(400); showButton = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Auction", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = OffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Input Card
            AnimatedVisibility(
                visible = showForm,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Auction Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        AppTextField(
                            value = winningMemberId,
                            onValueChange = { winningMemberId = it },
                            label = "Winning Member ID (Temporary)"
                        )

                        AppTextField(
                            value = winningBidStr,
                            onValueChange = { winningBidStr = it },
                            label = "Winning Bid (Discount in ₹)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // Live Preview Card with animated entry
            AnimatedVisibility(
                visible = showPreview && winningBid > 0,
                enter = fadeIn(tween(300)) + expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(tween(200)) + shrinkVertically(
                    animationSpec = tween(200)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Live Calculation Preview", fontWeight = FontWeight.Bold, color = GreenText)
                        Divider(color = GreenText.copy(alpha = 0.2f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Kasaru:", color = GreenText)
                            Text(CurrencyUtils.formatPaiseToRupee(totalKasaruPaise.toInt()), fontWeight = FontWeight.Bold, color = GreenText)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kasaru per Member:", color = GreenText)
                            Text(CurrencyUtils.formatPaiseToRupee(kasaruPerMember.toInt()), fontWeight = FontWeight.Bold, color = GreenText)
                        }
                        
                        Divider(color = GreenText.copy(alpha = 0.2f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Next Month Due:", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(CurrencyUtils.formatPaiseToRupee(nextMonthDue.toInt()), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Winner Payout:", color = GreenText, fontWeight = FontWeight.Bold)
                            Text(CurrencyUtils.formatPaiseToRupee(winnerPayout.toInt()), fontWeight = FontWeight.Bold, color = GreenText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                Button(
                    onClick = { 
                        onSaveClick(winningBidPaise, winningMemberId) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = winningMemberId.isNotBlank() && winningBid > 0
                ) {
                    Text("Lock Auction Result", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
