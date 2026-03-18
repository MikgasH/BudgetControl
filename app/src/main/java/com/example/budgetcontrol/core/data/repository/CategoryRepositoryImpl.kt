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
        return categoryDao.getCategoriesByType(type.name).map { it.toDomain() }
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

    override suspend fun incrementUsageCount(id: String) {
        categoryDao.incrementUsageCount(id)
    }

    override suspend fun initializeDefaultCategories() {
        val expenseCategories = listOf(
            Category(id = UUID.randomUUID().toString(), name = "Groceries", iconName = "shopping_cart", color = "#4CAF50", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_groceries", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Transport", iconName = "directions_car", color = "#2196F3", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_transport", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Entertainment", iconName = "movie", color = "#FF9800", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_entertainment", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Health", iconName = "local_hospital", color = "#F44336", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_health", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Home", iconName = "home", color = "#9C27B0", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_home", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Subscriptions", iconName = "subscriptions", color = "#607D8B", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_subscriptions", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Restaurants", iconName = "restaurant", color = "#E91E63", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_restaurants", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Clothing", iconName = "checkroom", color = "#795548", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_clothing", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Education", iconName = "school", color = "#3F51B5", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_education", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Gifts", iconName = "card_giftcard", color = "#E91E63", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_gifts", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Travel", iconName = "flight", color = "#00BCD4", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_travel", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Beauty", iconName = "spa", color = "#FF4081", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_beauty", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Pets", iconName = "pets", color = "#8D6E63", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_pets", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Sport", iconName = "fitness_center", color = "#4CAF50", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_sport", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Electronics", iconName = "devices", color = "#455A64", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_electronics", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Other", iconName = "more_horiz", color = "#9E9E9E", isDefault = true, type = CategoryType.EXPENSE, nameKey = "cat_other_expense", isSystem = true)
        )

        val incomeCategories = listOf(
            Category(id = UUID.randomUUID().toString(), name = "Salary", iconName = "work", color = "#29B6F6", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_salary", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Freelance", iconName = "computer", color = "#26C6DA", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_freelance", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Investments", iconName = "trending_up", color = "#66BB6A", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_investments", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Gifts", iconName = "card_giftcard", color = "#EF5350", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_gifts_income", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Sales", iconName = "sell", color = "#FFA726", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_sales", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Rental income", iconName = "apartment", color = "#AB47BC", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_rental", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Refund", iconName = "replay", color = "#78909C", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_refund", isSystem = true),
            Category(id = UUID.randomUUID().toString(), name = "Other", iconName = "more_horiz", color = "#9E9E9E", isDefault = true, type = CategoryType.INCOME, nameKey = "cat_other_income", isSystem = true)
        )

        insertCategories(expenseCategories + incomeCategories)
    }
}