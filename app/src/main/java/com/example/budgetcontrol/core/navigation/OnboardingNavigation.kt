package com.example.budgetcontrol.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.budgetcontrol.feature.onboarding.OnboardingScreen

fun NavGraphBuilder.onboardingNavigation(navController: NavHostController) {
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
