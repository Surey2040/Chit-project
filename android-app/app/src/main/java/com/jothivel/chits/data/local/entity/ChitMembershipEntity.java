package com.jothivel.chits.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "chit_memberships",
        indices = {
                @Index(value = {"memberId", "groupId"}, unique = true),
                @Index(value = {"groupId", "ticketNo"}, unique = true),
                @Index(value = {"groupId", "isActive"})
        }
)
public class ChitMembershipEntity {
    @PrimaryKey @NonNull public String id;
    @NonNull public String memberId;
    @NonNull public String groupId;
    public String ticketNo;
    public long installmentAmountPaise;
    public String joiningDate;
    public String dueDate;
    public boolean isActive;
}
