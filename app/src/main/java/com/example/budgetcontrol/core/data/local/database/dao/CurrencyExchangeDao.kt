package com.example.budgetcontrol.core.data.local.database.dao

import androidx.room.*
import com.example.budgetcontrol.core.data.local.database.entities.CurrencyExchangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyExchangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchange(entity: CurrencyExchangeEntity)

    @Query("SELECT * FROM currency_exchanges ORDER BY date DESC")
    fun getAllExchanges(): Flow<List<CurrencyExchangeEntity>>

    @Query("SELECT * FROM currency_exchanges WHERE fromCurrency = :fromCurrency AND toCurrency = :toCurrency ORDER BY date DESC LIMIT 1")
    suspend fun getLatestExchangeForCurrency(fromCurrency: String, toCurrency: String): CurrencyExchangeEntity?

    @Query("DELETE FROM currency_exchanges WHERE id = :id")
    suspend fun deleteExchange(id: String)
}
