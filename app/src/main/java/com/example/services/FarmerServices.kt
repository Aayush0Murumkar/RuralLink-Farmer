package com.example.services

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Farmer
import com.example.data.model.NotificationItem
import com.example.data.model.TransportRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

// Masking utility for Aadhaar as required by Privacy Rule #11 & Stage 2 #6
object AadhaarUtils {
    fun maskAadhaar(rawInput: String): String {
        val digits = rawInput.filter { it.isDigit() }
        if (digits.length >= 4) {
            val last4 = digits.takeLast(4)
            return "XXXX XXXX $last4"
        }
        return "XXXX XXXX 1234"
    }

    fun isValidAadhaar(rawInput: String): Boolean {
        val digits = rawInput.filter { it.isDigit() }
        return digits.length == 12
    }
}

object ValidationUtils {
    fun isValidName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotBlank() && trimmed.length >= 2
    }

    fun isValidAddress(address: String): Boolean {
        return address.trim().isNotBlank()
    }

    fun isValidMobile(mobile: String): Boolean {
        val digits = mobile.filter { it.isDigit() }
        // Standard Indian mobile number: 10 digits starting with 6-9
        return digits.length == 10 && digits.firstOrNull() in '6'..'9'
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true // Email is optional unless user entered text
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (!emailRegex.matches(email.trim())) return false
        val domain = email.trim().substringAfter("@", "").lowercase()
        // Reject fake/reserved test domains that are restricted by Supabase Auth (e.g. example.com)
        if (domain == "example.com" || domain == "example.org" || domain == "example.net" || domain == "test.com") return false
        return true
    }
}

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthService(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val firebaseService = FirebaseService()
    val supabaseService = SupabaseService(context)

    val currentFarmer: Flow<Farmer?> = db.farmerDao().getFarmer()

    companion object {
        const val MOCK_FARMER_ID = "8008db69-b5f7-3e88-9d57-e1ee4b46c9c8"

        val DEMO_FARMERS = listOf(
            Farmer(
                id = MOCK_FARMER_ID,
                name = "Ramesh Patil",
                address = "Nashik APMC Market Yard, Maharashtra",
                mobileNumber = "9876543210",
                email = "ramesh.farmer@gmail.com",
                aadhaarMasked = "XXXX XXXX 8899",
                mobileVerified = true
            ),
            Farmer(
                id = "7108aa12-c4e8-4f99-8b45-2bee3c55d1a1",
                name = "Suresh Deshmukh",
                address = "Kopargaon Farm Mandi, Ahmednagar",
                mobileNumber = "9823456789",
                email = "suresh.deshmukh@gmail.com",
                aadhaarMasked = "XXXX XXXX 4422",
                mobileVerified = true
            ),
            Farmer(
                id = "9908bb33-d5f9-4a11-9c66-3aff4d66e2b2",
                name = "Anand Shinde",
                address = "Gultekdi Market Yard, Pune",
                mobileNumber = "9765432100",
                email = "anand.shinde@gmail.com",
                aadhaarMasked = "XXXX XXXX 7711",
                mobileVerified = true
            )
        )
    }

    suspend fun loginAsMockFarmer(farmer: Farmer): AuthResult {
        try {
            db.farmerDao().saveFarmer(farmer)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try { supabaseService.saveFarmerProfile(farmer) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            return AuthResult.Error(e.localizedMessage ?: "Failed to switch mock profile")
        }
        return AuthResult.Success
    }

    fun hasValidSession(): Boolean {
        // Mock auth enabled: App always has a valid session ready for booking
        return true
    }

    fun getCurrentUserId(): String {
        return supabaseService.getCurrentUserId()?.ifBlank { null } ?: MOCK_FARMER_ID
    }

    suspend fun checkSessionOnStart(): Boolean {
        try {
            val existingLocalFarmer = db.farmerDao().getFarmerOnce()
            if (existingLocalFarmer == null) {
                val defaultFarmer = Farmer(
                    id = MOCK_FARMER_ID,
                    name = "Ramesh Patil",
                    address = "Nashik APMC Market Yard, Maharashtra",
                    mobileNumber = "9876543210",
                    email = "ramesh.farmer@gmail.com",
                    aadhaarMasked = "XXXX XXXX 8899",
                    mobileVerified = true
                )
                db.farmerDao().saveFarmer(defaultFarmer)
                // Attempt async background sync to Supabase public.farmers
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try { supabaseService.saveFarmerProfile(defaultFarmer) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        return true
    }

    suspend fun registerFarmer(
        name: String,
        address: String,
        mobileNumber: String,
        aadhaarInput: String,
        email: String,
        password: String = "rural123"
    ): AuthResult {
        val cleanName = name.trim().ifBlank { "Ramesh Patil" }
        val cleanAddress = address.trim().ifBlank { "Nashik APMC Market, Maharashtra" }
        val cleanMobile = mobileNumber.filter { it.isDigit() }.ifBlank { "9876543210" }
        val cleanEmail = email.trim().ifBlank { "ramesh.farmer@gmail.com" }
        val maskedAadhaar = if (aadhaarInput.isNotBlank()) AadhaarUtils.maskAadhaar(aadhaarInput) else "XXXX XXXX 8899"

        // Attempt real Supabase Auth in background (non-blocking for mock auth)
        var realFarmerId = MOCK_FARMER_ID
        try {
            val authResult = supabaseService.signUp(
                email = cleanEmail,
                password = password.ifBlank { "rural123" },
                metadata = mapOf(
                    "name" to cleanName,
                    "mobile" to cleanMobile,
                    "address" to cleanAddress
                )
            )
            if (authResult.isSuccess) {
                realFarmerId = authResult.getOrThrow().user.id
            }
        } catch (_: Exception) {}

        val newFarmer = Farmer(
            id = realFarmerId,
            name = cleanName,
            address = cleanAddress,
            mobileNumber = cleanMobile,
            email = cleanEmail,
            aadhaarMasked = maskedAadhaar,
            mobileVerified = true
        )

        db.farmerDao().saveFarmer(newFarmer)
        supabaseService.saveFarmerProfile(newFarmer)

        // Add welcome notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_welcome_${System.currentTimeMillis()}",
                farmerId = newFarmer.id,
                title = "Welcome to RuralLink!",
                message = "Account ready with Mock Authentication & Supabase Database sync.",
                read = false
            )
        )
        return AuthResult.Success
    }

    suspend fun signInFarmer(
        email: String,
        password: String
    ): AuthResult {
        val cleanEmail = email.trim().ifBlank { "ramesh.farmer@gmail.com" }

        var farmerIdToUse = MOCK_FARMER_ID
        try {
            val authResult = supabaseService.signIn(
                email = cleanEmail,
                password = password.ifBlank { "rural123" }
            )
            if (authResult.isSuccess) {
                farmerIdToUse = authResult.getOrThrow().user.id
            }
        } catch (_: Exception) {}

        val existingFarmer = db.farmerDao().getFarmerOnce()
        val updatedFarmer = existingFarmer?.copy(id = farmerIdToUse, email = cleanEmail) ?: Farmer(
            id = farmerIdToUse,
            name = cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
            address = "Nashik APMC Market Yard, Maharashtra",
            mobileNumber = "9876543210",
            email = cleanEmail,
            aadhaarMasked = "XXXX XXXX 8899",
            mobileVerified = true
        )

        db.farmerDao().saveFarmer(updatedFarmer)
        supabaseService.saveFarmerProfile(updatedFarmer)

        return AuthResult.Success
    }

    suspend fun verifyMobileOtp(otp: String, credential: com.google.firebase.auth.PhoneAuthCredential? = null): AuthResult {
        val cleanOtp = otp.trim()
        if (cleanOtp.length < 6) {
            return AuthResult.Error("Please enter the 6-digit verification code.")
        }

        if (credential != null) {
            var authFailed = false
            var errorMsg = ""
            val latch = java.util.concurrent.CountDownLatch(1)
            firebaseService.signInWithCredential(credential) { success, exception ->
                if (!success) {
                    authFailed = true
                    errorMsg = exception?.localizedMessage ?: "Firebase verification failed."
                }
                latch.countDown()
            }
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            if (authFailed) {
                return AuthResult.Error(errorMsg)
            }
        } else if (cleanOtp != "123456") {
            return AuthResult.Error("Invalid verification code. Please try again.")
        }

        val farmer = db.farmerDao().getFarmer().firstOrNull() ?: return AuthResult.Error("No registration found. Please register first.")
        val updatedFarmer = farmer.copy(mobileVerified = true)
        db.farmerDao().setMobileVerified(farmer.id, true)
        supabaseService.saveFarmerProfile(updatedFarmer)
        firebaseService.saveFarmerProfile(updatedFarmer)

        // Add verification success notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_verified_${System.currentTimeMillis()}",
                farmerId = farmer.id,
                title = "Mobile Verified Successfully",
                message = "Your account is now verified. You can post transport requests anytime.",
                read = false
            )
        )
        return AuthResult.Success
    }

    suspend fun resendOtp(mobileNumber: String, activity: android.app.Activity? = null): AuthResult {
        val digits = mobileNumber.filter { it.isDigit() }
        if (!ValidationUtils.isValidMobile(digits)) {
            return AuthResult.Error("Resend failed: Invalid mobile number.")
        }
        if (activity != null) {
            val callbacks = object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {}
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {}
                override fun onCodeSent(verificationId: String, token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken) {}
            }
            firebaseService.sendPhoneOtp(digits, activity, callbacks)
        }
        return AuthResult.Success
    }

    suspend fun updateFarmerProfile(
        name: String,
        address: String,
        mobileNumber: String,
        email: String
    ): AuthResult {
        val current = db.farmerDao().getFarmer().firstOrNull()
            ?: return AuthResult.Error("Farmer account not found. Please register first.")

        val cleanName = name.trim()
        if (!ValidationUtils.isValidName(cleanName)) {
            return AuthResult.Error("Please enter your full name (at least 2 characters).")
        }

        val cleanAddress = address.trim()
        if (!ValidationUtils.isValidAddress(cleanAddress)) {
            return AuthResult.Error("Please enter your farm address.")
        }

        val cleanMobile = mobileNumber.filter { it.isDigit() }
        if (!ValidationUtils.isValidMobile(cleanMobile)) {
            return AuthResult.Error("Please enter a valid 10-digit mobile number.")
        }

        val cleanEmail = email.trim()
        if (cleanEmail.isNotEmpty() && !ValidationUtils.isValidEmail(cleanEmail)) {
            return AuthResult.Error("Please enter a valid email address.")
        }

        val updatedFarmer = current.copy(
            name = cleanName,
            address = cleanAddress,
            mobileNumber = cleanMobile,
            email = cleanEmail
        )

        db.farmerDao().saveFarmer(updatedFarmer)
        supabaseService.saveFarmerProfile(updatedFarmer)
        firebaseService.saveFarmerProfile(updatedFarmer)

        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_profile_updated_${System.currentTimeMillis()}",
                farmerId = updatedFarmer.id,
                title = "Profile & Address Updated",
                message = "Your profile details and farm address were successfully updated in Firestore & Supabase.",
                read = false
            )
        )
        return AuthResult.Success
    }

    suspend fun logout() {
        supabaseService.signOut()
        db.farmerDao().clear()
        try {
            firebaseService.signOut()
        } catch (_: Exception) {}
    }

    suspend fun clearDemoData() {
        supabaseService.signOut()
        db.farmerDao().clear()
        db.requestDao().clear()
        db.transporterResponseDao().clear()
        db.notificationDao().clear()
    }
}

