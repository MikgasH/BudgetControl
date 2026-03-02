package com.example.budgetcontrol.core.data.remote.cerps.dto

data class TrendsResponse(
    val from: String,
    val to: String,
    val period: String,
    val startRate: Double,
    val endRate: Double,
    val changePercent: Double,
    val startTimestamp: String,
    val endTimestamp: String,
    val dataPoints: Int
)
