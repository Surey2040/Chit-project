package com.jothivel.chits.ui.labour

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.data.firebase.AgentAuthRepository
import com.jothivel.chits.data.firebase.AgentSummary
import com.jothivel.chits.data.firebase.FirebaseSetup
import com.jothivel.chits.data.firebase.FirestoreDataSync
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val firebaseReady = remember(refreshKey) { FirebaseSetup.firestoreOrNull(context) != null }
    val agentsResult by produceState<Result<List<AgentSummary>>>(initialValue = Result.success(emptyList()), refreshKey) {
        value = withContext(Dispatchers.IO) { runCatching { AgentAuthRepository.listAgents(context) } }
    }
    val agents = agentsResult.getOrDefault(emptyList())
    val groups by produceState(initialValue = emptyList<ChitGroupEntity>(), refreshKey) {
        value = withContext(Dispatchers.IO) { AppDatabase.getDatabase(context).groupDao().getAllGroupsSync().filter { it.status == "ACTIVE" } }
    }

    Box(Modifier.fillMaxSize().background(MaroonBackground)) {
        Column(Modifier.fillMaxSize()) {
            LabourTopBar(onBack)
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (!firebaseReady) {
                    item { FirebaseMissingBanner() }
                }
                errorMessage?.let { message ->
                    item { FirebaseErrorBanner(message) }
                }
                agentsResult.exceptionOrNull()?.let { issue ->
                    item { FirebaseErrorBanner(issue.message ?: "Could not load labour accounts") }
                }
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { FirestoreDataSync.syncAllToCloud(context) }
                                val flushed = withContext(Dispatchers.IO) { com.jothivel.chits.data.firebase.AgentCollectionSync.flushPending(context) }
                                result.onSuccess {
                                    errorMessage = null
                                    val suffix = if (flushed > 0) " • $flushed queued collections sent" else ""
                                    Toast.makeText(context, "Synced ${it.groups} chits, ${it.members} members$suffix", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    errorMessage = it.message ?: "Sync failed"
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Sync, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Sync Data to Cloud")
                    }
                }
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { FirestoreDataSync.restoreAllFromCloud(context) }
                                result.onSuccess {
                                    errorMessage = null
                                    refreshKey++
                                    Toast.makeText(context, "Restored ${it.groups} chits, ${it.members} members, ${it.collections} payments from cloud", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    errorMessage = it.message ?: "Restore failed"
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonSurfaceLight, contentColor = MaroonPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Restore Data from Cloud")
                    }
                }
                if (agents.isEmpty()) {
                    item { EmptyLabourState(if (firebaseReady) "No labour accounts yet" else "Firebase not ready") }
                } else {
                    items(agents, key = { it.id }) { agent ->
                        LabourAgentCard(
                            agent = agent,
                            onToggle = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { AgentAuthRepository.setActive(context, agent.id, !agent.isActive) }
                                    refreshKey++
                                }
                            },
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
            containerColor = MaroonPrimary,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add Labour") }
        )
    }

    if (showAdd) AgentEditDialog(
        title = "Add Labour",
        confirm = "Create",
        onDismiss = { showAdd = false },
        onSave = { name, phone, pin ->
            scope.launch {
                val result = withContext(Dispatchers.IO) { AgentAuthRepository.createAgent(context, name, phone, pin) }
                result.onSuccess {
                    errorMessage = null
                    showAdd = false
                    refreshKey++
                    Toast.makeText(context, "Labour added", Toast.LENGTH_LONG).show()
                }.onFailure {
                    errorMessage = it.message ?: "Could not create labour"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    resetAgent?.let { agent ->
        PinDialog(agent.name, onDismiss = { resetAgent = null }) { pin ->
            scope.launch {
                val result = withContext(Dispatchers.IO) { AgentAuthRepository.resetPin(context, agent.id, pin) }
                result.onSuccess {
                    errorMessage = null
                    resetAgent = null
                    refreshKey++
                }.onFailure {
                    errorMessage = it.message ?: "PIN reset failed"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    assignAgent?.let { agent ->
        AssignGroupsDialog(
            agent = agent,
            groups = groups,
            onDismiss = { assignAgent = null },
            onSave = { selected ->
                scope.launch {
                    val result = withContext(Dispatchers.IO) { AgentAuthRepository.setAssignedGroups(context, agent.id, selected) }
                    result.onSuccess {
                        errorMessage = null
                        assignAgent = null
                        refreshKey++
                    }.onFailure {
                        errorMessage = it.message ?: "Assign failed"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    deleteAgent?.let { agent ->
        AlertDialog(
            onDismissRequest = { deleteAgent = null },
            title = { Text("Delete labour account?") },
            text = { Text("This permanently removes ${agent.name.ifBlank { "this labour account" }} (${agent.phone}). They will no longer be able to log in. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { AgentAuthRepository.deleteAgent(context, agent.id) }
                            result.onSuccess {
                                errorMessage = null
                                deleteAgent = null
                                refreshKey++
                                Toast.makeText(context, "Labour account deleted", Toast.LENGTH_LONG).show()
                            }.onFailure {
                                errorMessage = it.message ?: "Delete failed"
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteAgent = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LabourTopBar(onBack: () -> Unit) {
    Surface(color = MaroonPrimary, shadowElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text("Labour", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun FirebaseMissingBanner() {
    Surface(shape = RoundedCornerShape(14.dp), color = AccentGold.copy(alpha = .10f), border = BorderStroke(1.dp, AccentGold.copy(alpha = .35f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, null, tint = AccentGold)
            Column(Modifier.padding(start = 9.dp)) {
                Text("Firebase not configured", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Add app/google-services.json and rebuild to enable labour sync.", color = TextGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun FirebaseErrorBanner(message: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = AccentRed.copy(alpha = .08f), border = BorderStroke(1.dp, AccentRed.copy(alpha = .35f))) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Firebase error", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(message, color = TextGray, fontSize = 10.sp)
            Text("Check Firestore Database is created and rules are deployed.", color = TextGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LabourAgentCard(agent: AgentSummary, onToggle: () -> Unit, onResetPin: () -> Unit, onAssign: () -> Unit, onDelete: () -> Unit) {
    val tint = if (agent.isActive) AccentGreen else AccentRed
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(MaroonSurfaceLight, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PersonPin, null, tint = MaroonPrimary)
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(agent.name.ifBlank { "Labour" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${agent.phone} • ${agent.assignedGroups.size} chits", color = TextGray, fontSize = 10.sp)
                }
                Text(if (agent.isActive) "Active" else "Disconnected", color = tint, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = AccentRed, modifier = Modifier.size(17.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                LabourAction(if (agent.isActive) "Disconnect" else "Reconnect", Icons.Default.PersonOff, tint, Modifier.weight(1f), onToggle)
                LabourAction("Set PIN", Icons.Default.LockReset, MaroonPrimary, Modifier.weight(1f), onResetPin)
                LabourAction("Assign", Icons.Default.Groups, MaroonPrimary, Modifier.weight(1f), onAssign)
            }
        }
    }
}

@Composable
private fun LabourAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.height(34.dp).clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), color = tint.copy(alpha = .08f), border = BorderStroke(1.dp, tint.copy(alpha = .14f))) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, label = { Text("4 digit PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onSave(name.trim(), phone, pin) }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PinDialog(agentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set PIN") },
        text = { OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, label = { Text("$agentName new PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true) },
        confirmButton = { Button(enabled = pin.length == 4, onClick = { onSave(pin) }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AssignGroupsDialog(agent: AgentSummary, groups: List<ChitGroupEntity>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var selected by remember(agent.id, groups) { mutableStateOf(agent.assignedGroups.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Chits") },
        text = {
            LazyColumn(Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(groups, key = { it.id }) { group ->
                    Row(Modifier.fillMaxWidth().clickable {
                        selected = if (group.id in selected) selected - group.id else selected + group.id
                    }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = group.id in selected, onCheckedChange = {
                            selected = if (it) selected + group.id else selected - group.id
                        })
                        Column {
                            Text(group.registerNo ?: group.id, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(group.name ?: "Chit", color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(selected.toList()) }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Icon(Icons.Default.Save, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyLabourState(title: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Groups, null, tint = MaroonPrimary)
            Spacer(Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Create labour accounts and assign chit groups.", color = TextGray, fontSize = 10.sp)
        }
    }
}
