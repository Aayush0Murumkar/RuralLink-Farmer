import re

with open('app/src/main/java/com/example/services/SupabaseService.kt', 'r') as f:
    content = f.read()

# Find the saveBooking(request: TransportRequest) method
start_marker = "suspend fun saveBooking(request: TransportRequest): Result<TransportRequest> = withContext(Dispatchers.IO) {"
end_marker = "    fun saveBooking(request: TransportRequest, onComplete: ((Boolean, Exception?) -> Unit)? = null) {"

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

new_method = """suspend fun saveBooking(request: TransportRequest): Result<TransportRequest> = withContext(Dispatchers.IO) {
        try {
            logSessionDetailsBeforeBooking()

            val user = getCurrentUser()
            val token = getCurrentAccessToken()

            // 1. Resolve actual authenticated user's UUID
            val currentFarmerId = when {
                !request.farmerId.isNullOrBlank() && isValidUUID(request.farmerId) -> request.farmerId
                user?.id != null && isValidUUID(user.id) -> user.id
                else -> {
                    Log.w(TAG, "Cannot resolve valid farmerId UUID for booking (got: ${request.farmerId}, user=${user?.id})")
                    request.farmerId ?: ""
                }
            }

            Log.i(TAG, "saveBooking: inserting transport request with farmerId=$currentFarmerId (payload: title=${request.title}, crop=${request.cropType})")

            val payload = buildJsonObject {
                put("title", request.title)
                put("pickup_location", request.pickupLocation)
                put("destination", request.destination)
                put("crop_type", request.cropType)
                put("quantity_tons", request.quantityTons)
                put("pickup_date", request.pickupDate)
                put("truck_type_needed", request.truckTypeNeeded)
                put("status", request.status)
                if (currentFarmerId.isNotBlank()) {
                    put("farmer_id", currentFarmerId)
                }
                if (request.estimatedCost != null) {
                    put("estimated_cost", request.estimatedCost)
                }
                if (request.notes != null) {
                    put("notes", request.notes)
                }
            }

            val client = getHttpClient()
            val restUrl = SupabaseClientConfig.getRestUrl()
            val response: HttpResponse = client.post("$restUrl/transport_requests") {
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                Log.i(TAG, "saveBooking: PostgREST insert SUCCESS -> $responseBody")
                val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
                val insertedObj = jsonArray.firstOrNull()?.jsonObject
                val createdId = insertedObj?.get("id")?.jsonPrimitive?.contentOrNull ?: request.id
                val createdReq = request.copy(id = createdId, farmerId = currentFarmerId)
                Result.success(createdReq)
            } else {
                val errorBody = response.bodyAsText()
                Log.e(TAG, "saveBooking: PostgREST insert FAILED (${response.status.value}) -> $errorBody")
                Result.failure(Exception("Failed to save booking to Supabase: ${response.status.value} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveBooking: exception -> ${e.message}", e)
            Result.failure(e)
        }
    }
"""

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_method + content[end_idx:]
    with open('app/src/main/java/com/example/services/SupabaseService.kt', 'w') as f:
        f.write(content)
    print("Successfully replaced saveBooking in SupabaseService.kt")
else:
    print("Could not find markers in SupabaseService.kt")
