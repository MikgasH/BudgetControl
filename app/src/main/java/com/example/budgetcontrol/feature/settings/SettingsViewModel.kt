package com.example.budgetcontrol.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.usecase.SyncDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val currentOperation: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncDataUseCase: SyncDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCloudExpensesCount()
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
                    // Обновляем счетчик
                    loadCloudExpensesCount()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Ошибка backup: ${exception.message}",
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
                        message = "Ошибка восстановления: ${exception.message}",
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