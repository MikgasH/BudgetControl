package com.example.budgetcontrol.core.data.remote.cerps.dto

data class TrendsResponse(
    val from: String,
    val to: String,
    val period: String,
    val oldRate: Double,
    val newRate: Double,
    val changePercentage: Double,
    val startDate: String,
    val endDate: String,
    val dataPoints: Int,
    val points: List<RatePoint>
)

data class RatePoint(
    val timestamp: String,
    val rate: Double
)
