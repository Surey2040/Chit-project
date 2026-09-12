package com.jothivel.chits.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "chit_groups")
public class ChitGroupEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String registerNo;
    public int chitValue;
    public int durationMonths;
    public int subscriberCount;
    public String branch;
    public String startDate;
    public String status;
}
