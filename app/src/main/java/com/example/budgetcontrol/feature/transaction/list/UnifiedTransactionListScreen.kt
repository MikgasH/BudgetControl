package com.example.budgetcontrol.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.util.AMOUNT_FORMAT
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
            groups = uiState.accountGroups,
            selectedAccountId = uiState.selectedAccountId,
            selectedGroupId = uiState.selectedGroupId,
            onAccountSelect = { id ->
                if (id == null) viewModel.clearAccountFilter() else viewModel.setAccount(id)
                showAccountSheet = false
            },
            onGroupSelect = { id ->
                viewModel.setGroup(id)
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showCategorySheet) {
        CategoryFilterBottomSheet(
            categories = uiState.categories,
            selectedCategoryIds = uiState.selectedCategoryIds,
            onCategoryToggle = { id -> viewModel.toggleCategory(id) },
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
                val selectedGroup = uiState.accountGroups.find { it.id == uiState.selectedGroupId }
                val selectedAccount = uiState.accounts.find { it.account.id == uiState.selectedAccountId }
                val accountChipLabel = when {
                    selectedGroup != null -> selectedGroup.name
                    selectedAccount != null -> selectedAccount.account.name
                    else -> stringResource(R.string.all_accounts)
                }
                FilterChip(
                    selected = uiState.selectedAccountId != null || uiState.selectedGroupId != null,
                    onClick = { showAccountSheet = true },
                    label = { Text(accountChipLabel) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (selectedGroup != null) Icons.Default.FolderOpen
                                else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                val selectedCategories = uiState.categories.filter { it.id in uiState.selectedCategoryIds }
                val categoryChipLabel = when {
                    selectedCategories.isEmpty() -> stringResource(R.string.all_categories)
                    selectedCategories.size == 1 -> selectedCategories.first().displayName()
                    else -> stringResource(R.string.filter_n_categories, selectedCategories.size)
                }
                FilterChip(
                    selected = uiState.selectedCategoryIds.isNotEmpty(),
                    onClick = { showCategorySheet = true },
                    label = { Text(categoryChipLabel) },
                    leadingIcon = {
                        val firstSelected = selectedCategories.firstOrNull()
                        if (firstSelected != null) {
                            Icon(
                                imageVector = getCategoryIcon(firstSelected.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = try {
                                    Color(android.graphics.Color.parseColor(firstSelected.color))
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

            if (uiState.selectedCategoryIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.categories
                        .filter { it.id in uiState.selectedCategoryIds }
                        .forEach { category ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(category.color))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            InputChip(
                                selected = true,
                                onClick = { viewModel.toggleCategory(category.id) },
                                label = { Text(category.displayName()) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(category.iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = color
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.delete_action),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                }
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
    groups: List<AccountGroup>,
    selectedAccountId: String?,
    selectedGroupId: String?,
    onAccountSelect: (String?) -> Unit,
    onGroupSelect: (String) -> Unit,
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
                trailingContent = if (selectedAccountId == null && selectedGroupId == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                modifier = Modifier.clickable { onAccountSelect(null) }
            )

            HorizontalDivider()

            if (groups.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.account_groups_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )

                groups.forEach { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.group_member_count, group.memberAccountIds.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingContent = if (selectedGroupId == group.id) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        modifier = Modifier.clickable { onGroupSelect(group.id) }
                    )
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.accounts_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }

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
    selectedCategoryIds: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val displayNames = categories.associateBy({ it.id }, { it.displayName() })

    val (expenseCategories, incomeCategories) = remember(categories, searchQuery, displayNames) {
        val filtered = if (searchQuery.isBlank()) categories
            else categories.filter {
                (displayNames[it.id] ?: it.name).contains(searchQuery, ignoreCase = true)
            }
        val expenses = filtered.filter { it.type == CategoryType.EXPENSE }
        val expenseNames = expenses.mapTo(mutableSetOf()) { displayNames[it.id] ?: it.name }
        val incomes = filtered
            .filter { it.type == CategoryType.INCOME }
            .filter { (displayNames[it.id] ?: it.name) !in expenseNames }
        expenses to incomes
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
                if (expenseCategories.isNotEmpty()) {
                    item(key = "header_expense", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.expense_tab),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(expenseCategories, key = { "expense_${it.id}" }) { category ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(category.color))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        CategoryFilterItem(
                            name = displayNames[category.id] ?: category.name,
                            icon = getCategoryIcon(category.iconName),
                            color = color,
                            isSelected = category.id in selectedCategoryIds,
                            onClick = { onCategoryToggle(category.id) }
                        )
                    }
                }

                if (incomeCategories.isNotEmpty()) {
                    item(key = "header_income", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.income_tab),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(incomeCategories, key = { "income_${it.id}" }) { category ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(category.color))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        CategoryFilterItem(
                            name = displayNames[category.id] ?: category.name,
                            icon = getCategoryIcon(category.iconName),
                            color = color,
                            isSelected = category.id in selectedCategoryIds,
                            onClick = { onCategoryToggle(category.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.done))
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
        String.format(Locale.US, "$AMOUNT_FORMAT %s", balance, currency)
    }
}
