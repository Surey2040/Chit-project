package com.jothivel.chits.ui.settings

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.ui.base.BaseActivity
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.ui.theme.JothiVelChitsTheme
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.OffWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.zip.ZipInputStream

// ─── Data classes ─────────────────────────────────────────────────────────────

data class CsvImportResult(
    val members: List<MemberEntity>,
    val groups: List<ChitGroupEntity>,
    val skippedRows: Int,
    val errors: List<String>
)

/** Counters returned after writing to the DB. */
data class ImportSummary(
    val newMembers: Int, val updatedMembers: Int,
    val newGroups: Int, val updatedGroups: Int
)

// ─── Activity ─────────────────────────────────────────────────────────────────

class CsvImportActivity : BaseActivity() {

    companion object {
        const val EXTRA_CSV_URI = "EXTRA_CSV_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriString = intent.getStringExtra(EXTRA_CSV_URI)
        val csvUri = uriString?.let { Uri.parse(it) }

        setContent {
            JothiVelChitsTheme {
                CsvImportScreen(
                    csvUri = csvUri,
                    onBack = { SmoothTransitions.finishSmooth(this) },
                    onImport = { members, groups ->
                        lifecycleScope.launch { importData(members, groups) }
                    }
                )
            }
        }
    }

    /**
     * Smart upsert:
     * - Members matched by phone → existing ID reused (REPLACE = update, no duplicate)
     * - Groups matched by name   → existing ID reused
     * - KYC/address fields of existing members are preserved
     */
    private suspend fun importData(members: List<MemberEntity>, groups: List<ChitGroupEntity>) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                var newMembers = 0; var updatedMembers = 0
                var newGroups = 0; var updatedGroups = 0

                // ── Groups ────────────────────────────────────────────────────
                val resolvedGroups = groups.map { g ->
                    val existing = g.name?.let { db.groupDao().getGroupByNameSync(it) }
                    if (existing != null) { g.id = existing.id; updatedGroups++ } else { newGroups++ }
                    g
                }
                if (resolvedGroups.isNotEmpty()) db.groupDao().insertAll(resolvedGroups)

                // ── Members ───────────────────────────────────────────────────
                val resolvedMembers = members.map { m ->
                    val existing = m.phone?.let { db.memberDao().getMemberByPhoneSync(it) }
                    if (existing != null) {
                        m.id = existing.id
                        // Preserve sensitive / enriched fields not present in the spreadsheet
                        m.aadhaarNoEncrypted = existing.aadhaarNoEncrypted
                        m.panNo = existing.panNo
                        m.aadhaarDocumentPath = existing.aadhaarDocumentPath
                        m.panDocumentPath = existing.panDocumentPath
                        m.addressLine = existing.addressLine
                        m.city = existing.city
                        m.state = existing.state
                        m.pincode = existing.pincode
                        m.dob = existing.dob
                        m.gender = existing.gender
                        m.nomineeName = existing.nomineeName
                        m.nomineePhone = existing.nomineePhone
                        m.nomineeRelationship = existing.nomineeRelationship
                        updatedMembers++
                    } else { newMembers++ }
                    m
                }
                if (resolvedMembers.isNotEmpty()) db.memberDao().insertAll(resolvedMembers)

                val summary = ImportSummary(newMembers, updatedMembers, newGroups, updatedGroups)
                withContext(Dispatchers.Main) {
                    val msg = buildString {
                        append("✓ Import complete!\n")
                        if (summary.newMembers > 0)     append("${summary.newMembers} new member(s) added\n")
                        if (summary.updatedMembers > 0) append("${summary.updatedMembers} member(s) updated\n")
                        if (summary.newGroups > 0)      append("${summary.newGroups} new chit group(s) added\n")
                        if (summary.updatedGroups > 0)  append("${summary.updatedGroups} chit group(s) updated")
                    }
                    Toast.makeText(this@CsvImportActivity, msg.trim(), Toast.LENGTH_LONG).show()
                    SmoothTransitions.finishSmooth(this@CsvImportActivity)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CsvImportActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        SmoothTransitions.applyExitTransition(this)
    }
}