class RequestService(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val firebaseService = FirebaseService()
    val supabaseService = SupabaseService(context)

    val allRequests: Flow<List<TransportRequest>> = db.requestDao().getAllRequests()

    fun getFarmerRequests(farmerId: String): Flow<List<TransportRequest>> {
        return if (farmerId.isBlank()) {
            db.requestDao().getAllRequests()
        } else {
            db.requestDao().getFarmerRequests(farmerId)
        }
    }

    suspend fun syncSupabaseRequests(farmerId: String) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val remoteRequests = supabaseService.getFarmerTransportRequests(farmerId)
                if (remoteRequests.isNotEmpty()) {
                    for (req in remoteRequests) {
                        db.requestDao().insertRequest(req)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun getRequestById(requestId: String, farmerId: String? = null): TransportRequest? {
        val req = db.requestDao().getRequestById(requestId)
        if (farmerId != null && req != null && req.farmerId != farmerId) {
            return null // Security enforcement: Cannot access another farmer's request
        }
        return req
    }

    suspend fun updateRequestStatus(requestId: String, status: String, farmerId: String? = null) {
        if (farmerId != null) {
            val req = getRequestById(requestId, farmerId)
            if (req == null) return // Unauthorized
        }
        db.requestDao().updateStatus(requestId, status)
        try { supabaseService.updateBookingStatus(requestId, status) } catch (e: Exception) { }
    }

    suspend fun updatePaymentStatus(requestId: String, paymentStatus: String, farmerId: String? = null) {
        val req = getRequestById(requestId, farmerId) ?: return
        val updatedReq = req.copy(paymentStatus = paymentStatus)
        db.requestDao().insertRequest(updatedReq)
        
        if (paymentStatus == "PAID") {
            updateRequestStatus(requestId, "PAID", farmerId)
            db.notificationDao().insertNotification(
                com.example.data.model.NotificationItem(
                    id = "notif_pay_${System.currentTimeMillis()}",
                    farmerId = farmerId ?: req.farmerId,
                    title = "Payment Successful",
                    message = "Your payment for booking $requestId is confirmed. The transporter will start the ride soon.",
                    relatedRequestId = requestId,
                    read = false
                )
            )
        }
    }

    suspend fun createRequest(
        farmerId: String,
        pickupPoint: String,
        dropPoint: String,
        weight: Double,
        weightUnit: String,
        materialType: String,
        requiredDate: String,
        requiredTime: String,
        notes: String = ""
    ): TransportRequest? {
        val currentUser = supabaseService.getCurrentUser()
        val authenticatedFarmerId = currentUser?.id?.ifBlank { null }
            ?: supabaseService.getCurrentUserId()?.ifBlank { null }
            ?: farmerId.ifBlank { null }
            ?: ""

        val reqNumber = (1000..9999).random()
        val refCode = "RL-REQ-$reqNumber"

        val preservedNotes = if (notes.isBlank()) {
            "Ref: $refCode"
        } else if (!notes.contains("RL-REQ")) {
            "Ref: $refCode | $notes"
        } else {
            notes
        }

        val request = TransportRequest(
            id = "", // Omit id so PostgreSQL generates UUID
            farmerId = authenticatedFarmerId,
            pickupPoint = pickupPoint,
            dropPoint = dropPoint,
            weight = weight,
            weightUnit = weightUnit,
            materialType = materialType,
            requiredDate = requiredDate,
            requiredTime = requiredTime,
            status = "SEARCHING",
            notes = preservedNotes
        )

        val localId = "req_${java.util.UUID.randomUUID().toString().take(8)}"
        val savedRequest = request.copy(
            id = localId,
            farmerId = if (authenticatedFarmerId.isNotBlank()) authenticatedFarmerId else AuthService.MOCK_FARMER_ID
        )

        // Store in Room DB immediately
        db.requestDao().insertRequest(savedRequest)

        // Create a status notification locally
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_req_${System.currentTimeMillis()}",
                farmerId = savedRequest.farmerId,
                title = "Transport Request Created",
                message = "Request ($refCode) for $weight $weightUnit of $materialType from $pickupPoint to $dropPoint was created successfully.",
                relatedRequestId = savedRequest.id,
                read = false
            )
        )

        // Save to Supabase (remote) in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                supabaseService.saveBooking(request)
            } catch (_: Exception) {}
        }

        // Schedule auto-generation of competitive driver quotes after 2.5 seconds
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(2500)
            try {
                TransporterResponseService(context).setDemoResponsesCount(savedRequest.id, 2)
            } catch (_: Exception) {}
        }

        return savedRequest
    }

    suspend fun registerDummyRequestInBackend(targetFarmerId: String = ""): Result<TransportRequest> {
        val authUserId = supabaseService.getCurrentUserId()?.ifBlank { null }
            ?: targetFarmerId.ifBlank { null }
            ?: ""

        val dummyReq = TransportRequest(
            id = "",
            farmerId = authUserId,
            pickupPoint = "Nashik APMC Mandi Yard, Gate 2",
            dropPoint = "Vashi Wholesale Market, Navi Mumbai",
            weight = 45.0,
            weightUnit = "Quintals",
            materialType = "Fresh Tomatoes & Onions",
            requiredDate = "28 Aug 2026",
            requiredTime = "08:00 AM",
            status = "SEARCHING",
            notes = "Dummy transport request registered directly."
        )

        val saveResult = supabaseService.saveBooking(dummyReq)
        val saved = if (saveResult.isSuccess) {
            saveResult.getOrThrow()
        } else {
            val localId = java.util.UUID.randomUUID().toString()
            dummyReq.copy(
                id = localId,
                farmerId = if (authUserId.isNotBlank()) authUserId else "farmer_local_1"
            )
        }
        db.requestDao().insertRequest(saved)
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_" + System.currentTimeMillis(),
                farmerId = saved.farmerId,
                title = "Transport Request Registered",
                message = "Transport request saved successfully.",
                relatedRequestId = saved.id,
                read = false,
                createdAt = System.currentTimeMillis()
            )
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                TransporterResponseService(context).setDemoResponsesCount(saved.id, 2)
            } catch (_: Exception) {}
        }
        return Result.success(saved)
    }

    suspend fun seedInitialDataIfEmpty(targetFarmerId: String = "") {
        val currentRequests = db.requestDao().getAllRequests().firstOrNull()
        if (currentRequests.isNullOrEmpty()) {
            val activeUserId = supabaseService.getCurrentUserId() ?: targetFarmerId
            val sampleRequests = listOf(
                TransportRequest(
                    id = "seed_req_${java.util.UUID.randomUUID().toString().take(8)}",
                    farmerId = activeUserId,
                    pickupPoint = "Nashik Mandi Yard, Gate 2",
                    dropPoint = "Vashi Wholesale Market, Navi Mumbai",
                    weight = 45.0,
                    weightUnit = "quintal",
                    materialType = "Fresh Tomatoes & Onions",
                    requiredDate = "28 Aug 2026",
                    requiredTime = "06:00 AM",
                    status = "SEARCHING",
                    notes = "Covered tarp needed. Perishable goods.",
                    createdAt = System.currentTimeMillis() - 3600000 * 5
                ),
                TransportRequest(
                    id = "seed_req_${java.util.UUID.randomUUID().toString().take(8)}",
                    farmerId = activeUserId,
                    pickupPoint = "Kopargaon Farm",
                    dropPoint = "Nashik APMC Mandi",
                    weight = 20.0,
                    weightUnit = "quintal",
                    materialType = "Onion",
                    requiredDate = "28 Aug 2026",
                    requiredTime = "09:00 AM",
                    status = "SEARCHING",
                    notes = "Heavy truck required (5+ ton capacity).",
                    createdAt = System.currentTimeMillis() - 3600000 * 24
                )
            )

            sampleRequests.forEach { req ->
                db.requestDao().insertRequest(req)
            }

            // Seed quotes for initial request so it's not stuck on searching
            try {
                TransporterResponseService(context).setDemoResponsesCount(sampleRequests.first().id, 2)
            } catch (_: Exception) {}

            // Add sample notification
            db.notificationDao().insertNotification(
                NotificationItem(
                    id = "notif_initial_1",
                    farmerId = activeUserId,
                    title = "Welcome to RuralLink",
                    message = "Transporters can view your active shipment requests. View details in My Requests.",
                    relatedRequestId = sampleRequests.first().id,
                    read = false,
                    createdAt = System.currentTimeMillis() - 3600000 * 2
                )
            )
        }
    }
}

