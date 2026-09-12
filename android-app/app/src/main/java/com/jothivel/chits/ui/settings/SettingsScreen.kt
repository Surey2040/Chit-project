package com.jothivel.chits.ui.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R
import com.jothivel.chits.ui.components.AppBottomNavigationBar
import com.jothivel.chits.ui.theme.OffWhite
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.AccentRed
import com.jothivel.chits.utils.AppPreferences
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    showBottomBar: Boolean = true,
    onBackClick: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onLanguageChanged: () -> Unit
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    var showPinDialog by remember { mutableStateOf(false) }

    // Staggered entry animations
    var showSection1 by remember { mutableStateOf(false) }
    var showSection2 by remember { mutableStateOf(false) }
    var showItem1 by remember { mutableStateOf(false) }
    var showItem2 by remember { mutableStateOf(false) }
    var showItem3 by remember { mutableStateOf(false) }
    var showItem4 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100); showSection1 = true
        delay(180); showItem1 = true
        delay(300); showSection2 = true
        delay(380); showItem2 = true
        delay(460); showItem3 = true
        delay(540); showItem4 = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (showBottomBar) AppBottomNavigationBar(currentRoute = "Settings")
        },
        containerColor = OffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ─── Security Section ─────────────────────────────────────
            AnimatedVisibility(
                visible = showSection1,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                Column {
                    Text(stringResource(id = R.string.security_settings), color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                }
            }

            AnimatedVisibility(
                visible = showItem1,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(id = R.string.change_pin),
                    subtitle = stringResource(id = R.string.change_pin_desc),
                    onClick = { showPinDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Data Management Section ──────────────────────────────
            AnimatedVisibility(
                visible = showSection2,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                Text(stringResource(id = R.string.data_management), color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            }

            AnimatedVisibility(
                visible = showItem2,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                SettingsItem(
                    icon = Icons.Default.CloudDownload,
                    title = stringResource(id = R.string.backup_db),
                    subtitle = stringResource(id = R.string.backup_db_desc),
                    onClick = onBackupDatabase
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showItem3,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Restore,
                        title = stringResource(id = R.string.restore_db),
                        subtitle = stringResource(id = R.string.restore_db_desc),
                        onClick = onRestoreDatabase
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        icon = Icons.Default.TableChart,
                        title = stringResource(id = R.string.export_csv),
                        subtitle = stringResource(id = R.string.export_csv_desc),
                        onClick = onExportCsv
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showItem4,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            ) {
                SettingsItem(
                    icon = Icons.Default.FileOpen,
                    title = "Import CSV / XLS / XLSX",
                    subtitle = "Import members & chit groups from a CSV, XLS or XLSX file",
                    onClick = onImportCsv
                )
            }

            Spacer(modifier = Modifier.height(100.dp)) // Extra space for floating nav bar
        }
    }

    // PIN Dialog with animated visibility
    AnimatedVisibility(
        visible = showPinDialog,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150))
    ) {
        ChangePinDialog(
            appPreferences = appPreferences,
            onDismiss = { showPinDialog = false },
            onSuccess = {
                Toast.makeText(context, "PIN Changed Successfully", Toast.LENGTH_SHORT).show()
                showPinDialog = false
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "settingsItemScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ChangePinDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = AccentRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!appPreferences.verifyPin(oldPin)) {
                        errorMessage = "Current PIN is incorrect"
                    } else if (newPin.length < 4) {
                        errorMessage = "PIN must be at least 4 digits"
                    } else if (newPin != confirmPin) {
                        errorMessage = "New PINs do not match"
                    } else {
                        appPreferences.savePin(newPin)
                        onSuccess()
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
