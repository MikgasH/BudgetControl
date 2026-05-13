package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import kotlinx.coroutines.flow.Flow

interface CurrencyExchangeRepository {
    fun getAllExchanges(): Flow<List<CurrencyExchange>>
    suspend fun getLatestExchangeForCurrency(fromCurrency: String, toCurrency: String): CurrencyExchange?
    fun getExchangesForCurrency(fromCurrency: String, toCurrency: String): Flow<List<CurrencyExchange>>
    suspend fun insertExchange(exchange: CurrencyExchange)
    suspend fun deleteExchange(id: String)
}
