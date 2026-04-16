package com.example.budgetcontrol.core.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

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
    object AllTransactions : Screen("all_transactions")
}

const val ANIMATION_DURATION = 200

// Slide-in-from-right animation for forward navigation
val slideInFromRight = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

val slideOutToLeft = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

// Slide-in-from-left animation for back navigation
val slideInFromLeft = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

val slideOutToRight = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(ANIMATION_DURATION)
)

// Slide-up animation for modal screens (add/edit)
val slideInFromBottom = slideInVertically(
    initialOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(ANIMATION_DURATION)
)

val slideOutToBottom = slideOutVertically(
    targetOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(ANIMATION_DURATION)
)

// Fade animation for quick transitions
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
        enterTransition = { slideInFromRight + fadeIn },
        exitTransition = { slideOutToLeft + fadeOut },
        popEnterTransition = { slideInFromLeft + fadeIn },
        popExitTransition = { slideOutToRight + fadeOut }
    ) {
        mainNavigation(navController)
        transactionNavigation(navController)
        analyticsNavigation(navController)
        settingsNavigation(navController)
        onboardingNavigation(navController)
    }
}
