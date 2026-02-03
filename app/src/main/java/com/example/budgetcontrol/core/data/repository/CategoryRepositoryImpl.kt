package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.dao.CategoryDao
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.data.mapper.toEntity
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { it.toDomain() }
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return getAllCategories().map { categories ->
            categories.filter { it.type == type }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories.map { it.toEntity() })
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun deleteCategoryById(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    override suspend fun initializeDefaultCategories() {
        // Категории расходов
        val expenseCategories = listOf(
            Category(
                id = UUID.randomUUID().toString(),
                name = "Продукты",
                iconName = "shopping_cart",
                color = "#4CAF50",
                isDefault = true,
                type = CategoryType.EXPENSE
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Транспорт",
                iconName = "directions_car",
                color = "#2196F3",
                isDefault = true,
                type = CategoryType.EXPENSE
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Развлечения",
                iconName = "movie",
                color = "#FF9800",
                isDefault = true,
                type = CategoryType.EXPENSE
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Здоровье",
                iconName = "local_hospital",
                color = "#F44336",
                isDefault = true,
                type = CategoryType.EXPENSE
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Дом",
                iconName = "home",
                color = "#9C27B0",
                isDefault = true,
                type = CategoryType.EXPENSE
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Подписки",
                iconName = "subscriptions",
                color = "#607D8B",
                isDefault = true,
                type = CategoryType.EXPENSE
            )
        )

        // Категории доходов
        val incomeCategories = listOf(
            Category(
                id = UUID.randomUUID().toString(),
                name = "Зарплата",
                iconName = "work",
                color = "#29B6F6",
                isDefault = true,
                type = CategoryType.INCOME
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Фриланс",
                iconName = "computer",
                color = "#26C6DA",
                isDefault = true,
                type = CategoryType.INCOME
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Инвестиции",
                iconName = "trending_up",
                color = "#66BB6A",
                isDefault = true,
                type = CategoryType.INCOME
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Подарки",
                iconName = "card_giftcard",
                color = "#EF5350",
                isDefault = true,
                type = CategoryType.INCOME
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Продажа",
                iconName = "sell",
                color = "#FFA726",
                isDefault = true,
                type = CategoryType.INCOME
            )
        )

        insertCategories(expenseCategories + incomeCategories)
    }
}