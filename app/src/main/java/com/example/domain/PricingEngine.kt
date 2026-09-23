package com.example.domain

import kotlin.math.max

data class PricingConfig(
    val dieselPrice: Double = 98.38,
    val platformFeePercent: Double = 0.05,
    val backloadDiscountPercent: Double = 0.25,
    val driverCostPerKm: Double = 5.0,
    val maintenanceCostPerKm: Double = 2.0,
    val otherCostPerKm: Double = 1.0,
    val defaultProfitMarginPercent: Double = 0.15,
    val minimumFare: Double = 500.0,
    val maximumDiscountPercent: Double = 0.50
)

data class VehicleConfig(
    val vehicleType: String,
    val capacityKg: Double,
    val defaultMileageKmpl: Double,
    val operatingCostFactor: Double = 1.0
)

data class PricingResult(
    val fuelCost: Double,
    val operatingCost: Double,
    val requiredTransportRevenue: Double,
    val capacitySharePercentage: Double,
    val basePrice: Double, // The raw price based on share
    val backloadDiscountAmount: Double,
    val ruralLinkPrice: Double, // Price after backload
    val normalPrice: Double, // The price if it was a dedicated truck
    val savings: Double,
    val platformFee: Double,
    val finalAmount: Double
)

object PricingEngine {
    
    // In a full production app, this would be fetched from Supabase
    var currentConfig = PricingConfig()
    
    fun getVehicleConfig(vehicleType: String): VehicleConfig {
        return when (vehicleType) {
            "Eicher 6-Wheeler Truck" -> VehicleConfig(vehicleType, 5000.0, 7.0)
            "Tata 407 Medium Truck" -> VehicleConfig(vehicleType, 4000.0, 9.0)
            "Tata Ace Small Truck" -> VehicleConfig(vehicleType, 750.0, 14.0)
            "Bolero Pickup" -> VehicleConfig(vehicleType, 1500.0, 12.0)
            else -> VehicleConfig(vehicleType, 2000.0, 10.0)
        }
    }

    fun calculatePricing(
        distanceKm: Double,
        vehicleType: String,
        farmerWeightKg: Double,
        isReturnTrip: Boolean,
        tolls: Double = 0.0
    ): PricingResult {
        val vConfig = getVehicleConfig(vehicleType)
        
        // 1. Fuel Cost
        val fuelCost = (distanceKm / vConfig.defaultMileageKmpl) * currentConfig.dieselPrice
        
        // 2. Operating Cost
        val driverCost = distanceKm * currentConfig.driverCostPerKm * vConfig.operatingCostFactor
        val maintenanceCost = distanceKm * currentConfig.maintenanceCostPerKm * vConfig.operatingCostFactor
        val otherCost = distanceKm * currentConfig.otherCostPerKm * vConfig.operatingCostFactor
        val operatingCost = fuelCost + driverCost + maintenanceCost + otherCost + tolls
        
        // 3. Required Revenue (including Transporter Profit)
        val transporterProfit = operatingCost * currentConfig.defaultProfitMarginPercent
        val requiredTransportRevenue = operatingCost + transporterProfit
        
        // 4. Capacity Share
        val rawShare = farmerWeightKg / vConfig.capacityKg
        val capacitySharePercentage = rawShare.coerceIn(0.1, 1.0) // Minimum 10% share
        
        // 5. Normal Dedicated Price (if they booked the whole truck)
        val normalPrice = requiredTransportRevenue
        
        // 6. Shared Capacity Base Price
        var basePrice = requiredTransportRevenue * capacitySharePercentage
        if (basePrice < currentConfig.minimumFare) {
            basePrice = currentConfig.minimumFare
        }
        
        // 7. Backload / Return Trip Discount
        var backloadDiscountAmount = 0.0
        if (isReturnTrip) {
            backloadDiscountAmount = basePrice * currentConfig.backloadDiscountPercent
        }
        
        // Enforce maximum total discount if necessary (e.g. from dedicated truck price)
        var ruralLinkPrice = basePrice - backloadDiscountAmount
        if (ruralLinkPrice < currentConfig.minimumFare) {
            ruralLinkPrice = currentConfig.minimumFare
            backloadDiscountAmount = basePrice - ruralLinkPrice
        }
        
        // 8. Farmer Savings
        val savings = normalPrice - ruralLinkPrice
        
        // 9. Platform Fee
        val platformFee = ruralLinkPrice * currentConfig.platformFeePercent
        
        // 10. Final Amount
        val finalAmount = ruralLinkPrice + platformFee
        
        return PricingResult(
            fuelCost = fuelCost,
            operatingCost = operatingCost,
            requiredTransportRevenue = requiredTransportRevenue,
            capacitySharePercentage = capacitySharePercentage,
            basePrice = basePrice,
            backloadDiscountAmount = backloadDiscountAmount,
            ruralLinkPrice = ruralLinkPrice,
            normalPrice = normalPrice,
            savings = savings,
            platformFee = platformFee,
            finalAmount = finalAmount
        )
    }
}
