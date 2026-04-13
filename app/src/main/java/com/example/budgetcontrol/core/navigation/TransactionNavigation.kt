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

    // Route variant that accepts a pre-selected date, optional categoryId, and optional accountId
    composable(
        route = "${Screen.AddExpense.route}?selectedDate={selectedDate}&categoryId={categoryId}&accountId={accountId}",
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) { backStackEntry ->
        val selectedDateString = backStackEntry.arguments?.getString("selectedDate")
        val selectedDate = selectedDateString?.toLongOrNull() ?: System.currentTimeMillis()
        val categoryId = backStackEntry.arguments?.getString("categoryId")?.takeIf { it != "{categoryId}" }
        val accountId = backStackEntry.arguments?.getString("accountId")?.takeIf { it != "{accountId}" }

        AddExpenseScreen(
            selectedDate = selectedDate,
            preSelectedCategoryId = categoryId,
            preSelectedAccountId = accountId,
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

    // Route variant that accepts a pre-selected date and optional accountId
    composable(
        route = "${Screen.AddIncome.route}?selectedDate={selectedDate}&accountId={accountId}",
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) { backStackEntry ->
        val selectedDateString = backStackEntry.arguments?.getString("selectedDate")
        val selectedDate = selectedDateString?.toLongOrNull() ?: System.currentTimeMillis()
        val accountId = backStackEntry.arguments?.getString("accountId")?.takeIf { it != "{accountId}" }

        AddIncomeScreen(
            selectedDate = selectedDate,
            preSelectedAccountId = accountId,
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

    composable("${Screen.ExpensesByCategory.route}/{categoryId}?startDate={startDate}&endDate={endDate}&isAllTime={isAllTime}&accountId={accountId}") { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        val startDate = backStackEntry.arguments?.getString("startDate")?.toLongOrNull()
        val endDate = backStackEntry.arguments?.getString("endDate")?.toLongOrNull()
        val isAllTime = backStackEntry.arguments?.getString("isAllTime")?.toBooleanStrictOrNull() ?: false
        val accountId = backStackEntry.arguments?.getString("accountId")?.takeIf { it != "{accountId}" }
        ExpensesByCategoryScreen(
            categoryId = categoryId,
            startDate = if (isAllTime) null else startDate,
            endDate = if (isAllTime) null else endDate,
            accountId = accountId,
            onBackClick = { navController.popBackStack() },
            onExpenseClick = { expenseId ->
                navController.navigate("${Screen.ExpenseDetail.route}/$expenseId")
            },
            onAddExpenseClick = { selectedDate ->
                navController.navigate("${Screen.AddExpense.route}?selectedDate=$selectedDate")
            }
        )
    }

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

    composable("${Screen.IncomesByCategory.route}/{categoryId}?startDate={startDate}&endDate={endDate}&isAllTime={isAllTime}&accountId={accountId}") { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        val startDate = backStackEntry.arguments?.getString("startDate")?.toLongOrNull()
        val endDate = backStackEntry.arguments?.getString("endDate")?.toLongOrNull()
        val isAllTime = backStackEntry.arguments?.getString("isAllTime")?.toBooleanStrictOrNull() ?: false
        val accountId = backStackEntry.arguments?.getString("accountId")?.takeIf { it != "{accountId}" }
        IncomesByCategoryScreen(
            categoryId = categoryId,
            startDate = if (isAllTime) null else startDate,
            endDate = if (isAllTime) null else endDate,
            accountId = accountId,
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