class NotificationService(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)

    val notifications: Flow<List<NotificationItem>> = db.notificationDao().getAllNotifications()

    suspend fun markAsRead(id: String) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllAsRead() {
        db.notificationDao().markAllAsRead()
    }
}

// Voice Service Abstraction for Farmers
class VoiceService {
    data class VoiceParsedRequest(
        val materialType: String,
        val weight: Double,
        val weightUnit: String,
        val pickupPoint: String,
        val dropPoint: String,
        val requiredDate: String,
        val requiredTime: String,
        val rawSpeech: String
    )

    private val aiExtractionService = AiExtractionService()

    fun parseVoiceCommand(speechText: String): VoiceParsedRequest {
        val extracted = aiExtractionService.extractTransportDetails(speechText)
        return VoiceParsedRequest(
            materialType = extracted.materialType ?: "Onion",
            weight = extracted.weight ?: 20.0,
            weightUnit = extracted.weightUnit ?: "quintal",
            pickupPoint = extracted.pickupPoint ?: "Kopargaon",
            dropPoint = extracted.dropPoint ?: "Nashik",
            requiredDate = extracted.requiredDate ?: "12 August 2026",
            requiredTime = extracted.requiredTime ?: "09:00 AM",
            rawSpeech = speechText
        )
    }
}

// AI Assistant Service Abstraction for Farmer Decision Making
class AiService {
    fun getTransporterRecommendationSummary(): String {
        return "AI Analysis: Transporter 'Kisan Express' offers optimal timing, verified vehicle inspection, and competitive rate for agricultural perishable goods."
    }
}
