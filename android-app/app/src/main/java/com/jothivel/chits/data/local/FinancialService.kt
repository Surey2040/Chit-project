package com.jothivel.chits.data.local

import com.jothivel.chits.data.local.entity.ActivityLogEntity
import com.jothivel.chits.data.local.entity.FinancialTransactionEntity
import java.util.UUID

object FinancialService {
    val allowedTypes = setOf("SETTLEMENT", "DELIVERY")

    fun record(db: AppDatabase, requestId: String, type: String, memberId: String, groupId: String, amountPaise: Long, mode: String, reference: String?, notes: String): String {
        require(type in allowedTypes) { "Unsupported transaction type" }
        require(amountPaise > 0) { "Enter a valid amount" }
        require(mode == "Cash" || !reference.isNullOrBlank()) { "Reference / UTR is required" }
        require(db.membershipDao().getSync(memberId, groupId)?.isActive == true) { "Customer is not active in this chit" }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        db.runInTransaction {
            db.financialTransactionDao().insert(FinancialTransactionEntity().apply {
                this.id=id; this.requestId=requestId; this.type=type; this.memberId=memberId; this.groupId=groupId
                this.amountPaise=amountPaise; this.mode=mode; referenceNo=reference?.trim()?.ifBlank { null }
                this.notes=notes.trim(); occurredAt=now; status="POSTED"; reversalReason=null; reversedAt=null
            })
            db.activityLogDao().insertLog(ActivityLogEntity(actionType="${type}_RECORDED", title="${type.lowercase().replaceFirstChar(Char::uppercase)} recorded", description="$memberId • $groupId • ₹${amountPaise/100}", timestamp=now))
        }
        return id
    }

    fun reverse(db: AppDatabase, id: String, reason: String) {
        require(reason.trim().length >= 4) { "Enter a reversal reason" }
        val now=System.currentTimeMillis()
        db.runInTransaction {
            check(db.financialTransactionDao().reversePosted(id, reason.trim(), now) == 1) { "Entry was already reversed or not found" }
            db.activityLogDao().insertLog(ActivityLogEntity(actionType="FINANCIAL_REVERSED", title="Financial entry reversed", description="$id • ${reason.trim()}", timestamp=now))
        }
    }
}
