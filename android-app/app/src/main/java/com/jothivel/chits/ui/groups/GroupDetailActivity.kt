package com.jothivel.chits.ui.groups

import android.content.Intent
import android.os.Bundle
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.ui.members.MemberViewModel

import com.jothivel.chits.ui.theme.*
import com.jothivel.chits.ui.components.DetailFindToolbar
import kotlinx.coroutines.launch

class GroupDetailActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val groupId = intent.getStringExtra("GROUP_ID") ?: ""
        
        val groupViewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        val memberViewModel = ViewModelProvider(this).get(MemberViewModel::class.java)

        setContent {
            JothiVelChitsTheme {
                GroupDetailScreen(
                    groupId = groupId,
                    groupViewModel = groupViewModel,
                    memberViewModel = memberViewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    groupViewModel: GroupViewModel,
    memberViewModel: MemberViewModel,
    onBack: () -> Unit
) {
    val group by groupViewModel.getGroupById(groupId).observeAsState()
    val members by memberViewModel.getMembersByChitId(groupId).observeAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var activeResultIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val matches = members.withIndex().filter { (_, member) ->
        query.isNotBlank() && listOf(member.name, member.phone, member.ticketNo, member.city, member.addressLine).any { it?.contains(query, true) == true }
    }
    val activeMember = matches.getOrNull(activeResultIndex)?.value

    fun moveResult(direction: Int) {
        if (matches.isEmpty()) return
        activeResultIndex = (activeResultIndex + direction + matches.size) % matches.size
        scope.launch { listState.animateScrollToItem(matches[activeResultIndex].index) }
    }

    fun downloadMembers() {
        val csv = buildString {
            appendLine("Name,Phone,Slot,City,Address")
            members.forEach { appendLine("${it.name.orEmpty()},${it.phone.orEmpty()},${it.ticketNo.orEmpty()},${it.city.orEmpty()},${it.addressLine.orEmpty()}") }
        }
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "${group?.registerNo ?: "Group"} members.csv")
            putExtra(Intent.EXTRA_TEXT, csv)
        }, "Download / share group details"))
    }

    LaunchedEffect(query) {
        activeResultIndex = 0
        matches.firstOrNull()?.let { listState.animateScrollToItem(it.index) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.registerNo ?: "Group Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // Group Header Summary
            group?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Chit Value: ₹${it.chitValue / 100}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Duration: ${it.durationMonths} Months")
                            Text(text = "Members: ${it.subscriberCount}")
                        }
                    }
                }
            }

            Text(
                text = "Enrolled Members (${members.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            DetailFindToolbar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Find member, phone, slot or area",
                resultCount = matches.size,
                activeResultIndex = activeResultIndex.coerceAtMost((matches.size - 1).coerceAtLeast(0)),
                onPrevious = { moveResult(-1) },
                onNext = { moveResult(1) },
                onDownload = { downloadMembers() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Members List
            if (members.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No members enrolled in this group yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        MemberItemRow(member, highlighted = query.isNotBlank() && member.id == activeMember?.id)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItemRow(member: MemberEntity, highlighted: Boolean = false) {
    val cardColor by androidx.compose.animation.animateColorAsState(if (highlighted) Color(0xFFE3E5E8) else Color.White, label = "memberHighlight")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (highlighted) androidx.compose.foundation.BorderStroke(1.dp, MaroonPrimary.copy(alpha = .55f)) else null,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE0E0E0), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name?.take(1)?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = member.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = member.phone ?: "No phone", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
