package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transporters")
data class Transporter(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val vehicleNumber: String = "",
    val vehicleType: String = "",
    val capacityTons: Double = 0.0,
    val rating: Double = 4.8,
    val verified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
