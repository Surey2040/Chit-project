package com.jothivel.chits.ui.labour

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R
import com.jothivel.chits.data.firebase.AgentSummary
import com.jothivel.chits.data.firebase.FirebaseSetup
import com.jothivel.chits.data.firebase.FirestoreSchema
import com.jothivel.chits.data.firebase.await
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.ui.theme.AccentGold
import com.jothivel.chits.ui.theme.AccentGreen
import com.jothivel.chits.ui.theme.AccentRed
import com.jothivel.chits.ui.theme.DividerGray
import com.jothivel.chits.ui.theme.MaroonBackground
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.MaroonSurfaceLight
import com.jothivel.chits.ui.theme.TextGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

private fun rupees(paise: Long): String = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).format(paise / 100)

private data class AgentGroupStat(
    val group: ChitGroupEntity,
    val memberCount: Int,
    val targetPaise: Long,
    val collectedPaise: Long
)

private data class AgentCollectionEntry(
    val memberName: String,
    val groupName: String,
    val amountPaise: Long,
    val date: String,
    val mode: String
)

/**
 * Admin-only drill-down for one field agent: what's assigned to them, how much they're
 * supposed to collect (target = chit value × active members, per assigned group), how
 * much they've actually collected so far (live from Firestore `collections`, not just
 * whatever happens to be in local Room), how much is still pending, and a day-by-day
 * timeline of their recent collections.
 */
@Composable
fun AgentDetailScreen(agent: AgentSummary, onBack: () -> Unit) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var groupStats by remember { mutableStateOf<List<AgentGroupStat>>(emptyList()) }
    var recent by remember { mutableStateOf<List<AgentCollectionEntry>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val firebaseNotConfiguredText = stringResource(R.string.agentdetail_firebase_not_configured)
    val couldNotLoadText = stringResource(R.string.agentdetail_could_not_load)
    val memberFallback = stringResource(R.string.addmember_members_title)
    val fallbackLabourName = stringResource(R.string.labour_fallback_name)
    val chitFallback = stringResource(R.string.collection_chit_fallback)

    LaunchedEffect(agent.id) {
        loading = true
        loadError = null
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val assignedGroups = db.groupDao().getAllGroupsSync().filter { it.id in agent.assignedGroups }
                val groupNameById = assignedGroups.associate { it.id to (it.name ?: it.registerNo ?: it.id) }

                val collectedByGroup = mutableMapOf<String, Long>()
                val recentList = mutableListOf<AgentCollectionEntry>()
                val firestore = FirebaseSetup.firestoreIfSignedIn(context)
                if (firestore != null) {
                    val docs = firestore.collection(FirestoreSchema.COLLECTIONS)
                        .whereEqualTo(FirestoreSchema.Collection.AGENT_ID, agent.id)
                        .get().await().documents
                    docs.forEach { doc ->
                        val groupId = doc.getString(FirestoreSchema.Collection.GROUP_ID) ?: return@forEach
                        val amt = doc.getLong(FirestoreSchema.Collection.AMOUNT_PAISE) ?: 0L
                        collectedByGroup[groupId] = (collectedByGroup[groupId] ?: 0L) + amt
                        recentList += AgentCollectionEntry(
                            memberName = doc.getString(FirestoreSchema.Collection.MEMBER_NAME)?.ifBlank { null } ?: memberFallback,
                            groupName = groupNameById[groupId] ?: groupId,
                            amountPaise = amt,
                            date = doc.getString(FirestoreSchema.Collection.BUSINESS_DATE).orEmpty(),
                            mode = doc.getString(FirestoreSchema.Collection.MODE).orEmpty()
                        )
                    }
                } else {
                    loadError = firebaseNotConfiguredText
                }

                groupStats = assignedGroups.map { g ->
                    val memberCount = db.membershipDao().countActiveForGroupSync(g.id)
                    AgentGroupStat(g, memberCount, g.chitValue.toLong() * memberCount, collectedByGroup[g.id] ?: 0L)
                }
                recent = recentList.sortedByDescending { it.date }.take(30)
            } catch (e: Exception) {
                loadError = e.message ?: couldNotLoadText
            }
        }
        loading = false
    }

    val totalTarget = groupStats.sumOf { it.targetPaise }
    val totalCollected = groupStats.sumOf { it.collectedPaise }
    val totalPending = (totalTarget - totalCollected).coerceAtLeast(0)
    val progress = if (totalTarget > 0) (totalCollected.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f) else 0f

    Column(Modifier.fillMaxSize().background(MaroonBackground)) {
        Row(
            Modifier.fillMaxWidth().background(MaroonPrimary).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(agent.name.ifBlank { fallbackLabourName }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(agent.phone, color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (agent.isActive) AccentGreen.copy(alpha = .18f) else AccentRed.copy(alpha = .18f)
            ) {
                Text(
                    if (agent.isActive) stringResource(R.string.labour_active) else stringResource(R.string.labour_disconnected),
                    color = if (agent.isActive) AccentGreen else AccentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaroonPrimary) }
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            loadError?.let { msg -> item { Text(msg, color = AccentRed, fontSize = 11.sp) } }

            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(stringResource(R.string.agentdetail_overall_progress), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${(progress * 100).toInt()}%", color = MaroonPrimary, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.agentdetail_of_target), color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)),
                            color = MaroonPrimary,
                            trackColor = MaroonSurfaceLight
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatBlock(stringResource(R.string.agentdetail_target), rupees(totalTarget), TextGray)
                            StatBlock(stringResource(R.string.agentdetail_collected), rupees(totalCollected), AccentGreen)
                            StatBlock(stringResource(R.string.agentdetail_pending), rupees(totalPending), AccentRed)
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Groups, null, tint = MaroonPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.agentdetail_assigned_chits, groupStats.size), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            if (groupStats.isEmpty()) {
                item { Text(stringResource(R.string.agentdetail_no_chits_assigned), color = TextGray, fontSize = 11.sp) }
            } else {
                items(groupStats, key = { it.group.id }) { stat -> AgentGroupCard(stat, chitFallback) }
            }

            item {
                Text(stringResource(R.string.agentdetail_recent_collections), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            if (recent.isEmpty()) {
                item { Text(stringResource(R.string.agentdetail_no_collections), color = TextGray, fontSize = 11.sp) }
            } else {
                items(recent, key = { "${it.date}-${it.memberName}-${it.amountPaise}" }) { entry -> AgentCollectionRow(entry) }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = TextGray, fontSize = 10.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun AgentGroupCard(stat: AgentGroupStat, chitFallback: String) {
    val progress = if (stat.targetPaise > 0) (stat.collectedPaise.toFloat() / stat.targetPaise.toFloat()).coerceIn(0f, 1f) else 0f
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stat.group.name ?: stat.group.registerNo ?: chitFallback,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(stringResource(R.string.agentdetail_members_count, stat.memberCount), color = TextGray, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = AccentGold,
                trackColor = MaroonSurfaceLight
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.agentdetail_target_amount, rupees(stat.targetPaise)), color = TextGray, fontSize = 10.sp)
                Text(stringResource(R.string.agentdetail_collected_amount, rupees(stat.collectedPaise)), color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.agentdetail_pending_amount, rupees((stat.targetPaise - stat.collectedPaise).coerceAtLeast(0))), color = AccentRed, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AgentCollectionRow(entry: AgentCollectionEntry) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(AccentGreen, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.memberName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text("${entry.groupName} • ${entry.mode.ifBlank { "—" }} • ${entry.date.ifBlank { "—" }}", color = TextGray, fontSize = 10.sp)
        }
        Text(rupees(entry.amountPaise), color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
