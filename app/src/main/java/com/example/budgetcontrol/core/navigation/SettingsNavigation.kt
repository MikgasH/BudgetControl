package com.example.budgetcontrol.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.budgetcontrol.feature.settings.CurrencyExchangeScreen
import com.example.budgetcontrol.feature.settings.SettingsScreen

fun NavGraphBuilder.settingsNavigation(navController: NavHostController) {
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
}
