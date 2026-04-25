package com.example.budgetcontrol.core.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgetcontrol.core.data.local.database.entities.CategoryLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryLimitDao {

    @Query("SELECT * FROM category_limits")
    fun getAllLimits(): Flow<List<CategoryLimitEntity>>

    @Query("SELECT * FROM category_limits WHERE categoryId = :categoryId LIMIT 1")
    fun getLimit(categoryId: String): Flow<CategoryLimitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: CategoryLimitEntity)

    @Query("DELETE FROM category_limits WHERE categoryId = :categoryId")
    suspend fun deleteLimit(categoryId: String)
}
