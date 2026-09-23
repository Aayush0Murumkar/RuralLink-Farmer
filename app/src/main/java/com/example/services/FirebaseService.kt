package com.example.services

import android.app.Activity
import com.example.data.model.Farmer
import com.example.data.model.TransportRequest
import com.example.data.model.Transporter
import com.example.data.model.TransporterResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

/**
 * Clean Firebase Service layer providing:
 * - Firebase Phone Authentication & OTP verification
 * - Cloud Firestore operations for:
 *   - Farmer profiles (`farmers`)
 *   - Transporter/Driver profiles (`transporters`)
 *   - Transport booking records (`transport_requests`)
 *   - Booking quotes & status updates (`transporter_responses`)
 */
class FirebaseService {

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (_: Throwable) {
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            null
        }

    // Collection References
    private val farmersCollection get() = firestore?.collection("farmers")
    private val transportersCollection get() = firestore?.collection("transporters")
    private val requestsCollection get() = firestore?.collection("transport_requests")
    private val responsesCollection get() = firestore?.collection("transporter_responses")

    // --- FIREBASE PHONE AUTHENTICATION ---

    /**
     * Initiates Firebase Phone Number Verification (Sends OTP).
     */
    fun sendPhoneOtp(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        timeoutSeconds: Long = 60L
    ) {
        val a = auth ?: return
        val formattedNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
        val options = PhoneAuthOptions.newBuilder(a)
            .setPhoneNumber(formattedNumber)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Signs in using a PhoneAuthCredential (received after OTP entry).
     */
    fun signInWithCredential(
        credential: PhoneAuthCredential,
        onResult: (Boolean, Exception?) -> Unit
    ) {
        val a = auth
        if (a == null) {
            onResult(true, null)
            return
        }
        a.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception)
                }
            }
    }

    /**
     * Returns the UID of the currently authenticated Firebase user, or null.
     */
    fun getCurrentUserUid(): String? {
        return auth?.currentUser?.uid
    }

    /**
     * Signs out the current Firebase user.
     */
    fun signOut() {
        auth?.signOut()
    }


    // --- CLOUD FIRESTORE: FARMER PROFILES ---

    /**
     * Saves or updates a farmer's profile in Firestore (`farmers` collection).
     */
    fun saveFarmerProfile(farmer: Farmer, onComplete: ((Boolean, Exception?) -> Unit)? = null) {
        val coll = farmersCollection
        if (coll == null) {
            onComplete?.invoke(true, null)
            return
        }
        val docId = if (farmer.id.isNotBlank()) farmer.id else (getCurrentUserUid() ?: "farmer_${System.currentTimeMillis()}")
        coll.document(docId)
            .set(farmer, SetOptions.merge())
            .addOnSuccessListener { onComplete?.invoke(true, null) }
            .addOnFailureListener { e -> onComplete?.invoke(false, e) }
    }

    /**
     * Real-time Flow of a Farmer's profile from Firestore.
     */
    fun getFarmerProfileFlow(farmerId: String): Flow<Farmer?> = callbackFlow {
        val coll = farmersCollection
        if (coll == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = coll.document(farmerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val farmer = snapshot?.toObject(Farmer::class.java)
                trySend(farmer)
            }
        awaitClose { listener.remove() }
    }


    // --- CLOUD FIRESTORE: TRANSPORTER PROFILES ---

    /**
     * Saves or updates a Transporter/Driver profile in Firestore (`transporters` collection).
     */
    fun saveTransporterProfile(transporter: Transporter, onComplete: ((Boolean, Exception?) -> Unit)? = null) {
        val coll = transportersCollection
        if (coll == null) {
            onComplete?.invoke(true, null)
            return
        }
        val docId = if (transporter.id.isNotBlank()) transporter.id else "trans_${System.currentTimeMillis()}"
        coll.document(docId)
            .set(transporter, SetOptions.merge())
            .addOnSuccessListener { onComplete?.invoke(true, null) }
            .addOnFailureListener { e -> onComplete?.invoke(false, e) }
    }

    /**
     * Fetches a Transporter profile from Firestore.
     */
    fun getTransporterProfile(transporterId: String, onResult: (Transporter?) -> Unit) {
        val coll = transportersCollection
        if (coll == null) {
            onResult(null)
            return
        }
        coll.document(transporterId)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.toObject(Transporter::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }


    // --- CLOUD FIRESTORE: TRANSPORT BOOKING RECORDS ---

    /**
     * Creates a new transport booking request in Firestore (`transport_requests` collection).
     */
    fun createTransportRequest(request: TransportRequest, onComplete: ((Boolean, Exception?) -> Unit)? = null) {
        val coll = requestsCollection
        if (coll == null) {
            onComplete?.invoke(true, null)
            return
        }
        coll.document(request.id)
            .set(request, SetOptions.merge())
            .addOnSuccessListener { onComplete?.invoke(true, null) }
            .addOnFailureListener { e -> onComplete?.invoke(false, e) }
    }

    /**
     * Real-time Flow of all transport requests for a given farmer from Firestore.
     */
    fun getFarmerRequestsFlow(farmerId: String): Flow<List<TransportRequest>> = callbackFlow {
        val coll = requestsCollection
        if (coll == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = coll
            .whereEqualTo("farmerId", farmerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject(TransportRequest::class.java) } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Updates the status or selected transporter of a transport request in Firestore.
     */
    fun updateBookingStatus(
        requestId: String,
        status: String,
        selectedTransporterId: String? = null,
        onComplete: ((Boolean, Exception?) -> Unit)? = null
    ) {
        val coll = requestsCollection
        if (coll == null) {
            onComplete?.invoke(true, null)
            return
        }
        val updates = mutableMapOf<String, Any>("status" to status)
        if (selectedTransporterId != null) {
            updates["selectedTransporterId"] = selectedTransporterId
        }
        coll.document(requestId)
            .update(updates)
            .addOnSuccessListener { onComplete?.invoke(true, null) }
            .addOnFailureListener { e -> onComplete?.invoke(false, e) }
    }


    // --- CLOUD FIRESTORE: TRANSPORTER RESPONSES / QUOTES ---

    /**
     * Saves a transporter response/quote in Firestore (`transporter_responses` collection).
     */
    fun saveTransporterResponse(response: TransporterResponse, onComplete: ((Boolean, Exception?) -> Unit)? = null) {
        val coll = responsesCollection
        if (coll == null) {
            onComplete?.invoke(true, null)
            return
        }
        coll.document(response.id)
            .set(response, SetOptions.merge())
            .addOnSuccessListener { onComplete?.invoke(true, null) }
            .addOnFailureListener { e -> onComplete?.invoke(false, e) }
    }

    /**
     * Real-time Flow of transporter responses for a given request from Firestore.
     */
    fun getResponsesForRequestFlow(requestId: String): Flow<List<TransporterResponse>> = callbackFlow {
        val coll = responsesCollection
        if (coll == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = coll
            .whereEqualTo("requestId", requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(TransporterResponse::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
}