// ─── Composable Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    csvUri: Uri?,
    onBack: () -> Unit,
    onImport: (List<MemberEntity>, List<ChitGroupEntity>) -> Unit
) {
    val context = LocalContext.current
    var parseState by remember { mutableStateOf<CsvImportResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // Auto-detect format (CSV / XLSX / XLS) and parse on launch
    LaunchedEffect(csvUri) {
        if (csvUri != null) {
            isLoading = true
            withContext(Dispatchers.IO) {
                val mime = context.contentResolver.getType(csvUri) ?: ""
                val path = csvUri.path ?: ""
                val result = when {
                    path.endsWith(".xlsx", ignoreCase = true) ||
                    mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                        parseXlsxFile(context, csvUri)

                    path.endsWith(".xls", ignoreCase = true) ||
                    mime == "application/vnd.ms-excel" ->
                        // Legacy XLS (BIFF8) requires Apache POI which is incompatible with minSdk 24.
                        // Guide user to re-export as XLSX or CSV.
                        CsvImportResult(
                            members = emptyList(), groups = emptyList(), skippedRows = 0,
                            errors = listOf(
                                "Legacy .xls format is not supported.",
                                "Please open the file in Excel / Google Sheets and save/export it as .xlsx or .csv, then try again."
                            )
                        )

                    else -> parseCsvFile(context, csvUri)
                }
                withContext(Dispatchers.Main) {
                    parseState = result
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import File", fontWeight = FontWeight.Bold) },
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
        containerColor = OffWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                csvUri == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FileOpen, contentDescription = null,
                                tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No file selected", color = Color.Gray, fontSize = 16.sp)
                            Text("Go back and select a CSV or XLSX file from Settings",
                                color = Color.LightGray, fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp).padding(horizontal = 24.dp))
                        }
                    }
                }
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaroonPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Reading file…", color = Color.Gray)
                        }
                    }
                }
                parseState == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Failed to read file.", color = Color.Red)
                    }
                }
                else -> {
                    val result = parseState!!
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // ── Summary card ──────────────────────────────────────
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("Import Preview", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    SummaryRow(Icons.Default.People, "Members to import",
                                        "${result.members.size}", Color(0xFF1E6B3B))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SummaryRow(Icons.Default.Group, "Chit Groups to import",
                                        "${result.groups.size}", MaroonPrimary)
                                    if (result.skippedRows > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        SummaryRow(Icons.Default.Warning, "Skipped rows",
                                            "${result.skippedRows}", Color(0xFFE65100))
                                    }
                                }
                            }
                        }

                        // ── Column format guide ───────────────────────────────
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null,
                                            tint = MaroonPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Expected Columns (CSV / XLSX)",
                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                            color = MaroonPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Member Name • Phone • Chit Group • Installment No • Base Amount • " +
                                        "Amount Due • Amount Paid • Payment Status • Payment Mode • Due Date",
                                        fontSize = 11.sp, color = Color.Gray, lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "ℹ Existing members matched by Phone, groups by Name — no duplicates created.",
                                        fontSize = 11.sp, color = Color(0xFF1565C0), lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // ── Members preview ───────────────────────────────────
                        if (result.members.isNotEmpty()) {
                            item {
                                Text("Members Preview",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                            }
                            items(result.members.take(5)) { member ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaroonPrimary.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                (member.name?.take(1) ?: "?").uppercase(),
                                                color = MaroonPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(member.name ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(member.phone ?: "—", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            if (result.members.size > 5) {
                                item {
                                    Text("+ ${result.members.size - 5} more members",
                                        color = Color.Gray, fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }

                        // ── Errors / warnings ─────────────────────────────────
                        if (result.errors.isNotEmpty()) {
                            item {
                                Text("Warnings", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                            }
                            items(result.errors) { err ->
                                Text("• $err", fontSize = 12.sp, color = Color(0xFFE65100),
                                    modifier = Modifier.padding(start = 8.dp))
                            }
                        }

                        // ── Import button ─────────────────────────────────────
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isImporting = true
                                    onImport(result.members, result.groups)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                enabled = !isImporting &&
                                          (result.members.isNotEmpty() || result.groups.isNotEmpty())
                            ) {
                                if (isImporting) {
                                    CircularProgressIndicator(color = Color.White,
                                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Importing…", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Import ${result.members.size} Members & ${result.groups.size} Chit Groups",
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Helper composable ────────────────────────────────────────────────────────

@Composable
fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f), color = Color(0xFF333333))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ─── Shared row builder ───────────────────────────────────────────────────────

/**
 * Converts one spreadsheet row into a [MemberEntity] and/or [ChitGroupEntity].
 * Column order (0-indexed):
 * 0 Member Name | 1 Phone | 2 Chit Group | 3 Installment No | 4 Base Amount |
 * 5 Amount Due  | 6 Amount Paid | 7 Payment Status | 8 Payment Mode | 9 Due Date
 *
 * @return 1 if the row was skipped, 0 if processed.
 */
private fun rowToMemberAndGroup(
    memberName: String,
    phone: String,
    groupName: String,
    baseAmountStr: String,
    members: MutableList<MemberEntity>,
    groupMap: MutableMap<String, ChitGroupEntity>,
    seenPhones: MutableSet<String>,
    errors: MutableList<String>,
    lineLabel: String
): Int {
    if (memberName.isBlank() || phone.isBlank()) {
        errors.add("$lineLabel: missing name or phone, skipped")
        return 1
    }
    if (phone !in seenPhones) {
        seenPhones.add(phone)
        val m = MemberEntity().apply {
            id = UUID.randomUUID().toString()
            name = memberName
            this.phone = phone
            isActive = true
            role = "MEMBER"
        }
        members.add(m)
    }
    if (groupName.isNotBlank() && groupName !in groupMap) {
        val baseAmount = baseAmountStr.toLongOrNull() ?: 0L
        val months = Regex("(\\d+)M").find(groupName)?.groupValues?.get(1)?.toIntOrNull() ?: 20
        val g = ChitGroupEntity().apply {
            id = UUID.randomUUID().toString()
            this.name = groupName
            registerNo = "IMP-${System.currentTimeMillis()}-${groupMap.size + 1}"
            chitValue = (baseAmount * months * 100).toInt()
            durationMonths = months
            subscriberCount = months
            branch = "Main"
            startDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            status = "ACTIVE"
        }
        groupMap[groupName] = g
    }
    return 0
}

// ─── CSV Parser ───────────────────────────────────────────────────────────────

/** Parse a plain UTF-8 comma-separated CSV. */
private fun parseCsvFile(context: android.content.Context, uri: Uri): CsvImportResult {
    val members = mutableListOf<MemberEntity>()
    val groupMap = mutableMapOf<String, ChitGroupEntity>()
    val errors = mutableListOf<String>()
    var skippedRows = 0
    val seenPhones = mutableSetOf<String>()

    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            var lineIndex = 0
            reader.forEachLine { rawLine ->
                lineIndex++
                if (lineIndex == 1) return@forEachLine // skip header
                val cols = rawLine.split(",").map { it.trim() }
                if (cols.size < 6) {
                    skippedRows++
                    errors.add("Row $lineIndex: not enough columns (${cols.size}), skipped")
                    return@forEachLine
                }
                skippedRows += rowToMemberAndGroup(
                    memberName    = cols.getOrElse(0) { "" },
                    phone         = cols.getOrElse(1) { "" },
                    groupName     = cols.getOrElse(2) { "" },
                    baseAmountStr = cols.getOrElse(4) { "0" },
                    members, groupMap, seenPhones, errors, "Row $lineIndex"
                )
            }
        }
    } catch (e: Exception) {
        errors.add("CSV read error: ${e.message}")
    }
    return CsvImportResult(members, groupMap.values.toList(), skippedRows, errors)
}

// ─── XLSX Parser (zero-dependency) ───────────────────────────────────────────
//
// XLSX = ZIP archive containing:
//   xl/sharedStrings.xml  – deduplicated string table (cells with t="s" reference this)
//   xl/worksheets/sheet1.xml – cell data
//
// We read both using ZipInputStream and parse XML with Android's XmlPullParser.

/** Parse a .xlsx file without any external library (works on minSdk 24). */
private fun parseXlsxFile(context: android.content.Context, uri: Uri): CsvImportResult {
    val members = mutableListOf<MemberEntity>()
    val groupMap = mutableMapOf<String, ChitGroupEntity>()
    val errors = mutableListOf<String>()
    var skippedRows = 0
    val seenPhones = mutableSetOf<String>()

    try {
        // Buffer the whole file so we can make two passes (strings + sheet)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return CsvImportResult(emptyList(), emptyList(), 0, listOf("Could not open file"))

        // ── Pass 1: read shared string table ─────────────────────────────────
        val sharedStrings = mutableListOf<String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    readSharedStrings(zip, sharedStrings)
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        // ── Pass 2: read sheet rows ───────────────────────────────────────────
        val rows = mutableListOf<List<String>>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    readSheetRows(zip, sharedStrings, rows)
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (rows.isEmpty()) {
            return CsvImportResult(emptyList(), emptyList(), 0,
                listOf("No data found in the first sheet. Is the file empty?"))
        }

        // rows[0] = header row — skip it
        rows.drop(1).forEachIndexed { idx, cols ->
            if (cols.size < 6) {
                skippedRows++
                errors.add("Row ${idx + 2}: not enough columns (${cols.size}), skipped")
                return@forEachIndexed
            }
            skippedRows += rowToMemberAndGroup(
                memberName    = cols.getOrElse(0) { "" },
                phone         = cols.getOrElse(1) { "" },
                groupName     = cols.getOrElse(2) { "" },
                baseAmountStr = cols.getOrElse(4) { "0" },
                members, groupMap, seenPhones, errors, "Row ${idx + 2}"
            )
        }
    } catch (e: Exception) {
        errors.add("XLSX read error: ${e.message}")
    }
    return CsvImportResult(members, groupMap.values.toList(), skippedRows, errors)
}

/**
 * Parse xl/sharedStrings.xml and populate [out].
 * Format: <sst><si><t>string value</t></si>…</sst>
 */
private fun readSharedStrings(stream: InputStream, out: MutableList<String>) {
    val parser = android.util.Xml.newPullParser()
    parser.setInput(stream, "UTF-8")
    var inSi = false
    var inT = false
    val currentText = StringBuilder()

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "si" -> { inSi = true; currentText.clear() }
                "t"  -> if (inSi) inT = true
            }
            XmlPullParser.TEXT -> if (inT) currentText.append(parser.text)
            XmlPullParser.END_TAG -> when (parser.name) {
                "t"  -> inT = false
                "si" -> { out.add(currentText.toString()); inSi = false }
            }
        }
        event = parser.next()
    }
}

/**
 * Parse xl/worksheets/sheet1.xml and append each row (as a list of strings) to [rows].
 * Handles shared-string cells (t="s"), numeric cells, and sparse columns (gaps in A→Z).
 */
private fun readSheetRows(
    stream: InputStream,
    sharedStrings: List<String>,
    rows: MutableList<List<String>>
) {
    val parser = android.util.Xml.newPullParser()
    parser.setInput(stream, "UTF-8")

    var inRow      = false
    var cellType   = ""        // "s" = shared string, else numeric/inline
    var inV        = false
    val cellValue  = StringBuilder()
    var currentRow = mutableListOf<Pair<Int, String>>() // (colIndex, value)
    var cellColIdx = 0

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "row" -> { inRow = true; currentRow = mutableListOf() }
                "c"   -> {
                    val cellRef = parser.getAttributeValue(null, "r") ?: ""
                    cellType   = parser.getAttributeValue(null, "t") ?: ""
                    cellColIdx = cellRefToColIndex(cellRef)
                    cellValue.clear()
                }
                "v"   -> if (inRow) inV = true
                "t"   -> if (inRow && cellType == "inlineStr") inV = true // <is><t> inline
            }
            XmlPullParser.TEXT -> if (inV) cellValue.append(parser.text)
            XmlPullParser.END_TAG -> when (parser.name) {
                "v", "t" -> inV = false
                "c" -> {
                    val raw = cellValue.toString().trim()
                    val resolved = if (cellType == "s") {
                        sharedStrings.getOrElse(raw.toIntOrNull() ?: -1) { raw }
                    } else {
                        // Numeric cells: drop trailing ".0" that Excel adds
                        raw.removeSuffix(".0")
                    }
                    currentRow.add(Pair(cellColIdx, resolved))
                }
                "row" -> {
                    if (inRow) {
                        // Rebuild as a dense list, respecting sparse column indices
                        val maxCol = currentRow.maxOfOrNull { it.first + 1 } ?: 0
                        val dense = Array(maxCol) { "" }
                        currentRow.forEach { (col, v) -> if (col < maxCol) dense[col] = v }
                        rows.add(dense.toList())
                        inRow = false
                    }
                }
            }
        }
        event = parser.next()
    }
}

/**
 * Converts an Excel cell reference column part (e.g. "A", "B", "AA") to a 0-based index.
 * "A" → 0, "B" → 1, "Z" → 25, "AA" → 26
 */
private fun cellRefToColIndex(cellRef: String): Int {
    val colStr = cellRef.takeWhile { it.isLetter() }.uppercase()
    var result = 0
    for (ch in colStr) result = result * 26 + (ch - 'A' + 1)
    return result - 1
}
