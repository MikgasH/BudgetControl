package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_exchanges")
data class CurrencyExchangeEntity(
    @PrimaryKey val id: String,
    val fromAmount: Double,
    val fromCurrency: String,
    val toAmount: Double,
    val toCurrency: String,
    val exchangeRate: Double,
    val location: String? = null,
    val date: Long,
    val createdAt: Long
)
