package com.example.budgetcontrol.core.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.budgetcontrol.feature.transaction.add.AddExpenseScreen
import com.example.budgetcontrol.feature.transaction.add.AddIncomeScreen
import com.example.budgetcontrol.feature.transaction.edit.EditExpenseScreen
import com.example.budgetcontrol.feature.transaction.edit.EditIncomeScreen
import com.example.budgetcontrol.feature.transaction.list.ExpensesScreen
import com.example.budgetcontrol.feature.transaction.list.ExpensesByCategoryScreen
import com.example.budgetcontrol.feature.transaction.detail.ExpenseDetailScreen
import com.example.budgetcontrol.feature.transaction.detail.IncomeDetailScreen
import com.example.budgetcontrol.feature.transaction.list.IncomesByCategoryScreen
import com.example.budgetcontrol.feature.main.MainScreen
import com.example.budgetcontrol.feature.main.OperationType
import com.example.budgetcontrol.feature.analytics.RateHistoryScreen
import com.example.budgetcontrol.feature.analytics.StatisticsScreen
import com.example.budgetcontrol.feature.onboarding.OnboardingScreen
import com.example.budgetcontrol.feature.settings.CurrencyExchangeScreen
import com.example.budgetcontrol.feature.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object Expenses : Screen("expenses")
    object AddExpense : Screen("add_expense")
    object AddIncome : Screen("add_income")
    object Statistics : Screen("statistics")
    object ExpenseDetail : Screen("expense_detail")
    object ExpensesByCategory : Screen("expenses_by_category")
    object EditExpense : Screen("edit_expense")
    object EditIncome : Screen("edit_income")
    object IncomeDetail : Screen("income_detail")
    object IncomesByCategory : Screen("incomes_by_category")
    object Settings : Screen("settings")
    object CurrencyExchange : Screen("currency_exchange")
    object RateHistory : Screen("rate_history")
}

const val ANIMATION_DURATION = 200

// Анимация входа слева направо (для перехода вперед)
val slideInFromRight = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

val slideOutToLeft = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

// Анимация входа справа налево (для возврата назад)
val slideInFromLeft = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

val slideOutToRight = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

// Анимация для модальных окон (добавление/редактирование)
val slideInFromBottom = slideInVertically(
    initialOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(ANIMATION_DURATION)
)

val slideOutToBottom = slideOutVertically(
    targetOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(ANIMATION_DURATION)
)

// Fade анимация для быстрых переходов
val fadeIn = fadeIn(animationSpec = tween(ANIMATION_DURATION))
val fadeOut = fadeOut(animationSpec = tween(ANIMATION_DURATION))

@Composable
fun AppNavigation(
    onboardingCompleted: Boolean = true,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = if (onboardingCompleted) Screen.Main.route else Screen.Onboarding.route,
        // Быстрые анимации по умолчанию
        enterTransition = { slideInFromRight + fadeIn },
        exitTransition = { slideOutToLeft + fadeOut },
        popEnterTransition = { slideInFromLeft + fadeIn },
        popExitTransition = { slideOutToRight + fadeOut }
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onAddExpenseClick = { selectedDate ->
                    navController.navigate("${Screen.AddExpense.route}?selectedDate=$selectedDate")
                },
                onAddIncomeClick = { selectedDate ->
                    navController.navigate("${Screen.AddIncome.route}?selectedDate=$selectedDate")
                },
                onExpensesListClick = {
                    navController.navigate(Screen.Expenses.route)
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
                },
                onTransactionClick = { transaction ->
                    when (transaction.type) {
                        com.example.budgetcontrol.core.domain.model.TransactionType.EXPENSE -> {
                            navController.navigate("${Screen.ExpenseDetail.route}/${transaction.id}")
                        }
                        com.example.budgetcontrol.core.domain.model.TransactionType.INCOME -> {
                            navController.navigate("${Screen.IncomeDetail.route}/${transaction.id}")
                        }
                    }
                }
            )
        }

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

        // Добавление расхода - ОБНОВЛЕНО для использования нового экрана
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

        // Добавление дохода - ОБНОВЛЕНО для использования нового экрана
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

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBackClick = {
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

        // Редактирование расхода - ОБНОВЛЕНО для использования нового экрана
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

        // Редактирование дохода - ОБНОВЛЕНО для использования нового экрана
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

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onCurrencyExchangesClick = {
                    navController.navigate(Screen.CurrencyExchange.route)
                }
            )
        }

        composable(Screen.CurrencyExchange.route) {
            CurrencyExchangeScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.RateHistory.route) {
            RateHistoryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeIn },
            exitTransition = { fadeOut },
            popEnterTransition = { fadeIn },
            popExitTransition = { fadeOut }
        ) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
    }
}