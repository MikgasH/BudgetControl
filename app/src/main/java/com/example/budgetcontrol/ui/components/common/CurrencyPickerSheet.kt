package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.example.budgetcontrol.R
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Compact "code + arrow" trigger. Tapping opens a search-enabled ModalBottomSheet
 * that groups currencies as: base (default) → favorites → all others.
 *
 * Height matches an unfocused OutlinedTextField (56.dp) so the selector sits flush
 * next to one on the same row. Width comes from the caller via [modifier].
 */
@Composable
fun CompactCurrencySelector(
    selectedCurrency: String,
    onCurrencySelect: (String) -> Unit,
    currencies: List<String>,
    baseCurrency: String,
    favoriteCurrencies: Set<String>,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        onClick = { showSheet = true },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedCurrency,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSheet) {
        CurrencyPickerBottomSheet(
            currencies = currencies,
            baseCurrency = baseCurrency,
            favoriteCurrencies = favoriteCurrencies,
            onSelect = {
                onCurrencySelect(it)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    currencies: List<String>,
    baseCurrency: String,
    favoriteCurrencies: Set<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val appLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    var query by remember { mutableStateOf("") }

    val filtered = remember(currencies, query, appLocale) {
        if (query.isBlank()) currencies
        else currencies.filter { code ->
            code.contains(query, ignoreCase = true) ||
                getCurrencyDisplayName(code, appLocale).contains(query, ignoreCase = true)
        }
    }
    val baseInFiltered = baseCurrency in filtered
    val favorites = filtered.filter { it != baseCurrency && it in favoriteCurrencies }
    val others = filtered.filter { it != baseCurrency && it !in favoriteCurrencies }

    fun pick(code: String) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onSelect(code)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_currencies)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                if (baseInFiltered) {
                    item(key = "base-$baseCurrency") {
                        CurrencyPickerRow(
                            code = baseCurrency,
                            displayName = getCurrencyDisplayName(baseCurrency, appLocale),
                            isBase = true,
                            onClick = { pick(baseCurrency) }
                        )
                    }
                }
                if (favorites.isNotEmpty()) {
                    item(key = "fav-header") {
                        CurrencyPickerSectionHeader(stringResource(R.string.favorite_currencies_header))
                    }
                    items(favorites, key = { "fav-$it" }) { code ->
                        CurrencyPickerRow(
                            code = code,
                            displayName = getCurrencyDisplayName(code, appLocale),
                            onClick = { pick(code) }
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "all-header") {
                        CurrencyPickerSectionHeader(stringResource(R.string.all_currencies_header))
                    }
                    items(others, key = { "all-$it" }) { code ->
                        CurrencyPickerRow(
                            code = code,
                            displayName = getCurrencyDisplayName(code, appLocale),
                            onClick = { pick(code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyPickerSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun CurrencyPickerRow(
    code: String,
    displayName: String,
    onClick: () -> Unit,
    isBase: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            fontWeight = if (isBase) FontWeight.Bold else FontWeight.Medium,
            fontSize = 16.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = displayName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isBase) {
                Text(
                    text = stringResource(R.string.default_label),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getCurrencyDisplayName(code: String, locale: Locale): String =
    try { Currency.getInstance(code).getDisplayName(locale) }
    catch (_: IllegalArgumentException) { code }
