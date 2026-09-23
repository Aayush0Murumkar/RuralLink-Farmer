package com.example.services

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlin.math.pow

/**
 * SupabaseClientConfig
 * 
 * Centralized configuration object for initializing and managing the Supabase client connection.
 * Resolves credentials securely from `.env` via [BuildConfig], environment variables,
 * or defaults, and provides standard headers, API endpoints, and client accessors.
 */
object SupabaseClientConfig {

    private const val TAG = "SupabaseClientConfig"

    // Default project credentials (fallback if .env is missing or unpopulated)
    const val DEFAULT_SUPABASE_URL = "https://xtdeopclcoqdpgukhbmk.supabase.co"
    const val DEFAULT_SUPABASE_PUBLISHABLE_KEY = "sb_publishable_DIN6G1DwdWGPqkak1WZyOg_gJlaW0F7"

    /**
     * Active Supabase project base URL (e.g., "https://<project-ref>.supabase.co")
     * Read securely from .env via BuildConfig.SUPABASE_URL with fallback.
     */
    val supabaseUrl: String by lazy { resolveUrl() }

    /**
     * Active Supabase API key (Publishable / Anon key)
     * Read securely from .env via BuildConfig.SUPABASE_PUBLISHABLE_KEY with fallback.
     */
    val apiKey: String by lazy { resolveApiKey() }

    /**
     * Alias for [apiKey]
     */
    val publishableKey: String get() = apiKey

    /**
     * PostgREST REST API endpoint URL
     */
    val restUrl: String get() = "$supabaseUrl/rest/v1"

    /**
     * GoTrue Authentication API endpoint URL
     */
    val authUrl: String get() = "$supabaseUrl/auth/v1"

    /**
     * Supabase Storage API endpoint URL
     */
    val storageUrl: String get() = "$supabaseUrl/storage/v1"

    /**
     * Supabase Realtime WebSocket URL
     */
    val realtimeUrl: String get() {
        val host = supabaseUrl.removePrefix("https://").removePrefix("http://")
        return "wss://$host/realtime/v1/websocket"
    }

    /**
     * Database schema (defaults to "public")
     */
    const val DEFAULT_SCHEMA = "public"

    /**
     * Connection timeout in milliseconds
     */
    const val CONNECT_TIMEOUT_MS = 3_000
    const val READ_TIMEOUT_MS = 4_000

    // --- Rate Limiting & Retry Policy Constants ---
    const val MAX_RETRIES = 1
    const val INITIAL_BACKOFF_MS = 500L
    const val MAX_BACKOFF_MS = 8000L
    const val BACKOFF_MULTIPLIER = 2.0

    // Client-side rate limiter limits (Min spacing between dispatches)
    private const val MIN_REQUEST_INTERVAL_MS = 100L
    private val lastRequestTimestamp = java.util.concurrent.atomic.AtomicLong(0L)

    // Dynamic rate limit tracking (from Supabase response headers)
    private val _rateLimitLimit = java.util.concurrent.atomic.AtomicInteger(-1)
    private val _rateLimitRemaining = java.util.concurrent.atomic.AtomicInteger(-1)
    private val _rateLimitResetSeconds = java.util.concurrent.atomic.AtomicLong(-1L)
    private val _retryAfterSeconds = java.util.concurrent.atomic.AtomicLong(-1L)

    val rateLimitLimit: Int get() = _rateLimitLimit.get()
    val rateLimitRemaining: Int get() = _rateLimitRemaining.get()
    val rateLimitResetSeconds: Long get() = _rateLimitResetSeconds.get()
    val retryAfterSeconds: Long get() = _retryAfterSeconds.get()

    val isRateLimited: Boolean
        get() = _retryAfterSeconds.get() > 0 || (_rateLimitRemaining.get() in 0..1)

