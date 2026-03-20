package com.example.budgetcontrol.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.budgetcontrol.feature.transaction.add.AddExpenseScreen
import com.example.budgetcontrol.feature.transaction.add.AddIncomeScreen
import com.example.budgetcontrol.feature.transaction.detail.ExpenseDetailScreen
import com.example.budgetcontrol.feature.transaction.detail.IncomeDetailScreen
import com.example.budgetcontrol.feature.transaction.edit.EditExpenseScreen
import com.example.budgetcontrol.feature.transaction.edit.EditIncomeScreen
import com.example.budgetcontrol.feature.transaction.list.ExpensesByCategoryScreen
import com.example.budgetcontrol.feature.transaction.list.ExpensesScreen
import com.example.budgetcontrol.feature.transaction.list.IncomesByCategoryScreen

fun NavGraphBuilder.transactionNavigation(navController: NavHostController) {
    composable(Screen.Expenses.route) {
        ExpensesScreen(
            onAddExpenseClick = {
                navController.navigate(Screen.AddExpense.route)
            },
            onStatisticsClick = {
                navController.navigate(Screen.Statistics.route)
            },
            onExpenseClick = { transaction ->
                navController.navigate("${Screen.ExpenseDetail.route}/${transaction.id}")
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
    }

    // Добавление расхода с выбранной датой
    composable(
        route = "${Screen.AddExpense.route}?selectedDate={selectedDate}",
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) { backStackEntry ->
        val selectedDateString = backStackEntry.arguments?.getString("selectedDate")
        val selectedDate = selectedDateString?.toLongOrNull() ?: System.currentTimeMillis()

        AddExpenseScreen(
            selectedDate = selectedDate,
            onBackClick = {
                navController.popBackStack()
            },
            onExpenseAdded = {
                navController.popBackStack()
            }
        )
    }

    composable(
        route = Screen.AddExpense.route,
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) {
        AddExpenseScreen(
            selectedDate = System.currentTimeMillis(),
            onBackClick = {
                navController.popBackStack()
            },
            onExpenseAdded = {
                navController.popBackStack()
            }
        )
    }

    // Добавление дохода с выбранной датой
    composable(
        route = "${Screen.AddIncome.route}?selectedDate={selectedDate}",
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) { backStackEntry ->
        val selectedDateString = backStackEntry.arguments?.getString("selectedDate")
        val selectedDate = selectedDateString?.toLongOrNull() ?: System.currentTimeMillis()

        AddIncomeScreen(
            selectedDate = selectedDate,
            onBackClick = {
                navController.popBackStack()
            },
            onIncomeAdded = {
                navController.popBackStack()
            }
        )
    }

    composable(
        route = Screen.AddIncome.route,
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) {
        AddIncomeScreen(
            selectedDate = System.currentTimeMillis(),
            onBackClick = {
                navController.popBackStack()
            },
            onIncomeAdded = {
                navController.popBackStack()
            }
        )
    }

    // Расходы по категории
    composable("${Screen.ExpensesByCategory.route}/{categoryId}?startDate={startDate}&endDate={endDate}&isAllTime={isAllTime}") { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        val startDate = backStackEntry.arguments?.getString("startDate")?.toLongOrNull()
        val endDate = backStackEntry.arguments?.getString("endDate")?.toLongOrNull()
        val isAllTime = backStackEntry.arguments?.getString("isAllTime")?.toBooleanStrictOrNull() ?: false
        ExpensesByCategoryScreen(
            categoryId = categoryId,
            startDate = if (isAllTime) null else startDate,
            endDate = if (isAllTime) null else endDate,
            onBackClick = { navController.popBackStack() },
            onExpenseClick = { expenseId ->
                navController.navigate("${Screen.ExpenseDetail.route}/$expenseId")
            },
            onAddExpenseClick = { selectedDate ->
                navController.navigate("${Screen.AddExpense.route}?selectedDate=$selectedDate")
            }
        )
    }

    // Детали расхода
    composable(
        route = "${Screen.ExpenseDetail.route}/{expenseId}",
        enterTransition = { fadeIn },
        exitTransition = { fadeOut },
        popEnterTransition = { fadeIn },
        popExitTransition = { fadeOut }
    ) { backStackEntry ->
        val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
        ExpenseDetailScreen(
            expenseId = expenseId,
            onBackClick = { navController.popBackStack() },
            onEditClick = { expenseId ->
                navController.navigate("${Screen.EditExpense.route}/$expenseId")
            },
            onDeleteSuccess = {
                navController.popBackStack(Screen.Main.route, false)
            }
        )
    }

    // Редактирование расхода
    composable(
        route = "${Screen.EditExpense.route}/{expenseId}",
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) { backStackEntry ->
        val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
        EditExpenseScreen(
            expenseId = expenseId,
            onBackClick = { navController.popBackStack() },
            onSaveSuccess = {
                navController.popBackStack()
                navController.popBackStack()
            }
        )
    }

    // Детали дохода
    composable(
        route = "${Screen.IncomeDetail.route}/{incomeId}",
        enterTransition = { fadeIn },
        exitTransition = { fadeOut },
        popEnterTransition = { fadeIn },
        popExitTransition = { fadeOut }
    ) { backStackEntry ->
        val incomeId = backStackEntry.arguments?.getString("incomeId") ?: ""
        IncomeDetailScreen(
            incomeId = incomeId,
            onBackClick = { navController.popBackStack() },
            onEditClick = { incomeId ->
                navController.navigate("${Screen.EditIncome.route}/$incomeId")
            },
            onDeleteSuccess = {
                navController.popBackStack(Screen.Main.route, false)
            }
        )
    }

    // Редактирование дохода
    composable(
        route = "${Screen.EditIncome.route}/{incomeId}",
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) { backStackEntry ->
        val incomeId = backStackEntry.arguments?.getString("incomeId") ?: ""
        EditIncomeScreen(
            incomeId = incomeId,
            onBackClick = { navController.popBackStack() },
            onSaveSuccess = {
                navController.popBackStack()
                navController.popBackStack()
            }
        )
    }

    // Доходы по категории
    composable("${Screen.IncomesByCategory.route}/{categoryId}?startDate={startDate}&endDate={endDate}&isAllTime={isAllTime}") { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        val startDate = backStackEntry.arguments?.getString("startDate")?.toLongOrNull()
        val endDate = backStackEntry.arguments?.getString("endDate")?.toLongOrNull()
        val isAllTime = backStackEntry.arguments?.getString("isAllTime")?.toBooleanStrictOrNull() ?: false
        IncomesByCategoryScreen(
            categoryId = categoryId,
            startDate = if (isAllTime) null else startDate,
            endDate = if (isAllTime) null else endDate,
            onBackClick = { navController.popBackStack() },
            onIncomeClick = { incomeId ->
                navController.navigate("${Screen.IncomeDetail.route}/$incomeId")
            },
            onAddIncomeClick = { selectedDate ->
                navController.navigate("${Screen.AddIncome.route}?selectedDate=$selectedDate")
            }
        )
    }
}
