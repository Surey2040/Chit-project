package com.jothivel.chits.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jothivel.chits.ui.base.BaseActivity
import com.jothivel.chits.ui.dashboard.DashboardScreen
import com.jothivel.chits.ui.dashboard.DashboardViewModel
import com.jothivel.chits.ui.ledger.LedgerScreen
import com.jothivel.chits.ui.ledger.LedgerViewModel
import com.jothivel.chits.ui.members.AddMemberPage
import com.jothivel.chits.ui.members.MemberViewModel
import com.jothivel.chits.ui.settings.CsvImportActivity
import com.jothivel.chits.ui.settings.SettingsScreen
import com.jothivel.chits.ui.theme.JothiVelChitsTheme
import com.jothivel.chits.ui.theme.OffWhite
import kotlinx.coroutines.launch
import com.jothivel.chits.ui.groups.GroupListScreen
import com.jothivel.chits.ui.groups.GroupViewModel
import com.jothivel.chits.ui.components.AppBottomNavPager
import com.jothivel.chits.ui.collections.PaymentViewModel
import com.jothivel.chits.utils.DataBackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainHostActivity : BaseActivity() {

    private val backupDbLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-sqlite3")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = DataBackupHelper.backupDatabaseToUri(this@MainHostActivity, it)
                if (result.isSuccess) {
                    com.jothivel.chits.utils.AppPreferences(this@MainHostActivity).setLastBackupAt()
                    Toast.makeText(this@MainHostActivity, "Backup Saved Successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainHostActivity, "Backup failed — check the selected folder/storage and try again. ${result.exceptionOrNull()?.message.orEmpty()}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = DataBackupHelper.exportDataToCsv(this@MainHostActivity, it)
                if (result.isSuccess) {
                    Toast.makeText(this@MainHostActivity, "Data Exported Successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainHostActivity, "Export Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val restoreDbLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = DataBackupHelper.restoreDatabaseFromUri(this@MainHostActivity, it)
                if (result.isSuccess) {
                    Toast.makeText(this@MainHostActivity, "Data Restored Successfully! Restarting app...", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@MainHostActivity, com.jothivel.chits.ui.auth.LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@MainHostActivity, "Restore failed — select a valid Jothi Vel Chits backup file. ${result.exceptionOrNull()?.message.orEmpty()}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val importCsvLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val intent = Intent(this, CsvImportActivity::class.java)
            intent.putExtra(CsvImportActivity.EXTRA_CSV_URI, it.toString())
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (com.jothivel.chits.utils.AppPreferences(this).getUserRole() == com.jothivel.chits.utils.AppPreferences.ROLE_ADMIN) {
            com.jothivel.chits.data.firebase.FirebaseSyncService.start(this)
        }

        showMainContent()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.jothivel.chits.data.firebase.FirebaseSyncService.stop()
    }

    private fun showMainContent() {
        val dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        val ledgerViewModel = ViewModelProvider(this)[LedgerViewModel::class.java]
        val memberViewModel = ViewModelProvider(this)[MemberViewModel::class.java]
        val groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        val paymentViewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

        setContent {
            JothiVelChitsTheme {
                MainHostScreen(
                    dashboardViewModel,
                    groupViewModel,
                    memberViewModel,
                    ledgerViewModel,
                    paymentViewModel,
                    onLogout = {
                        com.jothivel.chits.ui.auth.LoginActivity.isSessionActive = false
                        val intent = Intent(this@MainHostActivity, com.jothivel.chits.ui.auth.LoginActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finish()
                    },
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
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "*/*"
                        ))
                    }
                )
            }
        }
    }
}
