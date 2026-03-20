package com.example.budgetcontrol.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.budgetcontrol.feature.main.MainScreen
import com.example.budgetcontrol.feature.main.OperationType

fun NavGraphBuilder.mainNavigation(navController: NavHostController) {
    composable(Screen.Main.route) {
        MainScreen(
            onAddExpenseClick = { selectedDate ->
                navController.navigate("${Screen.AddExpense.route}?selectedDate=$selectedDate")
            },
            onAddIncomeClick = { selectedDate ->
                navController.navigate("${Screen.AddIncome.route}?selectedDate=$selectedDate")
            },
            onCategoryClick = { categoryId, operationType, startDate, endDate, isAllTime ->
                when (operationType) {
                    OperationType.INCOMES -> {
                        navController.navigate("${Screen.IncomesByCategory.route}/$categoryId?startDate=$startDate&endDate=$endDate&isAllTime=$isAllTime")
                    }
                    OperationType.EXPENSES -> {
                        navController.navigate("${Screen.ExpensesByCategory.route}/$categoryId?startDate=$startDate&endDate=$endDate&isAllTime=$isAllTime")
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
