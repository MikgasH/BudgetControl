package com.example.budgetcontrol.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BudgetControlApplication : Application() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeDefaultCategories()
        restoreSavedLocale()
    }

    private fun initializeDefaultCategories() {
        applicationScope.launch {
            val categories = categoryRepository.getAllCategories().first()
            if (categories.isEmpty()) {
                categoryRepository.initializeDefaultCategories()
            }
        }
    }

    private fun restoreSavedLocale() {
        applicationScope.launch {
            val tag = preferencesManager.languageFlow.first()
            if (tag.isNotEmpty()) {
                val locales = LocaleListCompat.forLanguageTags(tag)
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }
}
