package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryStatistic

fun <T> calculateCategoryStatistics(
    items: List<T>,
    getAmount: (T) -> Double,
    getCategoryId: (T) -> String,
    categories: List<Category>
): List<CategoryStatistic> {
    val totalAmount = items.sumOf(getAmount)
    // Avoid division by zero in percentage calc; also hides the pie chart when there's no data
    if (totalAmount == 0.0) return emptyList()

    val itemsByCategory = items.groupBy(getCategoryId)

    return categories.mapNotNull { category ->
        val categoryItems = itemsByCategory[category.id] ?: emptyList()
        if (categoryItems.isEmpty()) return@mapNotNull null

        val categoryTotal = categoryItems.sumOf(getAmount)
        val percentage = ((categoryTotal / totalAmount) * 100).toFloat()

        CategoryStatistic(
            category = category,
            totalAmount = categoryTotal,
            percentage = percentage,
            transactionCount = categoryItems.size
        )
    }.sortedByDescending { it.totalAmount }
}
