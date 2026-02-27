package com.example.budgetcontrol.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.navigation.AppNavigation
import com.example.budgetcontrol.core.theme.BudgetControlTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by preferencesManager.themeFlow.collectAsState(initial = "system")
            val onboardingCompleted by preferencesManager.onboardingCompletedFlow.collectAsState(initial = null)

            BudgetControlTheme(
                darkTheme = when (theme) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
            ) {
                val completed = onboardingCompleted
                if (completed != null) {
                    AppNavigation(onboardingCompleted = completed)
                }
            }
        }
    }
}
