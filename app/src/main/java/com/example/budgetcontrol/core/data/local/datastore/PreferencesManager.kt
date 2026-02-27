package com.example.budgetcontrol.core.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val languageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: ""
    }

    suspend fun setLanguage(languageTag: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageTag
        }
    }

    val favoriteCurrenciesFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[FAVORITE_CURRENCIES_KEY] ?: DEFAULT_FAVORITE_CURRENCIES
    }

    suspend fun setFavoriteCurrencies(currencies: Set<String>) {
        dataStore.edit { preferences ->
            preferences[FAVORITE_CURRENCIES_KEY] = currencies
        }
    }

    // Theme
    val themeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system"
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    // Onboarding
    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }

    // Initial balance
    val initialBalanceFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[INITIAL_BALANCE_KEY]?.toDoubleOrNull() ?: 0.0
    }

    suspend fun setInitialBalance(balance: Double) {
        dataStore.edit { preferences ->
            preferences[INITIAL_BALANCE_KEY] = balance.toString()
        }
    }

    // Base currency
    val baseCurrencyFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[BASE_CURRENCY_KEY] ?: "EUR"
    }

    suspend fun setBaseCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[BASE_CURRENCY_KEY] = currency
        }
    }

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val FAVORITE_CURRENCIES_KEY = stringSetPreferencesKey("favorite_currencies")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val INITIAL_BALANCE_KEY = stringPreferencesKey("initial_balance")
        private val BASE_CURRENCY_KEY = stringPreferencesKey("base_currency")
        val DEFAULT_FAVORITE_CURRENCIES = setOf("EUR", "USD", "GBP", "PLN", "BYN")
    }
}
