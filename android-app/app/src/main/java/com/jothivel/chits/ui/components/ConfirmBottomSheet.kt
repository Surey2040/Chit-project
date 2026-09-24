package com.jothivel.chits.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.AccentRed
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.TextGray
import kotlinx.coroutines.launch

/**
 * Branded replacement for AlertDialog confirmations — same ModalBottomSheet shell as
 * BottomSheetPicker.kt (rounded top corners, white background), title + optional
 * message + optional extra content slot, then a Cancel/Confirm button row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String,
    message: String? = null,
    confirmLabel: String = "Confirm",
    cancelLabel: String = "Cancel",
    isDestructive: Boolean = false,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    if (!show) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismiss(then: (() -> Unit)? = null) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then?.invoke() ?: onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = TextGray)
            }
            content?.let {
                Spacer(Modifier.height(12.dp))
                it()
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { dismiss() },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(cancelLabel, color = Color.Black, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { dismiss(onConfirm) },
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDestructive) AccentRed else MaroonPrimary)
                ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
