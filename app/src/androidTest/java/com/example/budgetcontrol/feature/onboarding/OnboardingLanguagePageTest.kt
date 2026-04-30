package com.example.budgetcontrol.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.budgetcontrol.core.theme.BudgetControlTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Smoke test for the first onboarding step. Renders the LanguagePage composable directly with
 * fake state — no Hilt, no Activity, no ViewModel — to keep this test fast and isolated from
 * the wider DI graph.
 */
class OnboardingLanguagePageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun languagePage_showsBothLanguageOptions() {
        composeTestRule.setContent {
            BudgetControlTheme {
                LanguagePage(
                    selectedLanguage = "",
                    onLanguageSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        composeTestRule.onNodeWithText("Русский").assertIsDisplayed()
    }

    @Test
    fun languagePage_clickingEnglish_invokesCallbackWithEn() {
        var selected by mutableStateOf("")

        composeTestRule.setContent {
            BudgetControlTheme {
                LanguagePage(
                    selectedLanguage = selected,
                    onLanguageSelected = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithText("English").performClick()

        assertEquals("en", selected)
    }
}
