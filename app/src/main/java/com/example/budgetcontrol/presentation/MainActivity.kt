package com.example.budgetcontrol.presentation

import android.annotation.SuppressLint
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.app.Activity.OVERRIDE_TRANSITION_OPEN
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.navigation.AppNavigation
import com.example.budgetcontrol.core.theme.BudgetControlTheme
import com.example.budgetcontrol.ui.util.LocalWindowWidthSizeClass
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by preferencesManager.themeFlow.collectAsState(initial = "system")
            val onboardingCompleted by preferencesManager.onboardingCompletedFlow.collectAsState(initial = null)
            val windowSizeClass = calculateWindowSizeClass(this)

            BudgetControlTheme(
                darkTheme = when (theme) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
            ) {
                CompositionLocalProvider(
                    LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass
                ) {
                    val completed = onboardingCompleted
                    if (completed != null) {
                        AppNavigation(onboardingCompleted = completed)
                    }
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    override fun recreate() {
        // Smooth crossfade instead of the default jarring flash
        // when the Activity recreates (language or locale change).
        // Constants are inlined ints on older APIs; applyCrossfade() guards the actual API 34 call.
        applyCrossfade(OVERRIDE_TRANSITION_CLOSE)
        super.recreate()
        applyCrossfade(OVERRIDE_TRANSITION_OPEN)
    }

    private fun applyCrossfade(overrideType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(overrideType, R.anim.fade_in, R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}
