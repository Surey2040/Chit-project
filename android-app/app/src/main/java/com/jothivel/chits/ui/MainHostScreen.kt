package com.jothivel.chits.ui

import androidx.compose.runtime.Composable
import com.jothivel.chits.ui.collections.PaymentViewModel
import com.jothivel.chits.ui.dashboard.DashboardViewModel
import com.jothivel.chits.ui.groups.GroupViewModel
import com.jothivel.chits.ui.ledger.LedgerViewModel
import com.jothivel.chits.ui.members.MemberViewModel

/**
 * The approved mobile-first shell. The ViewModels stay in this boundary so the
 * preview data in ApprovedAppFlow can be replaced by Room/API state without
 * changing navigation or screen components when the legacy configuration arrives.
 */
@Composable
fun MainHostScreen(
    dashboardViewModel: DashboardViewModel,
    groupViewModel: GroupViewModel,
    memberViewModel: MemberViewModel,
    ledgerViewModel: LedgerViewModel,
    paymentViewModel: PaymentViewModel,
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
