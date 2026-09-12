package com.jothivel.chits.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "payments", indices = {@Index(value={"memberId","groupId","installmentId"}), @Index(value={"paidAt"})})
public class PaymentEntity {
    @PrimaryKey
    @NonNull
    public String id;
    
    public String memberId;
    public String groupId;
    public String installmentId;
    
    public long amountPaid; // In paise
    public String mode; // CASH, UPI, BANK_TRANSFER, DOOR_COLLECTION
    public String referenceNo;
    public String receiptNo; // Must be generated
    
    public long paidAt; // timestamp
    public String status; // PAID, PARTIAL

    public String collectedBy; // Agent/labourer name, null when recorded by admin
    public String collectedByAgentId; // Firestore agents/{id}, null when recorded by admin
}
