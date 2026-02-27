package com.example.budgetcontrol.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category

private val nameKeyToResId = mapOf(
    // Expense categories
    "cat_groceries" to R.string.cat_groceries,
    "cat_transport" to R.string.cat_transport,
    "cat_entertainment" to R.string.cat_entertainment,
    "cat_health" to R.string.cat_health,
    "cat_home" to R.string.cat_home,
    "cat_subscriptions" to R.string.cat_subscriptions,
    "cat_restaurants" to R.string.cat_restaurants,
    "cat_clothing" to R.string.cat_clothing,
    "cat_education" to R.string.cat_education,
    "cat_gifts" to R.string.cat_gifts,
    "cat_travel" to R.string.cat_travel,
    "cat_beauty" to R.string.cat_beauty,
    "cat_pets" to R.string.cat_pets,
    "cat_sport" to R.string.cat_sport,
    "cat_electronics" to R.string.cat_electronics,
    "cat_other_expense" to R.string.cat_other_expense,
    // Income categories
    "cat_salary" to R.string.cat_salary,
    "cat_freelance" to R.string.cat_freelance,
    "cat_investments" to R.string.cat_investments,
    "cat_gifts_income" to R.string.cat_gifts_income,
    "cat_sales" to R.string.cat_sales,
    "cat_rental" to R.string.cat_rental,
    "cat_refund" to R.string.cat_refund,
    "cat_other_income" to R.string.cat_other_income
)

@Composable
fun Category.displayName(): String {
    if (nameKey != null) {
        val resId = nameKeyToResId[nameKey]
        if (resId != null) return stringResource(resId)
    }
    return name
}
