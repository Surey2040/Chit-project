package com.jothivel.chits.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val actionType: String, // e.g., "GROUP_CREATED", "MEMBER_ADDED", "PAYMENT_RECORDED"
    val title: String,      // e.g., "New Chit Group Created"
    val description: String, // e.g., "Group 2004 was successfully created."
    val timestamp: Long = System.currentTimeMillis()
)
