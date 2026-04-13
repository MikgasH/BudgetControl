package com.example.budgetcontrol.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.feature.settings.LookupState
import com.example.budgetcontrol.ui.components.common.CreateEditAccountBottomSheet
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale

private const val PAGE_COUNT = 8

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val commissionLookupState by viewModel.commissionLookupState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> LanguagePage(
                        selectedLanguage = uiState.selectedLanguage,
                        onLanguageSelected = viewModel::setLanguage
                    )
                    1 -> ThemePage(
                        selectedTheme = uiState.selectedTheme,
                        onThemeSelected = viewModel::setTheme
                    )
                    2 -> CurrencyPage(
                        currencies = uiState.currencies,
                        selectedCurrency = uiState.selectedCurrency,
                        isLoading = uiState.currenciesLoading,
                        onCurrencySelected = viewModel::setCurrency
                    )
                    3 -> BanksPage(
                        banks = banks,
                        onToggleFavorite = viewModel::toggleBankFavorite,
                        onAddBank = viewModel::addBank,
                        lookupState = commissionLookupState,
                        onLookup = viewModel::lookupBankCommission,
                        onResetLookup = viewModel::resetCommissionLookup
                    )
                    4 -> FavoriteCurrenciesPage(
                        currencies = uiState.currencies,
                        favoriteCurrencies = uiState.favoriteCurrencies,
                        baseCurrency = uiState.selectedCurrency,
                        isLoading = uiState.currenciesLoading,
                        onToggleCurrency = viewModel::toggleFavoriteCurrency
                    )
                    5 -> BalancePage(
                        balance = uiState.initialBalance,
                        currency = uiState.selectedCurrency,
                        onBalanceChanged = viewModel::setInitialBalance
                    )
                    6 -> AccountsPage(
                        accounts = uiState.onboardingAccounts,
                        currency = uiState.selectedCurrency,
                        availableCurrencies = uiState.currencies,
                        favoriteCurrencies = uiState.favoriteCurrencies,
                        isCurrenciesLoading = uiState.currenciesLoading,
                        onAddAccount = viewModel::addOnboardingAccount,
                        onUpdateAccount = viewModel::updateOnboardingAccount,
                        onDeleteAccount = viewModel::removeOnboardingAccount
                    )
                    7 -> ReadyPage(
                        language = uiState.selectedLanguage,
                        theme = uiState.selectedTheme,
                        currency = uiState.selectedCurrency,
                        banksCount = banks.count { it.isFavorite },
                        favoriteCurrenciesCount = uiState.favoriteCurrencies.size,
                        accountsCount = uiState.onboardingAccounts.size
                    )
                }
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(PAGE_COUNT) { index ->
                    val color by animateColorAsState(
                        targetValue = if (index == pagerState.currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant,
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage == PAGE_COUNT - 1) {
                    // Ready page: Back + Start
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                    Button(
                        onClick = {
                            viewModel.completeOnboarding()
                            onFinish()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_start),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else if (pagerState.currentPage == PAGE_COUNT - 2) {
                    // Accounts page: Back + Skip + Next
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                    TextButton(
                        onClick = {
                            viewModel.skipAccountSetup()
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                } else if (pagerState.currentPage > 0) {
                    // Pages 1-5: Back + Next
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                } else {
                    // Page 0: Next only
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 1: Language
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LanguagePage(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LanguageCard(
                flag = "\uD83C\uDDEC\uD83C\uDDE7",
                label = "English",
                isSelected = selectedLanguage == "en",
                onClick = { onLanguageSelected("en") },
                modifier = Modifier.weight(1f)
            )
            LanguageCard(
                flag = "\uD83C\uDDF7\uD83C\uDDFA",
                label = "Русский",
                isSelected = selectedLanguage == "ru",
                onClick = { onLanguageSelected("ru") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LanguageCard(
    flag: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = flag, fontSize = 40.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 2: Theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ThemePage(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_theme_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_theme_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeCard(
                icon = Icons.Default.LightMode,
                label = stringResource(R.string.theme_light),
                isSelected = selectedTheme == "light",
                onClick = { onThemeSelected("light") },
                modifier = Modifier.weight(1f)
            )
            ThemeCard(
                icon = Icons.Default.DarkMode,
                label = stringResource(R.string.theme_dark),
                isSelected = selectedTheme == "dark",
                onClick = { onThemeSelected("dark") },
                modifier = Modifier.weight(1f)
            )
            ThemeCard(
                icon = Icons.Default.SettingsBrightness,
                label = stringResource(R.string.theme_system),
                isSelected = selectedTheme == "system",
                onClick = { onThemeSelected("system") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 3: Currency
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CurrencyPage(
    currencies: List<String>,
    selectedCurrency: String,
    isLoading: Boolean,
    onCurrencySelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(currencies, searchQuery) {
        if (searchQuery.isBlank()) currencies
        else currencies.filter { code ->
            code.contains(searchQuery, ignoreCase = true) ||
                    getCurrencyName(code).contains(searchQuery, ignoreCase = true)
        }
    }

    // Sort locale-matching currency first (e.g. PLN for pl_PL), fall back to EUR
    val sortedCurrencies = remember(filteredCurrencies) {
        val localeCurrency = try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (_: IllegalArgumentException) {
            null
        }
        val preferredCurrency = if (localeCurrency != null && filteredCurrencies.contains(localeCurrency)) {
            localeCurrency
        } else {
            "EUR"
        }
        val preferred = filteredCurrencies.filter { it == preferredCurrency }
        val rest = filteredCurrencies.filter { it != preferredCurrency }
        preferred + rest
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_currency_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_currency_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(sortedCurrencies, key = { it }) { code ->
                    val isSelected = code == selectedCurrency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCurrencySelected(code) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 3: Banks
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun BanksPage(
    banks: List<Bank>,
    onToggleFavorite: (Bank) -> Unit,
    onAddBank: (String, Double) -> Unit,
    lookupState: LookupState = LookupState.Idle,
    onLookup: (String) -> Unit = {},
    onResetLookup: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredBanks = remember(banks, searchQuery) {
        val filtered = if (searchQuery.isBlank()) banks
        else banks.filter { it.name.contains(searchQuery, ignoreCase = true) }
        val favorites = filtered.filter { it.isFavorite }
        val nonFavorites = filtered.filter { !it.isFavorite }
        favorites + nonFavorites
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_banks_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_banks_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filteredBanks, key = { it.id }) { bank ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = bank.isFavorite,
                        onCheckedChange = { onToggleFavorite(bank) },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.onboarding_add_custom_bank))
                }
            }
        }
    }

    if (showAddDialog) {
        AddBankDialog(
            lookupState = lookupState,
            onLookup = onLookup,
            onDismiss = {
                showAddDialog = false
                onResetLookup()
            },
            onConfirm = { name, commission ->
                onAddBank(name, commission)
                showAddDialog = false
                onResetLookup()
            }
        )
    }
}

@Composable
private fun AddBankDialog(
    lookupState: LookupState = LookupState.Idle,
    onLookup: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var commission by remember { mutableStateOf("") }
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
        title = { Text(stringResource(R.string.add_bank)) },
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

// ═══════════════════════════════════════════════════════════════════════
// Page 4: Favorite Currencies
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun FavoriteCurrenciesPage(
    currencies: List<String>,
    favoriteCurrencies: Set<String>,
    baseCurrency: String,
    isLoading: Boolean,
    onToggleCurrency: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(currencies, searchQuery, baseCurrency, favoriteCurrencies) {
        val list = currencies.ifEmpty { PreferencesManager.DEFAULT_AVAILABLE_CURRENCIES }
        val withoutBase = list.filter { it != baseCurrency }
        val filtered = if (searchQuery.isBlank()) withoutBase
        else withoutBase.filter { code ->
            code.contains(searchQuery, ignoreCase = true) ||
                    getCurrencyName(code).contains(searchQuery, ignoreCase = true)
        }
        val favorites = filtered.filter { favoriteCurrencies.contains(it) }
        val rest = filtered.filter { !favoriteCurrencies.contains(it) }
        favorites + rest
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_currencies_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_currencies_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredCurrencies, key = { it }) { code ->
                    val isChecked = favoriteCurrencies.contains(code)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleCurrency(code) }
                            .background(
                                if (isChecked) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleCurrency(code) },
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
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 5: Balance
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun BalancePage(
    balance: String,
    currency: String,
    onBalanceChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_balance_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_balance_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = balance,
            onValueChange = onBalanceChanged,
            label = { Text(stringResource(R.string.onboarding_balance_title)) },
            suffix = { Text(currency) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 5: Ready
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ReadyPage(
    language: String,
    theme: String,
    currency: String,
    banksCount: Int,
    favoriteCurrenciesCount: Int = 0,
    accountsCount: Int = 1
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_ready_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_ready_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                SummaryRow(
                    icon = Icons.Default.Language,
                    label = stringResource(R.string.language),
                    value = when (language) {
                        "en" -> "English"
                        "ru" -> "Русский"
                        else -> stringResource(R.string.language_system)
                    }
                )
                SummaryRow(
                    icon = Icons.Default.Palette,
                    label = stringResource(R.string.appearance),
                    value = when (theme) {
                        "light" -> stringResource(R.string.theme_light)
                        "dark" -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    }
                )
                SummaryRow(
                    icon = Icons.Default.CurrencyExchange,
                    label = stringResource(R.string.currency),
                    value = currency
                )
                SummaryRow(
                    icon = Icons.Default.AccountBalance,
                    label = stringResource(R.string.onboarding_banks_title),
                    value = stringResource(R.string.banks_selected_count, banksCount)
                )
                SummaryRow(
                    icon = Icons.Default.Payments,
                    label = stringResource(R.string.onboarding_currencies_summary),
                    value = stringResource(R.string.currencies_selected_summary, favoriteCurrenciesCount)
                )
                SummaryRow(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = stringResource(R.string.accounts_management),
                    value = stringResource(R.string.accounts_count, accountsCount)
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 7: Accounts
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun AccountsPage(
    accounts: List<Account>,
    currency: String,
    availableCurrencies: List<String> = emptyList(),
    favoriteCurrencies: Set<String> = emptySet(),
    isCurrenciesLoading: Boolean = false,
    onAddAccount: (String, String, String, Double, String) -> Unit,
    onUpdateAccount: (String, String, String, String, Double, String) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_accounts_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_accounts_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                OnboardingAccountItem(
                    account = account,
                    currency = currency,
                    onClick = { editingAccount = account }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showCreateSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.new_account))
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateEditAccountBottomSheet(
            baseCurrency = currency,
            availableCurrencies = availableCurrencies,
            favoriteCurrencies = favoriteCurrencies,
            isCurrenciesLoading = isCurrenciesLoading,
            onSave = { name, iconName, color, initialBalance, cur ->
                onAddAccount(name, iconName, color, initialBalance, cur)
                showCreateSheet = false
            },
            onDismiss = { showCreateSheet = false }
        )
    }

    editingAccount?.let { account ->
        CreateEditAccountBottomSheet(
            isEditMode = true,
            account = account,
            baseCurrency = currency,
            availableCurrencies = availableCurrencies,
            favoriteCurrencies = favoriteCurrencies,
            isCurrenciesLoading = isCurrenciesLoading,
            onSave = { name, iconName, color, initialBalance, cur ->
                onUpdateAccount(account.id, name, iconName, color, initialBalance, cur)
                editingAccount = null
            },
            onDelete = if (!account.isDefault) {
                {
                    onDeleteAccount(account.id)
                    editingAccount = null
                }
            } else null,
            onDismiss = { editingAccount = null }
        )
    }
}

@Composable
private fun OnboardingAccountItem(
    account: Account,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconColor = try {
                Color(account.color.toColorInt())
            } catch (_: Exception) {
                MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveOnboardingAccountIcon(account.iconName),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (account.isDefault) {
                    Text(
                        text = stringResource(R.string.default_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val displayCurrency = account.currency.ifBlank { currency }
            Text(
                text = "${formatAccountBalance(account.initialBalance)} $displayCurrency",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun resolveOnboardingAccountIcon(key: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (key) {
        "account_balance" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "savings" -> Icons.Default.Savings
        "payments" -> Icons.Default.Payments
        "attach_money" -> Icons.Default.AttachMoney
        "work" -> Icons.Default.Work
        "home" -> Icons.Default.Home
        "shopping_cart" -> Icons.Default.ShoppingCart
        "flight" -> Icons.Default.Flight
        "star" -> Icons.Default.Star
        "favorite" -> Icons.Default.Favorite
        "wallet" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.AccountBalance
    }
}

private fun formatAccountBalance(value: Double): String {
    return if (value == 0.0) "0"
    else if (value == value.toLong().toDouble()) value.toLong().toString()
    else value.toString()
}

// ═══════════════════════════════════════════════════════════════════════
// Helpers
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
