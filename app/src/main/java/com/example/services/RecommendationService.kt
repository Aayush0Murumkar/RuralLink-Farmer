package com.example.services

import com.example.data.model.TransportRequest
import com.example.data.model.TransporterResponse

data class ScoreBreakdown(
    val distanceScore: Int,
    val distanceMax: Int = 25,
    val capacityScore: Int,
    val capacityMax: Int = 25,
    val routeScore: Int,
    val routeMax: Int = 30,
    val timeScore: Int,
    val timeMax: Int = 20,
    val totalScore: Int
)

data class AiRecommendationResult(
    val recommendedResponseId: String?,
    val explanationPoints: List<String>,
    val scoreBreakdowns: Map<String, ScoreBreakdown>, // responseId -> ScoreBreakdown
    val isCloseCall: Boolean = false,
    val closeCallNote: String? = null,
    val isLimitedData: Boolean = false,
    val limitedDataNote: String? = null
)

class RecommendationService {

    fun evaluateResponses(
        request: TransportRequest,
        responses: List<TransporterResponse>
    ): AiRecommendationResult {
        val acceptedResponses = responses.filter { it.status == "ACCEPTED" || it.status == "SELECTED" }

        if (acceptedResponses.isEmpty()) {
            return AiRecommendationResult(
                recommendedResponseId = null,
                explanationPoints = emptyList(),
                scoreBreakdowns = emptyMap()
            )
        }

        var hasIncompleteData = false
        val scoreBreakdowns = mutableMapOf<String, ScoreBreakdown>()

        for (res in acceptedResponses) {
            val distScore = calculateDistanceScore(res)
            val capScore = calculateCapacityScore(request, res)
            val rScore = res.routeMatchScore.coerceIn(0, 30)
            val tScore = res.timeScore.coerceIn(0, 20)

            if (res.vehicleCapacity.isBlank() || res.estimatedDistance.isBlank()) {
                hasIncompleteData = true
            }

            val total = distScore + capScore + rScore + tScore
            scoreBreakdowns[res.id] = ScoreBreakdown(
                distanceScore = distScore,
                capacityScore = capScore,
                routeScore = rScore,
                timeScore = tScore,
                totalScore = total
            )
        }

        // Sort by total score descending
        val sortedList = acceptedResponses.sortedByDescending {
            scoreBreakdowns[it.id]?.totalScore ?: 0
        }

        val topChoice = sortedList.first()
        val topScoreBreakdown = scoreBreakdowns[topChoice.id]

        // Explainable bullet points
        val explanations = mutableListOf<String>()
        if (topChoice.estimatedDistance.isNotBlank()) {
            explanations.add("Closest suitable vehicle (${topChoice.estimatedDistance} away)")
        } else {
            explanations.add("Vehicle in regional operating proximity")
        }

        if (topChoice.vehicleCapacity.isNotBlank()) {
            explanations.add("Enough capacity for your material (${topChoice.vehicleCapacity} capacity)")
        }

        if (topChoice.routeMatch.isNotBlank()) {
            explanations.add("Route matches your destination (${topChoice.routeMatch})")
        }

        if (topChoice.estimatedArrival.isNotBlank()) {
            explanations.add("Estimated arrival (${topChoice.estimatedArrival}) aligns with requested schedule")
        }

        // Similar options evaluation
        var isCloseCall = false
        var closeCallNote: String? = null
        if (sortedList.size >= 2) {
            val runnerUp = sortedList[1]
            val score1 = scoreBreakdowns[topChoice.id]?.totalScore ?: 0
            val score2 = scoreBreakdowns[runnerUp.id]?.totalScore ?: 0
            if (kotlin.math.abs(score1 - score2) <= 3) {
                isCloseCall = true
                closeCallNote = "Both ${topChoice.transporterName} and ${runnerUp.transporterName} are strong options. ${topChoice.transporterName} has a slightly better route match."
            }
        }

        return AiRecommendationResult(
            recommendedResponseId = topChoice.id,
            explanationPoints = explanations,
            scoreBreakdowns = scoreBreakdowns,
            isCloseCall = isCloseCall,
            closeCallNote = closeCallNote,
            isLimitedData = hasIncompleteData,
            limitedDataNote = if (hasIncompleteData) "Recommendation precision is limited as some transporter specifications are incomplete." else null
        )
    }

    private fun calculateDistanceScore(res: TransporterResponse): Int {
        val km = res.distanceKm
        return when {
            km <= 5 -> 25
            km <= 10 -> 22
            km <= 20 -> 18
            km <= 50 -> 14
            else -> 10
        }
    }

    private fun calculateCapacityScore(req: TransportRequest, res: TransporterResponse): Int {
        val reqTons = if (req.weightUnit.lowercase().contains("ton")) req.weight
        else if (req.weightUnit.lowercase().contains("quintal")) req.weight * 0.1
        else req.weight / 1000.0 // kg

        val vehicleTons = res.capacityTons
        return when {
            vehicleTons >= reqTons && vehicleTons <= reqTons * 2 -> 25
            vehicleTons > reqTons * 2 -> 20 // excess capacity
            vehicleTons >= reqTons * 0.9 -> 18 // slightly under
            else -> 10
        }
    }
}
