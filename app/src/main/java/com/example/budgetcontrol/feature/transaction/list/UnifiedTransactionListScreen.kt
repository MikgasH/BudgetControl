package com.example.budgetcontrol.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.ui.components.common.PeriodRangePicker
import com.example.budgetcontrol.ui.components.common.TransactionItem
import com.example.budgetcontrol.ui.util.displayName
import com.example.budgetcontrol.ui.util.getCategoryIcon
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTransactionListScreen(
    onBackClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit = {},
    viewModel: UnifiedTransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()

    var showPeriodPicker by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }

    if (showPeriodPicker) {
        PeriodRangePicker(
            onPeriodSelected = { startDate, endDate ->
                viewModel.setDateRange(startDate, endDate)
                showPeriodPicker = false
            },
            onDismiss = { showPeriodPicker = false },
            onAllTimeSelected = {
                viewModel.clearDateRange()
                showPeriodPicker = false
            }
        )
    }

    if (showAccountSheet) {
        AccountFilterBottomSheet(
            accounts = uiState.accounts,
            selectedAccountId = uiState.selectedAccountId,
            onAccountSelect = { id ->
                viewModel.setAccount(id)
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showCategorySheet) {
        CategoryFilterBottomSheet(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelect = { id ->
                viewModel.setCategory(id)
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.all_transactions),
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
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Transaction type filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.transactionTypeFilter == TransactionTypeFilter.ALL,
                    onClick = { viewModel.setTransactionTypeFilter(TransactionTypeFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all_types)) }
                )
                FilterChip(
                    selected = uiState.transactionTypeFilter == TransactionTypeFilter.INCOME,
                    onClick = { viewModel.setTransactionTypeFilter(TransactionTypeFilter.INCOME) },
                    label = { Text(stringResource(R.string.income_tab)) }
                )
                FilterChip(
                    selected = uiState.transactionTypeFilter == TransactionTypeFilter.EXPENSE,
                    onClick = { viewModel.setTransactionTypeFilter(TransactionTypeFilter.EXPENSE) },
                    label = { Text(stringResource(R.string.expense_tab)) }
                )
            }

            // Account, Category, Period filter chips (horizontally scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val selectedAccount = uiState.accounts.find { it.account.id == uiState.selectedAccountId }
                FilterChip(
                    selected = uiState.selectedAccountId != null,
                    onClick = { showAccountSheet = true },
                    label = { Text(selectedAccount?.account?.name ?: stringResource(R.string.all_accounts)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                val selectedCategory = uiState.categories.find { it.id == uiState.selectedCategoryId }
                FilterChip(
                    selected = uiState.selectedCategoryId != null,
                    onClick = { showCategorySheet = true },
                    label = { Text(selectedCategory?.displayName() ?: stringResource(R.string.all_categories)) },
                    leadingIcon = {
                        if (selectedCategory != null) {
                            Icon(
                                imageVector = getCategoryIcon(selectedCategory.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = try {
                                    Color(android.graphics.Color.parseColor(selectedCategory.color))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                val periodLabel = remember(uiState.startDate, uiState.endDate) {
                    val start = uiState.startDate
                    val end = uiState.endDate
                    if (start != null && end != null) {
                        val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                        "${formatter.format(Date(start))} – ${formatter.format(Date(end))}"
                    } else null
                }
                FilterChip(
                    selected = uiState.startDate != null,
                    onClick = { showPeriodPicker = true },
                    label = { Text(periodLabel ?: stringResource(R.string.period_all_time)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            HorizontalDivider()

            // Transaction list
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.transactions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_transactions),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.transactions,
                            key = { tx -> "${tx.type}_${tx.id}" }
                        ) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                category = viewModel.getCategoryById(transaction.categoryId),
                                baseCurrency = baseCurrency,
                                onTransactionClick = { onTransactionClick(it) },
                                onDeleteClick = { viewModel.deleteTransaction(it) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFilterBottomSheet(
    accounts: List<AccountWithBalance>,
    selectedAccountId: String?,
    onAccountSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.account_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.all_accounts)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = if (selectedAccountId == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                modifier = Modifier.clickable { onAccountSelect(null) }
            )

            HorizontalDivider()

            accounts.forEach { accountWithBalance ->
                val account = accountWithBalance.account
                val iconColor = try {
                    Color(android.graphics.Color.parseColor(account.color))
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }
                ListItem(
                    headlineContent = { Text(account.name) },
                    supportingContent = {
                        Text(
                            text = formatAccountBalance(accountWithBalance.currentBalance, account.currency),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = getCategoryIcon(account.iconName),
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = if (selectedAccountId == account.id) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    modifier = Modifier.clickable { onAccountSelect(account.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterBottomSheet(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val displayNames = categories.associateBy({ it.id }, { it.displayName() })

    val filtered = remember(categories, searchQuery, displayNames) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            (displayNames[it.id] ?: it.name).contains(searchQuery, ignoreCase = true)
        }
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
                text = stringResource(R.string.all_categories),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_categories)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                if (searchQuery.isBlank()) {
                    item(key = "all_categories") {
                        CategoryFilterItem(
                            name = stringResource(R.string.all_categories),
                            icon = Icons.Default.Category,
                            color = MaterialTheme.colorScheme.primary,
                            isSelected = selectedCategoryId == null,
                            onClick = { onCategorySelect(null) }
                        )
                    }
                }
                items(filtered, key = { it.id }) { category ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(category.color))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    CategoryFilterItem(
                        name = displayNames[category.id] ?: category.name,
                        icon = getCategoryIcon(category.iconName),
                        color = color,
                        isSelected = selectedCategoryId == category.id,
                        onClick = { onCategorySelect(category.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterItem(
    name: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatAccountBalance(balance: Double, currency: String): String {
    return if (balance == balance.toLong().toDouble()) {
        "${balance.toLong()} $currency"
    } else {
        String.format(Locale.US, "%.2f %s", balance, currency)
    }
}
