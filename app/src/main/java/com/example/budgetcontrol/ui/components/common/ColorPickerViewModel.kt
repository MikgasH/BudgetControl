package com.example.budgetcontrol.ui.components.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ColorPickerViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val customColors: StateFlow<List<String>> = preferencesManager.customColorsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    fun addCustomColor(hex: String) {
        viewModelScope.launch { preferencesManager.addCustomColor(hex) }
    }
}
