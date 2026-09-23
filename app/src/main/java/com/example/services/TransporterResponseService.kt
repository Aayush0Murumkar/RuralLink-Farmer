package com.example.services

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.NotificationItem
import com.example.data.model.TransporterResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TransporterResponseService(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val firebaseService = FirebaseService()
    val supabaseService = SupabaseService(context)

    fun getResponsesForRequest(requestId: String): Flow<List<TransporterResponse>> {
        return db.transporterResponseDao().getResponsesForRequest(requestId)
    }

    suspend fun syncFirestoreResponses(requestId: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // First check Supabase
            try {
                val supabaseResponses = supabaseService.getResponsesForRequest(requestId)
                if (supabaseResponses.isNotEmpty()) {
                    db.transporterResponseDao().insertResponses(supabaseResponses)
                }
            } catch (e: Exception) {
                // Fallback to Firebase
            }

            firebaseService.getResponsesForRequestFlow(requestId).collect { remoteResponses ->
                if (remoteResponses.isNotEmpty()) {
                    db.transporterResponseDao().insertResponses(remoteResponses)
                }
            }
        }
    }

    suspend fun selectTransporter(requestId: String, responseId: String, farmerId: String) {
        val responses = db.transporterResponseDao().getResponsesForRequestSync(requestId)
        val selectedResponse = responses.find { it.id == responseId } ?: return

        // Update request locally, in Supabase, and in Firestore
        db.requestDao().updateSelectedTransporter(
            id = requestId,
            transporterId = selectedResponse.transporterId,
            status = "TRANSPORTER_SELECTED"
        )
        // Async remote sync
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try { supabaseService.updateBookingStatus(requestId, "TRANSPORTER_SELECTED") } catch (_: Exception) {}
            try { firebaseService.updateBookingStatus(requestId, "TRANSPORTER_SELECTED", selectedResponse.transporterId) } catch (_: Exception) {}
        }

        // Mark selected response as SELECTED and others as NOT_SELECTED
        for (res in responses) {
            val newStatus = if (res.id == responseId) "SELECTED" else "NOT_SELECTED"
            val updatedRes = res.copy(status = newStatus)
            db.transporterResponseDao().updateResponseStatus(res.id, newStatus)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try { supabaseService.saveTransporterResponse(updatedRes) } catch (_: Exception) {}
                try { firebaseService.saveTransporterResponse(updatedRes) } catch (_: Exception) {}
            }
        }

        // Add Notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_select_${System.currentTimeMillis()}",
                farmerId = farmerId,
                title = "Transporter Selected for $requestId",
                message = "You selected ${selectedResponse.transporterName} (${selectedResponse.vehicleNumber}). Driver will arrive around ${selectedResponse.estimatedArrival}.",
                relatedRequestId = requestId,
                read = false
            )
        )
    }

    suspend fun setDemoResponsesCount(requestId: String, count: Int) {
        // Clear existing
        db.transporterResponseDao().deleteResponsesForRequest(requestId)

        if (count == 0) {
            db.requestDao().updateStatus(requestId, "SEARCHING")
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try { supabaseService.updateBookingStatus(requestId, "SEARCHING") } catch (_: Exception) {}
                try { firebaseService.updateBookingStatus(requestId, "SEARCHING") } catch (_: Exception) {}
            }
            return
        }

        val req = db.requestDao().getRequestById(requestId) ?: return
        val demoList = createDemoResponses(req).take(count)
        db.transporterResponseDao().insertResponses(demoList)
        db.requestDao().updateStatus(requestId, "RESPONSES_RECEIVED")

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            demoList.forEach { 
                try { supabaseService.saveTransporterResponse(it) } catch (_: Exception) {}
                try { firebaseService.saveTransporterResponse(it) } catch (_: Exception) {}
            }
            try { supabaseService.updateBookingStatus(requestId, "RESPONSES_RECEIVED") } catch (_: Exception) {}
            try { firebaseService.updateBookingStatus(requestId, "RESPONSES_RECEIVED") } catch (_: Exception) {}
        }
    }

    suspend fun seedDemoResponsesIfEmpty(requestId: String) {
        val existing = db.transporterResponseDao().getResponsesForRequestSync(requestId)
        if (existing.isEmpty()) {
            val req = db.requestDao().getRequestById(requestId)
            if (req != null) {
                val demoList = createDemoResponses(req)
                db.transporterResponseDao().insertResponses(demoList)
                db.requestDao().updateStatus(requestId, "RESPONSES_RECEIVED")

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    demoList.forEach { 
                        try { supabaseService.saveTransporterResponse(it) } catch (_: Exception) {}
                        try { firebaseService.saveTransporterResponse(it) } catch (_: Exception) {}
                    }
                    try { supabaseService.updateBookingStatus(requestId, "RESPONSES_RECEIVED") } catch (_: Exception) {}
                    try { firebaseService.updateBookingStatus(requestId, "RESPONSES_RECEIVED") } catch (_: Exception) {}
                }
            }
        }
    }

    private fun createDemoResponses(request: com.example.data.model.TransportRequest): List<TransporterResponse> {
        val requestId = request.id
        // We will simulate a distance (e.g. 150 km) and use the farmer's weight from request
        val mockDistanceKm = 150.0 
        
        // Response 1: Return trip (Backload)
        val p1 = com.example.domain.PricingEngine.calculatePricing(
            distanceKm = mockDistanceKm,
            vehicleType = "Eicher 6-Wheeler Truck",
            farmerWeightKg = request.weight,
            isReturnTrip = true
        )
        
        // Response 2: Dedicated / Non-return
        val p2 = com.example.domain.PricingEngine.calculatePricing(
            distanceKm = mockDistanceKm,
            vehicleType = "Tata 407 Medium Truck",
            farmerWeightKg = request.weight,
            isReturnTrip = false
        )
        
        // Response 3: Backload
        val p3 = com.example.domain.PricingEngine.calculatePricing(
            distanceKm = mockDistanceKm,
            vehicleType = "Bolero Pickup",
            farmerWeightKg = request.weight,
            isReturnTrip = true
        )

        // Formatter helper
        fun formatPrice(amount: Double) = "₹%,.0f".format(amount)

        return listOf(
            TransporterResponse(
                id = "RESP_${requestId}_1",
                requestId = requestId,
                transporterId = "TRANS_101",
                transporterName = "Rajesh Patil (Kisan Express)",
                vehicleNumber = "MH-15-AB-1234",
                vehicleType = "Eicher 6-Wheeler Truck",
                vehicleCapacity = "5 tonnes",
                capacityTons = 5.0,
                estimatedDistance = "8 km",
                distanceKm = 8.0,
                routeMatch = "Excellent match (Direct Mandi Highway)",
                routeMatchScore = 30,
                estimatedArrival = "4:20 PM",
                timeScore = 19,
                matchingScore = 96,
                status = "ACCEPTED",
                priceQuote = formatPrice(p1.finalAmount),
                fuelCost = p1.fuelCost,
                operatingCost = p1.operatingCost,
                transporterProfit = p1.requiredTransportRevenue - p1.operatingCost,
                totalTripCost = p1.requiredTransportRevenue,
                capacitySharePercentage = p1.capacitySharePercentage,
                isReturnTrip = true,
                basePrice = p1.basePrice,
                backloadDiscountAmount = p1.backloadDiscountAmount,
                ruralLinkPrice = p1.ruralLinkPrice,
                normalPrice = p1.normalPrice,
                savings = p1.savings,
                platformFee = p1.platformFee,
                finalAmount = p1.finalAmount
            ),
            TransporterResponse(
                id = "RESP_${requestId}_2",
                requestId = requestId,
                transporterId = "TRANS_102",
                transporterName = "Jai Maharashtra Freight (Suresh Deshmukh)",
                vehicleNumber = "MH-12-CD-5678",
                vehicleType = "Tata 407 Medium Truck",
                vehicleCapacity = "4 tonnes",
                capacityTons = 4.0,
                estimatedDistance = "12 km",
                distanceKm = 12.0,
                routeMatch = "Good route match",
                routeMatchScore = 28,
                estimatedArrival = "4:45 PM",
                timeScore = 18,
                matchingScore = 93,
                status = "ACCEPTED",
                priceQuote = formatPrice(p2.finalAmount),
                fuelCost = p2.fuelCost,
                operatingCost = p2.operatingCost,
                transporterProfit = p2.requiredTransportRevenue - p2.operatingCost,
                totalTripCost = p2.requiredTransportRevenue,
                capacitySharePercentage = p2.capacitySharePercentage,
                isReturnTrip = false,
                basePrice = p2.basePrice,
                backloadDiscountAmount = p2.backloadDiscountAmount,
                ruralLinkPrice = p2.ruralLinkPrice,
                normalPrice = p2.normalPrice,
                savings = p2.savings,
                platformFee = p2.platformFee,
                finalAmount = p2.finalAmount
            ),
            TransporterResponse(
                id = "RESP_${requestId}_3",
                requestId = requestId,
                transporterId = "TRANS_103",
                transporterName = "Godavari Agro Logistics (Anand Shinde)",
                vehicleNumber = "MH-18-EF-9012",
                vehicleType = "Bolero Pickup",
                vehicleCapacity = "1.5 tonnes",
                capacityTons = 1.5,
                estimatedDistance = "18 km",
                distanceKm = 18.0,
                routeMatch = "Satisfactory route match",
                routeMatchScore = 24,
                estimatedArrival = "5:15 PM",
                timeScore = 16,
                matchingScore = 85,
                status = "ACCEPTED",
                priceQuote = formatPrice(p3.finalAmount),
                fuelCost = p3.fuelCost,
                operatingCost = p3.operatingCost,
                transporterProfit = p3.requiredTransportRevenue - p3.operatingCost,
                totalTripCost = p3.requiredTransportRevenue,
                capacitySharePercentage = p3.capacitySharePercentage,
                isReturnTrip = true,
                basePrice = p3.basePrice,
                backloadDiscountAmount = p3.backloadDiscountAmount,
                ruralLinkPrice = p3.ruralLinkPrice,
                normalPrice = p3.normalPrice,
                savings = p3.savings,
                platformFee = p3.platformFee,
                finalAmount = p3.finalAmount
            )
        )
    }
}
