package com.jothivel.chits.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "installments", indices = {@Index(value={"groupId","installmentNo"})})
public class InstallmentEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String groupId;
    public int installmentNo;
    public int baseAmount;
    public Integer kasaruAmount; // Nullable
    public Integer payoutAmount; // Nullable
    public String auctionDate; // Nullable
    public String status;
    public String winningMemberId; // Nullable
}
