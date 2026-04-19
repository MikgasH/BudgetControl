package com.example.budgetcontrol.core.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
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
    private val gson = Gson()

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
        preferences[BASE_CURRENCY_KEY] ?: DEFAULT_BASE_CURRENCY
    }

    suspend fun setBaseCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[BASE_CURRENCY_KEY] = currency
        }
    }

    // Last payment method (CARD/CASH)
    val lastPaymentMethodFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_PAYMENT_METHOD_KEY] ?: "CARD"
    }

    suspend fun setLastPaymentMethod(method: String) {
        dataStore.edit { preferences ->
            preferences[LAST_PAYMENT_METHOD_KEY] = method
        }
    }

    // Selected account on main screen (for pre-selecting in transaction forms)
    val selectedAccountIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SELECTED_ACCOUNT_ID_KEY]
    }

    suspend fun setSelectedAccountId(accountId: String?) {
        dataStore.edit { preferences ->
            if (accountId != null) {
                preferences[SELECTED_ACCOUNT_ID_KEY] = accountId
            } else {
                preferences.remove(SELECTED_ACCOUNT_ID_KEY)
            }
        }
    }

    // Selected group on main screen
    val selectedGroupIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SELECTED_GROUP_ID_KEY]
    }

    suspend fun setSelectedGroupId(groupId: String?) {
        dataStore.edit { preferences ->
            if (groupId != null) {
                preferences[SELECTED_GROUP_ID_KEY] = groupId
            } else {
                preferences.remove(SELECTED_GROUP_ID_KEY)
            }
        }
    }

    // Cached exchange rates
    suspend fun saveLastRates(rates: Map<String, Double>, timestamp: Long) {
        val json = gson.toJson(rates)
        dataStore.edit { preferences ->
            preferences[LAST_RATES_KEY] = json
            preferences[LAST_RATES_TIMESTAMP_KEY] = timestamp
        }
    }

    fun getLastRates(): Flow<Map<String, Double>> = dataStore.data.map { preferences ->
        val json = preferences[LAST_RATES_KEY] ?: return@map emptyMap()
        try {
            // Anonymous TypeToken subclass captures the generic type at runtime (Gson's type erasure workaround)
            val type = object : TypeToken<Map<String, Double>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getLastRatesTimestamp(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_RATES_TIMESTAMP_KEY] ?: 0L
    }

    // Custom (recently-used) picker colors — ordered list, most recent first, max 12
    val customColorsFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        val json = preferences[CUSTOM_COLORS_KEY] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addCustomColor(hex: String) {
        dataStore.edit { preferences ->
            val json = preferences[CUSTOM_COLORS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<String>>() {}.type
            val list: MutableList<String> = try {
                gson.fromJson(json, type)
            } catch (_: Exception) {
                mutableListOf()
            }
            list.remove(hex)
            list.add(0, hex)
            if (list.size > 12) list.removeAt(list.lastIndex)
            preferences[CUSTOM_COLORS_KEY] = gson.toJson(list)
        }
    }

    // Cached available currencies list (for offline mode)
    suspend fun saveAvailableCurrencies(currencies: List<String>) {
        val json = gson.toJson(currencies)
        dataStore.edit { preferences ->
            preferences[AVAILABLE_CURRENCIES_KEY] = json
        }
    }

    fun getAvailableCurrencies(): Flow<List<String>> = dataStore.data.map { preferences ->
        val json = preferences[AVAILABLE_CURRENCIES_KEY] ?: return@map DEFAULT_AVAILABLE_CURRENCIES
        try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            DEFAULT_AVAILABLE_CURRENCIES
        }
    }

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val FAVORITE_CURRENCIES_KEY = stringSetPreferencesKey("favorite_currencies")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val INITIAL_BALANCE_KEY = stringPreferencesKey("initial_balance")
        private val BASE_CURRENCY_KEY = stringPreferencesKey("base_currency")
        private val LAST_PAYMENT_METHOD_KEY = stringPreferencesKey("last_payment_method")
        private val SELECTED_ACCOUNT_ID_KEY = stringPreferencesKey("selected_account_id")
        private val SELECTED_GROUP_ID_KEY = stringPreferencesKey("selected_group_id")
        private val LAST_RATES_KEY = stringPreferencesKey("last_exchange_rates")
        private val LAST_RATES_TIMESTAMP_KEY = longPreferencesKey("last_exchange_rates_timestamp")
        private val AVAILABLE_CURRENCIES_KEY = stringPreferencesKey("available_currencies")
        private val CUSTOM_COLORS_KEY = stringPreferencesKey("custom_colors")
        val DEFAULT_FAVORITE_CURRENCIES = setOf("EUR", "USD", "GBP", "PLN", "BYN")
        val DEFAULT_AVAILABLE_CURRENCIES = listOf(
            "EUR", "USD", "GBP", "CHF", "JPY", "PLN", "CZK",
            "HUF", "BYN", "UAH", "GEL", "SEK", "NOK", "DKK",
            "TRY", "CAD", "AUD", "NZD"
        )
    }
}
