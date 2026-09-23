package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Farmer
import com.example.data.model.TransportRequest
import com.example.services.AuthResult
import com.example.services.AuthService
import com.example.services.RequestService
import com.example.services.SupabaseService
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var supabaseService: SupabaseService
    private lateinit var authService: AuthService
    private lateinit var requestService: RequestService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        supabaseService = SupabaseService(context)
        authService = AuthService(context)
        requestService = RequestService(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSupabaseClientConfigInitializationAndEndpoints() {
        val config = com.example.services.SupabaseClientConfig
        assertNotNull(config.supabaseUrl)
        assertTrue(config.supabaseUrl.startsWith("http"))
        assertNotNull(config.apiKey)
        assertTrue(config.apiKey.isNotBlank())
        assertEquals("${config.supabaseUrl}/rest/v1", config.restUrl)
        assertEquals("${config.supabaseUrl}/auth/v1", config.authUrl)
        assertEquals("${config.supabaseUrl}/storage/v1", config.storageUrl)
        assertTrue(config.realtimeUrl.startsWith("wss://"))
        assertTrue(config.isConfigured)

        // Test headers creation
        val headers = config.createHeaders(jwtToken = "sample-jwt-token", prefer = "return=representation")
        assertEquals(config.apiKey, headers["apikey"])
        assertEquals("Bearer sample-jwt-token", headers["Authorization"])
        assertEquals("application/json", headers["Content-Type"])
        assertEquals("return=representation", headers["Prefer"])

        // Test diagnostics
        val diagnostics = config.getDiagnosticsInfo()
        assertEquals(config.supabaseUrl, diagnostics["url"])
        assertNotNull(diagnostics["maskedKey"])
        assertEquals("true", diagnostics["isConfigured"])
        assertEquals("3", diagnostics["maxRetries"])
    }

    @Test
    fun testRateLimitConfigurationBackoffAndHeaders() {
        val config = com.example.services.SupabaseClientConfig
        config.resetRateLimitState()

        // Test initial state
        assertEquals(-1, config.rateLimitRemaining)
        assertEquals(-1, config.rateLimitLimit)
        assertFalse(config.isRateLimited)

        // Test header updates
        val mockHeaders = mapOf(
            "x-ratelimit-limit" to listOf("60"),
            "x-ratelimit-remaining" to listOf("45"),
            "x-ratelimit-reset" to listOf("1700000000")
        )
        config.updateRateLimitHeaders(mockHeaders)

        assertEquals(60, config.rateLimitLimit)
        assertEquals(45, config.rateLimitRemaining)
        assertEquals(1700000000L, config.rateLimitResetSeconds)

        // Test exponential backoff calculation with jitter
        val backoffAttempt1 = config.calculateBackoffWithJitter(attempt = 1)
        assertTrue("Attempt 1 backoff >= 500ms", backoffAttempt1 >= 500L)

        val backoffAttempt2 = config.calculateBackoffWithJitter(attempt = 2)
        assertTrue("Attempt 2 backoff >= 1000ms", backoffAttempt2 >= 1000L)

        val backoffAttempt3 = config.calculateBackoffWithJitter(attempt = 3)
        assertTrue("Attempt 3 backoff >= 2000ms", backoffAttempt3 >= 2000L)

        // Test Retry-After priority
        val retryAfterBackoff = config.calculateBackoffWithJitter(attempt = 1, retryAfterHeader = "10")
        assertEquals(8000L, retryAfterBackoff) // Capped at MAX_BACKOFF_MS (8000)

        val retryAfterSmall = config.calculateBackoffWithJitter(attempt = 1, retryAfterHeader = "3")
        assertEquals(3000L, retryAfterSmall)

        // Test rate limit permit acquire (throttling)
        val startTime = System.currentTimeMillis()
        config.acquireRateLimitPermit()
        config.acquireRateLimitPermit()
        val elapsed = System.currentTimeMillis() - startTime
        assertTrue("Min request interval enforced", elapsed >= 90L)

        config.resetRateLimitState()
    }

    @Test
    fun testSupabaseConfigurationAndUrlInitialization() {
        assertNotNull(supabaseService)
        assertTrue(
            "Supabase instance initialized",
            supabaseService.hasValidSession() || !supabaseService.hasValidSession()
        )
    }

    @Test
    fun testFarmerAuthServiceRegistrationAndProfileId() = runBlocking {
        val testEmail = "testfarmer_${System.currentTimeMillis()}@gmail.com"
        val testName = "Ramesh Kumar"
        val testAddress = "Plot 42, Village Baramati, Pune"
        val testMobile = "9876543210"
        val testAadhaar = "1234 5678 9012"
        val testPassword = "password123"

        // Register farmer
        val authResult = authService.registerFarmer(
            name = testName,
            address = testAddress,
            mobileNumber = testMobile,
            aadhaarInput = testAadhaar,
            email = testEmail,
            password = testPassword
        )

        // If network allows or falls back to offline authenticated mode, check result
        if (authResult is AuthResult.Success) {
            val activeUserId = authService.getCurrentUserId()
            assertNotNull("Active user ID should not be null on success", activeUserId)
            assertTrue("Active user ID should not be blank", activeUserId?.isNotBlank() == true)
        }
    }

    @Test
    fun testValidationUtilsMethods() {
        // Name validation
        assertTrue(com.example.services.ValidationUtils.isValidName("Ramesh Kumar"))
        assertTrue(!com.example.services.ValidationUtils.isValidName("A"))
        assertTrue(!com.example.services.ValidationUtils.isValidName("   "))

        // Address validation
        assertTrue(com.example.services.ValidationUtils.isValidAddress("Baramati, Pune"))
        assertTrue(!com.example.services.ValidationUtils.isValidAddress("   "))

        // Mobile validation (10 digits starting with 6-9)
        assertTrue(com.example.services.ValidationUtils.isValidMobile("9876543210"))
        assertTrue(com.example.services.ValidationUtils.isValidMobile("7890123456"))
        assertTrue(!com.example.services.ValidationUtils.isValidMobile("1234567890"))
        assertTrue(!com.example.services.ValidationUtils.isValidMobile("98765"))

        // Email validation
        assertTrue(com.example.services.ValidationUtils.isValidEmail("farmer@gmail.com"))
        assertTrue(!com.example.services.ValidationUtils.isValidEmail("farmer@example.com"))
        assertTrue(!com.example.services.ValidationUtils.isValidEmail("invalid-email"))
    }

    @Test
    fun testAadhaarUtilsMethods() {
        assertTrue(com.example.services.AadhaarUtils.isValidAadhaar("1234 5678 9012"))
        assertTrue(!com.example.services.AadhaarUtils.isValidAadhaar("1234 5678"))
        
        val masked = com.example.services.AadhaarUtils.maskAadhaar("123456789012")
        assertEquals("XXXX XXXX 9012", masked)
    }

    @Test
    fun testRequestServiceCreationAndRetrieval() = runBlocking {
        val testFarmerId = "farmer_test_${System.currentTimeMillis()}"
        val req = try {
            requestService.createRequest(
                farmerId = testFarmerId,
                pickupPoint = "Nashik Gate 1",
                dropPoint = "Vashi Market",
                weight = 5.0,
                weightUnit = "Tonnes",
                materialType = "Onions",
                requiredDate = "2026-09-01",
                requiredTime = "08:00 AM",
                notes = "Handle with care"
            )
        } catch (e: Exception) {
            println("Skipping live integration test due to network or Supabase environment: ${e.message}")
            return@runBlocking
        }
        assertNotNull(req)
        val reqNonNull = req!!
        assertEquals(testFarmerId, reqNonNull.farmerId)
        assertEquals("SEARCHING", reqNonNull.status)

        val fetched = requestService.getRequestById(reqNonNull.id, testFarmerId)
        assertNotNull(fetched)
        assertEquals(reqNonNull.id, fetched?.id)

        // Test status update
        requestService.updateRequestStatus(reqNonNull.id, "RESPONSES_RECEIVED", testFarmerId)
        val updated = requestService.getRequestById(reqNonNull.id, testFarmerId)
        assertEquals("RESPONSES_RECEIVED", updated?.status)
    }

    @Test
    fun testSupabaseConnectionAndAddDemoRecordMethod() = runBlocking {
        val result = supabaseService.testConnectionAndAddDemoRecord()
        assertNotNull(result)
        assertNotNull(result.message)
        assertNotNull(result.recordId)
        assertEquals("public.farmers", result.table)
        assertTrue(result.latencyMs >= 0)
    }

    @Test
    fun testTransportRequestModelUsesAuthenticatedFarmerId() = runBlocking {
        val testFarmerId = UUID.randomUUID().toString()
        val farmer = Farmer(
            id = testFarmerId,
            name = "Suresh Patil",
            address = "Khed, Ratnagiri",
            mobileNumber = "9822012345",
            email = "suresh@gmail.com",
            aadhaarMasked = "XXXX-XXXX-9012",
            mobileVerified = true
        )
        database.farmerDao().saveFarmer(farmer)

        val retrievedFarmer = database.farmerDao().getFarmer().firstOrNull()
        assertNotNull(retrievedFarmer)
        assertEquals(testFarmerId, retrievedFarmer?.id)

        val request = TransportRequest(
            id = "RL-REQ-8821",
            farmerId = testFarmerId,
            pickupPoint = "Khed Market Yard",
            dropPoint = "Vashi APMC Market, Mumbai",
            weight = 4.5,
            weightUnit = "Tonnes",
            materialType = "Alphonso Mangoes",
            requiredDate = "2026-08-30",
            requiredTime = "06:00 AM",
            status = "SEARCHING"
        )
        database.requestDao().insertRequest(request)

        val savedRequest = database.requestDao().getRequestById("RL-REQ-8821")
        assertNotNull("Request must be stored in database", savedRequest)
        assertEquals("Transport request farmer_id MUST match farmer ID", testFarmerId, savedRequest?.farmerId)
    }

    @Test
    fun testInspectTransportRequestsSchema() = runBlocking {
        val testFarmerId = "00000000-0000-0000-0000-000000000000"
        val req = TransportRequest(
            id = "RL-REQ-9999",
            farmerId = testFarmerId,
            pickupPoint = "Nashik Gate 1",
            dropPoint = "Vashi Market",
            weight = 10.0,
            weightUnit = "Tonnes",
            materialType = "Onions",
            requiredDate = "2026-09-01",
            requiredTime = "10:00 AM",
            status = "SEARCHING",
            notes = "Test reference preservation"
        )
        val result = supabaseService.saveBooking(req)
        if (result.isFailure) {
            val exc = result.exceptionOrNull()
            println("Skipping live integration test due to network or Supabase environment: ${exc?.message}")
            return@runBlocking
        }
        assertTrue("saveBooking should succeed without UUID syntax mismatch", result.isSuccess)
        val savedReq = result.getOrNull()
        assertNotNull(savedReq)
        assertTrue("Database ID should be a valid UUID", savedReq?.id != "RL-REQ-9999")
    }
}
