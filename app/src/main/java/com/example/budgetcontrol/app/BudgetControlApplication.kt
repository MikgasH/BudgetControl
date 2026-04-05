package com.example.budgetcontrol.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.repository.AccountRepository
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
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeDefaultCategories()
        migrateInitialBalanceToDefaultAccount()
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

    private fun migrateInitialBalanceToDefaultAccount() {
        applicationScope.launch {
            val account = accountRepository.getAccountById(Account.DEFAULT_ACCOUNT_ID)
                ?: return@launch
            // Only migrate once: if the default account still has initialBalance=0
            // and DataStore has a non-zero value, copy it over
            if (account.initialBalance == 0.0) {
                val dsBalance = preferencesManager.initialBalanceFlow.first()
                val dsCurrency = preferencesManager.baseCurrencyFlow.first()
                if (dsBalance != 0.0 || account.currency != dsCurrency) {
                    accountRepository.updateAccount(
                        account.copy(
                            initialBalance = dsBalance,
                            currency = dsCurrency
                        )
                    )
                }
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
