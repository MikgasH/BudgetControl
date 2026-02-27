package com.example.budgetcontrol.core.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val FAVORITE_CURRENCIES_KEY = stringSetPreferencesKey("favorite_currencies")
        val DEFAULT_FAVORITE_CURRENCIES = setOf("EUR", "USD", "GBP", "PLN", "BYN")
    }
}
