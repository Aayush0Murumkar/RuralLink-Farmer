package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transport_requests")
data class TransportRequest(
    @PrimaryKey val id: String = "",
    val farmerId: String = "",
    val pickupPoint: String = "",
    val dropPoint: String = "",
    val pickupLatitude: Double? = null,
    val pickupLongitude: Double? = null,
    val dropLatitude: Double? = null,
    val dropLongitude: Double? = null,
    val weight: Double = 0.0,
    val weightUnit: String = "", // e.g. "Quintal", "Ton", "kg"
    val materialType: String = "", // e.g. "Wheat / Grains", "Vegetables", "Sugarcane"
    val requiredDate: String = "",
    val requiredTime: String = "",
    val status: String = "SEARCHING", // "SEARCHING", "RESPONSES_RECEIVED", "TRANSPORTER_SELECTED", "PAYMENT_PENDING", "PAID", "IN_TRANSIT", "DELIVERED", "COMPLETED", "CANCELLED"
    val paymentStatus: String = "PENDING", // "PENDING", "PAID"
    val selectedTransporterId: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
