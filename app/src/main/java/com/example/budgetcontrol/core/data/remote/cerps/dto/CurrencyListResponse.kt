package com.example.budgetcontrol.core.data.remote.cerps.dto

data class CurrencyListResponse(
    val currencies: List<CurrencyInfo>
)

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String?
)