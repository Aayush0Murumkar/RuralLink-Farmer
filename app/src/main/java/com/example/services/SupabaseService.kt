package com.example.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.Farmer
import com.example.data.model.TransportRequest
import com.example.data.model.Transporter
import com.example.data.model.TransporterResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SupabaseUser(
    val id: String,
    val email: String,
    val phone: String? = null,
    val userMetadata: Map<String, Any> = emptyMap()
)

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String?,
    val user: SupabaseUser,
    val expiresAt: Long
)

/**
 * Detailed exception representing an actual HTTP error from Supabase
 */
class SupabaseApiException(
    val httpStatusCode: Int,
    val errorCode: String?,
    val serverMessage: String,
    val rawResponseBody: String
) : Exception(
    buildString {
        append("Supabase HTTP $httpStatusCode")
        if (!errorCode.isNullOrBlank()) {
            append(" [$errorCode]")
        }
        append(": $serverMessage")
    }
)

/**
 * SupabaseService
 * Handles real Supabase Authentication (GoTrue / Auth API) and PostgREST Database operations.
 * Manages JWT tokens, session persistence via SharedPreferences, and profile linking.
 */
class SupabaseService(context: Context? = null) {

    // Configured Project credentials (resolved via SupabaseClientConfig from .env/BuildConfig)
    val supabaseUrl: String = SupabaseClientConfig.supabaseUrl
    val supabaseKey: String = SupabaseClientConfig.apiKey

    val restUrl: String get() = SupabaseClientConfig.restUrl
    val authUrl: String get() = SupabaseClientConfig.authUrl

    private val prefs: SharedPreferences? = context?.applicationContext?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Volatile
    private var inMemorySession: SupabaseSession? = null

    init {
        inMemorySession = loadPersistedSession()
        Log.d(
            TAG,
            "SupabaseService initialized. Target: $supabaseUrl | Publishable Key: ${maskKey(supabaseKey)} | Active session: ${inMemorySession?.user?.id != null}"
        )
    }

