package com.example.budgetcontrol.core.data.local.database.dao

import androidx.room.*
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {

    @Query("SELECT * FROM banks ORDER BY isDefault DESC, name ASC")
    fun getAllBanks(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks WHERE isFavorite = 1 ORDER BY isDefault DESC, name ASC")
    fun getFavoriteBanks(): Flow<List<BankEntity>>

    @Query("SELECT COUNT(*) FROM banks")
    suspend fun getBankCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: BankEntity)

    @Update
    suspend fun updateBank(bank: BankEntity)

    @Delete
    suspend fun deleteBank(bank: BankEntity)

    @Query("DELETE FROM banks")
    suspend fun deleteAllBanks()
}

