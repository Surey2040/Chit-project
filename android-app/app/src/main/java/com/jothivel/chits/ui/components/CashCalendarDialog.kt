package com.jothivel.chits.ui.components

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.jothivel.chits.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class CashCalendarStatus { PAID, DUE }

data class CashCalendarEntry(
    val dateKey: String,
    val customerName: String,
    val customerCode: String,
    val chitNo: String,
    val amount: Long,
    val status: CashCalendarStatus,
    val modeOrInstallment: String,
    val reference: String = ""
)

@Composable
fun CashCalendarDialog(
    entries: List<CashCalendarEntry>,
    onDismiss: () -> Unit,
    onCustomerClick: (CashCalendarEntry) -> Unit
) {
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val today = remember { Calendar.getInstance() }
    var cursorYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var cursorMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var selectedKey by remember { mutableStateOf(todayKey) }
    var yearMode by remember { mutableStateOf(false) }
    val selectedIsFuture = selectedKey > todayKey
    val selectedEntries = entries.filter { it.dateKey == selectedKey }
        .sortedWith(compareBy<CashCalendarEntry> { it.status != CashCalendarStatus.DUE }.thenBy { it.customerName })
    val selectedDueEntries = selectedEntries.filter { it.status == CashCalendarStatus.DUE }
    val selectedPaidEntries = selectedEntries.filter { it.status == CashCalendarStatus.PAID }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = MaroonBackground,
            border = BorderStroke(1.dp, DividerGray),
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cash Calendar", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Close") }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = {
                        if (yearMode) cursorYear -= 12 else if (cursorMonth == 0) { cursorMonth = 11; cursorYear-- } else cursorMonth--
                    }) { Icon(Icons.Default.ChevronLeft, "Previous") }
                    Text(
                        if (yearMode) "$cursorYear – ${cursorYear + 11}" else monthTitle(cursorYear, cursorMonth),
                        modifier = Modifier.clickable { yearMode = !yearMode }.padding(8.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaroonPrimary
                    )
                    IconButton(onClick = {
                        if (yearMode) cursorYear += 12 else if (cursorMonth == 11) { cursorMonth = 0; cursorYear++ } else cursorMonth++
                    }) { Icon(Icons.Default.ChevronRight, "Next") }
                }

                if (yearMode) {
                    YearPicker(cursorYear) { year -> cursorYear = year; yearMode = false }
                } else {
                    MonthGrid(cursorYear, cursorMonth, selectedKey, todayKey, entries) { key -> selectedKey = key }
                }

                Divider(Modifier.padding(vertical = 10.dp), color = DividerGray)
                val selectedLabel = displayDate(selectedKey)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(selectedLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            when {
                                selectedKey == todayKey -> "Due today: ${selectedDueEntries.size} • Paid today: ${selectedPaidEntries.size}"
                                selectedIsFuture -> "Expected members: ${selectedDueEntries.size}"
                                else -> "Due: ${selectedDueEntries.size} • Paid: ${selectedPaidEntries.size}"
                            },
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (selectedDueEntries.isNotEmpty()) Text("Due ${formatMoney(selectedDueEntries.sumOf { it.amount })}", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (selectedPaidEntries.isNotEmpty()) Text("Paid ${formatMoney(selectedPaidEntries.sumOf { it.amount })}", color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(7.dp))
                if (selectedEntries.isEmpty()) {
                    Text("No due members or received payments for this date", modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), color = TextGray, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp)) {
                        items(selectedEntries, key = { "${it.status}-${it.dateKey}-${it.customerCode}-${it.chitNo}-${it.modeOrInstallment}-${it.reference}" }) { entry ->
                            CashEntryRow(entry, onCustomerClick)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.Center) {
                    LegendDot(AccentGreen, "Paid"); Spacer(Modifier.width(18.dp)); LegendDot(AccentGold, "Due")
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(year: Int, month: Int, selectedKey: String, todayKey: String, entries: List<CashCalendarEntry>, onSelect: (String) -> Unit) {
    val first = Calendar.getInstance().apply { set(year, month, 1, 0, 0, 0) }
    val firstDayPadding = first.get(Calendar.DAY_OF_WEEK) - 1
    val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = firstDayPadding + days
    repeat((cells + 6) / 7) { week ->
        Row(
            Modifier
                .fillMaxWidth()
                .offset(y = (-week * 3).dp)
                .zIndex(week.toFloat())
        ) {
            repeat(7) { weekday ->
                val day = week * 7 + weekday - firstDayPadding + 1
                if (day !in 1..days) {
                    Spacer(Modifier.weight(1f).height(49.dp))
                } else {
                    val key = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    val dayEntries = entries.filter { it.dateKey == key }
                    val selected = key == selectedKey
                    val isToday = key == todayKey
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(49.dp)
                            .offset(x = if (weekday == 0) 0.dp else (-weekday * 2).dp)
                            .zIndex(weekday.toFloat())
                            .clickable { onSelect(key) },
                        shape = RoundedCornerShape(11.dp),
                        color = if (selected) MaroonPrimary else Color(0xFFF0F0EE),
                        border = BorderStroke(
                            if (selected || isToday) 1.5.dp else 1.dp,
                            when {
                                selected -> MaroonPrimary
                                isToday -> AccentGold
                                else -> Color(0xFFD4D5D2)
                            }
                        ),
                        shadowElevation = if (selected) 3.dp else 1.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(day.toString(), color = if (selected) Color.White else Color(0xFF242528), fontSize = 12.sp, fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium)
                            if (dayEntries.isNotEmpty()) {
                                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (dayEntries.any { it.status == CashCalendarStatus.DUE }) Box(Modifier.size(5.dp).background(AccentGold, CircleShape))
                                    if (dayEntries.any { it.status == CashCalendarStatus.PAID }) Box(Modifier.size(5.dp).background(AccentGreen, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearPicker(startYear: Int, onSelect: (Int) -> Unit) {
    repeat(4) { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { col ->
                val year = startYear + row * 3 + col
                OutlinedButton(onClick = { onSelect(year) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) { Text(year.toString(), fontSize = 11.sp) }
            }
        }
        Spacer(Modifier.height(5.dp))
    }
}

@Composable
private fun CashEntryRow(entry: CashCalendarEntry, onClick: (CashCalendarEntry) -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onClick(entry) }, shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, DividerGray)) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).background((if (entry.status == CashCalendarStatus.PAID) AccentGreen else AccentGold).copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) { Text(entry.customerName.take(1), fontWeight = FontWeight.Bold, color = if (entry.status == CashCalendarStatus.PAID) AccentGreen else AccentGold) }
            Column(Modifier.padding(start = 8.dp).weight(1f)) { Text("${entry.customerName} • ${entry.customerCode}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${entry.chitNo} • ${entry.modeOrInstallment}", color = TextGray, fontSize = 9.sp) }
            Text(formatMoney(entry.amount), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (entry.status == CashCalendarStatus.PAID) AccentGreen else AccentGold)
        }
    }
}

@Composable private fun LegendDot(color: Color, text: String) = Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).background(color, CircleShape)); Spacer(Modifier.width(5.dp)); Text(text, fontSize = 9.sp, color = TextGray) }
private fun monthTitle(year: Int, month: Int): String = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Calendar.getInstance().apply { set(year, month, 1) }.time)
private fun displayDate(key: String): String = runCatching { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)!!) }.getOrDefault(key)
private fun formatMoney(value: Long): String = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
