package com.example.budgetcontrol.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity

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
    val baseCurrency by viewModel.baseCurrency.collectAsState()

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

            AppearanceSection(
                currentTheme = uiState.currentTheme,
                onThemeChange = viewModel::setTheme
            )

            LanguageSection(
                currentLanguage = uiState.currentLanguage,
                onLanguageChange = viewModel::setLanguage
            )

            BalanceSection(
                totalBalance = totalBalance,
                baseCurrency = baseCurrency,
                onEditClick = { showBalanceDialog = true }
            )

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

            BanksSection(
                banks = banks,
                onManageClick = { showBanksSheet = true }
            )

            CurrenciesSection(
                count = favoriteCurrencies.size,
                isLoading = uiState.isCurrenciesLoading,
                onManageClick = { showCurrenciesSheet = true }
            )

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
            baseCurrency = baseCurrency,
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
        BalanceEditDialog(
            totalBalance = totalBalance,
            baseCurrency = baseCurrency,
            onSave = { value ->
                viewModel.setTotalBalance(value)
                showBalanceDialog = false
            },
            onDismiss = { showBalanceDialog = false }
        )
    }

    // Reset confirmation dialog
    if (showResetConfirm) {
        ResetConfirmDialog(
            onConfirm = {
                viewModel.resetBanksToDefaults()
                showResetConfirm = false
            },
            onDismiss = { showResetConfirm = false }
        )
    }
}
