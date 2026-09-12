package com.jothivel.chits.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.jothivel.chits.ui.base.BaseActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.jothivel.chits.ui.components.SmoothTransitions
import com.jothivel.chits.ui.theme.JothiVelChitsTheme
import com.jothivel.chits.utils.DataBackupHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : BaseActivity() {

    private val backupDbLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-sqlite3")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = DataBackupHelper.backupDatabaseToUri(this@SettingsActivity, it)
                if (result.isSuccess) {
                    Toast.makeText(this@SettingsActivity, "Backup Saved Successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Backup Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = DataBackupHelper.exportDataToCsv(this@SettingsActivity, it)
                if (result.isSuccess) {
                    Toast.makeText(this@SettingsActivity, "Data Exported Successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Export Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val restoreDbLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = DataBackupHelper.restoreDatabaseFromUri(this@SettingsActivity, it)
                if (result.isSuccess) {
                    Toast.makeText(this@SettingsActivity, "Data Restored Successfully! Restarting app...", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@SettingsActivity, com.jothivel.chits.ui.auth.LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@SettingsActivity, "Restore Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Import — open CSV, XLS or XLSX and pass URI to CsvImportActivity
    private val importCsvLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val intent = Intent(this, CsvImportActivity::class.java)
            intent.putExtra(CsvImportActivity.EXTRA_CSV_URI, it.toString())
            // Grant read permission for the chosen URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JothiVelChitsTheme {
                SettingsScreen(
                    onBackClick = { SmoothTransitions.finishSmooth(this) },
                    onBackupDatabase = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        backupDbLauncher.launch("ChitsBackup_$timestamp.db")
                    },
                    onRestoreDatabase = {
                        restoreDbLauncher.launch(arrayOf("*/*"))
                    },
                    onExportCsv = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportCsvLauncher.launch("ChitsData_$timestamp.csv")
                    },
                    onImportCsv = {
                        importCsvLauncher.launch(arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "application/vnd.ms-excel",                                        // .xls
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                            "*/*"
                        ))
                    },
                    onLanguageChanged = {
                        recreate()
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
