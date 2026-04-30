package com.example.budgetcontrol.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.ui.components.common.CreateCategoryBottomSheet
import com.example.budgetcontrol.ui.components.common.CreateEditAccountBottomSheet
import com.example.budgetcontrol.ui.components.common.CurrencyChangeConfirmDialog
import com.example.budgetcontrol.ui.util.LocalWindowWidthSizeClass
import com.example.budgetcontrol.ui.util.displayName

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
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryLimits by viewModel.categoryLimits.collectAsState()
    val categoryLimitProgress by viewModel.categoryLimitProgress.collectAsState()

    var showBanksSheet by remember { mutableStateOf(false) }
    var showCurrenciesSheet by remember { mutableStateOf(false) }
    var showCategoriesSheet by remember { mutableStateOf(false) }
    var pendingCategoryType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<Bank?>(null) }
    var initialBankName by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        val sections: List<@Composable () -> Unit> = listOf(
            {
                AppearanceSection(
                    currentTheme = uiState.currentTheme,
                    onThemeChange = viewModel::setTheme
                )
            },
            {
                LanguageSection(
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = viewModel::setLanguage
                )
            },
            {
                AccountsSection(
                    accountCount = uiState.accounts.size,
                    onManageClick = { viewModel.showCreateAccountSheet() },
                    onAccountClick = { accountId -> viewModel.showEditAccountSheet(accountId) },
                    accounts = uiState.accounts,
                    baseCurrency = baseCurrency
                )
            },
            {
                CategoriesSection(
                    expenseCount = categories.count { it.type == CategoryType.EXPENSE },
                    incomeCount = categories.count { it.type == CategoryType.INCOME },
                    onManageClick = { showCategoriesSheet = true }
                )
            },
            {
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
            },
            {
                BanksSection(
                    banks = banks,
                    onManageClick = { showBanksSheet = true }
                )
            },
            {
                CurrenciesSection(
                    count = favoriteCurrencies.size,
                    isLoading = uiState.isCurrenciesLoading,
                    onManageClick = { showCurrenciesSheet = true }
                )
            },
            {
                Card(modifier = Modifier.fillMaxWidth()) {
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
        )

        val isExpanded = LocalWindowWidthSizeClass.current == WindowWidthSizeClass.Expanded
        if (isExpanded) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sections.size) { index -> sections[index]() }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                sections.forEach { section -> section() }
            }
        }
    }

    // ── Categories bottom sheet ─────────────────────────────────────────
    if (showCategoriesSheet) {
        CategoriesBottomSheet(
            categories = categories,
            onDismiss = { showCategoriesSheet = false },
            onCreate = { type ->
                pendingCategoryType = type
                viewModel.showCreateCategorySheet()
            },
            onEdit = { category -> viewModel.showEditCategorySheet(category.id) },
            onDelete = viewModel::deleteCategory,
            onResetDefaults = viewModel::resetCategoriesToDefaults,
            limits = categoryLimits,
            limitProgressMap = categoryLimitProgress,
            baseCurrency = baseCurrency
        )
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

    // Account create/edit sheet
    if (uiState.showCreateEditAccountSheet) {
        val editingAccount = viewModel.getEditingAccount()
        CreateEditAccountBottomSheet(
            isEditMode = editingAccount != null,
            account = editingAccount,
            baseCurrency = baseCurrency,
            transactionCount = uiState.editingAccountTransactionCount,
            availableCurrencies = uiState.allCurrencies,
            favoriteCurrencies = favoriteCurrencies,
            isCurrenciesLoading = uiState.isCurrenciesLoading,
            onSave = { name, iconName, color, initialBalance, currency ->
                if (editingAccount != null) {
                    viewModel.updateAccount(name, iconName, color, initialBalance, currency)
                } else {
                    viewModel.createAccount(name, iconName, color, initialBalance, currency)
                }
            },
            onDelete = if (editingAccount != null && !editingAccount.isDefault) {
                { viewModel.deleteAccount(editingAccount.id) }
            } else null,
            onDismiss = { viewModel.dismissCreateEditAccountSheet() }
        )
    }

    // ── Category create/edit sheet ──────────────────────────────────────
    if (uiState.showCreateEditCategorySheet) {
        val editingCategory = viewModel.getEditingCategory()
        if (editingCategory != null) {
            CreateCategoryBottomSheet(
                categoryType = editingCategory.type,
                initialName = editingCategory.displayName(),
                initialIconName = editingCategory.iconName,
                initialColor = editingCategory.color,
                initialLimitAmount = categoryLimits[editingCategory.id]?.amount,
                baseCurrency = baseCurrency,
                isEditMode = true,
                onSave = { name, iconName, color, _, limitAmount ->
                    viewModel.updateCategory(name, iconName, color, limitAmount)
                },
                onDismiss = { viewModel.dismissCategorySheet() }
            )
        } else {
            CreateCategoryBottomSheet(
                categoryType = pendingCategoryType,
                baseCurrency = baseCurrency,
                isEditMode = false,
                onSave = { name, iconName, color, type, limitAmount ->
                    viewModel.createCategory(name, iconName, color, type, limitAmount)
                },
                onDismiss = { viewModel.dismissCategorySheet() }
            )
        }
    }

    uiState.pendingCurrencyChange?.let { pending ->
        CurrencyChangeConfirmDialog(
            pending = pending,
            onConfirm = { viewModel.confirmPendingCurrencyChange() },
            onDismiss = { viewModel.cancelPendingCurrencyChange() }
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
