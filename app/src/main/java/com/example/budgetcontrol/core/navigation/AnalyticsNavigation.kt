package com.example.budgetcontrol.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.budgetcontrol.feature.analytics.RateHistoryScreen
import com.example.budgetcontrol.feature.analytics.StatisticsScreen

fun NavGraphBuilder.analyticsNavigation(navController: NavHostController) {
    composable(Screen.Statistics.route) {
        StatisticsScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }

    composable(Screen.RateHistory.route) {
        RateHistoryScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}
