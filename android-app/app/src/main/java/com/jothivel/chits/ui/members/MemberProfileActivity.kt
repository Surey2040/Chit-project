package com.jothivel.chits.ui.members

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.ui.base.BaseActivity
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.ui.theme.JothiVelChitsTheme
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.OffWhite

class MemberProfileActivity : BaseActivity() {

    companion object {
        const val EXTRA_MEMBER_ID = "EXTRA_MEMBER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val memberId = intent.getStringExtra(EXTRA_MEMBER_ID) ?: run { finish(); return }
        val viewModel = ViewModelProvider(this)[MemberViewModel::class.java]

        setContent {
            JothiVelChitsTheme {
                MemberProfileScreen(
                    memberId = memberId,
                    viewModel = viewModel,
                    onBack = { SmoothTransitions.finishSmooth(this) },
                    onSaved = {
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    }
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
fun MemberProfileScreen(
    memberId: String,
    viewModel: MemberViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val member by viewModel.getMemberById(memberId).observeAsState()
    var isEditMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Profile" else "Member Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditMode) {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaroonPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = OffWhite
    ) { padding ->
        AnimatedContent(
            targetState = member,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "profileContent"
        ) { m ->
            if (m == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaroonPrimary)
                }
            } else {
                AnimatedContent(
                    targetState = isEditMode,
                    transitionSpec = {
                        if (targetState)
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        else
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    },
                    label = "editModeAnim"
                ) { editMode ->
                    if (editMode) {
                        MemberEditForm(
                            member = m,
                            viewModel = viewModel,
                            onCancel = { isEditMode = false },
                            onSaved = {
                                isEditMode = false
                                onSaved()
                            },
                            paddingValues = padding
                        )
                    } else {
                        MemberProfileView(member = m, paddingValues = padding)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberProfileView(member: MemberEntity, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
    ) {
        // Header card with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaroonPrimary, Color(0xFF6B1A2A))
                    )
                )
                .padding(bottom = 32.dp, top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (member.name?.take(1) ?: "?").uppercase(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = member.name ?: "Unknown",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (member.isActive) Color(0xFF1E6B3B) else Color(0xFFC62828)
                ) {
                    Text(
                        text = if (member.isActive) "● Active" else "● Blocked",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Quick actions
        Surface(
            modifier = Modifier.fillMaxWidth().offset(y = (-16).dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = OffWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val phone = member.phone ?: return@OutlinedButton
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaroonPrimary)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call", color = MaroonPrimary, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val phone = member.phone ?: return@Button
                        val uri = Uri.parse("https://wa.me/91$phone")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.whatsapp")
                        try { context.startActivity(intent) }
                        catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Details sections
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileSection(title = "Personal Details", icon = Icons.Default.Person) {
                ProfileRow("Mobile", member.phone ?: "—")
                ProfileRow("Date of Birth", member.dob ?: "—")
                ProfileRow("Gender", member.gender ?: "—")
            }

            ProfileSection(title = "Address", icon = Icons.Default.Home) {
                ProfileRow("Address", member.addressLine ?: "—")
                ProfileRow("City", member.city ?: "—")
                ProfileRow("State", member.state ?: "—")
                ProfileRow("Pincode", member.pincode ?: "—")
            }

            ProfileSection(title = "Chit Details", icon = Icons.Default.CreditCard) {
                ProfileRow("Chit Group ID", member.selectedChitId ?: "—")
                ProfileRow("Ticket No", member.ticketNo ?: "—")
                ProfileRow("Installment", member.installmentAmount ?: "—")
                ProfileRow("Joining Date", member.joiningDate ?: "—")
                ProfileRow("Due Date", member.dueDate ?: "—")
            }

            ProfileSection(title = "Nominee Details", icon = Icons.Default.People) {
                ProfileRow("Name", member.nomineeName ?: "—")
                ProfileRow("Relationship", member.nomineeRelationship ?: "—")
                ProfileRow("Phone", member.nomineePhone ?: "—")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaroonPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF333333))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.weight(0.55f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun MemberEditForm(
    member: MemberEntity,
    viewModel: MemberViewModel,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    paddingValues: PaddingValues
) {
    var name by remember { mutableStateOf(member.name ?: "") }
    var phone by remember { mutableStateOf(member.phone ?: "") }
    var dob by remember { mutableStateOf(member.dob ?: "") }
    var gender by remember { mutableStateOf(member.gender ?: "") }
    var addressLine by remember { mutableStateOf(member.addressLine ?: "") }
    var city by remember { mutableStateOf(member.city ?: "") }
    var state by remember { mutableStateOf(member.state ?: "") }
    var pincode by remember { mutableStateOf(member.pincode ?: "") }
    var nomineeName by remember { mutableStateOf(member.nomineeName ?: "") }
    var nomineeRel by remember { mutableStateOf(member.nomineeRelationship ?: "") }
    var nomineePhone by remember { mutableStateOf(member.nomineePhone ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Personal Details", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        EditCard {
            EditField("Full Name", name, { name = it })
            EditField("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            EditField("Date of Birth (YYYY-MM-DD)", dob, { dob = it })
            EditField("Gender", gender, { gender = it })
        }

        Text("Address", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        EditCard {
            EditField("Address Line", addressLine, { addressLine = it })
            EditField("City", city, { city = it })
            EditField("State", state, { state = it })
            EditField("Pincode", pincode, { pincode = it }, keyboardType = KeyboardType.Number)
        }

        Text("Nominee Details", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        EditCard {
            EditField("Nominee Name", nomineeName, { nomineeName = it })
            EditField("Relationship", nomineeRel, { nomineeRel = it })
            EditField("Nominee Phone", nomineePhone, { nomineePhone = it }, keyboardType = KeyboardType.Phone)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Cancel") }

            Button(
                onClick = {
                    val updated = member.copy()
                    updated.name = name
                    updated.phone = phone
                    updated.dob = dob
                    updated.gender = gender
                    updated.addressLine = addressLine
                    updated.city = city
                    updated.state = state
                    updated.pincode = pincode
                    updated.nomineeName = nomineeName
                    updated.nomineeRelationship = nomineeRel
                    updated.nomineePhone = nomineePhone
                    viewModel.updateMember(updated)
                    onSaved()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun EditCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaroonPrimary,
            unfocusedBorderColor = Color(0xFFBDBDBD)
        )
    )
}

// Extension to copy MemberEntity (Java class, no data class copy())
private fun MemberEntity.copy(): MemberEntity {
    val m = MemberEntity()
    m.id = this.id
    m.name = this.name
    m.phone = this.phone
    m.photoUrl = this.photoUrl
    m.nomineeName = this.nomineeName
    m.nomineePhone = this.nomineePhone
    m.nomineeRelationship = this.nomineeRelationship
    m.role = this.role
    m.isActive = this.isActive
    m.dob = this.dob
    m.gender = this.gender
    m.addressLine = this.addressLine
    m.city = this.city
    m.state = this.state
    m.pincode = this.pincode
    m.aadhaarNoEncrypted = this.aadhaarNoEncrypted
    m.panNo = this.panNo
    m.aadhaarDocumentPath = this.aadhaarDocumentPath
    m.panDocumentPath = this.panDocumentPath
    m.selectedChitId = this.selectedChitId
    m.ticketNo = this.ticketNo
    m.installmentAmount = this.installmentAmount
    m.joiningDate = this.joiningDate
    m.dueDate = this.dueDate
    return m
}
