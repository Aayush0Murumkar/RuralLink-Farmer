package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_profile")
data class Farmer(
    @PrimaryKey val id: String = "farmer_101",
    val name: String = "",
    val address: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val aadhaarMasked: String = "",
    val mobileVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
