package com.example.budgetcontrol.feature.main

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import com.example.budgetcontrol.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import com.example.budgetcontrol.ui.components.common.PeriodNavigationCard
import com.example.budgetcontrol.ui.components.common.PeriodRangePicker
import com.example.budgetcontrol.ui.util.displayName
import androidx.core.graphics.toColorInt
import java.util.Locale
import com.example.budgetcontrol.ui.util.getCategoryIcon
import com.example.budgetcontrol.core.util.DateRangeHelper
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.BackHandler
import com.example.budgetcontrol.ui.components.common.AccountsBottomSheet
import com.example.budgetcontrol.ui.components.common.AccountGroupSheet
import com.example.budgetcontrol.ui.components.common.CreateEditAccountBottomSheet
import com.example.budgetcontrol.ui.components.common.CurrencyChangeConfirmDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onAddExpenseClick: (Long, String?) -> Unit,
    onAddIncomeClick: (Long, String?) -> Unit,
    onAddExpenseWithCategory: (Long, String, String?) -> Unit = { date, _, accountId -> onAddExpenseClick(date, accountId) },
    onCategoryClick: (categoryId: String, operationType: OperationType, startDate: Long, endDate: Long, isAllTime: Boolean, accountId: String?) -> Unit = { _, _, _, _, _, _ -> },
    onSettingsClick: () -> Unit = {},
    onRateHistoryClick: () -> Unit = {},
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val isApproximateBalance by viewModel.isApproximateBalance.collectAsState()
    val openingBalance by viewModel.openingBalance.collectAsState()
    val currentContext = LocalContext.current
    val periodDisplayText = DateRangeHelper.getPeriodDisplayText(
        context = currentContext,
        periodType = uiState.selectedPeriodType,
        periodOffset = uiState.currentPeriodIndex,
        customStartDate = uiState.customStartDate,
        customEndDate = uiState.customEndDate,
        isAllTimePeriod = uiState.isAllTimePeriod
    )
    var showPeriodPicker by remember { mutableStateOf(false) }
    var showGroupAccountPicker by remember { mutableStateOf(false) }

    if (showPeriodPicker) {
        PeriodRangePicker(
            onPeriodSelected = { startDate, endDate ->
                viewModel.selectCustomPeriod(startDate, endDate)
                showPeriodPicker = false
            },
            onDismiss = { showPeriodPicker = false },
            onAllTimeSelected = {
                viewModel.selectAllTime()
                showPeriodPicker = false
            }
        )
    }

    // Account picker when FAB pressed with a group selected
    if (showGroupAccountPicker) {
        val memberAccounts = viewModel.getGroupMemberAccounts()
        AlertDialog(
            onDismissRequest = { showGroupAccountPicker = false },
            title = { Text(stringResource(R.string.choose_account)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.choose_account_for_transaction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    memberAccounts.forEach { accountWithBalance ->
                        val account = accountWithBalance.account
                        val accountColor = try {
                            Color(account.color.toColorInt())
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showGroupAccountPicker = false
                                    val selectedDate = viewModel.getCurrentSelectedDate()
                                    when (viewModel.getCurrentSelectedOperationType()) {
                                        OperationType.EXPENSES -> onAddExpenseClick(selectedDate, account.id)
                                        OperationType.INCOMES -> onAddIncomeClick(selectedDate, account.id)
                                    }
                                },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(accountColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(account.iconName),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGroupAccountPicker = false }) {
                    Text(stringResource(R.string.cancel_upper))
                }
            }
        )
    }

    BackHandler(enabled = uiState.showCreateEditAccountSheet) {
        viewModel.dismissCreateEditAccountSheet()
    }
    BackHandler(enabled = uiState.showCreateEditGroupSheet) {
        viewModel.dismissCreateEditGroupSheet()
    }

    if (uiState.showAccountsSheet) {
        AccountsBottomSheet(
            accounts = uiState.accounts,
            groups = uiState.accountGroups,
            selectedAccountId = uiState.selectedAccountId,
            selectedGroupId = uiState.selectedGroupId,
            totalBalance = viewModel.getTotalBalance(),
            baseCurrency = baseCurrency,
            hasMixedCurrencies = viewModel.hasMixedCurrencies(),
            onAccountSelect = { accountId ->
                if (accountId != uiState.selectedAccountId || uiState.selectedGroupId != null) {
                    viewModel.selectAccount(accountId)
                }
            },
            onGroupSelect = { groupId ->
                if (groupId != uiState.selectedGroupId) {
                    viewModel.selectGroup(groupId)
                }
            },
            onCreateAccount = {
                viewModel.dismissAccountsSheet()
                viewModel.showCreateAccountSheet()
            },
            onEditAccount = { accountId ->
                viewModel.dismissAccountsSheet()
                viewModel.showEditAccountSheet(accountId)
            },
            onCreateGroup = {
                viewModel.dismissAccountsSheet()
                viewModel.showCreateGroupSheet()
            },
            onEditGroup = { groupId ->
                viewModel.dismissAccountsSheet()
                viewModel.showEditGroupSheet(groupId)
            },
            onAddAccountToGroup = { accountId, groupId ->
                viewModel.addAccountToGroup(accountId, groupId)
            },
            onDismiss = { viewModel.dismissAccountsSheet() }
        )
    }

    if (uiState.showCreateEditAccountSheet) {
        val editingAccount = viewModel.getEditingAccount()
        CreateEditAccountBottomSheet(
            isEditMode = editingAccount != null,
            account = editingAccount,
            baseCurrency = baseCurrency,
            transactionCount = uiState.editingAccountTransactionCount,
            availableCurrencies = uiState.availableCurrencies,
            favoriteCurrencies = uiState.favoriteCurrencies,
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

    uiState.pendingCurrencyChange?.let { pending ->
        CurrencyChangeConfirmDialog(
            pending = pending,
            onConfirm = { viewModel.confirmPendingCurrencyChange() },
            onDismiss = { viewModel.cancelPendingCurrencyChange() }
        )
    }

    if (uiState.showCreateEditGroupSheet) {
        val editingGroup = viewModel.getEditingGroup()
        AccountGroupSheet(
            isEditMode = editingGroup != null,
            group = editingGroup,
            accounts = uiState.accounts,
            onSave = { name, memberAccountIds ->
                if (editingGroup != null) {
                    viewModel.updateGroup(name, memberAccountIds)
                } else {
                    viewModel.createGroup(name, memberAccountIds)
                }
            },
            onDelete = if (editingGroup != null) {
                { viewModel.deleteGroup(editingGroup.id) }
            } else null,
            onDismiss = { viewModel.dismissCreateEditGroupSheet() }
        )
    }

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 60.dp,
                            bottom = 10.dp
                        )
                ) {
                    IconButton(
                        onClick = onRateHistoryClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = stringResource(R.string.rate_history),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable { viewModel.toggleAccountsSheet() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AutoSizeBalanceText(
                            text = viewModel.formatBalance(balance, displayCurrency, isApproximateBalance),
                            color = Color.White
                        )
                        val selectedName = viewModel.getSelectedAccountName()
                        if (selectedName != null) {
                            Text(
                                text = selectedName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            var showQuickAdd by remember { mutableStateOf(false) }
            val topCategories = remember(uiState.categories) {
                viewModel.getTopExpenseCategories()
            }

            Box {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(FloatingActionButtonDefaults.shape)
                        .combinedClickable(
                            onClick = {
                                if (uiState.selectedGroupId != null) {
                                    showGroupAccountPicker = true
                                } else {
                                    val selectedDate = viewModel.getCurrentSelectedDate()
                                    val accountId = uiState.selectedAccountId
                                    when (viewModel.getCurrentSelectedOperationType()) {
                                        OperationType.EXPENSES -> onAddExpenseClick(selectedDate, accountId)
                                        OperationType.INCOMES -> onAddIncomeClick(selectedDate, accountId)
                                    }
                                }
                            },
                            onLongClick = {
                                if (topCategories.isNotEmpty() &&
                                    viewModel.getCurrentSelectedOperationType() == OperationType.EXPENSES
                                ) {
                                    showQuickAdd = true
                                }
                            }
                        ),
                    shape = FloatingActionButtonDefaults.shape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_operation),
                            tint = Color.White
                        )
                    }
                }

                DropdownMenu(
                    expanded = showQuickAdd,
                    onDismissRequest = { showQuickAdd = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    topCategories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                try { Color(category.color.toColorInt()) }
                                                catch (_: Exception) { Color.Gray }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(category.iconName),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = category.displayName(),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            },
                            onClick = {
                                showQuickAdd = false
                                val selectedDate = viewModel.getCurrentSelectedDate()
                                onAddExpenseWithCategory(selectedDate, category.id, uiState.selectedAccountId)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val density = LocalDensity.current
        val fullHeight = 200.dp
        val ringFraction = 0.22f
        val collapsedBarHeight = fullHeight * ringFraction // 44dp — matches PieChart ring
        val maxCollapseOffsetPx = with(density) { (fullHeight - collapsedBarHeight).toPx() }
        var collapseOffsetPx by remember { mutableFloatStateOf(0f) }
        var snapAnimJob by remember { mutableStateOf<Job?>(null) }

        fun snapCollapseToNearest() {
            val fraction = if (maxCollapseOffsetPx > 0f) collapseOffsetPx / maxCollapseOffsetPx else 0f
            val target = if (fraction > 0.5f) maxCollapseOffsetPx else 0f
            if (collapseOffsetPx == target) return
            snapAnimJob?.cancel()
            snapAnimJob = scope.launch {
                animate(
                    initialValue = collapseOffsetPx,
                    targetValue = target,
                    animationSpec = tween(300)
                ) { value, _ ->
                    collapseOffsetPx = value
                }
            }
        }

        val nestedScrollConnection = remember(maxCollapseOffsetPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y < 0f) {
                        snapAnimJob?.cancel()
                        // Scrolling down → collapse chart first
                        val oldOffset = collapseOffsetPx
                        collapseOffsetPx = (collapseOffsetPx - available.y)
                            .coerceIn(0f, maxCollapseOffsetPx)
                        return Offset(0f, -(collapseOffsetPx - oldOffset))
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y > 0f) {
                        snapAnimJob?.cancel()
                        // Scrolling up + list at top → expand chart
                        val oldOffset = collapseOffsetPx
                        collapseOffsetPx = (collapseOffsetPx - available.y)
                            .coerceIn(0f, maxCollapseOffsetPx)
                        return Offset(0f, oldOffset - collapseOffsetPx)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    snapCollapseToNearest()
                    return Velocity.Zero
                }
            }
        }

        val collapseFraction = if (maxCollapseOffsetPx > 0f) {
            (collapseOffsetPx / maxCollapseOffsetPx).coerceIn(0f, 1f)
        } else 0f
        val chartHeight = lerp(fullHeight, collapsedBarHeight, collapseFraction)

        val isEmpty = uiState.categoryStatistics.isEmpty()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(isEmpty, maxCollapseOffsetPx) {
                    if (!isEmpty) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastX = down.position.x
                        var lastY = down.position.y
                        var directionDecided = false
                        var isVertical = false
                        do {
                            val event = awaitPointerEvent()
                            val drag = event.changes.firstOrNull() ?: break
                            val dx = drag.position.x - lastX
                            val dy = drag.position.y - lastY
                            if (!directionDecided) {
                                val totalMove = kotlin.math.abs(dx) + kotlin.math.abs(dy)
                                if (totalMove > 10f) {
                                    directionDecided = true
                                    isVertical = kotlin.math.abs(dy) > kotlin.math.abs(dx)
                                    if (!isVertical) break // horizontal — let it pass through
                                }
                            }
                            if (isVertical) {
                                lastX = drag.position.x
                                lastY = drag.position.y
                                collapseOffsetPx = (collapseOffsetPx - dy)
                                    .coerceIn(0f, maxCollapseOffsetPx)
                                drag.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        if (isVertical) snapCollapseToNearest()
                    }
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // Fixed header: toggle, period selector, chart
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .pointerInput(maxCollapseOffsetPx) {
                        detectVerticalDragGestures(
                            onDragStart = { snapAnimJob?.cancel() },
                            onDragEnd = { snapCollapseToNearest() },
                            onDragCancel = { snapCollapseToNearest() }
                        ) { _, dragAmount ->
                            collapseOffsetPx = (collapseOffsetPx - dragAmount)
                                .coerceIn(0f, maxCollapseOffsetPx)
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpenseIncomeToggle(
                    selectedType = uiState.selectedOperationType,
                    onTypeSelected = viewModel::selectOperationType
                )

                FixedPeriodTypeSelector(
                    selectedPeriod = uiState.selectedPeriodType,
                    onPeriodSelected = { period ->
                        if (period == PeriodType.PERIOD) {
                            showPeriodPicker = true
                        } else {
                            viewModel.selectPeriodType(period)
                        }
                    }
                )

                PeriodNavigationCard(
                    uiState = uiState,
                    periodDisplayText = periodDisplayText,
                    baseCurrency = baseCurrency,
                    openingBalance = openingBalance,
                    displayCurrency = displayCurrency,
                    onNavigate = viewModel::navigatePeriod,
                    collapseFraction = collapseFraction,
                    chartHeight = chartHeight,
                    barHeight = collapsedBarHeight
                )
            }

            if (uiState.categoryStatistics.isEmpty()) {
                // Empty state: gesture handled by outer Box
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // Scrollable category list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(uiState.isAllTimePeriod, uiState.selectedPeriodType) {
                            if (uiState.isAllTimePeriod) return@pointerInput
                            val swipeThresholdPx = 50.dp.toPx()
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag > swipeThresholdPx) {
                                        viewModel.navigatePeriod(-1)
                                    } else if (totalDrag < -swipeThresholdPx) {
                                        viewModel.navigatePeriod(1)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        },
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 16.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.categoryStatistics) { stat ->
                        CategoryStatisticItem(
                            statistic = stat,
                            baseCurrency = baseCurrency,
                            onClick = {
                                val (startDate, endDate) = viewModel.getCurrentPeriodDateRange()
                                val effectiveAccountId = when {
                                    uiState.selectedGroupId != null -> {
                                        viewModel.getSelectedGroupMemberIds().joinToString(",")
                                            .takeIf { it.isNotBlank() }
                                    }
                                    else -> uiState.selectedAccountId
                                }
                                onCategoryClick(
                                    stat.category.id,
                                    uiState.selectedOperationType,
                                    startDate,
                                    endDate,
                                    uiState.isAllTimePeriod,
                                    effectiveAccountId
                                )
                            }
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

@Composable
private fun ExpenseIncomeToggle(
    selectedType: OperationType,
    onTypeSelected: (OperationType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .clickable { onTypeSelected(OperationType.EXPENSES) },
                color = if (selectedType == OperationType.EXPENSES) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.expenses_upper),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selectedType == OperationType.EXPENSES) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .clickable { onTypeSelected(OperationType.INCOMES) },
                color = if (selectedType == OperationType.INCOMES) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.incomes_upper),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selectedType == OperationType.INCOMES) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FixedPeriodTypeSelector(
    selectedPeriod: PeriodType,
    onPeriodSelected: (PeriodType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PeriodType.entries.forEach { period ->
            val isSelected = selectedPeriod == period

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPeriodSelected(period) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(period.displayNameRes),
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun AutoSizeBalanceText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: Float = 32f,
    minFontSize: Float = 18f
) {
    // Keyed on text so that a new balance value starts auto-shrinking from maxFontSize again.
    // The text is always drawn — brief fontSize adjustments via onTextLayout are imperceptible,
    // whereas a drawWithContent guard can hide the text entirely after recomposition on return
    // to the screen.
    var fontSize by remember(text) { mutableFloatStateOf(maxFontSize) }

    Text(
        text = text,
        color = color,
        maxLines = 1,
        softWrap = false,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize - 2f).coerceAtLeast(minFontSize)
            }
        }
    )
}

@Composable
private fun CategoryStatisticItem(
    statistic: CategoryStatistic,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Color(statistic.category.color.toColorInt())
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(statistic.category.iconName),
                    contentDescription = statistic.category.displayName(),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statistic.category.displayName(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", statistic.percentage)}%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${formatAmount(statistic.totalAmount)} ${getCurrencySymbol(baseCurrency)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
