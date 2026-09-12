package com.jothivel.chits.ui.ledger

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jothivel.chits.ui.components.AshAnimatedSearchBar
import com.jothivel.chits.ui.components.PremiumInputField
import com.jothivel.chits.ui.theme.*
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.CollectionService
import com.jothivel.chits.utils.CsvDownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

private data class SheetColumn(val title: String, val initialWidth: Dp, val alignEnd: Boolean = false)

private data class LedgerSheetRow(
    val memberId: String, val groupId: String,
    val citsNo: String, val name: String, val startDate: String, val endDate: String,
    val customerCode: String, val serial: String, val oldCode: String, val mobile: String,
    val customerName: String, val address: String, val city: String,
    val collection: String, val settlement: String, val balance: String, val delivery: String,
    val collectionValue: Long, val settlementValue: Long, val balanceValue: Long, val deliveryValue: Long
) {
    fun cells() = listOf(citsNo, name, startDate, endDate, customerCode, serial, oldCode, mobile, customerName, address, city, collection, settlement, balance, delivery)

    fun withCell(index: Int, value: String) = when (index) {
        0 -> copy(citsNo = value); 1 -> copy(name = value); 2 -> copy(startDate = value)
        3 -> copy(endDate = value); 4 -> copy(customerCode = value); 5 -> copy(serial = value)
        6 -> copy(oldCode = value); 7 -> copy(mobile = value); 8 -> copy(customerName = value)
        9 -> copy(address = value); 10 -> copy(city = value); 11 -> copy(collection = value)
        12 -> copy(settlement = value); 13 -> copy(balance = value); 14 -> copy(delivery = value)
        else -> this
    }
}

private val sheetColumns = listOf(
    SheetColumn("Cits No", 66.dp), SheetColumn("Name", 76.dp), SheetColumn("Start Date", 78.dp),
    SheetColumn("End Date", 78.dp), SheetColumn("Customer Code", 94.dp), SheetColumn("Serial", 54.dp),
    SheetColumn("OLD Code", 72.dp), SheetColumn("Mobile No", 92.dp), SheetColumn("Customer Name", 112.dp),
    SheetColumn("Address1", 104.dp), SheetColumn("City", 84.dp), SheetColumn("Collection Amount", 108.dp, true),
    SheetColumn("Settlement Amount", 112.dp, true), SheetColumn("Balance", 88.dp, true), SheetColumn("Delivery Amount", 104.dp, true)
)

