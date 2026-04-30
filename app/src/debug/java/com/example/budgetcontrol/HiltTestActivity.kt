package com.example.budgetcontrol

import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Empty host activity for Compose UI tests that need a Hilt-aware activity.
 * Lives in the debug source set so it ships in the app APK, sharing a process
 * with HiltTestApplication. Tests call composeTestRule.activity.setContent { ... }
 * to render content.
 */
@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity()
