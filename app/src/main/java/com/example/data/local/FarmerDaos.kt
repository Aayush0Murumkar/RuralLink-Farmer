package com.example.data.local

import androidx.room.*
import com.example.data.model.Farmer
import com.example.data.model.NotificationItem
import com.example.data.model.TransportRequest
import com.example.data.model.TransporterResponse
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmerDao {
    @Query("SELECT * FROM farmer_profile LIMIT 1")
    fun getFarmer(): Flow<Farmer?>

    @Query("SELECT * FROM farmer_profile LIMIT 1")
    suspend fun getFarmerOnce(): Farmer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFarmer(farmer: Farmer)

    @Query("UPDATE farmer_profile SET mobileVerified = :verified WHERE id = :farmerId")
    suspend fun setMobileVerified(farmerId: String, verified: Boolean)

    @Query("DELETE FROM farmer_profile")
    suspend fun clear()
}

@Dao
interface RequestDao {
    @Query("SELECT * FROM transport_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<TransportRequest>>

    @Query("SELECT * FROM transport_requests WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun getFarmerRequests(farmerId: String): Flow<List<TransportRequest>>

    @Query("SELECT * FROM transport_requests WHERE id = :id")
    suspend fun getRequestById(id: String): TransportRequest?

    @Query("SELECT * FROM transport_requests WHERE id = :id")
    fun getRequestByIdFlow(id: String): Flow<TransportRequest?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: TransportRequest)

    @Query("UPDATE transport_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE transport_requests SET status = :status, selectedTransporterId = :transporterId WHERE id = :id")
    suspend fun updateSelectedTransporter(id: String, transporterId: String, status: String)

    @Query("DELETE FROM transport_requests")
    suspend fun clear()
}

@Dao
interface TransporterResponseDao {
    @Query("SELECT * FROM transporter_responses WHERE requestId = :requestId ORDER BY matchingScore DESC")
    fun getResponsesForRequest(requestId: String): Flow<List<TransporterResponse>>

    @Query("SELECT * FROM transporter_responses WHERE requestId = :requestId ORDER BY matchingScore DESC")
    suspend fun getResponsesForRequestSync(requestId: String): List<TransporterResponse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponses(responses: List<TransporterResponse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: TransporterResponse)

    @Query("UPDATE transporter_responses SET status = :status WHERE id = :responseId")
    suspend fun updateResponseStatus(responseId: String, status: String)

    @Query("UPDATE transporter_responses SET status = 'NOT_SELECTED' WHERE requestId = :requestId")
    suspend fun markAllNotSelectedForRequest(requestId: String)

    @Query("DELETE FROM transporter_responses WHERE requestId = :requestId")
    suspend fun deleteResponsesForRequest(requestId: String)

    @Query("DELETE FROM transporter_responses")
    suspend fun clear()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clear()
}
