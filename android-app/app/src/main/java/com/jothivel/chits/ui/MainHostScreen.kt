package com.jothivel.chits.ui

import androidx.compose.runtime.Composable

/**
 * The approved mobile-first shell. Thin wrapper kept as its own boundary so
 * ApprovedAppFlow's internals can keep evolving without touching MainHostActivity.
 */
@Composable
fun MainHostScreen(
    onLogout: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit
) {
    ApprovedAppFlow(
        onLogout = onLogout,
        onBackupDatabase = onBackupDatabase,
        onRestoreDatabase = onRestoreDatabase,
        onExportCsv = onExportCsv,
        onImportCsv = onImportCsv
    )
}
