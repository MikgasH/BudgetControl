package com.example.budgetcontrol.core.data.remote.cerps.dto

data class RatesResponse(
    val base: String,
    val rates: Map<String, Double>,
    val timestamp: String?
)
