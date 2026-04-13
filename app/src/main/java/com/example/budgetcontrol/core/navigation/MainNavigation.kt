package com.example.budgetcontrol.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.budgetcontrol.feature.main.MainScreen
import com.example.budgetcontrol.feature.main.OperationType

fun NavGraphBuilder.mainNavigation(navController: NavHostController) {
    composable(Screen.Main.route) {
        MainScreen(
            onAddExpenseClick = { selectedDate, accountId ->
                val accountParam = if (accountId != null) "&accountId=$accountId" else ""
                navController.navigate("${Screen.AddExpense.route}?selectedDate=$selectedDate$accountParam")
            },
            onAddIncomeClick = { selectedDate, accountId ->
                val accountParam = if (accountId != null) "&accountId=$accountId" else ""
                navController.navigate("${Screen.AddIncome.route}?selectedDate=$selectedDate$accountParam")
            },
            onAddExpenseWithCategory = { selectedDate, categoryId, accountId ->
                val accountParam = if (accountId != null) "&accountId=$accountId" else ""
                navController.navigate("${Screen.AddExpense.route}?selectedDate=$selectedDate&categoryId=$categoryId$accountParam")
            },
            onCategoryClick = { categoryId, operationType, startDate, endDate, isAllTime, accountId ->
                val accountParam = if (accountId != null) "&accountId=$accountId" else ""
                when (operationType) {
                    OperationType.INCOMES -> {
                        navController.navigate("${Screen.IncomesByCategory.route}/$categoryId?startDate=$startDate&endDate=$endDate&isAllTime=$isAllTime$accountParam")
                    }
                    OperationType.EXPENSES -> {
                        navController.navigate("${Screen.ExpensesByCategory.route}/$categoryId?startDate=$startDate&endDate=$endDate&isAllTime=$isAllTime$accountParam")
                    }
                }
            },
            onSettingsClick = {
                navController.navigate(Screen.Settings.route)
            },
            onRateHistoryClick = {
                navController.navigate(Screen.RateHistory.route)
            }
        )
    }
}
