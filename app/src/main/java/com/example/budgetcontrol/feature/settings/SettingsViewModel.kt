package com.example.budgetcontrol.feature.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.usecase.SyncDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val cloudExpensesCount: Int = -1,
    val currentOperation: String? = null,
    val currentLanguage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncDataUseCase: SyncDataUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCloudExpensesCount()
        observeLanguage()
    }

    private fun observeLanguage() {
        viewModelScope.launch {
            preferencesManager.languageFlow.collect { tag ->
                _uiState.value = _uiState.value.copy(currentLanguage = tag)
            }
        }
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(tag)
            val locales = if (tag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    private fun loadCloudExpensesCount() {
        viewModelScope.launch {
            try {
                val count = syncDataUseCase.getCloudExpensesCount()
                _uiState.value = _uiState.value.copy(cloudExpensesCount = count)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(cloudExpensesCount = 0)
            }
        }
    }

    fun backupToCloud() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                message = null,
                currentOperation = "backup"
            )

            val result = syncDataUseCase.backupToCloud()

            result.fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = message,
                        isError = false,
                        currentOperation = null
                    )
                    loadCloudExpensesCount()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = context.getString(R.string.error_backup, exception.message ?: ""),
                        isError = true,
                        currentOperation = null
                    )
                }
            )
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                message = null,
                currentOperation = "restore"
            )

            val result = syncDataUseCase.restoreFromCloud()

            result.fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = message,
                        isError = false,
                        currentOperation = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = context.getString(R.string.error_restore, exception.message ?: ""),
                        isError = true,
                        currentOperation = null
                    )
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
