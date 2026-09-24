package com.jothivel.chits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.MaroonSurfaceLight
import com.jothivel.chits.ui.theme.TextGray
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private fun formatRupees(digits: String): String {
    if (digits.isEmpty()) return "0"
    val value = digits.toLongOrNull() ?: 0L
    return NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
}

/**
 * PhonePe/GPay-style amount entry: the field is read-only (no system IME), tapping it
 * opens a bottom sheet with a large running "₹ <amount>" display and a custom digit
 * keypad. Sibling of BottomSheetPickerField.kt — same ModalBottomSheet shell/shape/colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountKeypadField(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    maxDigits: Int = 10,
    enabled: Boolean = true
) {
    var showSheet by remember { mutableStateOf(false) }
    var draft by remember(showSheet) { mutableStateOf(amount) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismiss(commit: Boolean) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showSheet = false
            if (commit) onAmountChange(draft)
        }
    }

    Box(modifier = modifier) {
        PremiumInputField(
            value = amount,
            onValueChange = {},
            label = label,
            readOnly = true,
            enabled = enabled,
            leadingIcon = Icons.Default.CurrencyRupee,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth()
        )
        // readOnly PremiumInputField is still enabled, so it steals the tap for its own
        // cursor/focus before a plain clickable below it would ever see it - this overlay
        // guarantees the tap opens the sheet regardless.
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showSheet = true }
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { dismiss(commit = false) },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(label.trim().trimEnd('*').trim(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextGray)
                Spacer(Modifier.height(4.dp))
                Text(
                    "₹ ${formatRupees(draft)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = MaroonPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider(color = Color(0xFFEDEDED))
                Spacer(Modifier.height(12.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("back", "0", "done")
                )
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { key ->
                            KeypadKey(
                                key = key,
                                modifier = Modifier.weight(1f),
                                onDigit = { d -> if (draft.length < maxDigits) draft = (draft + d).trimStart('0').ifEmpty { if (d == "0") "" else d } },
                                onBackspace = { draft = draft.dropLast(1) },
                                onDone = { dismiss(commit = true) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    key: String,
    modifier: Modifier,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit
) {
    when (key) {
        "back" -> Box(
            modifier
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF3F3F3), RoundedCornerShape(14.dp))
                .clickable(onClick = onBackspace),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Backspace, "Backspace", tint = TextGray) }

        "done" -> Box(
            modifier
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaroonPrimary, RoundedCornerShape(14.dp))
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Check, "Done", tint = Color.White) }

        else -> Box(
            modifier
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaroonSurfaceLight.copy(alpha = .5f), RoundedCornerShape(14.dp))
                .clickable { onDigit(key) },
            contentAlignment = Alignment.Center
        ) { Text(key, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.Black) }
    }
}
