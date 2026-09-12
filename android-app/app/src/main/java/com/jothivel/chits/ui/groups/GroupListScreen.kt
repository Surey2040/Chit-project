package com.jothivel.chits.ui.groups

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.ui.components.AppBottomNavigationBar
import com.jothivel.chits.ui.theme.OffWhite
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    showBottomBar: Boolean = true,
    viewModel: GroupViewModel
) {
    val context = LocalContext.current
    val groups by viewModel.allGroups.observeAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chit Groups", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaroonPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(currentRoute = "Chits")
            }
        },
        containerColor = OffWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(groups) { group ->
                GroupItemCard(group = group, onClick = {
                    val intent = Intent(context, GroupDetailActivity::class.java)
                    intent.putExtra("GROUP_ID", group.id)
                    context.startActivity(intent)
                })
            }
        }
    }
}

@Composable
fun GroupItemCard(group: ChitGroupEntity, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.registerNo ?: "N/A",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = group.name ?: "Chit Group",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (group.status == "ACTIVE") Color(0xFF1E6B3B) else Color.Gray
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = group.status ?: "",
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand details",
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Chit Value", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = CurrencyUtils.formatInr((group.chitValue / 100).toInt()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Duration", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = "${group.durationMonths} Months",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