    /**
     * Enforces client-side rate limit spacing between request dispatches.
     * Throttles rapid request bursts to avoid exceeding server rate limits.
     */
    fun acquireRateLimitPermit() {
        val now = System.currentTimeMillis()
        val last = lastRequestTimestamp.get()
        val elapsed = now - last
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            val sleepTime = MIN_REQUEST_INTERVAL_MS - elapsed
            try {
                Thread.sleep(sleepTime)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        lastRequestTimestamp.set(System.currentTimeMillis())
    }

    /**
     * Updates internal rate limiting metadata from HTTP response headers.
     */
    fun updateRateLimitHeaders(headers: Map<String, List<String>>) {
        try {
            headers.forEach { (key, values) ->
                if (values.isEmpty()) return@forEach
                val value = values.firstOrNull() ?: return@forEach
                when (key.lowercase()) {
                    "retry-after" -> {
                        val secs = value.toLongOrNull() ?: 5L
                        _retryAfterSeconds.set(secs)
                        Log.w(TAG, "Supabase Rate Limit Active: Retry-After header = ${secs}s")
                    }
                    "x-ratelimit-limit" -> {
                        value.toIntOrNull()?.let { _rateLimitLimit.set(it) }
                    }
                    "x-ratelimit-remaining" -> {
                        value.toIntOrNull()?.let { rem ->
                            _rateLimitRemaining.set(rem)
                            if (rem < 5) {
                                Log.w(TAG, "Warning: Low rate limit remaining ($rem left)")
                            }
                        }
                    }
                    "x-ratelimit-reset" -> {
                        value.toLongOrNull()?.let { _rateLimitResetSeconds.set(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error parsing rate limit headers: ${e.message}")
        }
    }

    /**
     * Calculates exponential backoff with randomized jitter for retries.
     */
    fun calculateBackoffWithJitter(attempt: Int, retryAfterHeader: String? = null): Long {
        // 1. If Retry-After header is provided, prioritize it
        retryAfterHeader?.trim()?.toLongOrNull()?.let { secs ->
            if (secs > 0) return kotlin.math.min(secs * 1000L, MAX_BACKOFF_MS)
        }

        // 2. Exponential backoff: initial * multiplier^(attempt-1)
        val exponential = INITIAL_BACKOFF_MS * BACKOFF_MULTIPLIER.toDouble().pow((attempt - 1).toDouble()).toLong()
        val cappedBackoff = kotlin.math.min(exponential, MAX_BACKOFF_MS)

        // 3. Add 0-200ms randomized jitter to prevent thundering herd problem
        val jitter = kotlin.random.Random.nextLong(0, 200)
        return cappedBackoff + jitter
    }

    /**
     * Resets rate limit metrics (used after successful requests or during testing).
     */
    fun resetRateLimitState() {
        _rateLimitRemaining.set(-1)
        _rateLimitLimit.set(-1)
        _rateLimitResetSeconds.set(-1L)
        _retryAfterSeconds.set(-1L)
    }

    /**
     * Masked representation of the API key for safe logging and UI badges.
     */
    val maskedApiKey: String
        get() {
            val key = apiKey
            return if (key.length <= 8) "***" else "${key.take(4)}...${key.takeLast(4)}"
        }

    /**
     * Indicates whether the Supabase client has been configured with a non-empty URL and API key.
     */
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && apiKey.isNotBlank()

    /**
     * Generates standard HTTP headers for Supabase API requests.
     *
     * @param jwtToken Optional JWT Bearer token for authenticated user sessions
     * @param prefer Optional PostgREST Prefer header (e.g. "return=representation", "resolution=merge-duplicates")
     * @param accept Optional Accept header (defaults to "application/json")
     * @param schema Optional Postgres schema (defaults to "public")
     */
    fun createHeaders(
        jwtToken: String? = null,
        prefer: String? = null,
        accept: String = "application/json",
        schema: String = DEFAULT_SCHEMA
    ): Map<String, String> {
        val headers = mutableMapOf(
            "apikey" to apiKey,
            "Authorization" to if (!jwtToken.isNullOrBlank()) "Bearer $jwtToken" else "Bearer $apiKey",
            "Content-Type" to "application/json",
            "Accept" to accept,
            "Accept-Profile" to schema,
            "Content-Profile" to schema
        )
        if (!prefer.isNullOrBlank()) {
            headers["Prefer"] = prefer
        }
        return headers
    }

    /**
     * Obtains or creates the singleton [SupabaseService] client instance initialized
     * with this configuration.
     */
    fun getClient(context: Context? = null): SupabaseService {
        return SupabaseService.getInstance(context)
    }

    /**
     * Returns diagnostic info about the active Supabase connection configuration and rate limits.
     */
    fun getDiagnosticsInfo(): Map<String, String> {
        return mapOf(
            "url" to supabaseUrl,
            "maskedKey" to maskedApiKey,
            "restEndpoint" to restUrl,
            "authEndpoint" to authUrl,
            "isConfigured" to isConfigured.toString(),
            "maxRetries" to MAX_RETRIES.toString(),
            "rateLimitLimit" to if (rateLimitLimit >= 0) rateLimitLimit.toString() else "untracked",
            "rateLimitRemaining" to if (rateLimitRemaining >= 0) rateLimitRemaining.toString() else "untracked",
            "isRateLimited" to isRateLimited.toString()
        )
    }

    // --- Private Credential Resolvers ---

    private fun resolveUrl(): String {
        // 1. Try reading from BuildConfig (populated by Secrets Gradle Plugin from .env)
        try {
            val field = BuildConfig::class.java.getField("SUPABASE_URL")
            val v = field.get(null) as? String
            if (!v.isNullOrBlank()) {
                Log.d(TAG, "Loaded SUPABASE_URL from BuildConfig/.env")
                return v.trim().trimEnd('/')
            }
        } catch (_: Throwable) {}

        // 2. Try System environment variable
        val env = System.getenv("SUPABASE_URL")
        if (!env.isNullOrBlank()) {
            Log.d(TAG, "Loaded SUPABASE_URL from System.getenv")
            return env.trim().trimEnd('/')
        }

        // 3. Fallback to default
        Log.i(TAG, "Using default SUPABASE_URL: $DEFAULT_SUPABASE_URL")
        return DEFAULT_SUPABASE_URL
    }

    private fun resolveApiKey(): String {
        // 1. Try reading from BuildConfig (populated by Secrets Gradle Plugin from .env)
        val candidates = listOf(
            "SUPABASE_PUBLISHABLE_KEY",
            "SUPABASE_ANON_KEY",
            "SUPABASE_KEY"
        )
        for (fieldName in candidates) {
            try {
                val field = BuildConfig::class.java.getField(fieldName)
                val v = field.get(null) as? String
                if (!v.isNullOrBlank()) {
                    Log.d(TAG, "Loaded $fieldName from BuildConfig/.env")
                    return v.trim()
                }
            } catch (_: Throwable) {}
        }

        // 2. Try System environment variables
        for (envName in candidates) {
            val env = System.getenv(envName)
            if (!env.isNullOrBlank()) {
                Log.d(TAG, "Loaded $envName from System.getenv")
                return env.trim()
            }
        }

        // 3. Fallback to default
        Log.i(TAG, "Using default SUPABASE_PUBLISHABLE_KEY")
        return DEFAULT_SUPABASE_PUBLISHABLE_KEY
    }
}