@Composable
fun ResizableLedgerSheet(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val rows = remember { mutableStateListOf<LedgerSheetRow>() }
    var selectedRow by remember { mutableStateOf<LedgerSheetRow?>(null) }
    var editingCell by remember { mutableStateOf<Pair<LedgerSheetRow, Int>?>(null) }
    var editValue by remember { mutableStateOf("") }
    val widths = remember { mutableStateListOf<Dp>().apply { addAll(sheetColumns.map { it.initialWidth }) } }
    val density = LocalDensity.current
    val horizontalState = rememberScrollState()
    val filteredRows = rows.filter { row -> query.isBlank() || row.cells().any { it.contains(query, true) } }
    val totalWidth = widths.fold(0.dp) { acc, width -> acc + width }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val databaseRows = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val members = db.memberDao().getAllMembersSync().associateBy { it.id }
            val groups = db.groupDao().getAllGroupsSync().associateBy { it.id }
            val financial = db.financialTransactionDao().getAllSync().filter { it.status == "POSTED" }.groupBy { it.memberId to it.groupId }
            db.membershipDao().getAllActiveSync().mapNotNull { membership ->
                val member = members[membership.memberId] ?: return@mapNotNull null
                val group = groups[membership.groupId] ?: return@mapNotNull null
                val breakdown = CollectionService.calculateDueBreakdown(db, member.id, group.id)
                val paid = breakdown.paidPaise / 100
                val entries = financial[member.id to group.id].orEmpty()
                val settlement = entries.filter { it.type == "SETTLEMENT" }.sumOf { it.amountPaise } / 100
                val delivery = entries.filter { it.type == "DELIVERY" }.sumOf { it.amountPaise } / 100
                val balance = paid - settlement
                LedgerSheetRow(
                    memberId = member.id,
                    groupId = group.id,
                    citsNo = group.registerNo ?: group.id,
                    name = formatLedgerNumber(group.chitValue.toLong() / 100),
                    startDate = group.startDate.orEmpty(),
                    endDate = ledgerEndDate(group.startDate,group.durationMonths),
                    customerCode = member.id,
                    serial = membership.ticketNo.orEmpty(),
                    oldCode = member.panNo.orEmpty(),
                    mobile = member.phone.orEmpty(),
                    customerName = member.name.orEmpty(),
                    address = member.addressLine.orEmpty(),
                    city = member.city.orEmpty(),
                    collection = "₹${formatLedgerNumber(paid)}",
                    settlement = "₹${formatLedgerNumber(settlement)}",
                    balance = (if (balance < 0) "-₹" else "₹") + formatLedgerNumber(kotlin.math.abs(balance)),
                    delivery = "₹${formatLedgerNumber(delivery)}",
                    collectionValue = paid,
                    settlementValue = settlement,
                    balanceValue = balance,
                    deliveryValue = delivery
                )
            }
        }
        rows.clear()
        rows.addAll(databaseRows)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(MaroonBackground)) {
        Row(Modifier.fillMaxWidth().height(46.dp).background(Color.White).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, "Back", tint = MaroonPrimary, modifier = Modifier.size(20.dp)) }
            AshAnimatedSearchBar(query, { query = it }, "Search ledger", Modifier.weight(1f))
            Text("${filteredRows.size} rows", fontSize = 9.sp, color = TextGray)
            IconButton(onClick = { shareLedgerCsv(context, filteredRows) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.FileDownload, "Download ledger", tint = MaroonPrimary, modifier = Modifier.size(20.dp)) }
        }
        Row(Modifier.fillMaxWidth().height(40.dp).background(Color.White).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${filteredRows.size} rows", fontSize = 9.sp, color = TextGray)
            LedgerTotal("Collection", filteredRows.sumOf { it.collectionValue }, AccentGreen)
            LedgerTotal("Settlement", filteredRows.sumOf { it.settlementValue }, Color(0xFF285A9B))
            LedgerTotal("Balance", filteredRows.sumOf { it.balanceValue }, AccentRed)
        }
        Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, DividerGray).horizontalScroll(horizontalState)) {
            Column(Modifier.width(totalWidth).fillMaxHeight()) {
                Row(Modifier.height(34.dp).background(Color(0xFFECEDEF))) {
                    sheetColumns.forEachIndexed { index, column ->
                        ResizableHeaderCell(column, widths[index]) { dragPx ->
                            val dragDp = with(density) { dragPx.toDp() }
                            widths[index] = (widths[index] + dragDp).coerceIn(64.dp, 240.dp)
                        }
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filteredRows) { row ->
                        Row(Modifier.height(34.dp)) {
                            row.cells().forEachIndexed { index, value ->
                                LedgerDataCell(
                                    value = value,
                                    width = widths[index],
                                    index = index,
                                    rowSelected = selectedRow === row,
                                    onTap = { if (index == 5) selectedRow = row },
                                    onDoubleTap = {
                                        if (index in setOf(5, 6, 7, 8, 9, 10)) {
                                            editingCell = row to index
                                            editValue = value
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        } // closes Column(2)
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )

    editingCell?.let { (row, columnIndex) ->
        Dialog(
            onDismissRequest = { editingCell = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.width(276.dp).wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFAFA),
                tonalElevation = 5.dp,
                shadowElevation = 8.dp
            ) {
                Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
                    Text("Edit ${sheetColumns[columnIndex].title}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    PremiumInputField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = sheetColumns[columnIndex].title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingCell = null }, modifier = Modifier.height(36.dp)) { Text("Cancel", fontSize = 12.sp) }
                        TextButton(onClick = {
                            val rowIndex = rows.indexOfFirst { it === row }
                            if (rowIndex >= 0) {
                                val oldRow = row
                                val oldValue = oldRow.cells()[columnIndex]
                                val updated = row.withCell(columnIndex, editValue.trim())
                                rows[rowIndex] = updated
                                if (selectedRow === row) selectedRow = updated
                                
                                coroutineScope.launch(Dispatchers.IO) {
                                    persistLedgerEdit(AppDatabase.getDatabase(context), row, columnIndex, editValue.trim())
                                }
                                
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "${sheetColumns[columnIndex].title} updated",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        // Undo action
                                        rows[rowIndex] = oldRow
                                        if (selectedRow === updated) selectedRow = oldRow
                                        withContext(Dispatchers.IO) {
                                            persistLedgerEdit(AppDatabase.getDatabase(context), oldRow, columnIndex, oldValue)
                                        }
                                    }
                                }
                            }
                            editingCell = null
                        }, modifier = Modifier.height(36.dp)) { Text("Save", color = MaroonPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        }
    }
}


private fun formatLedgerNumber(value: Long): String = NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)

private fun ledgerEndDate(start:String?,months:Int):String {
    val parsed=listOf("dd-MMM-yyyy","yyyy-MM-dd","dd-MM-yyyy").firstNotNullOfOrNull { p->runCatching{SimpleDateFormat(p,Locale.ENGLISH).apply{isLenient=false}.parse(start.orEmpty())}.getOrNull() } ?: return "-"
    return SimpleDateFormat("dd-MMM-yyyy",Locale.ENGLISH).format(Calendar.getInstance().apply{time=parsed;add(Calendar.MONTH,months)}.time)
}

private fun persistLedgerEdit(db: AppDatabase, row: LedgerSheetRow, columnIndex: Int, value: String) {
    when (columnIndex) {
        5 -> db.membershipDao().updateTicketNo(row.memberId, row.groupId, value)
        6 -> db.memberDao().updateOldCode(row.memberId, value)
        7 -> db.memberDao().updatePhone(row.memberId, value)
        8 -> db.memberDao().updateName(row.memberId, value)
        9 -> db.memberDao().updateAddress(row.memberId, value)
        10 -> db.memberDao().updateCity(row.memberId, value)
    }
}

private fun shareLedgerCsv(context: android.content.Context, rows: List<LedgerSheetRow>) {
    fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""
    val content = buildString {
        appendLine(sheetColumns.joinToString(",") { csv(it.title) })
        rows.forEach { row -> appendLine(row.cells().joinToString(",") { csv(it) }) }
    }
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        val fileName="Ledger-${SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(Date())}.csv"
        val result=CsvDownloadHelper.save(context,fileName,content)
        withContext(Dispatchers.Main){android.widget.Toast.makeText(context,result.fold({"Downloaded to $it"},{"Download failed: ${it.message}"}),android.widget.Toast.LENGTH_LONG).show()}
    }
}

@Composable
private fun LedgerTotal(label: String, value: Long, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, fontSize = 7.sp, color = TextGray)
        Text((if (value < 0) "-₹" else "₹") + formatLedgerNumber(kotlin.math.abs(value)), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (value < 0) AccentRed else color)
    }
}

@Composable
private fun ResizableHeaderCell(column: SheetColumn, width: Dp, onDrag: (Float) -> Unit) {
    Box(Modifier.width(width).fillMaxHeight().drawBehind {
        val stroke = 1.dp.toPx()
        val borderX = size.width - (stroke / 2f)
        val borderY = size.height - (stroke / 2f)
        drawLine(color = DividerGray, start = Offset(borderX, 0f), end = Offset(borderX, size.height), strokeWidth = stroke)
        drawLine(color = DividerGray, start = Offset(0f, borderY), end = Offset(size.width, borderY), strokeWidth = stroke)
    }) {
        Text(column.title, Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.align(Alignment.CenterEnd).width(12.dp).fillMaxHeight().pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount) }
            }, contentAlignment = Alignment.Center
        ) { Box(Modifier.width(2.dp).fillMaxHeight().background(MaroonPrimary.copy(alpha = .35f))) }
    }
}

