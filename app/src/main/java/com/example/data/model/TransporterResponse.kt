package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transporter_responses")
data class TransporterResponse(
    @PrimaryKey val id: String = "",
    val requestId: String = "",
    val transporterId: String = "",
    val transporterName: String = "",
    val vehicleNumber: String = "",
    val vehicleType: String = "",
    val vehicleCapacity: String = "", // e.g. "5 tonnes"
    val capacityTons: Double = 5.0,
    val estimatedDistance: String = "", // e.g. "8 km"
    val distanceKm: Double = 8.0,
    val routeMatch: String = "", // e.g. "Excellent match"
    val routeMatchScore: Int = 30, // out of 30
    val estimatedArrival: String = "", // e.g. "4:20 PM"
    val timeScore: Int = 19, // out of 20
    val matchingScore: Int = 94, // total 0-100
    val status: String = "ACCEPTED", // "ACCEPTED", "DECLINED", "SELECTED", "NOT_SELECTED"
    val priceQuote: String = "₹12,000",
    
    // RURAL LINK PRICING ENGINE FIELDS
    val fuelCost: Double = 0.0,
    val operatingCost: Double = 0.0,
    val transporterProfit: Double = 0.0,
    val totalTripCost: Double = 0.0,
    val capacitySharePercentage: Double = 1.0,
    val isReturnTrip: Boolean = false,
    val basePrice: Double = 0.0,
    val backloadDiscountAmount: Double = 0.0,
    val ruralLinkPrice: Double = 0.0,
    val normalPrice: Double = 0.0,
    val savings: Double = 0.0,
    val platformFee: Double = 0.0,
    val finalAmount: Double = 0.0,
    
    val responseTime: Long = System.currentTimeMillis()
)

