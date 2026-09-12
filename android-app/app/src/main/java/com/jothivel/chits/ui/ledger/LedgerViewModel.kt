package com.jothivel.chits.ui.ledger

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.jothivel.chits.data.local.AppDatabase
import com.jothivel.chits.data.local.entity.ChitGroupEntity
import com.jothivel.chits.data.local.entity.MemberEntity
import com.jothivel.chits.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LedgerMemberItem(
    val member: MemberEntity,
    val totalPaid: Long,
    val totalDue: Long,
    val status: String // "PAID", "PARTIAL", "OVERDUE"
)

data class LedgerSummary(
    val totalCollected: Long = 0,
    val totalPending: Long = 0,
    val overdueMembers: Int = 0
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val groupDao = db.groupDao()
    private val memberDao = db.memberDao()
    private val paymentDao = db.paymentDao()

    private val prefs = application.getSharedPreferences("ledger_prefs", Context.MODE_PRIVATE)

    // Level 1: All Groups for Bottom Sheet
    val allGroups: LiveData<List<ChitGroupEntity>> = groupDao.allGroups

    // Selected Group State
    private val _selectedGroupId = MutableLiveData<String>().apply {
        value = prefs.getString("last_selected_chit_id", "")
    }
    val selectedGroupId: LiveData<String> = _selectedGroupId

    val selectedGroup: LiveData<ChitGroupEntity> = _selectedGroupId.switchMap { id ->
        groupDao.getGroupById(id ?: "")
    }

    // Level 2: Members List scoped to Group
    val membersInSelectedGroup: LiveData<List<MemberEntity>> = _selectedGroupId.switchMap { id ->
        memberDao.getMembersByChitId(id ?: "")
    }

    // The reconciled data (Level 2 list)
    private val _ledgerMemberItems = MutableLiveData<List<LedgerMemberItem>>()
    val ledgerMemberItems: LiveData<List<LedgerMemberItem>> = _ledgerMemberItems

    private val _ledgerSummary = MutableLiveData<LedgerSummary>()
    val ledgerSummary: LiveData<LedgerSummary> = _ledgerSummary

    private val membersObserver = Observer<List<MemberEntity>> { members ->
        recalculateLedger(members ?: emptyList())
    }

    init {
        membersInSelectedGroup.observeForever(membersObserver)
        
        // Auto-select first group if none selected
        allGroups.observeForever { groups ->
            val currentSelected = _selectedGroupId.value
            if (currentSelected.isNullOrEmpty() && groups.isNotEmpty()) {
                selectGroup(groups[0].id)
            }
        }
    }

    fun selectGroup(groupId: String) {
        prefs.edit().putString("last_selected_chit_id", groupId).apply()
        _selectedGroupId.value = groupId
    }

    private fun recalculateLedger(members: List<MemberEntity>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val items = mutableListOf<LedgerMemberItem>()
            var collected = 0L
            var pending = 0L
            var overdueCount = 0

            val groupId = _selectedGroupId.value
            val group = if (!groupId.isNullOrEmpty()) groupDao.getGroupByIdSync(groupId) else null

            for (member in members) {
                // Expected total is the full chit value for this member in Rupees
                val expectedTotal = if (group != null) {
                    (group.chitValue / 100).toLong()
                } else {
                    (member.installmentAmount?.toLongOrNull() ?: 0L) * 20
                }
                
                // Fetch actual payments from DB synchronously
                val memberPayments = paymentDao.getPaymentsByMemberSync(member.id)
                val paidPaise = memberPayments.sumOf { it.amountPaid }
                val paid = paidPaise / 100 // Convert paise to rupees
                
                val due = if (expectedTotal > paid) expectedTotal - paid else 0L
                val status = if (paid >= expectedTotal) "PAID" else if (paid > 0) "PARTIAL" else "OVERDUE"

                items.add(LedgerMemberItem(member, paid, due, status))

                collected += paid
                pending += due
                if (status == "OVERDUE") overdueCount++
            }

            _ledgerMemberItems.postValue(items)
            _ledgerSummary.postValue(LedgerSummary(collected, pending, overdueCount))
        }
    }

    override fun onCleared() {
        super.onCleared()
        membersInSelectedGroup.removeObserver(membersObserver)
    }
}
