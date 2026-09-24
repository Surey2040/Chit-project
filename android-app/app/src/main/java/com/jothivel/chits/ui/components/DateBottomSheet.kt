package com.jothivel.chits.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.MaroonPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Branded replacement for android.app.DatePickerDialog — wraps Material 3's built-in
 * DatePicker inside the same ModalBottomSheet shell used by BottomSheetPicker.kt,
 * themed with MaroonPrimary instead of the system's default date-picker colors.
 * Preserves CompactDateField's value/pattern contract so replaced call sites don't
 * change their stored date format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateBottomSheetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    pattern: String = "dd-MMM-yyyy",
    enabled: Boolean = true
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun parseInitialMillis(): Long? {
        // Must mirror the Confirm handler below: Material3's DatePicker reads
        // initialSelectedDateMillis as UTC midnight. Parsing in the device's local timezone
        // (IST included - local midnight is the previous UTC calendar day) would pre-select the
        // wrong date the moment this sheet opens for editing an existing value.
        val formats = listOf(pattern, "dd-MMM-yyyy", "yyyy-MM-dd")
        val utc = TimeZone.getTimeZone("UTC")
        for (p in formats) {
            runCatching {
                val fmt = SimpleDateFormat(p, Locale.ENGLISH).apply { isLenient = false; timeZone = utc }
                fmt.parse(value)?.time
            }.getOrNull()?.let { return it }
        }
        return null
    }

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
    }

    Box(modifier = modifier) {
        PremiumInputField(
            value = value,
            onValueChange = {},
            label = label,
            readOnly = true,
            enabled = enabled,
            leadingIcon = Icons.Default.CalendarMonth,
            trailingContent = { Icon(Icons.Default.EditCalendar, "Change date", tint = MaroonPrimary, modifier = Modifier.size(18.dp)) },
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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = parseInitialMillis() ?: System.currentTimeMillis())
        ModalBottomSheet(
            onDismissRequest = { dismiss() },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(
                    label.trim().trimEnd('*').trim(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = MaroonPrimary,
                        selectedDayContentColor = Color.White,
                        todayDateBorderColor = MaroonPrimary,
                        todayContentColor = MaroonPrimary,
                        currentYearContentColor = MaroonPrimary,
                        selectedYearContainerColor = MaroonPrimary
                    )
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { dismiss() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Cancel", color = Color.Black, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                // Material3's DatePicker documents selectedDateMillis as UTC
                                // midnight for the tapped date - reading it in the device's
                                // default (local) timezone can land on the previous calendar day
                                // for any timezone behind UTC. Read and format in UTC instead so
                                // the saved date always matches what was actually tapped.
                                val utc = TimeZone.getTimeZone("UTC")
                                val calendar = Calendar.getInstance(utc).apply { timeInMillis = millis }
                                val formatter = SimpleDateFormat(pattern, Locale.ENGLISH).apply { timeZone = utc }
                                onValueChange(formatter.format(calendar.time))
                            }
                            dismiss()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                    ) { Text("Confirm", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
