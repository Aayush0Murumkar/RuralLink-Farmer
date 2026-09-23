package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiRecommendation(
    @Json(name = "curator_badge") val curatorBadge: CuratorBadge,
    @Json(name = "recommended_order_id") val recommendedOrderId: String,
    @Json(name = "crop_name") val cropName: String,
    @Json(name = "quantity") val quantity: String,
    @Json(name = "offered_price") val offeredPrice: String,
    @Json(name = "buyer_location") val buyerLocation: String,
    @Json(name = "curator_summary") val curatorSummary: String,
    @Json(name = "justifications") val justifications: Justifications,
    @Json(name = "recommended_transporters") val recommendedTransporters: List<RecommendedTransporter>
)

@JsonClass(generateAdapter = true)
data class CuratorBadge(
    val icon: String,
    val title: String
)

@JsonClass(generateAdapter = true)
data class Justifications(
    val location: String,
    val feasibility: String,
    val affordability: String
)

@JsonClass(generateAdapter = true)
data class RecommendedTransporter(
    @Json(name = "transporter_id") val transporterId: String,
    val name: String,
    @Json(name = "vehicle_type") val vehicleType: String,
    val capacity: String,
    @Json(name = "estimated_cost") val estimatedCost: String,
    val rating: Double,
    @Json(name = "default_selected") val defaultSelected: Boolean
)
