package com.example.budgetcontrol.core.domain.repository

sealed class CurrencyRateResult {
    data class Success(val rates: Map<String, Double>) : CurrencyRateResult()
    data class Error(val message: String) : CurrencyRateResult()
}

interface CurrencyRateProvider {
    suspend fun getRates(): CurrencyRateResult
    fun areRatesStale(): Boolean
}
