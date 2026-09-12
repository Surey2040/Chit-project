package com.jothivel.chits.ui.groups

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.jothivel.chits.R
import com.jothivel.chits.ui.components.AppBottomNavigationBar
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.models.ChitTemplate
import com.jothivel.chits.utils.CurrencyUtils
import com.jothivel.chits.utils.LanguageManager
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.ui.theme.*
import java.util.UUID

class AddGroupActivity : BaseActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        val languageCode = LanguageManager.getLanguage(newBase)
        super.attachBaseContext(LanguageManager.updateResources(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[GroupViewModel::class.java]

        setContent {
            JothiVelChitsTheme {
                AddGroupPage(
                    viewModel = viewModel,
                    onBack = { SmoothTransitions.finishSmooth(this) }
                )
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
fun AddGroupPage(viewModel: GroupViewModel, onBack: () -> Unit) {
    val allGroups by viewModel.allGroups.observeAsState(emptyList<ChitGroupEntity>())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.create_a_group), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OffWhite)
            )
        },
        containerColor = OffWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Form Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                AddGroupFormCard(
                    onSaveClick = { groupName, registerNo, chitValuePaise, durationMonths, branch, startDate, baseInstallment ->
                        val entity = ChitGroupEntity().apply {
                            this.id = UUID.randomUUID().toString()
                            this.name = groupName
                            this.registerNo = registerNo
                            this.chitValue = chitValuePaise.toInt()
                            this.durationMonths = durationMonths
                            this.subscriberCount = durationMonths
                            this.branch = branch
                            this.startDate = startDate
                            this.status = "ACTIVE"
                        }
                        viewModel.insertGroup(entity, baseInstallment)
                    }
                )
            }

            // Groups List Title
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "All Chit Groups (${allGroups.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Groups List
            if (allGroups.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No groups yet", color = Color.Gray, fontSize = 14.sp)
                            Text("Fill the form above to create one", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(allGroups, key = { it.id }) { group ->
                    GroupRow(group = group)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun AddGroupFormCard(
    onSaveClick: (groupName: String, registerNo: String, chitValue: Long, durationMonths: Int, branch: String, startDate: String, baseInstallment: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf(ChitTemplate.CUSTOM) }

    var groupName by remember { mutableStateOf("") }
    var registerNo by remember { mutableStateOf("") }
    var chitValueStr by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("Main") }
    var startDate by remember { mutableStateOf("2026-09-01") }

    val chitValue = chitValueStr.toLongOrNull() ?: 0L
    val durationMonths = durationStr.toIntOrNull() ?: 0
    val baseAmount = if (selectedTemplate != ChitTemplate.CUSTOM) selectedTemplate.baseInstallment.toLong() else (if (durationMonths > 0) chitValue / durationMonths else 0L)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.background(brush = LightMaroonGradient).padding(20.dp)) {
            Box(
                modifier = Modifier.size(56.dp).background(Color(0xFFF5F5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(painterResource(id = R.drawable.ic_add), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.height(16.dp))

            // Template Dropdown
            Box {
                OutlinedTextField(
                    value = selectedTemplate.title,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Chit Template") },
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.primary,
                        disabledLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ChitTemplate.values().forEach { template ->
                        DropdownMenuItem(
                            text = { Text(template.title) },
                            onClick = {
                                selectedTemplate = template
                                expanded = false
                                if (template != ChitTemplate.CUSTOM) {
                                    chitValueStr = template.chitValue.toString()
                                    durationStr = template.durationMonths.toString()
                                    groupName = template.title
                                } else {
                                    chitValueStr = ""
                                    durationStr = ""
                                    groupName = ""
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Chit Name (e.g., Diwali Chit)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFBDBDBD)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = registerNo,
                onValueChange = { registerNo = it },
                label = { Text("Register Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFBDBDBD)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = chitValueStr,
                onValueChange = { chitValueStr = it },
                label = { Text("Chit Value (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFBDBDBD)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = durationStr,
                onValueChange = { durationStr = it },
                label = { Text("Duration (Months) / Subscribers") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFBDBDBD)
                ),
                supportingText = { Text("Duration months must equal subscriber count") }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFBDBDBD)
                    )
                )

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFBDBDBD)
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (chitValue > 0 && durationMonths > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Group Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Base Monthly Due:", fontSize = 14.sp)
                            Text(CurrencyUtils.formatPaiseToRupee((baseAmount * 100).toInt()), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subscribers:", fontSize = 14.sp)
                            Text("$durationMonths", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val chitValuePaise = chitValue * 100
                    onSaveClick(groupName, registerNo, chitValuePaise, durationMonths, branch, startDate, baseAmount.toInt())
                    groupName = ""
                    registerNo = ""
                    chitValueStr = ""
                    durationStr = ""
                    selectedTemplate = ChitTemplate.CUSTOM
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                enabled = chitValue > 0 && durationMonths > 0 && branch.isNotBlank()
            ) {
                Text("Create Chit Group", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GroupRow(group: ChitGroupEntity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDetails by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = android.content.Intent(context, com.jothivel.chits.ui.members.MemberListActivity::class.java)
                intent.putExtra("GROUP_ID", group.id)
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.background(brush = LightMaroonGradient).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        group.registerNo?.take(1)?.uppercase() ?: "G",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name?.takeIf { it.isNotBlank() } ?: group.registerNo ?: "Unknown", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Reg No: ${group.registerNo}", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(onClick = { showDetails = !showDetails }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (showDetails) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Toggle details",
                        tint = Color.Gray
                    )
                }
            }
            if (showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Value", color = Color.Gray, fontSize = 11.sp)
                        Text(CurrencyUtils.formatPaiseToRupee(group.chitValue), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Members", color = Color.Gray, fontSize = 11.sp)
                        Text("${group.subscriberCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Status", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            group.status ?: "ACTIVE",
                            color = if (group.status == "ACTIVE") AccentGreen else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
