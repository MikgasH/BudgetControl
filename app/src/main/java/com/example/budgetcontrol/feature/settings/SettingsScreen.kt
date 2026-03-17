package com.example.budgetcontrol.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onCurrencyExchangesClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val favoriteCurrencies by viewModel.favoriteCurrencies.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()

    var showBanksSheet by remember { mutableStateOf(false) }
    var showCurrenciesSheet by remember { mutableStateOf(false) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<BankEntity?>(null) }
    var initialBankName by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Appearance selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.appearance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val themes = listOf(
                        "system" to stringResource(R.string.theme_system),
                        "light" to stringResource(R.string.theme_light),
                        "dark" to stringResource(R.string.theme_dark)
                    )

                    val currentThemeName = themes.firstOrNull { it.first == uiState.currentTheme }?.second
                        ?: stringResource(R.string.theme_system)

                    var themeExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = !themeExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentThemeName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            themes.forEach { (tag, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.setTheme(tag)
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Language selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val languages = listOf(
                        "" to stringResource(R.string.language_system),
                        "en" to stringResource(R.string.language_english),
                        "ru" to stringResource(R.string.language_russian)
                    )

                    val currentName = languages.firstOrNull { it.first == uiState.currentLanguage }?.second
                        ?: stringResource(R.string.language_system)

                    var langExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = langExpanded,
                        onExpandedChange = { langExpanded = !langExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false }
                        ) {
                            languages.forEach { (tag, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.setLanguage(tag)
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Initial balance — clickable row
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { showBalanceDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.current_balance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalBalance)} EUR",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Currency exchanges
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.currency_exchanges),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.exchange_rate_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = onCurrencyExchangesClick) {
                        Text(stringResource(R.string.manage))
                    }
                }
            }

            // Banks — collapsed summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.banks_and_commissions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val favoriteCount = banks.count { it.isFavorite }
                        Text(
                            text = stringResource(R.string.banks_selected_count, favoriteCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { showBanksSheet = true }) {
                        Text(stringResource(R.string.manage))
                    }
                }
            }

            // Favourite currencies — collapsed summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.favourite_currencies),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.currencies_selected_count, favoriteCurrencies.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCurrenciesSheet = true },
                        enabled = !uiState.isCurrenciesLoading
                    ) {
                        if (uiState.isCurrenciesLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.manage))
                        }
                    }
                }
            }

            // About section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_app),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = stringResource(R.string.app_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ── Banks bottom sheet ──────────────────────────────────────────────
    if (showBanksSheet) {
        BanksBottomSheet(
            banks = banks,
            onDismiss = { showBanksSheet = false },
            onToggleFavorite = viewModel::toggleFavorite,
            onSetDefault = viewModel::setDefaultBank,
            onEdit = { bank ->
                editingBank = bank
                showAddEditDialog = true
            },
            onDelete = viewModel::deleteBank,
            onAddBank = { name ->
                editingBank = null
                initialBankName = name
                showAddEditDialog = true
            },
            onResetDefaults = { showResetConfirm = true }
        )
    }

    // ── Currencies bottom sheet ─────────────────────────────────────────
    if (showCurrenciesSheet) {
        CurrenciesBottomSheet(
            allCurrencies = uiState.allCurrencies,
            favoriteCurrencies = favoriteCurrencies,
            onToggleFavorite = viewModel::toggleFavoriteCurrency,
            onDismiss = { showCurrenciesSheet = false }
        )
    }

    // Add/Edit bank dialog
    if (showAddEditDialog) {
        val lookupState by viewModel.commissionLookupState.collectAsState()

        AddEditBankDialog(
            bank = editingBank,
            initialName = initialBankName,
            lookupState = lookupState,
            onLookup = { bankName -> viewModel.lookupBankCommission(bankName) },
            onDismiss = {
                showAddEditDialog = false
                editingBank = null
                initialBankName = null
                viewModel.resetCommissionLookup()
            },
            onConfirm = { name, commission ->
                val existing = editingBank
                if (existing != null) {
                    viewModel.updateBank(
                        existing.copy(name = name, commissionPercent = commission)
                    )
                } else {
                    viewModel.addBank(name, commission)
                }
                showAddEditDialog = false
                editingBank = null
                initialBankName = null
                viewModel.resetCommissionLookup()
            }
        )
    }

    // Total balance dialog
    if (showBalanceDialog) {
        var balanceText by remember(totalBalance) {
            mutableStateOf(String.format(Locale.US, "%.2f", totalBalance))
        }

        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text(stringResource(R.string.current_balance)) },
            text = {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("EUR") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = balanceText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    viewModel.setTotalBalance(value)
                    showBalanceDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceDialog = false }) {
                    Text(stringResource(R.string.cancel_upper))
                }
            }
        )
    }

    // Reset confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_banks_to_defaults)) },
            text = { Text(stringResource(R.string.reset_banks_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetBanksToDefaults()
                    showResetConfirm = false
                }) {
                    Text(stringResource(R.string.yes_upper))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.no_upper))
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Banks BottomSheet
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BanksBottomSheet(
    banks: List<BankEntity>,
    onDismiss: () -> Unit,
    onToggleFavorite: (BankEntity) -> Unit,
    onSetDefault: (BankEntity) -> Unit,
    onEdit: (BankEntity) -> Unit,
    onDelete: (BankEntity) -> Unit,
    onAddBank: (String?) -> Unit,
    onResetDefaults: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val minOneRequiredMsg = stringResource(R.string.bank_min_one_required)

    val filteredBanks = remember(banks, searchQuery) {
        val filtered = if (searchQuery.isBlank()) banks
        else banks.filter { it.name.contains(searchQuery, ignoreCase = true) }
        // Sort: favorites first, then non-favorites
        val favorites = filtered.filter { it.isFavorite }
        val nonFavorites = filtered.filter { !it.isFavorite }
        favorites + nonFavorites
    }

    val favoriteCount = remember(filteredBanks) { filteredBanks.count { it.isFavorite } }
    val nonFavoriteCount = remember(filteredBanks) { filteredBanks.count { !it.isFavorite } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.banks_and_commissions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Hint lines
                Text(
                    text = stringResource(R.string.bank_checkbox_hint),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.bank_star_hint),
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_banks)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bank list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (filteredBanks.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.no_banks_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredBanks, key = { it.id }) { bank ->
                            BankRow(
                                bank = bank,
                                onToggleFavorite = {
                                    // Prevent unchecking the last favorite
                                    if (bank.isFavorite && banks.count { it.isFavorite } <= 1) {
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(minOneRequiredMsg)
                                        }
                                    } else {
                                        onToggleFavorite(bank)
                                    }
                                },
                                onSetDefault = { onSetDefault(bank) },
                                onEdit = { onEdit(bank) },
                                onDelete = { onDelete(bank) }
                            )

                            // Divider between favorite and non-favorite groups
                            if (bank.isFavorite && favoriteCount > 0 && nonFavoriteCount > 0) {
                                val currentIndex = filteredBanks.indexOf(bank)
                                val nextBank = filteredBanks.getOrNull(currentIndex + 1)
                                if (nextBank != null && !nextBank.isFavorite) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add bank button
                OutlinedButton(
                    onClick = {
                        val prefill = searchQuery.trim().takeIf { it.isNotBlank() }
                        onAddBank(prefill)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_bank))
                }

                // Reset to defaults
                TextButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.reset_banks_to_defaults),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun BankRow(
    bank: BankEntity,
    onToggleFavorite: () -> Unit,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = bank.isFavorite,
            onCheckedChange = { onToggleFavorite() },
            modifier = Modifier.size(36.dp)
        )

        IconButton(
            onClick = onSetDefault,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (bank.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = stringResource(R.string.default_bank),
                tint = if (bank.isDefault) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = bank.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formatCommission(bank.commissionPercent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_bank),
                modifier = Modifier.size(18.dp)
            )
        }

        if (!bank.isDefault) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_action),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Currencies BottomSheet
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrenciesBottomSheet(
    allCurrencies: List<String>,
    favoriteCurrencies: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val baseCurrency = "EUR"

    val filteredCurrencies = remember(allCurrencies, searchQuery) {
        if (searchQuery.isBlank()) allCurrencies
        else allCurrencies.filter { code ->
            code.contains(searchQuery, ignoreCase = true) ||
                    getCurrencyName(code).contains(searchQuery, ignoreCase = true)
        }
    }

    // EUR always first, then the rest in server order
    val sortedCurrencies = remember(filteredCurrencies, baseCurrency) {
        val base = filteredCurrencies.filter { it == baseCurrency }
        val rest = filteredCurrencies.filter { it != baseCurrency }
        base + rest
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.favourite_currencies),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = stringResource(R.string.favourite_currencies_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_currencies)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Currency list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (sortedCurrencies.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_currencies_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    items(sortedCurrencies, key = { it }) { code ->
                        val isBase = code == baseCurrency
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isBase || favoriteCurrencies.contains(code),
                                onCheckedChange = {
                                    if (!isBase) onToggleFavorite(code)
                                },
                                enabled = !isBase,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = code,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(52.dp)
                            )
                            Text(
                                text = getCurrencyName(code),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (isBase) {
                                Text(
                                    text = stringResource(R.string.default_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Helpers & Dialogs
// ═══════════════════════════════════════════════════════════════════════

private fun getCurrencyName(code: String): String =
    try { Currency.getInstance(code).displayName }
    catch (_: IllegalArgumentException) { code }

private fun formatCommission(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        "${value.toLong()}%"
    } else {
        "$value%"
    }
}

@Composable
private fun AddEditBankDialog(
    bank: BankEntity?,
    initialName: String? = null,
    lookupState: LookupState,
    onLookup: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(bank?.name ?: initialName ?: "") }
    var commission by remember {
        mutableStateOf(
            bank?.let { formatCommission(it.commissionPercent).dropLast(1) } ?: ""
        )
    }
    var nameError by remember { mutableStateOf(false) }

    // Fill commission when AI lookup succeeds
    LaunchedEffect(lookupState) {
        if (lookupState is LookupState.Success) {
            val v = lookupState.value
            commission = if (v == v.toLong().toDouble()) {
                "${v.toLong()}"
            } else {
                "$v"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (bank != null) R.string.edit_bank else R.string.add_bank
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.bank_name_label)) },
                    placeholder = { Text(stringResource(R.string.bank_name_placeholder)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.bank_name_required)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = commission,
                    onValueChange = { commission = it },
                    label = { Text(stringResource(R.string.commission_percent_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = when (lookupState) {
                        is LookupState.Success -> {
                            {
                                Text(
                                    stringResource(R.string.ai_estimate_hint),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        is LookupState.NotFound -> {
                            {
                                Text(
                                    stringResource(R.string.not_found_hint),
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                        is LookupState.Error -> {
                            {
                                Text(
                                    lookupState.message ?: stringResource(R.string.lookup_error_hint),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // AI lookup button
                OutlinedButton(
                    onClick = { onLookup(name) },
                    enabled = name.isNotBlank() && lookupState !is LookupState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (lookupState is LookupState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.find_with_ai))
                    } else {
                        Text("\uD83E\uDD16 " + stringResource(R.string.find_with_ai))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                    return@TextButton
                }
                val commissionVal = commission.toDoubleOrNull() ?: 0.0
                if (commissionVal < 0) return@TextButton
                onConfirm(name.trim(), commissionVal)
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_upper))
            }
        }
    )
}