@Composable
private fun LedgerDataCell(
    value: String,
    width: Dp,
    index: Int,
    rowSelected: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val isMoney = index >= 11
    val color = when {
        value.startsWith("-") -> AccentRed
        index == 12 -> Color(0xFF285A9B)
        index == 14 && value != "₹0" -> AccentGreen
        else -> Color(0xFF242528)
    }
    val rowColor = if (rowSelected) MaroonPrimary.copy(alpha = .11f) else Color.White
    Box(
        Modifier.width(width).fillMaxHeight().background(rowColor).drawBehind {
            val stroke = if (rowSelected) 1.5.dp.toPx() else 1.dp.toPx()
            val borderColor = if (rowSelected) MaroonPrimary.copy(alpha = 0.42f) else DividerGray
            val borderX = size.width - (stroke / 2f)
            val borderY = size.height - (stroke / 2f)
            drawLine(color = borderColor, start = Offset(borderX, 0f), end = Offset(borderX, size.height), strokeWidth = stroke)
            drawLine(color = borderColor, start = Offset(0f, borderY), end = Offset(size.width, borderY), strokeWidth = stroke)
        }.pointerInput(index, value) {
            detectTapGestures(onTap = { onTap() }, onDoubleTap = { onDoubleTap() })
        },
        contentAlignment = if (isMoney) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(value, Modifier.padding(horizontal = 7.dp), fontSize = 10.sp, color = color, fontWeight = if (isMoney) FontWeight.Medium else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = if (isMoney) TextAlign.End else TextAlign.Start)
    }
}
