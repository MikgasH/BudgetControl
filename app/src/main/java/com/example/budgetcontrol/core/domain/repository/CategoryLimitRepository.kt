package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.domain.model.CategoryLimit
import kotlinx.coroutines.flow.Flow

interface CategoryLimitRepository {
    fun getAllLimits(): Flow<List<CategoryLimit>>
    fun getLimit(categoryId: String): Flow<CategoryLimit?>
    suspend fun setLimit(categoryId: String, amount: Double, periodType: String = "MONTH")
    suspend fun clearLimit(categoryId: String)
}
