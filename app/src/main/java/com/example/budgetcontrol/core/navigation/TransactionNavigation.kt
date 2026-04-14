package com.example.budgetcontrol.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.TransactionDetailScreen
import com.example.budgetcontrol.feature.transaction.common.TransactionFormMode
import com.example.budgetcontrol.feature.transaction.common.TransactionFormScreen
import com.example.budgetcontrol.feature.transaction.common.TransactionsByCategoryScreen
import com.example.budgetcontrol.feature.transaction.list.ExpensesScreen

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

        TransactionFormScreen(
            transactionType = TransactionType.EXPENSE,
            mode = TransactionFormMode.ADD,
            initialDate = selectedDate,
            preSelectedCategoryId = categoryId,
            preSelectedAccountId = accountId,
            onBackClick = { navController.popBackStack() },
            onSuccess = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.AddExpense.route,
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) {
        TransactionFormScreen(
            transactionType = TransactionType.EXPENSE,
            mode = TransactionFormMode.ADD,
            initialDate = System.currentTimeMillis(),
            onBackClick = { navController.popBackStack() },
            onSuccess = { navController.popBackStack() }
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

        TransactionFormScreen(
            transactionType = TransactionType.INCOME,
            mode = TransactionFormMode.ADD,
            initialDate = selectedDate,
            preSelectedAccountId = accountId,
            onBackClick = { navController.popBackStack() },
            onSuccess = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.AddIncome.route,
        enterTransition = { slideInFromBottom + fadeIn },
        exitTransition = { slideOutToBottom + fadeOut },
        popEnterTransition = { slideInFromBottom + fadeIn },
        popExitTransition = { slideOutToBottom + fadeOut }
    ) {
        TransactionFormScreen(
            transactionType = TransactionType.INCOME,
            mode = TransactionFormMode.ADD,
            initialDate = System.currentTimeMillis(),
            onBackClick = { navController.popBackStack() },
            onSuccess = { navController.popBackStack() }
        )
    }

    composable("${Screen.ExpensesByCategory.route}/{categoryId}?startDate={startDate}&endDate={endDate}&isAllTime={isAllTime}&accountId={accountId}") { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        val startDate = backStackEntry.arguments?.getString("startDate")?.toLongOrNull()
        val endDate = backStackEntry.arguments?.getString("endDate")?.toLongOrNull()
        val isAllTime = backStackEntry.arguments?.getString("isAllTime")?.toBooleanStrictOrNull() ?: false
        val accountId = backStackEntry.arguments?.getString("accountId")?.takeIf { it != "{accountId}" }
        TransactionsByCategoryScreen(
            categoryId = categoryId,
            transactionType = TransactionType.EXPENSE,
            startDate = if (isAllTime) null else startDate,
            endDate = if (isAllTime) null else endDate,
            accountId = accountId,
            onBackClick = { navController.popBackStack() },
            onTransactionClick = { expenseId ->
                navController.navigate("${Screen.ExpenseDetail.route}/$expenseId")
            },
            onAddTransactionClick = { selectedDate ->
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
        TransactionDetailScreen(
            transactionId = expenseId,
            transactionType = TransactionType.EXPENSE,
            onBackClick = { navController.popBackStack() },
            onEditClick = { id ->
                navController.navigate("${Screen.EditExpense.route}/$id")
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
        TransactionFormScreen(
            transactionType = TransactionType.EXPENSE,
            mode = TransactionFormMode.EDIT,
            transactionId = expenseId,
            onBackClick = { navController.popBackStack() },
            onSuccess = {
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
        TransactionDetailScreen(
            transactionId = incomeId,
            transactionType = TransactionType.INCOME,
            onBackClick = { navController.popBackStack() },
            onEditClick = { id ->
                navController.navigate("${Screen.EditIncome.route}/$id")
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
        TransactionFormScreen(
            transactionType = TransactionType.INCOME,
            mode = TransactionFormMode.EDIT,
            transactionId = incomeId,
            onBackClick = { navController.popBackStack() },
            onSuccess = {
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
        TransactionsByCategoryScreen(
            categoryId = categoryId,
            transactionType = TransactionType.INCOME,
            startDate = if (isAllTime) null else startDate,
            endDate = if (isAllTime) null else endDate,
            accountId = accountId,
            onBackClick = { navController.popBackStack() },
            onTransactionClick = { incomeId ->
                navController.navigate("${Screen.IncomeDetail.route}/$incomeId")
            },
            onAddTransactionClick = { selectedDate ->
                navController.navigate("${Screen.AddIncome.route}?selectedDate=$selectedDate")
            }
        )
    }
}
