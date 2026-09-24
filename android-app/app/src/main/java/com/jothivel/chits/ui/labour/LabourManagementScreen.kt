package com.jothivel.chits.ui.labour

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R
import com.jothivel.chits.data.firebase.AgentAuthRepository
import com.jothivel.chits.data.firebase.AgentSummary
import com.jothivel.chits.data.firebase.FirebaseSetup
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.ChitMembershipEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.ui.theme.AccentGold
import com.jothivel.chits.ui.theme.AccentGreen
import com.jothivel.chits.ui.theme.AccentOrange
import com.jothivel.chits.ui.theme.AccentRed
import com.jothivel.chits.ui.theme.DividerGray
import com.jothivel.chits.ui.theme.MaroonBackground
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.MaroonSurfaceLight
import com.jothivel.chits.ui.theme.TextGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun rupees(paise: Long): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).format(paise / 100)

private data class MemberCollectionRow(
    val membership: ChitMembershipEntity,
    val member: MemberEntity?,
    val collectedThisMonth: Boolean,
    val collectedAmount: Long
)

@Composable
fun LabourManagementScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var resetAgent by remember { mutableStateOf<AgentSummary?>(null) }
    var assignAgent by remember { mutableStateOf<AgentSummary?>(null) }
    var deleteAgent by remember { mutableStateOf<AgentSummary?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pre-resolved strings for use inside non-composable coroutine/click closures below.
    val couldNotLoadText = stringResource(R.string.labour_could_not_load)
    val noneYetText = stringResource(R.string.labour_none_yet)
    val firebaseNotReadyText = stringResource(R.string.labour_firebase_not_ready)
    val labourAddedText = stringResource(R.string.labour_added_toast)
    val couldNotCreateText = stringResource(R.string.labour_could_not_create)
    val pinResetFailedText = stringResource(R.string.labour_pin_reset_failed)
    val assignFailedText = stringResource(R.string.labour_assign_failed)
    val deleteFallbackName = stringResource(R.string.labour_delete_fallback_name)
    val deleteMessageTemplate = stringResource(R.string.labour_delete_message)
    val deletedToastText = stringResource(R.string.labour_deleted_toast)
    val deleteFailedText = stringResource(R.string.labour_delete_failed)

    val firebaseReady = remember(refreshKey) { FirebaseSetup.firestoreOrNull(context) != null }
    val agentsResult by produceState<Result<List<AgentSummary>>>(initialValue = Result.success(emptyList()), refreshKey) {
        value = withContext(Dispatchers.IO) { runCatching { AgentAuthRepository.listAgents(context) } }
    }
    val agents = agentsResult.getOrDefault(emptyList())
    val groups by produceState(initialValue = emptyList<ChitGroupEntity>(), refreshKey) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).groupDao().getAllGroupsSync().filter { it.status.isNullOrBlank() || it.status == "ACTIVE" }
        }
    }

    Box(Modifier.fillMaxSize().background(MaroonBackground)) {
        Column(Modifier.fillMaxSize()) {
            LabourTopBar(onBack)
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (!firebaseReady) { item { FirebaseMissingBanner() } }
                errorMessage?.let { item { FirebaseErrorBanner(it) } }
                agentsResult.exceptionOrNull()?.let { item { FirebaseErrorBanner(it.message ?: couldNotLoadText) } }
                if (agents.isEmpty()) {
                    item { EmptyLabourState(if (firebaseReady) noneYetText else firebaseNotReadyText) }
                } else {
                    items(agents, key = { it.id }) { agent ->
                        LabourAgentCard(
                            agent = agent, groups = groups,
                            onToggle = { scope.launch { withContext(Dispatchers.IO) { AgentAuthRepository.setActive(context, agent.id, !agent.isActive) }; refreshKey++ } },
                            onResetPin = { resetAgent = agent },
                            onAssign = { assignAgent = agent },
                            onDelete = { deleteAgent = agent }
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaroonPrimary, contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text(stringResource(R.string.labour_add)) }
        )
    }

    if (showAdd) AgentEditDialog(title = stringResource(R.string.labour_add), confirm = stringResource(R.string.labour_create), onDismiss = { showAdd = false }) { name, phone, pin ->
        scope.launch {
            val result = withContext(Dispatchers.IO) { AgentAuthRepository.createAgent(context, name, phone, pin) }
            result.onSuccess { errorMessage = null; showAdd = false; refreshKey++; Toast.makeText(context, labourAddedText, Toast.LENGTH_LONG).show() }
                .onFailure { errorMessage = it.message ?: couldNotCreateText; Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show() }
        }
    }

    resetAgent?.let { agent ->
        PinDialog(agent.name, onDismiss = { resetAgent = null }) { pin ->
            scope.launch {
                val result = withContext(Dispatchers.IO) { AgentAuthRepository.resetPin(context, agent.id, pin) }
                result.onSuccess { errorMessage = null; resetAgent = null; refreshKey++ }
                    .onFailure { errorMessage = it.message ?: pinResetFailedText; Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show() }
            }
        }
    }

    assignAgent?.let { agent ->
        AssignGroupsDialog(agent = agent, groups = groups, onDismiss = { assignAgent = null }) { selected ->
            scope.launch {
                val result = withContext(Dispatchers.IO) { AgentAuthRepository.setAssignedGroups(context, agent.id, selected) }
                result.onSuccess { errorMessage = null; assignAgent = null; refreshKey++ }
                    .onFailure { errorMessage = it.message ?: assignFailedText; Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show() }
            }
        }
    }

    deleteAgent?.let { agent ->
        AlertDialog(
            onDismissRequest = { deleteAgent = null },
            title = { Text(stringResource(R.string.labour_delete_title)) },
            text = { Text(String.format(deleteMessageTemplate, agent.name.ifBlank { deleteFallbackName }, agent.phone)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { AgentAuthRepository.deleteAgent(context, agent.id) }
                        result.onSuccess { errorMessage = null; deleteAgent = null; refreshKey++; Toast.makeText(context, deletedToastText, Toast.LENGTH_LONG).show() }
                            .onFailure { errorMessage = it.message ?: deleteFailedText; Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show() }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text(stringResource(R.string.labour_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteAgent = null }) { Text(stringResource(R.string.labour_cancel)) } }
        )
    }
}

// =============================================================================
// Labour Agent Card -- inline expandable, NO navigation to AgentDetailScreen
// =============================================================================
@Composable
private fun LabourAgentCard(
    agent: AgentSummary,
    groups: List<ChitGroupEntity>,
    onToggle: () -> Unit,
    onResetPin: () -> Unit,
    onAssign: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, animationSpec = tween(250), label = "la")
    val statusTint = if (agent.isActive) AccentGreen else AccentRed
    val assignedGroups = groups.filter { it.id in agent.assignedGroups }

    Surface(
        shape = RoundedCornerShape(16.dp), color = Color.White,
        border = BorderStroke(1.dp, if (expanded) MaroonPrimary.copy(alpha = .28f) else DividerGray),
        shadowElevation = if (expanded) 3.dp else 1.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PersonPin, null, tint = MaroonPrimary, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(agent.name.ifBlank { stringResource(R.string.labour_fallback_name) }, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.labour_phone_chits_assigned, agent.phone, agent.assignedGroups.size), color = TextGray, fontSize = 10.sp)
                    }
                    Text(if (agent.isActive) stringResource(R.string.labour_active) else stringResource(R.string.labour_disconnected), color = statusTint, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = AccentRed, modifier = Modifier.size(17.dp))
                    }
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = MaroonPrimary,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = arrowRotation })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    LabourAction(if (agent.isActive) stringResource(R.string.labour_disconnect) else stringResource(R.string.labour_reconnect), Icons.Default.PersonOff, statusTint, Modifier.weight(1f), onToggle)
                    LabourAction(stringResource(R.string.labour_set_pin), Icons.Default.LockReset, MaroonPrimary, Modifier.weight(1f), onResetPin)
                    LabourAction(stringResource(R.string.labour_assign), Icons.Default.Groups, MaroonPrimary, Modifier.weight(1f), onAssign)
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(tween(280)) + fadeIn(tween(200)), exit = shrinkVertically(tween(220)) + fadeOut(tween(150))) {
                Column(Modifier.fillMaxWidth().background(MaroonSurfaceLight.copy(alpha = .35f)).padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)) {
                    Divider(color = MaroonPrimary.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    if (assignedGroups.isEmpty()) {
                        Text(stringResource(R.string.labour_no_chits_assigned), fontSize = 11.sp, color = TextGray, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        assignedGroups.forEach { group ->
                            ChitDropdownForAgent(group = group)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChitDropdownForAgent(group: ChitGroupEntity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember(group.id) { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, animationSpec = tween(220), label = "ca")

    val rows by produceState(initialValue = emptyList<MemberCollectionRow>(), group.id, expanded) {
        if (expanded) {
            value = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(context)
                val memberships = db.membershipDao().getActiveForGroupSync(group.id)
                val allMembers = db.memberDao().getAllMembersSync().associateBy { it.id }
                val allPayments = db.paymentDao().getAllPaymentsSync()
                val monthKey = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
                memberships.map { ms ->
                    val paid = allPayments.filter { p ->
                        p.memberId == ms.memberId && p.groupId == group.id &&
                        SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date(p.paidAt)) == monthKey
                    }
                    MemberCollectionRow(ms, allMembers[ms.memberId], paid.isNotEmpty(), paid.sumOf { it.amountPaid })
                }.sortedWith(compareBy({ !it.collectedThisMonth }, { it.member?.name ?: "" }))
            }
        }
    }
    val collectedCount = rows.count { it.collectedThisMonth }
    val totalCount = rows.size

    Surface(shape = RoundedCornerShape(12.dp), color = Color.White,
        border = BorderStroke(1.dp, if (expanded) MaroonPrimary.copy(alpha = .22f) else DividerGray),
        shadowElevation = if (expanded) 2.dp else 0.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).background(MaroonPrimary.copy(alpha = .10f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Savings, null, tint = MaroonPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("${group.registerNo} • ${group.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (expanded && rows.isNotEmpty()) {
                        Text(stringResource(R.string.labour_collected_this_month, collectedCount, totalCount), fontSize = 9.sp,
                            color = if (collectedCount == totalCount) AccentGreen else AccentOrange)
                    } else {
                        Text(stringResource(R.string.labour_members_and_value, group.subscriberCount, rupees(group.chitValue.toLong() / 100)), fontSize = 9.sp, color = TextGray)
                    }
                }
                if (expanded && rows.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(20.dp), color = if (collectedCount == totalCount) AccentGreen.copy(.12f) else AccentOrange.copy(.12f)) {
                        Text("$collectedCount/$totalCount", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = if (collectedCount == totalCount) AccentGreen else AccentOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaroonPrimary,
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = arrowRotation })
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(tween(240)) + fadeIn(tween(180)), exit = shrinkVertically(tween(200)) + fadeOut(tween(130))) {
                Column(Modifier.fillMaxWidth().background(MaroonSurfaceLight.copy(.30f)).padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp)) {
                    Divider(color = DividerGray, thickness = 0.5.dp)
                    Spacer(Modifier.height(6.dp))
                    if (rows.isEmpty()) {
                        Text(stringResource(R.string.labour_no_members_in_chit), fontSize = 10.sp, color = TextGray, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        val fraction = if (totalCount > 0) collectedCount.toFloat() / totalCount else 0f
                        Box(Modifier.fillMaxWidth().height(5.dp).background(DividerGray, RoundedCornerShape(3.dp))) {
                            Box(Modifier.fillMaxWidth(fraction).height(5.dp).background(
                                if (fraction >= 1f) AccentGreen else MaroonPrimary, RoundedCornerShape(3.dp)))
                        }
                        Spacer(Modifier.height(8.dp))
                        rows.forEachIndexed { idx, row ->
                            if (idx > 0) Divider(color = DividerGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                            MemberCollectionRowItem(row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCollectionRowItem(row: MemberCollectionRow) {
    val name = row.member?.name ?: row.membership.memberId
    val phone = row.member?.phone.orEmpty()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(
            if (row.collectedThisMonth) AccentGreen.copy(.12f) else AccentRed.copy(.10f), CircleShape
        ), contentAlignment = Alignment.Center) {
            Text(name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = if (row.collectedThisMonth) AccentGreen else AccentRed)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (phone.isNotBlank()) phone else stringResource(R.string.labour_no_phone), fontSize = 9.sp, color = TextGray, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (row.collectedThisMonth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, "Collected", tint = AccentGreen, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(rupees(row.collectedAmount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentGreen)
                }
                Text(stringResource(R.string.labour_collected_status), fontSize = 8.sp, color = AccentGreen)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PendingActions, "Pending", tint = AccentRed, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(rupees(row.membership.installmentAmountPaise), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentRed)
                }
                Text(stringResource(R.string.labour_pending_status), fontSize = 8.sp, color = AccentRed)
            }
        }
    }
}

// =============================================================================
// Supporting composables
// =============================================================================
@Composable
private fun LabourTopBar(onBack: () -> Unit) {
    Surface(color = MaroonPrimary, shadowElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text(stringResource(R.string.labour_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun FirebaseMissingBanner() {
    Surface(shape = RoundedCornerShape(14.dp), color = AccentGold.copy(.10f), border = BorderStroke(1.dp, AccentGold.copy(.35f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, null, tint = AccentGold)
            Column(Modifier.padding(start = 9.dp)) {
                Text(stringResource(R.string.labour_firebase_missing_title), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(stringResource(R.string.labour_firebase_missing_desc), color = TextGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun FirebaseErrorBanner(message: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = AccentRed.copy(.08f), border = BorderStroke(1.dp, AccentRed.copy(.35f))) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(stringResource(R.string.labour_firebase_error_title), color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(message, color = TextGray, fontSize = 10.sp)
            Text(stringResource(R.string.labour_firebase_error_desc), color = TextGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LabourAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.height(34.dp).clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), color = tint.copy(.08f), border = BorderStroke(1.dp, tint.copy(.14f))) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AgentEditDialog(title: String, confirm: String, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && phone.filter(Char::isDigit).length >= 10 && pin.length == 4
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.labour_name_field)) }, singleLine = true)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, label = { Text(stringResource(R.string.labour_phone_field)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.labour_pin_field)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onSave(name.trim(), phone, pin) }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.labour_cancel)) } }
    )
}

@Composable
private fun PinDialog(agentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.labour_set_pin)) },
        text = { OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.labour_new_pin_field, agentName)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true) },
        confirmButton = { Button(enabled = pin.length == 4, onClick = { onSave(pin) }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text(stringResource(R.string.labour_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.labour_cancel)) } }
    )
}

@Composable
private fun AssignGroupsDialog(agent: AgentSummary, groups: List<ChitGroupEntity>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var selected by remember(agent.id, groups) { mutableStateOf(agent.assignedGroups.toSet()) }
    val chitFallback = stringResource(R.string.collection_chit_fallback)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.labour_assign_chits)) },
        text = {
            LazyColumn(Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(groups, key = { it.id }) { group ->
                    Row(Modifier.fillMaxWidth().clickable { selected = if (group.id in selected) selected - group.id else selected + group.id }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = group.id in selected, onCheckedChange = { selected = if (it) selected + group.id else selected - group.id })
                        Column {
                            Text(group.registerNo ?: group.id, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(group.name ?: chitFallback, color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(selected.toList()) }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Icon(Icons.Default.Save, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.labour_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.labour_cancel)) } }
    )
}

@Composable
private fun EmptyLabourState(title: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Groups, null, tint = MaroonPrimary)
            Spacer(Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(stringResource(R.string.labour_empty_subtitle), color = TextGray, fontSize = 10.sp)
        }
    }
}
