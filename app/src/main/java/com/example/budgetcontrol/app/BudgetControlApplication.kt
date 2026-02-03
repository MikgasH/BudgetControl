package com.example.budgetcontrol.app

import android.app.Application
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeDefaultCategories()
    }

    private fun initializeDefaultCategories() {
        applicationScope.launch {
            val categories = categoryRepository.getAllCategories().first()
            if (categories.isEmpty()) {
                categoryRepository.initializeDefaultCategories()
            }
        }
    }
}