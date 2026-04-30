package com.example.budgetcontrol.feature.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.budgetcontrol.HiltTestActivity
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.navigation.AppNavigation
import com.example.budgetcontrol.core.theme.BudgetControlTheme
import com.example.budgetcontrol.ui.components.common.AMOUNT_FIELD_TEST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that tapping the FAB on the main screen navigates to the Add Transaction
 * form. Uses the full DI graph (Hilt) with an in-memory Room DB and faked CERPS APIs so
 * no real network or persistent storage is touched.
 */
@HiltAndroidTest
class MainScreenFabNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun tappingFab_navigatesToAddTransactionScreen() {
        composeTestRule.setContent {
            BudgetControlTheme {
                AppNavigation(onboardingCompleted = true)
            }
        }

        val addOperationLabel =
            composeTestRule.activity.getString(R.string.add_operation)

        composeTestRule
            .onNodeWithContentDescription(addOperationLabel)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(AMOUNT_FIELD_TEST_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithTag(AMOUNT_FIELD_TEST_TAG).assertIsDisplayed()
    }
}
