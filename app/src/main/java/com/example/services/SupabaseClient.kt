package com.example.services

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient

/**
 * SupabaseClient singleton object for initializing and managing the official Supabase client SDK instance.
 * Reads SUPABASE_URL and SUPABASE_KEY from BuildConfig (via SupabaseClientConfig).
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"

    /**
     * The resolved Supabase project base URL.
     */
    val url: String by lazy {
        SupabaseClientConfig.supabaseUrl
    }

    /**
     * The resolved Supabase API key.
     */
    val key: String by lazy {
        SupabaseClientConfig.apiKey
    }

    /**
     * The official Supabase Kotlin SDK client instance.
     */
    val client: JanSupabaseClient by lazy {
        Log.i(TAG, "Initializing Supabase SDK client with URL: $url")
        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
            install(Storage)
        }
    }

    /**
     * Convenience accessor for the underlying client instance.
     */
    val instance: JanSupabaseClient get() = client
}
