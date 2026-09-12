package com.jothivel.chits.ui.members

import android.os.Bundle
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.ChitMembershipEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

import com.jothivel.chits.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.jothivel.chits.ui.components.AppBottomNavigationBar
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.ui.theme.*
private val GreenAccent = Color(0xFF1E6B3B)
private val RedAccent = Color(0xFFC62828)

class AddMemberActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[MemberViewModel::class.java]
        setContent {
            JothiVelChitsTheme {
                AddMemberPage(
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
fun AddMemberPage(showBottomBar: Boolean = true,
    viewModel: MemberViewModel,
    onBack: () -> Unit
) {
    val allMembers by viewModel.getAllMembers().observeAsState(emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var memberToEdit by remember { mutableStateOf<MemberEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.members), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaroonPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (showBottomBar) AppBottomNavigationBar(currentRoute = "Members")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = OffWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Add Member Form (in a styled card) ────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                AddMemberFormCard(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                    memberToEdit = memberToEdit,
                    onMemberAdded = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Member saved successfully!")
                            memberToEdit = null
                        }
                    }
                )
            }

            // ── Members List ──────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "All Members (${allMembers.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (allMembers.isEmpty()) {
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
                            Text("No members yet", color = Color.Gray, fontSize = 14.sp)
                            Text("Fill the form above to add your first member", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(allMembers, key = { it.id }) { member ->
                    MemberRow(
                        member = member,
                        onEditClick = {
                            memberToEdit = member
                            scope.launch {
                                snackbarHostState.showSnackbar("Editing ${member.name}. Scroll up to view form.")
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { Spacer(modifier = Modifier.height(100.dp)) } // Extra space for floating nav bar
        }
    }
}

// ─── Styled Form Card ─────────────────────────────────────────────────────────
@Composable
fun AddMemberFormCard(
    viewModel: MemberViewModel,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    memberToEdit: MemberEntity? = null,
    onMemberAdded: () -> Unit
) {
    val allGroups by viewModel.allGroups.observeAsState(emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current

    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 5

    // Step 1
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    // Step 2
    var addressLine by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateVal by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    // Step 3
    var selectedGroup by remember { mutableStateOf<ChitGroupEntity?>(null) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var installmentAmt by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    // Step 4
    var aadhaar by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var aadhaarUri by remember { mutableStateOf<Uri?>(null) }
    var panUri by remember { mutableStateOf<Uri?>(null) }
    
    val aadhaarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) aadhaarUri = uri
    }
    val panPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) panUri = uri
    }
    // Step 5
    var nomineeName by remember { mutableStateOf("") }
    var nomineeRel by remember { mutableStateOf("") }
    var nomineeMob by remember { mutableStateOf("") }

    LaunchedEffect(memberToEdit) {
        if (memberToEdit != null) {
            name = memberToEdit.name ?: ""
            mobile = memberToEdit.phone ?: ""
            dob = memberToEdit.dob ?: ""
            gender = memberToEdit.gender ?: ""
            addressLine = memberToEdit.addressLine ?: ""
            city = memberToEdit.city ?: ""
            stateVal = memberToEdit.state ?: ""
            pincode = memberToEdit.pincode ?: ""
            nomineeName = memberToEdit.nomineeName ?: ""
            nomineeRel = memberToEdit.nomineeRelationship ?: ""
            nomineeMob = memberToEdit.nomineePhone ?: ""
            // Not decrypting Aadhaar/PAN for now for simplicity, they will need re-entry or keep as is if unchanged
            currentStep = 1
        } else {
            // Reset form
            name = ""; mobile = ""; dob = ""; gender = ""
            addressLine = ""; city = ""; stateVal = ""; pincode = ""
            selectedGroup = null; installmentAmt = ""; joiningDate = ""; dueDate = ""
            aadhaar = ""; pan = ""; aadhaarUri = null; panUri = null
            nomineeName = ""; nomineeRel = ""; nomineeMob = ""
            currentStep = 1
        }
    }

    fun resetForm() {
        currentStep = 1
        name = ""; mobile = ""; dob = ""; gender = ""
        addressLine = ""; city = ""; stateVal = ""; pincode = ""
        selectedGroup = null; installmentAmt = ""; joiningDate = ""; dueDate = ""
        aadhaar = ""; pan = ""
        nomineeName = ""; nomineeRel = ""; nomineeMob = ""
    }

    fun validateStep(): String? = when (currentStep) {
        1 -> when {
            name.isBlank() -> "Name is required"
            mobile.isBlank() -> "Mobile is required"
            dob.isBlank() -> "Date of birth is required"
            gender.isBlank() -> "Gender is required"
            else -> null
        }
        2 -> when {
            addressLine.isBlank() -> "Address is required"
            city.isBlank() -> "City is required"
            stateVal.isBlank() -> "State is required"
            pincode.isBlank() -> "Pincode is required"
            else -> null
        }
        3 -> when {
            selectedGroup == null -> "Please select a chit group"
            installmentAmt.isBlank() -> "Installment amount is required"
            joiningDate.isBlank() -> "Joining date is required"
            dueDate.isBlank() -> "Due date is required"
            else -> null
        }
        4 -> when {
            aadhaar.isBlank() -> "Aadhaar number is required"
            pan.isBlank() -> "PAN is required"
            else -> null
        }
        5 -> when {
            nomineeName.isBlank() -> "Nominee name is required"
            nomineeRel.isBlank() -> "Relationship is required"
            nomineeMob.isBlank() -> "Nominee mobile is required"
            else -> null
        }
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.background(brush = LightMaroonGradient).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalSteps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (i <= currentStep) MaterialTheme.colorScheme.primary else Color.LightGray,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val stepLabel = when (currentStep) {
                1 -> "Personal"; 2 -> "Address"; 3 -> "Chit"
                4 -> "KYC"; 5 -> "Nominee"; else -> ""
            }
            Text("Step $currentStep of $totalSteps — $stepLabel", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Animated step content
            AnimatedContent<Int>(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState)
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                    else
                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                },
                label = "StepAnim"
            ) { step ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (step) {
                        1 -> {
                            Box(
                                modifier = Modifier.size(56.dp).background(Color(0xFFF5F5F5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(painterResource(id = R.drawable.ic_add), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp)) }
                            Spacer(modifier = Modifier.height(12.dp))
                            LabeledTextField("Name *", name, { name = it })
                            LabeledTextField("Mobile *", mobile, { mobile = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DatePickerField("Date of Birth *", dob, { dob = it }, Modifier.weight(1f))
                                DropdownField("Gender *", gender, listOf("Male", "Female", "Other"), { gender = it }, Modifier.weight(1f))
                            }
                        }
                        2 -> {
                            LabeledTextField("Address *", addressLine, { addressLine = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LabeledTextField("City *", city, { city = it }, Modifier.weight(1f))
                                LabeledTextField("State *", stateVal, { stateVal = it }, Modifier.weight(1f))
                            }
                            LabeledTextField("Pincode *", pincode, { pincode = it }, Modifier.fillMaxWidth(0.5f))
                        }
                        3 -> {
                            Text("Select Chit Group *", color = Color.Gray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (groupDropdownExpanded) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { groupDropdownExpanded = !groupDropdownExpanded }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedGroup?.let {
                                            "${it.registerNo} · Rs ${it.chitValue / 100} · ${it.subscriberCount} members"
                                        } ?: if (allGroups.isEmpty()) "No groups available" else "Select a chit group",
                                        fontSize = 14.sp,
                                        color = if (selectedGroup != null) Color.Black else Color.Gray
                                    )
                                    Icon(
                                        if (groupDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null, tint = Color.Gray
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = groupDropdownExpanded,
                                onDismissRequest = { groupDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.88f)
                            ) {
                                if (allGroups.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No chit groups yet. Create one first.", color = Color.Gray, fontSize = 13.sp) },
                                        onClick = { groupDropdownExpanded = false }
                                    )
                                } else {
                                    allGroups.forEach { group ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(group.registerNo ?: "Unknown", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    Text(
                                                        "Rs ${group.chitValue / 100} · ${group.subscriberCount} members · ${group.durationMonths}mo",
                                                        fontSize = 12.sp, color = Color.Gray
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedGroup = group
                                                groupDropdownExpanded = false
                                            },
                                            modifier = Modifier.background(
                                                if (selectedGroup?.id == group.id) Color(0xFFF9EAEA) else Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            LabeledTextField("Installment Amount *", installmentAmt, { installmentAmt = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DatePickerField("Joining Date *", joiningDate, { joiningDate = it }, Modifier.weight(1f))
                                DatePickerField("Due Date *", dueDate, { dueDate = it }, Modifier.weight(1f))
                            }
                        }
                        4 -> {
                            LabeledTextField("Aadhaar Number *", aadhaar, { aadhaar = it })
                            LabeledTextField("PAN *", pan, { pan = it })
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Documents", color = Color.Gray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { aadhaarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f).height(80.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (aadhaarUri != null) Color(0xFFE8F5E9) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        if (aadhaarUri != null) "Aadhaar\nSelected ✓" else "Upload\nAadhaar copy",
                                        color = if (aadhaarUri != null) Color(0xFF2E7D32) else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = { panPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f).height(80.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (panUri != null) Color(0xFFE8F5E9) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        if (panUri != null) "PAN\nSelected ✓" else "Upload\nPAN copy",
                                        color = if (panUri != null) Color(0xFF2E7D32) else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp)).padding(12.dp)
                            ) {
                                Text("Aadhaar is encrypted on save and never shown in plain text again.", color = Color(0xFFE65100), fontSize = 12.sp)
                            }
                        }
                        5 -> {
                            LabeledTextField("Nominee Name *", nomineeName, { nomineeName = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LabeledTextField("Relationship *", nomineeRel, { nomineeRel = it }, Modifier.weight(1f))
                                LabeledTextField("Mobile *", nomineeMob, { nomineeMob = it }, Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp)).padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Review before adding", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ReviewRow("Name", name)
                                    ReviewRow("Mobile", mobile)
                                    ReviewRow("Chit Group", selectedGroup?.registerNo ?: "—")
                                    ReviewRow("City", city)
                                }
                            }
                        }
                    }
                }
            }

            // Navigation buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Back", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        val error = validateStep()
                        if (error != null) {
                            scope.launch { snackbarHostState.showSnackbar(error) }
                        } else if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            val member = MemberEntity().apply {
                                id = memberToEdit?.id ?: UUID.randomUUID().toString()
                                role = "MEMBER"
                                isActive = true
                                this.name = name
                                phone = mobile
                                this.dob = dob
                                this.gender = gender
                                this.addressLine = addressLine
                                this.city = city
                                this.state = stateVal
                                this.pincode = pincode
                                selectedChitId = selectedGroup?.id
                                ticketNo = null
                                installmentAmount = installmentAmt
                                this.joiningDate = joiningDate
                                this.dueDate = dueDate
                                if (aadhaar.isNotBlank()) aadhaarNoEncrypted = EncryptionUtils.encrypt(aadhaar)
                                if (pan.isNotBlank()) panNo = pan
                                aadhaarDocumentPath = aadhaarUri?.toString()
                                panDocumentPath = panUri?.toString()
                                this.nomineeName = nomineeName
                                nomineeRelationship = nomineeRel
                                nomineePhone = nomineeMob
                            }
                            viewModel.insertMember(member)
                            // Also create ChitMembership link if a group is selected
                            if (selectedGroup != null) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val db = AppDatabase.getDatabase(context)
                                        val existing = db.membershipDao().getSync(member.id, selectedGroup!!.id)
                                        if (existing == null) {
                                            val membership = ChitMembershipEntity().apply {
                                                id = UUID.randomUUID().toString()
                                                memberId = member.id
                                                groupId = selectedGroup!!.id
                                                ticketNo = null
                                                installmentAmountPaise = (installmentAmt.toLongOrNull() ?: 0L) * 100
                                                this.joiningDate = joiningDate
                                                this.dueDate = dueDate
                                                isActive = true
                                            }
                                            db.membershipDao().insert(membership)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            resetForm()
                            onMemberAdded()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (currentStep == totalSteps) "Add Member" else "Next", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Member row in the list ──────────────────────────────────────────────────
@Composable
fun MemberRow(member: MemberEntity, onEditClick: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(brush = LightMaroonGradient).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        member.name?.take(2)?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(member.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(member.phone ?: "No Phone", color = Color.Gray, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (member.isActive) GreenAccent.copy(alpha = 0.15f) else RedAccent.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (member.isActive) "Active" else "Blocked",
                        color = if (member.isActive) GreenAccent else RedAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem(icon = Icons.Default.DateRange, label = "DOB", value = member.dob?.takeIf { it.isNotBlank() } ?: "—")
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailItem(icon = Icons.Default.Person, label = "Gender", value = member.gender?.takeIf { it.isNotBlank() } ?: "—")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem(icon = Icons.Default.CreditCard, label = "Aadhar", value = member.aadhaarNoEncrypted?.takeIf { it.isNotBlank() }?.let { com.jothivel.chits.utils.EncryptionUtils.decrypt(it).takeLast(4).padStart(12, '*') } ?: "—")
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailItem(icon = Icons.Default.LocationCity, label = "City", value = member.city?.takeIf { it.isNotBlank() } ?: "—")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                DetailItem(icon = Icons.Default.Home, label = "Address", value = member.addressLine?.takeIf { it.isNotBlank() } ?: "—")

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        try { onEditClick() } catch (e: Exception) { e.printStackTrace() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Member Details", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Review row ──────────────────────────────────────────────────────────────
@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label:", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.width(90.dp))
        Text(value.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Shared LabeledTextField ─────────────────────────────────────────────────
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 14.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFBDBDBD)
            )
        )
    }
}

// ─── Shared DatePickerField ──────────────────────────────────────────────────
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH)
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, y, m, d ->
            val formattedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
            onValueChange(formattedDate)
        }, year, month, day
    )

    Column(modifier = modifier.padding(bottom = 14.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color(0xFFBDBDBD)
                ),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) }
            )
        }
    }
}

// ─── Shared DropdownField ────────────────────────────────────────────────────
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.padding(bottom = 14.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color(0xFFBDBDBD)
                ),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            onValueChange(opt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
