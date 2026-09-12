package com.jothivel.chits.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "financial_transactions", indices = {
        @Index(value = {"memberId", "groupId", "occurredAt"}),
        @Index(value = {"requestId"}, unique = true)
})
public class FinancialTransactionEntity {
    @PrimaryKey @NonNull public String id;
    @NonNull public String requestId;
    @NonNull public String type;
    @NonNull public String memberId;
    @NonNull public String groupId;
    public long amountPaise;
    @NonNull public String mode;
    public String referenceNo;
    public String notes;
    public long occurredAt;
    @NonNull public String status;
    public String reversalReason;
    public Long reversedAt;
}
