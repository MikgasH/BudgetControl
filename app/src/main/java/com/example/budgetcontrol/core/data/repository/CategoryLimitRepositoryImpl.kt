package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.dao.CategoryLimitDao
import com.example.budgetcontrol.core.data.local.database.entities.CategoryLimitEntity
import com.example.budgetcontrol.core.data.mapper.toCategoryLimitDomain
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.domain.model.CategoryLimit
import com.example.budgetcontrol.core.domain.repository.CategoryLimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryLimitRepositoryImpl @Inject constructor(
    private val categoryLimitDao: CategoryLimitDao
) : CategoryLimitRepository {

    override fun getAllLimits(): Flow<List<CategoryLimit>> =
        categoryLimitDao.getAllLimits().map { it.toCategoryLimitDomain() }

    override fun getLimit(categoryId: String): Flow<CategoryLimit?> =
        categoryLimitDao.getLimit(categoryId).map { it?.toDomain() }

    override suspend fun setLimit(categoryId: String, amount: Double, periodType: String) {
        val now = System.currentTimeMillis()
        categoryLimitDao.upsertLimit(
            CategoryLimitEntity(
                categoryId = categoryId,
                amount = amount,
                periodType = periodType,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun clearLimit(categoryId: String) {
        categoryLimitDao.deleteLimit(categoryId)
    }
}
