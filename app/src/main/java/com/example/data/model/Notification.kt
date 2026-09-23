package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey val id: String,
    val farmerId: String,
    val title: String,
    val message: String,
    val read: Boolean = false,
    val relatedRequestId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