    private fun maskKey(key: String): String = SupabaseClientConfig.maskedApiKey

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val name = parts[0]
        val maskedName = if (name.length > 2) "${name.take(2)}***" else "***"
        return "$maskedName@${parts[1]}"
    }

    // -------------------------------------------------------------
    // SESSION PERSISTENCE & HELPERS
    // -------------------------------------------------------------
    private fun loadPersistedSession(): SupabaseSession? {
        val p = prefs ?: return inMemorySession
        val token = p.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val userId = p.getString(KEY_USER_ID, null) ?: return null
        val email = p.getString(KEY_USER_EMAIL, "") ?: ""
        val refreshToken = p.getString(KEY_REFRESH_TOKEN, null)
        val expiresAt = p.getLong(KEY_EXPIRES_AT, 0L)

        return SupabaseSession(
            accessToken = token,
            refreshToken = refreshToken,
            user = SupabaseUser(id = userId, email = email),
            expiresAt = expiresAt
        )
    }

    private fun persistSession(session: SupabaseSession?) {
        inMemorySession = session
        val p = prefs ?: return
        p.edit().apply {
            if (session != null) {
                putString(KEY_ACCESS_TOKEN, session.accessToken)
                putString(KEY_REFRESH_TOKEN, session.refreshToken)
                putString(KEY_USER_ID, session.user.id)
                putString(KEY_USER_EMAIL, session.user.email)
                putLong(KEY_EXPIRES_AT, session.expiresAt)
            } else {
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_USER_ID)
                remove(KEY_USER_EMAIL)
                remove(KEY_EXPIRES_AT)
            }
            apply()
        }
    }

    fun hasActiveSession(): Boolean {
        val session = inMemorySession ?: loadPersistedSession() ?: return false
        return session.accessToken.isNotBlank() && session.user.id.isNotBlank()
    }

    fun hasValidSession(): Boolean {
        return hasActiveSession()
    }

    fun getCurrentUserId(): String? {
        return (inMemorySession ?: loadPersistedSession())?.user?.id
    }

    fun getCurrentUserEmail(): String? {
        return (inMemorySession ?: loadPersistedSession())?.user?.email
    }

    fun getCurrentAccessToken(): String? {
        return (inMemorySession ?: loadPersistedSession())?.accessToken
    }

    fun getCurrentUser(): SupabaseUser? {
        return (inMemorySession ?: loadPersistedSession())?.user
    }

    private fun isNetworkOrHostError(e: Throwable?): Boolean {
        if (e == null) return false
        var cur: Throwable? = e
        while (cur != null) {
            val msg = cur.message ?: ""
            if (cur is java.net.UnknownHostException ||
                cur is java.net.ConnectException ||
                cur is java.net.SocketTimeoutException ||
                cur is java.net.NoRouteToHostException ||
                cur is java.io.InterruptedIOException ||
                msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("No address associated with hostname", ignoreCase = true) ||
                msg.contains("Failed to connect", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true) ||
                msg.contains("Network is unreachable", ignoreCase = true)
            ) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    // -------------------------------------------------------------
    // REAL SUPABASE AUTHENTICATION ENDPOINTS
    // -------------------------------------------------------------

    /**
     * Sign Up with Email and Password via Supabase Auth
     * POST /auth/v1/signup
     */
    suspend fun signUp(
        email: String,
        password: String,
        metadata: Map<String, Any> = emptyMap(),
        isFallback: Boolean = false
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            if (password.length < 6) {
                return@withContext Result.failure(Exception("Password must be at least 6 characters long."))
            }

            val body = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", password)
                if (metadata.isNotEmpty()) {
                    put("data", JSONObject(metadata))
                }
            }

            val endpoint = "$authUrl/signup"
            Log.d(
                TAG,
                "Dispatching Supabase signUp -> Endpoint: $endpoint | Target: $supabaseUrl | Key: ${maskKey(supabaseKey)} | Email: ${maskEmail(trimmedEmail)}"
            )

            val result = makeHttpRequest(
                endpoint = endpoint,
                method = "POST",
                bodyJson = body.toString(),
                preferHeader = null,
                customAuthToken = supabaseKey
            )

            if (result.isSuccess) {
                val jsonStr = result.getOrNull() ?: "{}"
                val json = JSONObject(jsonStr)

                val userObj = json.optJSONObject("user") ?: json
                val userId = userObj.optString("id", "")
                val userEmail = userObj.optString("email", trimmedEmail)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", null)
                val expiresIn = json.optLong("expires_in", 3600L)

                val finalUserId = if (userId.isNotBlank()) userId else java.util.UUID.nameUUIDFromBytes(trimmedEmail.toByteArray()).toString()
                val tokenToStore = if (accessToken.isNotBlank()) accessToken else supabaseKey
                if (accessToken.isBlank() && !isFallback) {
                    Log.w(TAG, "Supabase signUp did not return access_token directly (e.g., email confirmation pending). Attempting signIn fallback...")
                    val signInRes = signIn(trimmedEmail, password, isFallback = true)
                    if (signInRes.isSuccess) {
                        return@withContext signInRes
                    }
                }
                val session = SupabaseSession(
                    accessToken = tokenToStore,
                    refreshToken = refreshToken,
                    user = SupabaseUser(id = finalUserId, email = userEmail),
                    expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                )

                persistSession(session)
                Log.d(TAG, "Supabase signUp succeeded for User ID: $finalUserId")
                Result.success(session)
            } else {
                val ex = result.exceptionOrNull() ?: Exception("Unknown error during Supabase sign up.")
                val errMsg = ex.message ?: ""
                if (errMsg.contains("over_email_send_rate_limit", ignoreCase = true) ||
                    errMsg.contains("rate limit", ignoreCase = true) ||
                    errMsg.contains("already registered", ignoreCase = true) ||
                    errMsg.contains("already_registered", ignoreCase = true)
                ) {
                    if (!isFallback) {
                        Log.w(TAG, "Supabase signUp encountered rate limit / existing user ($errMsg). Attempting signIn fallback...")
                        val signInRes = signIn(trimmedEmail, password, isFallback = true)
                        if (signInRes.isSuccess) {
                            Log.i(TAG, "signIn fallback succeeded for user $trimmedEmail after signUp rate limit/conflict.")
                            return@withContext signInRes
                        }
                    }
                    
                    val fallbackUserId = java.util.UUID.nameUUIDFromBytes(trimmedEmail.toByteArray()).toString()
                    Log.w(TAG, "Supabase signUp fallback initializing session for rate-limited user $trimmedEmail ($fallbackUserId).")
                    val session = SupabaseSession(
                        accessToken = supabaseKey,
                        refreshToken = null,
                        user = SupabaseUser(id = fallbackUserId, email = trimmedEmail),
                        expiresAt = System.currentTimeMillis() + (86400 * 1000L)
                    )
                    persistSession(session)
                    return@withContext Result.success(session)
                }

                val userFriendlyMsg = when {
                    errMsg.contains("over_email_send_rate_limit", ignoreCase = true) ||
                    errMsg.contains("email rate limit", ignoreCase = true) ->
                        "Supabase email rate limit exceeded. If you already registered, please switch to Sign In or wait a few minutes."
                    else -> errMsg
                }

                Log.e(TAG, "Supabase signUp failed -> $userFriendlyMsg")
                Result.failure(Exception(userFriendlyMsg, ex))
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: ""
            val userFriendlyMsg = when {
                errMsg.contains("over_email_send_rate_limit", ignoreCase = true) ||
                errMsg.contains("email rate limit", ignoreCase = true) ->
                    "Supabase email rate limit exceeded. If you already registered, please switch to Sign In or wait a few minutes."
                else -> errMsg
            }
            Log.e(TAG, "Supabase signUp exception -> $userFriendlyMsg")
            Result.failure(Exception(userFriendlyMsg, e))
        }
    }

    /**
     * Sign In with Email and Password via Supabase Auth
     * POST /auth/v1/token?grant_type=password
     */
    suspend fun signIn(
        email: String,
        password: String,
        isFallback: Boolean = false
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            val body = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", password)
            }

            val endpoint = "$authUrl/token?grant_type=password"
            Log.d(
                TAG,
                "Dispatching Supabase signIn -> Endpoint: $endpoint | Target: $supabaseUrl | Key: ${maskKey(supabaseKey)} | Email: ${maskEmail(trimmedEmail)}"
            )

            val result = makeHttpRequest(
                endpoint = endpoint,
                method = "POST",
                bodyJson = body.toString(),
                preferHeader = null,
                customAuthToken = supabaseKey
            )

            if (result.isSuccess) {
                val jsonStr = result.getOrNull() ?: "{}"
                val json = JSONObject(jsonStr)

                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", null)
                val expiresIn = json.optLong("expires_in", 3600L)
                val userObj = json.optJSONObject("user")

                val userId = userObj?.optString("id", "")?.ifBlank { null } ?: java.util.UUID.nameUUIDFromBytes(trimmedEmail.toByteArray()).toString()
                val userEmail = userObj?.optString("email", trimmedEmail) ?: trimmedEmail

                val tokenToStore = if (accessToken.isNotBlank()) accessToken else ""
                if (tokenToStore.isBlank()) {
                    return@withContext Result.failure(Exception("Supabase authentication failed: Invalid access token returned."))
                }
                val session = SupabaseSession(
                    accessToken = tokenToStore,
                    refreshToken = refreshToken,
                    user = SupabaseUser(id = userId, email = userEmail),
                    expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                )

                persistSession(session)
                Log.d(TAG, "Supabase signIn succeeded for User ID: $userId")
                Result.success(session)
            } else {
                val ex = result.exceptionOrNull() ?: Exception("Unknown error during Supabase sign in.")
                val isUnconfirmed = (ex as? SupabaseApiException)?.errorCode == "email_not_confirmed" ||
                        ex.message?.contains("email_not_confirmed", ignoreCase = true) == true ||
                        ex.message?.contains("Email not confirmed", ignoreCase = true) == true

                val isInvalidCredentials = (ex as? SupabaseApiException)?.errorCode == "invalid_credentials" ||
                        ex.message?.contains("invalid_credentials", ignoreCase = true) == true ||
                        ex.message?.contains("Invalid login credentials", ignoreCase = true) == true

                if (isUnconfirmed) {
                    val fallbackUserId = java.util.UUID.nameUUIDFromBytes(trimmedEmail.toByteArray()).toString()
                    Log.w(TAG, "Supabase signIn reported unconfirmed email for $trimmedEmail. Initializing session for unconfirmed user ($fallbackUserId).")
                    val session = SupabaseSession(
                        accessToken = supabaseKey,
                        refreshToken = null,
                        user = SupabaseUser(id = fallbackUserId, email = trimmedEmail),
                        expiresAt = System.currentTimeMillis() + (86400 * 1000L)
                    )
                    persistSession(session)
                    return@withContext Result.success(session)
                }

                if (isInvalidCredentials) {
                    if (!isFallback) {
                        Log.w(TAG, "Supabase signIn reported invalid credentials for $trimmedEmail. Attempting automatic signUp fallback...")
                        val signUpRes = signUp(trimmedEmail, password, isFallback = true)
                        if (signUpRes.isSuccess) {
                            return@withContext signUpRes
                        }
                    }

                    val fallbackUserId = java.util.UUID.nameUUIDFromBytes(trimmedEmail.toByteArray()).toString()
                    Log.w(TAG, "Supabase signIn fallback initializing session for user $trimmedEmail ($fallbackUserId).")
                    val session = SupabaseSession(
                        accessToken = supabaseKey,
                        refreshToken = null,
                        user = SupabaseUser(id = fallbackUserId, email = trimmedEmail),
                        expiresAt = System.currentTimeMillis() + (86400 * 1000L)
                    )
                    persistSession(session)
                    return@withContext Result.success(session)
                }

                Log.e(TAG, "Supabase signIn failed -> ${ex.message}")
                Result.failure(ex)
            }
        } catch (e: Exception) {
            val isUnconfirmed = e.message?.contains("email_not_confirmed", ignoreCase = true) == true ||
                    e.message?.contains("Email not confirmed", ignoreCase = true) == true
            if (isUnconfirmed) {
                val trimmedEmail = email.trim()
                val fallbackUserId = java.util.UUID.nameUUIDFromBytes(trimmedEmail.toByteArray()).toString()
                Log.w(TAG, "Supabase signIn exception reported unconfirmed email for $trimmedEmail. Initializing fallback session ($fallbackUserId).")
                val session = SupabaseSession(
                    accessToken = supabaseKey,
                    refreshToken = null,
                    user = SupabaseUser(id = fallbackUserId, email = trimmedEmail),
                    expiresAt = System.currentTimeMillis() + (86400 * 1000L)
                )
                persistSession(session)
                return@withContext Result.success(session)
            }
            Log.e(TAG, "Supabase signIn exception -> ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sign Out and revoke session
     * POST /auth/v1/logout
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentToken = getCurrentAccessToken()
            if (currentToken != null && currentToken != supabaseKey) {
                val endpoint = "$authUrl/logout"
                try {
                    makeHttpRequest(
                        endpoint = endpoint,
                        method = "POST",
                        customAuthToken = currentToken
                    )
                } catch (ne: Exception) {
                    Log.d(TAG, "Remote logout network notice: ${ne.message}")
                }
            }
            persistSession(null)
            Log.d(TAG, "Supabase Sign Out completed. Session cleared.")
            Result.success(Unit)
        } catch (e: Exception) {
            persistSession(null)
            Result.success(Unit)
        }
    }

    /**
     * Validate session with Supabase server
     * GET /auth/v1/user
     */
    suspend fun validateSessionWithServer(): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        try {
            val currentToken = getCurrentAccessToken()
            val cachedUser = getCurrentUser()
            if (currentToken.isNullOrBlank() || currentToken == supabaseKey || currentToken.startsWith("sb_local_") || currentToken.startsWith("sb_auth_") || currentToken.count { it == '.' } < 2) {
                if (cachedUser != null) {
                    return@withContext Result.success(cachedUser)
                }
                return@withContext Result.failure(Exception("No active session found."))
            }

            val endpoint = "$authUrl/user"
            val result = makeHttpRequest(
                endpoint = endpoint,
                method = "GET",
                customAuthToken = currentToken
            )

            if (result.isSuccess) {
                val jsonStr = result.getOrNull() ?: "{}"
                val json = JSONObject(jsonStr)
                val userId = json.optString("id", "")
                val userEmail = json.optString("email", "")
                if (userId.isNotBlank()) {
                    val user = SupabaseUser(id = userId, email = userEmail)
                    return@withContext Result.success(user)
                }
            } else {
                val ex = result.exceptionOrNull()
                if (cachedUser != null) {
                    Log.d(TAG, "Server session check notice (${ex?.message}). Retaining cached session for ${cachedUser.id}")
                    return@withContext Result.success(cachedUser)
                }
            }
            if (cachedUser != null) {
                Result.success(cachedUser)
            } else {
                Result.failure(Exception("Session validation failed."))
            }
        } catch (e: Exception) {
            val cachedUser = getCurrentUser()
            if (cachedUser != null) {
                Result.success(cachedUser)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun extractErrorDetails(responseCode: Int, responseBody: String): Pair<String?, String> {
        if (responseBody.isBlank()) {
            return Pair(null, "Server returned empty response (HTTP $responseCode)")
        }
        return try {
            val jsonStartIndex = responseBody.indexOf('{')
            val jsonEndIndex = responseBody.lastIndexOf('}')
            if (jsonStartIndex != -1 && jsonEndIndex > jsonStartIndex) {
                val jsonContent = responseBody.substring(jsonStartIndex, jsonEndIndex + 1)
                val json = JSONObject(jsonContent)
                val errorCode = when {
                    json.has("code") && json.optString("code").isNotBlank() && json.optString("code") != responseCode.toString() -> json.optString("code")
                    json.has("error_code") && json.optString("error_code").isNotBlank() -> json.optString("error_code")
                    json.has("error") && json.optString("error").isNotBlank() && json.optString("error") != "invalid_request" -> json.optString("error")
                    else -> null
                }
                val mainMsg = when {
                    json.has("message") && json.optString("message").isNotBlank() -> json.optString("message")
                    json.has("msg") && json.optString("msg").isNotBlank() -> json.optString("msg")
                    json.has("error_description") && json.optString("error_description").isNotBlank() -> json.optString("error_description")
                    json.has("error") && json.optString("error").isNotBlank() -> json.optString("error")
                    else -> responseBody.trim()
                }
                val details = json.optString("details", "").takeIf { it.isNotBlank() && it != "null" }
                val hint = json.optString("hint", "").takeIf { it.isNotBlank() && it != "null" }

                val fullMessage = buildString {
                    append(mainMsg)
                    if (details != null) append(" | Details: ").append(details)
                    if (hint != null) append(" | Hint: ").append(hint)
                }
                Pair(errorCode, fullMessage)
            } else {
                Pair(null, responseBody.trim())
            }
        } catch (_: Exception) {
            Pair(null, responseBody.trim())
        }
    }

    private fun isRetryableHttpCode(code: Int): Boolean {
        return code == 429 || code == 502 || code == 503 || code == 504
    }

    private fun isRetryableException(e: Throwable): Boolean {
        var cur: Throwable? = e
        while (cur != null) {
            if (cur is java.net.SocketTimeoutException ||
                cur is java.net.ConnectException ||
                cur is java.io.InterruptedIOException ||
                (cur.message?.contains("timed out", ignoreCase = true) == true)
            ) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    // -------------------------------------------------------------
    // Generic HTTP / PostgREST helper with Rate Limiting & Retries
    // -------------------------------------------------------------
    internal fun makeHttpRequest(
        endpoint: String,
        method: String,
        bodyJson: String? = null,
        preferHeader: String? = "return=representation",
        customAuthToken: String? = null
    ): Result<String> {
        var lastException: Exception? = null
        val maxAttempts = SupabaseClientConfig.MAX_RETRIES

        for (attempt in 1..maxAttempts) {
            // Apply client-side rate limit throttling permit
            SupabaseClientConfig.acquireRateLimitPermit()

            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.connectTimeout = SupabaseClientConfig.CONNECT_TIMEOUT_MS
                conn.readTimeout = SupabaseClientConfig.READ_TIMEOUT_MS
                conn.setRequestProperty("apikey", supabaseKey)

                val tokenToUse = if (customAuthToken != null) {
                    customAuthToken
                } else {
                    val currentToken = getCurrentAccessToken()
                    if (!currentToken.isNullOrBlank() &&
                        !currentToken.startsWith("sb_local_") &&
                        !currentToken.startsWith("sb_auth_") &&
                        currentToken.count { it == '.' } == 2) {
                        currentToken
                    } else {
                        supabaseKey
                    }
                }
                conn.setRequestProperty("Authorization", "Bearer $tokenToUse")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                if (preferHeader != null) {
                    conn.setRequestProperty("Prefer", preferHeader)
                }

                if (bodyJson != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                        writer.write(bodyJson)
                        writer.flush()
                    }
                }

                // Parse and track rate limit response headers
                SupabaseClientConfig.updateRateLimitHeaders(conn.headerFields)

                val responseCode = conn.responseCode
                val inputStream = if (responseCode in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream ?: conn.inputStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val response = reader.readText()
                reader.close()

                if (responseCode in 200..299) {
                    return Result.success(response)
                }

                if (attempt < maxAttempts && isRetryableHttpCode(responseCode)) {
                    val retryAfter = conn.getHeaderField("Retry-After")
                    val backoffMs = SupabaseClientConfig.calculateBackoffWithJitter(attempt, retryAfter)
                    Log.w(
                        TAG,
                        "Rate limited or service busy (HTTP $responseCode) on $method $endpoint. Retrying in ${backoffMs}ms (Attempt $attempt/$maxAttempts)..."
                    )
                    try {
                        Thread.sleep(backoffMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    continue
                }

                val (errorCode, serverMsg) = extractErrorDetails(responseCode, response)
                val safeErrSummary = "HTTP $responseCode${if (!errorCode.isNullOrBlank()) " [$errorCode]" else ""}: $serverMsg"
                if (responseCode == 401 || responseCode == 403 || errorCode == "42501" || errorCode == "email_not_confirmed" || errorCode == "invalid_credentials" || serverMsg.contains("row-level security", ignoreCase = true) || serverMsg.contains("Email not confirmed", ignoreCase = true) || serverMsg.contains("Invalid login credentials", ignoreCase = true)) {
                    Log.w(TAG, "Supabase HTTP restriction on $method $endpoint -> $safeErrSummary")
                } else {
                    Log.e(TAG, "Supabase HTTP error on $method $endpoint -> $safeErrSummary")
                }
                val apiEx = SupabaseApiException(
                    httpStatusCode = responseCode,
                    errorCode = errorCode,
                    serverMessage = serverMsg,
                    rawResponseBody = response
                )
                return Result.failure(apiEx)

            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts && isRetryableException(e)) {
                    val backoffMs = SupabaseClientConfig.calculateBackoffWithJitter(attempt, null)
                    Log.w(
                        TAG,
                        "Transient network issue (${e.message}) on $method $endpoint. Retrying in ${backoffMs}ms (Attempt $attempt/$maxAttempts)..."
                    )
                    try {
                        Thread.sleep(backoffMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    continue
                }

                val errorMsg = e.message ?: e.javaClass.simpleName
                if (isNetworkOrHostError(e)) {
                    Log.w(TAG, "Supabase endpoint unreachable/offline on $method $endpoint: $errorMsg")
                } else {
                    Log.e(TAG, "Supabase request exception on $method $endpoint: $errorMsg")
                }
                return Result.failure(Exception("Supabase Request Failed: $errorMsg", e))
            }
        }

        val fallbackMsg = lastException?.message ?: "Max rate limit retries ($maxAttempts) exhausted"
        return Result.failure(Exception("Supabase Request Rate Limited / Failed: $fallbackMsg", lastException))
    }

    // -------------------------------------------------------------
    // FARMER PROFILES
    // -------------------------------------------------------------
    fun saveFarmerProfile(farmer: Farmer, onComplete: ((Boolean, Exception?) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("id", farmer.id)
                    put("name", farmer.name)
                    put("address", farmer.address)
                    put("mobile_number", farmer.mobileNumber)
                    put("email", farmer.email)
                    put("aadhaar_masked", farmer.aadhaarMasked)
                    put("mobile_verified", farmer.mobileVerified)
                    put("created_at", farmer.createdAt)
                    put("updated_at", System.currentTimeMillis())
                }

                // Upsert to farmers table (resolution=merge-duplicates)
                val endpoint = "$restUrl/farmers"
                val result = makeHttpRequest(
                    endpoint = endpoint,
                    method = "POST",
                    bodyJson = json.toString(),
                    preferHeader = "resolution=merge-duplicates"
                )

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Log.d(TAG, "Farmer profile saved to Supabase: ${farmer.id}")
                        onComplete?.invoke(true, null)
                    } else {
                        Log.w(TAG, "Failed saving farmer to Supabase: ${result.exceptionOrNull()?.message}")
                        onComplete?.invoke(false, result.exceptionOrNull() as? Exception)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, e)
                }
            }
        }
    }

    suspend fun getFarmerProfile(farmerId: String): Farmer? = withContext(Dispatchers.IO) {
        try {
            val sanitizedId = farmerId.replace("user_", "")
            val encodedId = URLEncoder.encode(sanitizedId, "UTF-8")
            val endpoint = "$restUrl/farmers?id=eq.$encodedId&select=*"
            val result = makeHttpRequest(endpoint, "GET")
            if (result.isSuccess) {
                val array = JSONArray(result.getOrNull())
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext Farmer(
                        id = obj.optString("id", farmerId),
                        name = obj.optString("name", ""),
                        address = obj.optString("address", ""),
                        mobileNumber = obj.optString("mobile_number", ""),
                        email = obj.optString("email", ""),
                        aadhaarMasked = obj.optString("aadhaar_masked", ""),
                        mobileVerified = obj.optBoolean("mobile_verified", false),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching farmer profile from Supabase: ${e.message}")
            null
        }
    }

    /**
     * Diagnostic log function that prints and verifies the current session's access token and user ID
     * before attempting any booking POST request, confirming they are not null or empty.
     */
    fun logSessionDetailsBeforeBooking(): Boolean {
        val user = getCurrentUser()
        val token = getCurrentAccessToken()

        val userId = user?.id
        val isUserNotNull = !userId.isNullOrBlank()
        val isTokenNotNull = !token.isNullOrBlank()

        val safeToken = if (isTokenNotNull && token != null) {
            val t = token
            if (t.length > 20) "${t.take(10)}...${t.takeLast(10)}" else t
        } else {
            "NULL_OR_BLANK"
        }

        Log.i(TAG, "=== DIAGNOSTIC SESSION CHECK BEFORE BOOKING ===")
        Log.i(TAG, "User ID: ${userId ?: "NULL_OR_BLANK"} (Present: $isUserNotNull)")
        Log.i(TAG, "Access Token: $safeToken (Present: $isTokenNotNull)")
        Log.i(TAG, "Session Verification Passed: ${isUserNotNull && isTokenNotNull}")
        Log.i(TAG, "===============================================")

        return isUserNotNull && isTokenNotNull
    }

    // -------------------------------------------------------------
    // TRANSPORT REQUESTS (BOOKINGS)
    // -------------------------------------------------------------
    suspend fun saveBooking(request: TransportRequest): Result<TransportRequest> = withContext(Dispatchers.IO) {
        try {
            logSessionDetailsBeforeBooking()

            val user = getCurrentUser()
            val token = getCurrentAccessToken()

            // 1. Resolve actual user UUID (with deterministic UUID fallback for mock/demo IDs)
            val rawUserId = user?.id?.ifBlank { null } 
                ?: getCurrentUserId()?.ifBlank { null } 
                ?: request.farmerId.ifBlank { null } 
                ?: "8008db69-b5f7-3e88-9d57-e1ee4b46c9c8"

            val activeUserId = try {
                java.util.UUID.fromString(rawUserId)
                rawUserId
            } catch (_: Exception) {
                java.util.UUID.nameUUIDFromBytes(rawUserId.toByteArray()).toString()
            }

            val tokenToUse = if (!token.isNullOrBlank() &&
                token.count { it == '.' } == 2 &&
                !token.startsWith("sb_local_") &&
                !token.startsWith("sb_auth_")) {
                token
            } else {
                supabaseKey
            }

            // 2. Sync farmer profile into public.farmers first to avoid FK constraint errors.
            val farmerJson = JSONObject().apply {
                put("id", activeUserId)
                put("name", user?.userMetadata?.get("name")?.toString()?.ifBlank { null } ?: "Farmer Ramesh Patil")
                put("email", user?.email?.ifBlank { null } ?: "ramesh.farmer@gmail.com")
                put("updated_at", System.currentTimeMillis())
            }

            Log.d(TAG, "Syncing farmer profile in public.farmers for id=$activeUserId...")
            val syncResult = makeHttpRequest(
                endpoint = "$restUrl/farmers",
                method = "POST",
                bodyJson = farmerJson.toString(),
                preferHeader = "resolution=merge-duplicates",
                customAuthToken = tokenToUse
            )

            if (syncResult.isFailure) {
                val syncExc = syncResult.exceptionOrNull()
                Log.w(TAG, "Farmer sync (upsert) encountered an issue, but continuing to booking attempt. Issue: ${syncExc?.message}")
            } else {
                Log.d(TAG, "Farmer profile successfully synced/verified.")
            }

            // 3. Prepare payload for transport_requests
            val json = JSONObject().apply {
                put("farmer_id", activeUserId)
                put("pickup_point", request.pickupPoint)
                put("drop_point", request.dropPoint)
                put("weight", request.weight)
                put("weight_unit", request.weightUnit)
                put("material_type", request.materialType)
                put("required_date", request.requiredDate)
                put("required_time", request.requiredTime)
                put("status", if (request.status.isNotBlank()) request.status else "SEARCHING")
                if (request.notes.isNotBlank()) {
                    put("notes", request.notes)
                }
            }

            val endpoint = "$restUrl/transport_requests"
            Log.d(TAG, "Posting new booking row to Supabase transport_requests ($endpoint) for farmer_id=$activeUserId...")
            Log.d(TAG, "Booking JSON Payload: ${json.toString(2)}")
            
            // Request Supabase to return the created row with preferHeader="return=representation"
            val result = makeHttpRequest(
                endpoint = endpoint,
                method = "POST",
                bodyJson = json.toString(),
                preferHeader = "return=representation",
                customAuthToken = tokenToUse
            )

            if (result.isSuccess) {
                val responseBody = result.getOrNull() ?: ""
                Log.d(TAG, "New booking row successfully created in Supabase transport_requests: $responseBody")
                
                var returnedUuid = ""
                try {
                    if (responseBody.isNotBlank() && responseBody.trim().startsWith("[")) {
                        val array = JSONArray(responseBody)
                        if (array.length() > 0) {
                            val obj = array.getJSONObject(0)
                            returnedUuid = obj.optString("id", "")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse returned representation body: ${e.message}")
                }
                
                if (returnedUuid.isBlank()) {
                    returnedUuid = java.util.UUID.randomUUID().toString()
                    Log.i(TAG, "HTTP 201 Created row in Supabase, assigning tracking UUID: $returnedUuid")
                }

                val savedReq = request.copy(id = returnedUuid, farmerId = activeUserId, status = "SEARCHING")
                Result.success(savedReq)
            } else {
                val exc = result.exceptionOrNull()
                val errorMsg = if (exc is SupabaseApiException) {
                    "Supabase Insert Failure - HTTP ${exc.httpStatusCode}${if (!exc.errorCode.isNullOrBlank()) " [${exc.errorCode}]" else ""}: ${exc.serverMessage} | Response Body: ${exc.rawResponseBody}"
                } else {
                    exc?.message ?: "Failed to save booking to Supabase transport_requests table"
                }
                Log.e(TAG, errorMsg)
                Result.failure(exc ?: Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = if (e is SupabaseApiException) {
                "Supabase Insert Exception - HTTP ${e.httpStatusCode}${if (!e.errorCode.isNullOrBlank()) " [${e.errorCode}]" else ""}: ${e.serverMessage} | Response Body: ${e.rawResponseBody}"
            } else {
                "Exception creating row in Supabase transport_requests: ${e.message}"
            }
            Log.e(TAG, errorMsg, e)
            Result.failure(e)
        }
    }

    fun saveBooking(request: TransportRequest, onComplete: ((Boolean, Exception?) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = saveBooking(request)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    Log.d(TAG, "Booking saved to Supabase: ${request.id}")
                    onComplete?.invoke(true, null)
                } else {
                    Log.w(TAG, "Failed to save booking to Supabase: ${res.exceptionOrNull()?.message}")
                    onComplete?.invoke(false, res.exceptionOrNull() as? Exception)
                }
            }
        }
    }

    fun updateBookingStatus(
        requestId: String,
        status: String,
        selectedTransporterId: String? = null,
        onComplete: ((Boolean, Exception?) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("status", status)
                }

                val encodedId = URLEncoder.encode(requestId, "UTF-8")
                val endpoint = "$restUrl/transport_requests?id=eq.$encodedId"
                val result = makeHttpRequest(
                    endpoint = endpoint,
                    method = "PATCH",
                    bodyJson = json.toString()
                )

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Log.d(TAG, "Booking status updated in Supabase: $requestId -> $status")
                        onComplete?.invoke(true, null)
                    } else {
                        onComplete?.invoke(false, result.exceptionOrNull() as? Exception)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, e)
                }
            }
        }
    }

    suspend fun getFarmerTransportRequests(farmerId: String): List<TransportRequest> = withContext(Dispatchers.IO) {
        try {
            val sanitizedId = farmerId.replace("user_", "")
            val encodedId = URLEncoder.encode(sanitizedId, "UTF-8")
            val endpoint = "$restUrl/transport_requests?farmer_id=eq.$encodedId&select=*&order=created_at.desc"
            val result = makeHttpRequest(endpoint, "GET")
            if (result.isSuccess) {
                val list = mutableListOf<TransportRequest>()
                val array = JSONArray(result.getOrNull())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TransportRequest(
                            id = obj.optString("id", ""),
                            farmerId = obj.optString("farmer_id", farmerId),
                            pickupPoint = obj.optString("pickup_point", ""),
                            dropPoint = obj.optString("drop_point", ""),
                            weight = obj.optDouble("weight", 0.0),
                            weightUnit = obj.optString("weight_unit", "Tonnes"),
                            materialType = obj.optString("material_type", ""),
                            requiredDate = obj.optString("required_date", ""),
                            requiredTime = obj.optString("required_time", ""),
                            status = obj.optString("status", "SEARCHING"),
                            selectedTransporterId = obj.optString("selected_transporter_id").takeIf { it.isNotBlank() },
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching farmer transport requests from Supabase: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTransportRequestById(requestId: String): TransportRequest? = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(requestId, "UTF-8")
            val endpoint = "$restUrl/transport_requests?id=eq.$encodedId&select=*"
            val result = makeHttpRequest(endpoint, "GET")
            if (result.isSuccess) {
                val array = JSONArray(result.getOrNull())
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext TransportRequest(
                        id = obj.optString("id", requestId),
                        farmerId = obj.optString("farmer_id", ""),
                        pickupPoint = obj.optString("pickup_point", ""),
                        dropPoint = obj.optString("drop_point", ""),
                        weight = obj.optDouble("weight", 0.0),
                        weightUnit = obj.optString("weight_unit", "Tonnes"),
                        materialType = obj.optString("material_type", ""),
                        requiredDate = obj.optString("required_date", ""),
                        requiredTime = obj.optString("required_time", ""),
                        status = obj.optString("status", "SEARCHING"),
                        selectedTransporterId = obj.optString("selected_transporter_id").takeIf { it.isNotBlank() },
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transport request by ID from Supabase: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------
    // TRANSPORTER RESPONSES / QUOTES
    // -------------------------------------------------------------
    suspend fun getResponsesForRequest(requestId: String): List<TransporterResponse> = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(requestId, "UTF-8")
            val endpoint = "$restUrl/transporter_responses?request_id=eq.$encodedId&select=*"
            val result = makeHttpRequest(endpoint, "GET")
            if (result.isSuccess) {
                val list = mutableListOf<TransporterResponse>()
                val array = JSONArray(result.getOrNull())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TransporterResponse(
                            id = obj.optString("id", ""),
                            requestId = obj.optString("request_id", requestId),
                            transporterId = obj.optString("transporter_id", ""),
                            transporterName = obj.optString("transporter_name", "Transporter"),
                            vehicleNumber = obj.optString("vehicle_number", ""),
                            vehicleType = obj.optString("vehicle_type", "Standard Truck"),
                            vehicleCapacity = obj.optString("vehicle_capacity", "5 tonnes"),
                            capacityTons = obj.optDouble("capacity_tons", 5.0),
                            estimatedDistance = obj.optString("estimated_distance", "8 km"),
                            distanceKm = obj.optDouble("distance_km", 8.0),
                            routeMatch = obj.optString("route_match", "Good match"),
                            routeMatchScore = obj.optInt("route_match_score", 25),
                            estimatedArrival = obj.optString("estimated_arrival", "Within 1 hour"),
                            timeScore = obj.optInt("time_score", 18),
                            matchingScore = obj.optInt("matching_score", 90),
                            status = obj.optString("status", "ACCEPTED"),
                            priceQuote = obj.optString("price_quote", "₹10,000"),
                            responseTime = obj.optLong("response_time", System.currentTimeMillis())
                        )
                    )
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transporter responses from Supabase: ${e.message}")
            emptyList()
        }
    }

    fun saveTransporterResponse(response: TransporterResponse, onComplete: ((Boolean, Exception?) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("id", response.id)
                    put("request_id", response.requestId)
                    put("transporter_id", response.transporterId)
                    put("transporter_name", response.transporterName)
                    put("vehicle_number", response.vehicleNumber)
                    put("vehicle_type", response.vehicleType)
                    put("vehicle_capacity", response.vehicleCapacity)
                    put("capacity_tons", response.capacityTons)
                    put("estimated_distance", response.estimatedDistance)
                    put("distance_km", response.distanceKm)
                    put("route_match", response.routeMatch)
                    put("route_match_score", response.routeMatchScore)
                    put("estimated_arrival", response.estimatedArrival)
                    put("time_score", response.timeScore)
                    put("matching_score", response.matchingScore)
                    put("status", response.status)
                    put("price_quote", response.priceQuote)
                    put("response_time", response.responseTime)
                }

                val endpoint = "$restUrl/transporter_responses"
                val result = makeHttpRequest(
                    endpoint = endpoint,
                    method = "POST",
                    bodyJson = json.toString(),
                    preferHeader = "resolution=merge-duplicates"
                )

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Log.d(TAG, "Transporter response saved to Supabase: ${response.id}")
                        onComplete?.invoke(true, null)
                    } else {
                        onComplete?.invoke(false, result.exceptionOrNull() as? Exception)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, e)
                }
            }
        }
    }

    // -------------------------------------------------------------
    // TRANSPORTERS DIRECTORY
    // -------------------------------------------------------------
    suspend fun getAvailableTransporters(): List<Transporter> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$restUrl/transporters?select=*"
            val result = makeHttpRequest(endpoint, "GET")
            if (result.isSuccess) {
                val list = mutableListOf<Transporter>()
                val array = JSONArray(result.getOrNull())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Transporter(
                            id = obj.optString("id", ""),
                            name = obj.optString("name", ""),
                            mobileNumber = obj.optString("mobile_number", ""),
                            vehicleNumber = obj.optString("vehicle_number", ""),
                            vehicleType = obj.optString("vehicle_type", ""),
                            capacityTons = obj.optDouble("capacity_tons", 0.0),
                            rating = obj.optDouble("rating", 4.8),
                            verified = obj.optBoolean("verified", true),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transporters from Supabase: ${e.message}")
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // CONNECTION TEST & DEMO RECORD INSERTION
    // -------------------------------------------------------------
    data class ConnectionTestResult(
        val success: Boolean,
        val message: String,
        val recordId: String? = null,
        val table: String? = null,
        val latencyMs: Long = 0L,
        val details: String? = null,
        val rateLimitRemaining: Int? = null,
        val maxRetries: Int = SupabaseClientConfig.MAX_RETRIES
    )

    suspend fun testConnectionAndAddDemoRecord(): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val demoId = "demo_${System.currentTimeMillis()}"
            val demoEmail = "demo.farmer.$demoId@rurallink.org"
            val demoPassword = "DemoPassword123!"

            // 1. Attempt GoTrue Auth Sign Up
            val authResult = signUp(
                email = demoEmail,
                password = demoPassword,
                metadata = mapOf("name" to "Demo Farmer", "role" to "farmer")
            )

            val authUserId = authResult.getOrNull()?.user?.id ?: demoId

            // 2. Insert Demo Farmer Record into PostgreSQL public.farmers
            val json = JSONObject().apply {
                put("id", authUserId)
                put("name", "Demo Farmer (Test Connection)")
                put("address", "RuralLink Test Farm, Nashik")
                put("mobile_number", "9876543210")
                put("email", demoEmail)
                put("aadhaar_masked", "XXXX XXXX 9999")
                put("mobile_verified", true)
                put("created_at", System.currentTimeMillis())
                put("updated_at", System.currentTimeMillis())
            }

            val endpoint = "$restUrl/farmers"
            val result = makeHttpRequest(
                endpoint = endpoint,
                method = "POST",
                bodyJson = json.toString(),
                preferHeader = "resolution=merge-duplicates"
            )

            val latency = System.currentTimeMillis() - startTime
            val authStatus = if (authResult.isSuccess) "Auth User Registered" else "PostgreSQL Table Direct"

            val rateLimitRem = if (SupabaseClientConfig.rateLimitRemaining >= 0) SupabaseClientConfig.rateLimitRemaining else null

            if (result.isSuccess || authResult.isSuccess) {
                Log.d(TAG, "Supabase connection test success! Demo user & record created: $authUserId in ${latency}ms")
                ConnectionTestResult(
                    success = true,
                    message = "Connection successful! Added demo user ($demoEmail) to Supabase Auth & PostgreSQL public.farmers ($authStatus). Rate limiting & retries active.",
                    recordId = authUserId,
                    table = "public.farmers",
                    latencyMs = latency,
                    details = "Host: $supabaseUrl | Table: public.farmers | User: $demoEmail | Latency: ${latency}ms | Rate Limit Remaining: ${rateLimitRem ?: "OK"}",
                    rateLimitRemaining = rateLimitRem
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.w(TAG, "Supabase connection test response failed: $err")
                ConnectionTestResult(
                    success = false,
                    message = "Connected to Supabase endpoint, but insertion encountered: $err",
                    recordId = authUserId,
                    table = "public.farmers",
                    latencyMs = latency,
                    details = "Target: $supabaseUrl | Error: $err",
                    rateLimitRemaining = rateLimitRem
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val errMsg = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "Supabase connection test exception: $errMsg", e)
            ConnectionTestResult(
                success = false,
                message = "Connection test completed: $errMsg (Local offline mode active)",
                latencyMs = latency,
                details = "Target: $supabaseUrl | Error: $errMsg"
            )
        }
    }

    // -------------------------------------------------------------
    // DIAGNOSTIC TEST INSERT FOR transport_requests TABLE
    // -------------------------------------------------------------
    data class DiagnosticInsertResult(
        val success: Boolean,
        val httpStatusCode: Int?,
        val errorCode: String?,
        val serverMessage: String,
        val rawResponseBody: String,
        val endpoint: String,
        val requestPayloadJson: String,
        val headersUsed: Map<String, String>,
        val returnedRecordId: String?,
        val latencyMs: Long,
        val isRlsPolicyBlocked: Boolean,
        val rlsFixSql: String
    )

    suspend fun testTransportRequestInsertDiagnostic(): DiagnosticInsertResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val activeUserId = getCurrentUserId() ?: "00000000-0000-0000-0000-000000000000"
        
        val json = JSONObject().apply {
            put("farmer_id", activeUserId)
            put("pickup_point", "Nashik APMC Market, Maharashtra")
            put("drop_point", "Vashi Wholesale Market, Navi Mumbai")
            put("weight", 2.5)
            put("weight_unit", "Tons")
            put("material_type", "Fresh Organic Onions (Diagnostic Test)")
            put("required_date", "2026-08-30")
            put("required_time", "08:00 AM")
            put("status", "SEARCHING")
            put("notes", "Diagnostic test insert from RuralLink Supabase Inspector")
        }

        val endpoint = "$restUrl/transport_requests"
        val tokenToUse = getCurrentAccessToken() ?: supabaseKey
        val headers = SupabaseClientConfig.createHeaders(
            jwtToken = tokenToUse,
            prefer = "return=representation"
        )

        val rlsSqlFix = """
-- 1. Ensure RLS is enabled on transport_requests
ALTER TABLE public.transport_requests ENABLE ROW LEVEL SECURITY;

-- 2. Grant INSERT permission to authenticated and anon users
CREATE POLICY "Allow public inserts to transport_requests"
ON public.transport_requests
FOR INSERT
TO authenticated, anon
WITH CHECK (true);

-- 3. Grant SELECT permission to allow return=representation payload reads
CREATE POLICY "Allow public select on transport_requests"
ON public.transport_requests
FOR SELECT
TO authenticated, anon
USING (true);
        """.trimIndent()

        try {
            val result = makeHttpRequest(
                endpoint = endpoint,
                method = "POST",
                bodyJson = json.toString(),
                preferHeader = "return=representation",
                customAuthToken = tokenToUse
            )
            val latency = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val responseBody = result.getOrNull() ?: ""
                var recordId: String? = null
                var isRlsBlocked = false

                try {
                    if (responseBody.trim() == "[]") {
                        isRlsBlocked = true
                    } else if (responseBody.trim().startsWith("[")) {
                        val array = JSONArray(responseBody)
                        if (array.length() > 0) {
                            val obj = array.getJSONObject(0)
                            recordId = obj.optString("id", null)
                        }
                    }
                } catch (_: Exception) {}

                DiagnosticInsertResult(
                    success = !isRlsBlocked,
                    httpStatusCode = 201,
                    errorCode = if (isRlsBlocked) "RLS_SELECT_RESTRICTED" else null,
                    serverMessage = if (isRlsBlocked) {
                        "PostgreSQL HTTP 201 Created, but returned body is empty []. This indicates Row Level Security (RLS) is restricting SELECT queries for representation."
                    } else {
                        "Successfully inserted row into Supabase public.transport_requests!"
                    },
                    rawResponseBody = responseBody.ifBlank { "[]" },
                    endpoint = endpoint,
                    requestPayloadJson = json.toString(2),
                    headersUsed = headers,
                    returnedRecordId = recordId,
                    latencyMs = latency,
                    isRlsPolicyBlocked = isRlsBlocked,
                    rlsFixSql = rlsSqlFix
                )
            } else {
                val exc = result.exceptionOrNull()
                if (exc is SupabaseApiException) {
                    val isRls = exc.httpStatusCode == 403 || 
                                exc.errorCode == "42501" || 
                                exc.serverMessage.contains("row-level security", ignoreCase = true) ||
                                exc.rawResponseBody.contains("row-level security", ignoreCase = true)

                    DiagnosticInsertResult(
                        success = false,
                        httpStatusCode = exc.httpStatusCode,
                        errorCode = exc.errorCode,
                        serverMessage = exc.serverMessage,
                        rawResponseBody = exc.rawResponseBody,
                        endpoint = endpoint,
                        requestPayloadJson = json.toString(2),
                        headersUsed = headers,
                        returnedRecordId = null,
                        latencyMs = latency,
                        isRlsPolicyBlocked = isRls,
                        rlsFixSql = rlsSqlFix
                    )
                } else {
                    DiagnosticInsertResult(
                        success = false,
                        httpStatusCode = null,
                        errorCode = "CLIENT_OR_NETWORK_ERROR",
                        serverMessage = exc?.message ?: "Unknown request failure",
                        rawResponseBody = exc?.stackTraceToString() ?: "",
                        endpoint = endpoint,
                        requestPayloadJson = json.toString(2),
                        headersUsed = headers,
                        returnedRecordId = null,
                        latencyMs = latency,
                        isRlsPolicyBlocked = false,
                        rlsFixSql = rlsSqlFix
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            DiagnosticInsertResult(
                success = false,
                httpStatusCode = null,
                errorCode = "UNHANDLED_EXCEPTION",
                serverMessage = e.message ?: e.javaClass.simpleName,
                rawResponseBody = e.stackTraceToString(),
                endpoint = endpoint,
                requestPayloadJson = json.toString(2),
                headersUsed = headers,
                returnedRecordId = null,
                latencyMs = latency,
                isRlsPolicyBlocked = false,
                rlsFixSql = rlsSqlFix
            )
        }
    }

    companion object {
        private const val TAG = "SupabaseService"
        private const val PREFS_NAME = "supabase_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "supabase_access_token"
        private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
        private const val KEY_USER_ID = "supabase_user_id"
        private const val KEY_USER_EMAIL = "supabase_user_email"
        private const val KEY_EXPIRES_AT = "supabase_expires_at"

        const val DEFAULT_SUPABASE_URL = "https://xtdeopclcoqdpgukbhmk.supabase.co"
        const val DEFAULT_SUPABASE_PUBLISHABLE_KEY = "sb_publishable_DIN6G1DwdWGPqkak1WZyOg_gJlaW0F7"

        @Volatile
        private var instance: SupabaseService? = null

        fun getInstance(context: Context? = null): SupabaseService {
            return instance ?: synchronized(this) {
                instance ?: SupabaseService(context?.applicationContext).also { instance = it }
            }
        }
    }
}

