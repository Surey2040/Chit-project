package com.jothivel.chits.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R
import com.jothivel.chits.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    agentViewModel: AgentLoginViewModel? = null,
    isSetupMode: Boolean,
    onLanguageToggle: () -> Unit
) {
    var loginMode by rememberSaveable { mutableStateOf("ADMIN") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = LoginGradient)
    ) {
        if (isSetupMode) {
            SetupAdminScreen(viewModel, onLanguageToggle)
        } else if (loginMode == "ADMIN" || agentViewModel == null) {
            PinLoginScreen(viewModel, onLanguageToggle, showLabourToggle = agentViewModel != null, onSwitchToLabour = { loginMode = "LABOUR" })
        } else {
            AgentPinLoginScreen(agentViewModel, onLanguageToggle, onSwitchToAdmin = { loginMode = "ADMIN" })
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ADMIN / LABOUR MODE TOGGLE — shared pill switch shown above both login forms
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RoleModeToggle(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .background(GlassWhite, RoundedCornerShape(24.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf("ADMIN" to "Admin", "LABOUR" to "Labour").forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HANGING TEMPLE LAMP — decorative "kuthu vilakku" style oil lamp drawn in code
// (no image asset - a gold-on-maroon illustration matching the app's traditional theme)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HangingLamp(modifier: Modifier = Modifier) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFF8E7B0)
    val goldMid = Color(0xFFC9962E)
    val goldDark = Color(0xFF7A5A18)
    val flameCore = Color(0xFFFFF7CE)
    val flameMid = Color(0xFFFFC94A)
    val flameOuter = Color(0xFFFF7A00)

    Canvas(modifier = modifier.width(68.dp).height(128.dp)) {
        val cx = size.width / 2f
        val w = size.width
        val h = size.height

        // Chain from the ceiling
        drawLine(goldLight.copy(alpha = 0.9f), Offset(cx, 0f), Offset(cx, h * 0.30f), strokeWidth = 3f)
        listOf(0.06f, 0.14f, 0.22f).forEach { f ->
            drawCircle(goldLight, radius = 3.6f, center = Offset(cx, h * f))
        }

        // Ornate lotus-bud finial where the chain meets the lamp
        val finialY = h * 0.34f
        val finial = Path().apply {
            moveTo(cx, finialY - 13f)
            cubicTo(cx + 11f, finialY - 10f, cx + 12f, finialY + 4f, cx, finialY + 13f)
            cubicTo(cx - 12f, finialY + 4f, cx - 11f, finialY - 10f, cx, finialY - 13f)
            close()
        }
        drawPath(finial, brush = Brush.verticalGradient(listOf(goldLight, gold, goldMid)))
        drawPath(finial, color = goldDark.copy(alpha = 0.6f), style = Stroke(width = 1.5f))

        // Short stem down to the bowl
        val stemTop = finialY + 13f
        val bowlTop = h * 0.56f
        drawLine(gold, Offset(cx, stemTop), Offset(cx, bowlTop), strokeWidth = 4f)

        // Decorative side arms with small drop-beads (kuthu vilakku style)
        listOf(-1f, 1f).forEach { side ->
            val armEnd = Offset(cx + side * w * 0.34f, bowlTop - 6f)
            val arm = Path().apply {
                moveTo(cx, stemTop + 6f)
                cubicTo(cx + side * w * 0.20f, stemTop - 2f, cx + side * w * 0.20f, stemTop - 2f, armEnd.x, armEnd.y)
            }
            drawPath(arm, color = goldMid, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
            drawCircle(gold, radius = 3.4f, center = armEnd)
        }

        // Lamp bowl - a filled, wide shallow saucer that catches the light
        val bowlWidth = w * 0.9f
        val bowlHeight = h * 0.16f
        val bowl = Path().apply {
            moveTo(cx - bowlWidth / 2f, bowlTop)
            cubicTo(cx - bowlWidth / 4f, bowlTop + bowlHeight * 1.9f, cx + bowlWidth / 4f, bowlTop + bowlHeight * 1.9f, cx + bowlWidth / 2f, bowlTop)
            cubicTo(cx + bowlWidth / 4f, bowlTop + bowlHeight * 0.55f, cx - bowlWidth / 4f, bowlTop + bowlHeight * 0.55f, cx - bowlWidth / 2f, bowlTop)
            close()
        }
        drawPath(bowl, brush = Brush.verticalGradient(listOf(goldMid, gold, goldDark)))
        // Rim highlight
        drawArc(
            color = goldLight.copy(alpha = 0.9f),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(cx - bowlWidth / 2f, bowlTop - bowlHeight * 0.35f),
            size = Size(bowlWidth, bowlHeight * 1.1f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
        // Little decorative petals at the bowl's edges
        listOf(-1f, 1f).forEach { side ->
            val petal = Path().apply {
                moveTo(cx + side * bowlWidth / 2f, bowlTop)
                lineTo(cx + side * (bowlWidth / 2f + 7f), bowlTop + 3f)
                lineTo(cx + side * bowlWidth / 2f, bowlTop + 7f)
                close()
            }
            drawPath(petal, brush = Brush.verticalGradient(listOf(goldLight, goldMid)))
        }

        // Glowing flame - soft outer halo, warm mid glow, bright core
        val flameCenter = Offset(cx, bowlTop - 16f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(flameOuter.copy(alpha = 0.45f), flameOuter.copy(alpha = 0.15f), Color.Transparent),
                center = flameCenter,
                radius = 40f
            ),
            radius = 40f,
            center = flameCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(flameCore.copy(alpha = 0.95f), flameMid.copy(alpha = 0.55f), Color.Transparent),
                center = flameCenter,
                radius = 22f
            ),
            radius = 22f,
            center = flameCenter
        )
        val flame = Path().apply {
            moveTo(flameCenter.x, flameCenter.y - 14f)
            cubicTo(flameCenter.x + 8f, flameCenter.y - 5f, flameCenter.x + 5f, flameCenter.y + 9f, flameCenter.x, flameCenter.y + 12f)
            cubicTo(flameCenter.x - 5f, flameCenter.y + 9f, flameCenter.x - 8f, flameCenter.y - 5f, flameCenter.x, flameCenter.y - 14f)
            close()
        }
        drawPath(flame, brush = Brush.verticalGradient(listOf(flameCore, flameMid, flameOuter)))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PREMIUM PIN LOGIN SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PinLoginScreen(
    viewModel: LoginViewModel,
    onLanguageToggle: () -> Unit,
    showLabourToggle: Boolean = false,
    onSwitchToLabour: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.observeAsState(false)
    val haptic = LocalHapticFeedback.current

    // ── Shake animation on error ─────────────────────────────────────────
    var triggerShake by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (triggerShake) 1f else 0f,
        animationSpec = if (triggerShake) {
            keyframes {
                durationMillis = 400
                0f at 0
                -18f at 50
                18f at 100
                -14f at 150
                14f at 200
                -8f at 250
                8f at 300
                -4f at 350
                0f at 400
            }
        } else {
            tween(0)
        },
        label = "shake",
        finishedListener = { triggerShake = false }
    )

    // ── Error state for PIN dots ─────────────────────────────────────────
    var isError by remember { mutableStateOf(false) }

    // ── Logo pulse animation ─────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    val logoGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoGlow"
    )

    // ── Staggered entry animations ───────────────────────────────────────
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Observe login error to trigger shake
    val loginError by viewModel.loginError.observeAsState()
    LaunchedEffect(loginError) {
        if (loginError != null) {
            isError = true
            triggerShake = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(600)
            pin = ""
            delay(300)
            isError = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hanging temple lamps ──────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HangingLamp()
            HangingLamp()
        }

        Spacer(modifier = Modifier.weight(0.8f))

        // ── Admin/Labour switch, centered ────────────────────────────────
        if (showLabourToggle) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                RoleModeToggle(selected = "ADMIN") { if (it == "LABOUR") onSwitchToLabour() }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── App Name ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(id = R.string.app_name),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "உழைப்பாளர்களுக்கு மட்டும்..",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── "Enter PIN" label ────────────────────────────────────────────
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 400))
        ) {
            Text(
                if (isError) "Wrong PIN. Try again." else "Enter your PIN to continue",
                fontSize = 14.sp,
                color = if (isError) PinDotError.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── PIN DOTS ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 500)) + scaleIn(
                initialScale = 0.5f,
                animationSpec = tween(500, delayMillis = 500)
            )
        ) {
            Row(
                modifier = Modifier.offset(x = shakeOffset.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    PinDot(
                        isFilled = index < pin.length,
                        isError = isError,
                        index = index,
                        filledCount = pin.length
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── NUMBER PAD ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 600)) + slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = tween(500, delayMillis = 600, easing = FastOutSlowInEasing)
            )
        ) {
            NumberPad(
                onNumberClick = { digit ->
                    if (pin.length < 4) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pin += digit.toString()
                        if (pin.length == 4) {
                            // Auto-submit after 4th digit
                            viewModel.verifyPin(pin)
                        }
                    }
                },
                onDeleteClick = {
                    if (pin.isNotEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pin = pin.dropLast(1)
                    }
                },
                enabled = !isLoading && pin.length < 4
            )
        }

        // ── Loading indicator ────────────────────────────────────────────
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                color = AccentGold,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LABOUR (FIELD AGENT) LOGIN — phone number + PIN, verified against Firestore
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentPinLoginScreen(
    viewModel: AgentLoginViewModel,
    onLanguageToggle: () -> Unit,
    onSwitchToAdmin: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.observeAsState(false)
    val loginError by viewModel.loginError.observeAsState()
    val haptic = LocalHapticFeedback.current

    var triggerShake by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (triggerShake) 1f else 0f,
        animationSpec = if (triggerShake) {
            keyframes {
                durationMillis = 400
                0f at 0; -18f at 50; 18f at 100; -14f at 150; 14f at 200; -8f at 250; 8f at 300; -4f at 350; 0f at 400
            }
        } else tween(0),
        label = "agentShake",
        finishedListener = { triggerShake = false }
    )
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(loginError) {
        if (loginError != null) {
            isError = true
            triggerShake = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(600)
            pin = ""
            delay(300)
            isError = false
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); isVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HangingLamp()
            HangingLamp()
        }

        Spacer(modifier = Modifier.height(if (isVisible) 24.dp else 24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RoleModeToggle(selected = "LABOUR") { if (it == "ADMIN") onSwitchToAdmin() }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(id = R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Field Collection Login", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            if (isError) (loginError ?: "Login failed. Try again.") else "Enter your mobile number & PIN",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = if (isError) PinDotError.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.7f),
            fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) { phone = it; isError = false } },
            label = { Text("Mobile Number", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold.copy(alpha = 0.8f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                cursorColor = AccentGold,
                focusedContainerColor = GlassWhite.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.Transparent,
                focusedLabelColor = AccentGold,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.offset(x = shakeOffset.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index -> PinDot(isFilled = index < pin.length, isError = isError, index = index, filledCount = pin.length) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        NumberPad(
            onNumberClick = { digit ->
                if (pin.length < 4) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pin += digit.toString()
                    if (pin.length == 4 && phone.length == 10) viewModel.login(phone, pin)
                }
            },
            onDeleteClick = {
                if (pin.isNotEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pin = pin.dropLast(1)
                }
            },
            enabled = !isLoading && pin.length < 4 && phone.length == 10
        )

        if (phone.length in 1..9) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Enter your full 10-digit mobile number", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = AccentGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PIN DOT — Animated filled/empty indicator
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PinDot(isFilled: Boolean, isError: Boolean, index: Int, filledCount: Int) {
    val dotScale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dotScale"
    )
    
    val dotColor by animateColorAsState(
        targetValue = when {
            isError -> PinDotError
            isFilled -> PinDotFilled
            else -> PinDotEmpty
        },
        animationSpec = tween(200),
        label = "dotColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> PinDotError.copy(alpha = 0.8f)
            isFilled -> AccentGold.copy(alpha = 0.6f)
            index == filledCount -> Color.White.copy(alpha = 0.5f) // next dot to fill
            else -> Color.White.copy(alpha = 0.2f)
        },
        animationSpec = tween(200),
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .size(20.dp)
            .scale(dotScale)
            .clip(CircleShape)
            .background(dotColor, CircleShape)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = CircleShape
            )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// NUMBER PAD — Custom 3x4 grid
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun NumberPad(
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    enabled: Boolean
) {
    val numbers = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(-1, 0, -2)  // -1 = empty, -2 = delete
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { num ->
                    when (num) {
                        -1 -> {
                            // Empty spacer
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                        -2 -> {
                            // Delete button
                            NumberPadButton(
                                onClick = onDeleteClick,
                                enabled = true,
                                content = {
                                    Icon(
                                        Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            )
                        }
                        else -> {
                            // Number button
                            NumberPadButton(
                                onClick = { if (enabled) onNumberClick(num) },
                                enabled = enabled,
                                content = {
                                    Text(
                                        num.toString(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPadButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "btnScale"
    )
    
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.25f else 0.1f,
        animationSpec = tween(100),
        label = "bgAlpha"
    )

    Surface(
        onClick = {
            if (enabled) {
                isPressed = true
                onClick()
            }
        },
        modifier = Modifier
            .size(72.dp)
            .scale(scale),
        shape = CircleShape,
        color = Color.White.copy(alpha = bgAlpha),
        border = null
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
    
    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ADMIN SETUP SCREEN — Polished with staggered animations
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupAdminScreen(viewModel: LoginViewModel, onLanguageToggle: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.observeAsState(false)

    // Staggered entry
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); isVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Language toggle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(
                onClick = onLanguageToggle,
                modifier = Modifier.size(40.dp).background(GlassWhite, CircleShape)
            ) {
                Icon(Icons.Default.Language, contentDescription = "Switch Language", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logo removed per user request
        
        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(400, delayMillis = 200))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(id = R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
                Text("உழைப்பாளர்களுக்கு மட்டும்..", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(400, delayMillis = 300))) {
            Text("Setup Admin Profile", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Form fields with staggered slide-up ──────────────────────────
        val fields = listOf(
            Triple("Admin Name", name) { v: String -> name = v },
            Triple(stringResource(id = R.string.mobile_number), phone) { v: String -> phone = v },
            Triple("Username", username) { v: String -> username = v }
        )

        fields.forEachIndexed { index, (label, value, onChange) ->
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400, delayMillis = 400 + index * 80)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(400, delayMillis = 400 + index * 80, easing = FastOutSlowInEasing)
                        )
            ) {
                SetupTextField(
                    value = value,
                    onValueChange = onChange,
                    label = label,
                    isPhone = index == 1
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // PIN field
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 640)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(400, delayMillis = 640, easing = FastOutSlowInEasing)
                    )
        ) {
            SetupTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = "Create 4-Digit PIN",
                isPassword = true
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // ── Submit button with gradient ──────────────────────────────────
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 750)) +
                    slideInVertically(
                        initialOffsetY = { 30 },
                        animationSpec = tween(400, delayMillis = 750, easing = FastOutSlowInEasing)
                    )
        ) {
            Button(
                onClick = { viewModel.setupAdminProfile(name, phone, username, pin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                enabled = !isLoading && name.isNotBlank() && phone.isNotBlank() && pin.length == 4
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaroonPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Complete Setup",
                        color = MaroonPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Setup form text field with glass effect ──────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPhone: Boolean = false,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        ),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                isPhone -> KeyboardType.Phone
                isPassword -> KeyboardType.NumberPassword
                else -> KeyboardType.Text
            }
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGold.copy(alpha = 0.8f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
            cursorColor = AccentGold,
            focusedContainerColor = GlassWhite.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.Transparent,
            focusedLabelColor = AccentGold,
            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
        )
    )
}
