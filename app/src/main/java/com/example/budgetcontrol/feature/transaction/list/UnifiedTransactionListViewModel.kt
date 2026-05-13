package com.example.budgetcontrol.feature.transaction.list

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.model.PairedStackedTrendBucket
import com.example.budgetcontrol.core.domain.model.StackedTrendBucket
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TrendBucket
import com.example.budgetcontrol.core.domain.model.TrendChartData
import com.example.budgetcontrol.core.domain.model.TrendSegment
import com.example.budgetcontrol.core.domain.model.findById
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.AccountGroupRepository
import com.example.budgetcontrol.core.domain.repository.CategoryLimitRepository
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.DeleteExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.DeleteIncomeUseCase
import com.example.budgetcontrol.core.domain.usecase.GetAccountsUseCase
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.DateRangeHelper
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import com.example.budgetcontrol.core.domain.model.PeriodType
import com.example.budgetcontrol.ui.components.charts.TrendBarPeriod
import com.example.budgetcontrol.ui.components.charts.trendLabelFor
import com.example.budgetcontrol.ui.util.toSafeColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TREND_BUCKET_COUNT = 6

enum class TransactionTypeFilter { ALL, INCOME, EXPENSE }

@Immutable
data class UnifiedTransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val accountGroups: List<AccountGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedGroupId: String? = null,
    val selectedCategoryIds: Set<String> = emptySet(),
    val transactionTypeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = true,
    val trendChart: TrendChartData = TrendChartData.Empty,
    val selectedTrendPeriod: PeriodType = PeriodType.MONTH,
    val availableTypeTabs: Set<TransactionTypeFilter> = setOf(
        TransactionTypeFilter.ALL,
        TransactionTypeFilter.INCOME,
        TransactionTypeFilter.EXPENSE
    )
)

@Immutable
private data class FilterParams(
    val accountId: String?,
    val groupId: String?,
    val categoryIds: Set<String>,
    val typeFilter: TransactionTypeFilter
)

@HiltViewModel
class UnifiedTransactionListViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountGroupRepository: AccountGroupRepository,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    private val preferencesManager: PreferencesManager,
    private val categoryLimitRepository: CategoryLimitRepository
) : ViewModel() {

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _selectedTrendPeriod = MutableStateFlow(PeriodType.MONTH)

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DEFAULT_BASE_CURRENCY)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedCategoryLimitAmountFlow: Flow<Double?> =
        _selectedCategoryIds.flatMapLatest { ids ->
            val singleId = ids.singleOrNull()
            if (singleId == null) {
                flowOf<Double?>(null)
            } else {
                categoryLimitRepository.getLimit(singleId).map { it?.amount }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UnifiedTransactionListUiState> = combine(
        combine(_startDate, _endDate) { start, end ->
            if (start != null && end != null) start to end else null
        }.distinctUntilChanged(),
        combine(
            _selectedAccountId,
            _selectedGroupId,
            accountGroupRepository.getAllGroups()
        ) { accountId, groupId, groups ->
            // null  → no account filter (load everything)
            // empty → group present but has no members (yields zero rows)
            // non-empty → restrict to these account IDs at the DAO level
            when {
                groupId != null -> groups.find { it.id == groupId }?.memberAccountIds ?: emptyList()
                accountId != null -> listOf(accountId)
                else -> null
            }
        }.distinctUntilChanged()
    ) { dateRange, effectiveAccountIds ->
        dateRange to effectiveAccountIds
    }.flatMapLatest { (dateRange, effectiveAccountIds) ->
        val expensesFlow = expenseSource(effectiveAccountIds, dateRange)
        val incomesFlow = incomeSource(effectiveAccountIds, dateRange)
        // Trend buckets always span 6 periods regardless of the current date filter, so the
        // trend series ignores `dateRange`. The account/group filter, however, *is* applied
        // here at the DAO level — selecting a single account narrows both the tx list and
        // the chart.
        val trendInputsFlow = combine(
            _selectedTrendPeriod,
            selectedCategoryLimitAmountFlow,
            expenseSource(effectiveAccountIds, null),
            incomeSource(effectiveAccountIds, null)
        ) { period, limitAmount, allExp, allInc ->
            TrendInputs(period, limitAmount, allExp, allInc)
        }
        combine(
            combine(expensesFlow, incomesFlow, getCategoriesUseCase()) { expenses, incomes, categories ->
                Triple(expenses, incomes, categories)
            },
            combine(
                _selectedAccountId,
                _selectedGroupId,
                _selectedCategoryIds,
                _transactionTypeFilter
            ) { accountId, groupId, categoryIds, typeFilter ->
                FilterParams(accountId, groupId, categoryIds, typeFilter)
            },
            getAccountsUseCase.getAccountsWithBalances(
                baseCurrencyFlow = preferencesManager.baseCurrencyFlow,
                ratesFlow = preferencesManager.getLastRates()
            ),
            accountGroupRepository.getAllGroups(),
            trendInputsFlow
        ) { coreData, filters, accounts, groups, trendInputs ->
            val (expenses, incomes, categories) = coreData
            val trendPeriod = trendInputs.period
            val selectedCategoryLimitAmount = trendInputs.limitAmount
            val trendExpenses = trendInputs.allExpenses
            val trendIncomes = trendInputs.allIncomes

            val allTransactions = buildList {
                addAll(expenses.map { it.toTransaction() })
                addAll(incomes.map { it.toTransaction() })
            }

            // Account/group filter is already applied at the DAO level; only the
            // category + type filters remain in memory.
            val filtered = allTransactions
                .filter { tx ->
                    val categoryMatch = filters.categoryIds.isEmpty() || tx.categoryId in filters.categoryIds
                    val typeMatch = when (filters.typeFilter) {
                        TransactionTypeFilter.ALL -> true
                        TransactionTypeFilter.INCOME -> tx is Transaction.IncomeTransaction
                        TransactionTypeFilter.EXPENSE -> tx is Transaction.ExpenseTransaction
                    }
                    categoryMatch && typeMatch
                }
                .sortedByDescending { it.date }

            // Pre-resolve category color/index map once per recomputation. `Color` is parsed
            // here (not in composable) so segment lists carry stable, parsed colors and the
            // chart can rely on `@Immutable` skipping.
            val categoryById: Map<String, Category> = categories.associateBy { it.id }

            // Type-tab availability based on the *types of selected categories*. Drives both
            // the visible tabs and the guarded type-filter value.
            val selectedCategories = categories.filter { it.id in filters.categoryIds }
            val selectedTypes = selectedCategories.map { it.type }.toSet()
            val availableTypeTabs: Set<TransactionTypeFilter> = when {
                filters.categoryIds.isEmpty() -> setOf(
                    TransactionTypeFilter.ALL,
                    TransactionTypeFilter.INCOME,
                    TransactionTypeFilter.EXPENSE
                )
                selectedTypes == setOf(CategoryType.EXPENSE) -> setOf(TransactionTypeFilter.EXPENSE)
                selectedTypes == setOf(CategoryType.INCOME) -> setOf(TransactionTypeFilter.INCOME)
                else -> setOf(
                    TransactionTypeFilter.ALL,
                    TransactionTypeFilter.INCOME,
                    TransactionTypeFilter.EXPENSE
                )
            }
            val effectiveTypeFilter: TransactionTypeFilter =
                if (filters.typeFilter in availableTypeTabs) filters.typeFilter
                else availableTypeTabs.first()

            // Mode dispatch:
            //   (any, 1)         → SingleCategory (with limit-line if set)
            //   (any, 2+)        → Stacked across selected categories only
            //                       (segregated by the type of each category)
            //   (ALL, 0)         → PairedStacked (expenses left, incomes right)
            //   (EXPENSE, 0)     → Stacked expenses by category
            //   (INCOME, 0)      → Stacked incomes by category
            val trendChart: TrendChartData = when {
                filters.categoryIds.size == 1 -> {
                    val targetCategoryId = filters.categoryIds.first()
                    val category = categoryById[targetCategoryId]
                    val sourceItems: List<DatedAmount> = if (category?.type == CategoryType.INCOME) {
                        trendIncomes.filter { it.categoryId == targetCategoryId }
                            .map { DatedAmount(it.date, it.amount) }
                    } else {
                        trendExpenses.filter { it.categoryId == targetCategoryId }
                            .map { DatedAmount(it.date, it.amount) }
                    }
                    val buckets = computeAmountBuckets(
                        items = sourceItems,
                        dateOf = { it.date },
                        amountOf = { it.amount },
                        period = trendPeriod,
                        count = 6
                    )
                    val color = category?.color?.toSafeColor() ?: Color.Gray
                    // Limit only meaningful for expense categories (income has no spending cap).
                    val limit = if (category?.type == CategoryType.EXPENSE) selectedCategoryLimitAmount else null
                    TrendChartData.SingleCategory(buckets, color, limit)
                }
                filters.categoryIds.size >= 2 -> {
                    // 2+ categories: stack only the selected ones, sourcing items from the
                    // matching transaction series for each category's type.
                    val expenseSelectedIds = selectedCategories
                        .filter { it.type == CategoryType.EXPENSE }
                        .mapTo(mutableSetOf()) { it.id }
                    val incomeSelectedIds = selectedCategories
                        .filter { it.type == CategoryType.INCOME }
                        .mapTo(mutableSetOf()) { it.id }
                    val expenseItems = trendExpenses
                        .filter { it.categoryId in expenseSelectedIds }
                        .map { CategorizedAmount(it.date, it.amount, it.categoryId) }
                    val incomeItems = trendIncomes
                        .filter { it.categoryId in incomeSelectedIds }
                        .map { CategorizedAmount(it.date, it.amount, it.categoryId) }
                    val combinedItems = expenseItems + incomeItems
                    val buckets = computeStackedBuckets(
                        items = combinedItems,
                        dateOf = { it.date },
                        amountOf = { it.amount },
                        categoryIdOf = { it.categoryId },
                        categoryById = categoryById,
                        period = trendPeriod,
                        count = 6
                    )
                    TrendChartData.Stacked(buckets)
                }
                effectiveTypeFilter == TransactionTypeFilter.EXPENSE -> {
                    val items = trendExpenses.map { CategorizedAmount(it.date, it.amount, it.categoryId) }
                    val buckets = computeStackedBuckets(
                        items = items,
                        dateOf = { it.date },
                        amountOf = { it.amount },
                        categoryIdOf = { it.categoryId },
                        categoryById = categoryById,
                        period = trendPeriod,
                        count = 6
                    )
                    TrendChartData.Stacked(buckets)
                }
                effectiveTypeFilter == TransactionTypeFilter.INCOME -> {
                    val items = trendIncomes.map { CategorizedAmount(it.date, it.amount, it.categoryId) }
                    val buckets = computeStackedBuckets(
                        items = items,
                        dateOf = { it.date },
                        amountOf = { it.amount },
                        categoryIdOf = { it.categoryId },
                        categoryById = categoryById,
                        period = trendPeriod,
                        count = 6
                    )
                    TrendChartData.Stacked(buckets)
                }
                else -> {
                    // ALL + no category filter — paired stack, expenses and incomes side by side.
                    val expenseItems = trendExpenses.map { CategorizedAmount(it.date, it.amount, it.categoryId) }
                    val incomeItems = trendIncomes.map { CategorizedAmount(it.date, it.amount, it.categoryId) }
                    val buckets = computePairedStackedBuckets(
                        expenseItems = expenseItems,
                        incomeItems = incomeItems,
                        dateOf = { it.date },
                        amountOf = { it.amount },
                        categoryIdOf = { it.categoryId },
                        categoryById = categoryById,
                        period = trendPeriod,
                        count = 6
                    )
                    TrendChartData.PairedStacked(buckets)
                }
            }

            UnifiedTransactionListUiState(
                transactions = filtered,
                accounts = accounts,
                accountGroups = groups,
                categories = categories,
                selectedAccountId = filters.accountId,
                selectedGroupId = filters.groupId,
                selectedCategoryIds = filters.categoryIds,
                transactionTypeFilter = effectiveTypeFilter,
                startDate = dateRange?.first,
                endDate = dateRange?.second,
                isLoading = false,
                trendChart = trendChart,
                selectedTrendPeriod = trendPeriod,
                availableTypeTabs = availableTypeTabs
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        UnifiedTransactionListUiState()
    )

    fun setAccount(accountId: String?) {
        _selectedAccountId.value = accountId
        _selectedGroupId.value = null
    }

    fun setGroup(groupId: String?) {
        _selectedGroupId.value = groupId
        _selectedAccountId.value = null
    }

    fun clearAccountFilter() {
        _selectedAccountId.value = null
        _selectedGroupId.value = null
    }

    fun toggleCategory(categoryId: String) {
        val current = _selectedCategoryIds.value
        _selectedCategoryIds.value = if (categoryId in current) current - categoryId else current + categoryId
        // The reducer above will reconcile typeFilter against availableTypeTabs on the next
        // emission, so we don't need to mutate _transactionTypeFilter here.
    }

    fun clearCategories() {
        _selectedCategoryIds.value = emptySet()
    }

    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
    }

    fun setDateRange(startDate: Long, endDate: Long) {
        _startDate.value = startDate
        _endDate.value = endDate
    }

    fun setCustomDateRange(startDate: Long, endDate: Long) = setDateRange(startDate, endDate)

    fun clearDateRange() {
        _startDate.value = null
        _endDate.value = null
    }

    fun setTrendPeriod(period: PeriodType) {
        _selectedTrendPeriod.value = period
    }

    fun onTrendBucketTapped(start: Long, end: Long, total: Double) {
        // Defensive: the chart already suppresses taps on empty buckets, but if a future
        // caller wires this up directly we still want to ignore zero-data periods.
        if (total == 0.0) return
        setCustomDateRange(start, end)
    }

    /**
     * Picks the right DAO query for the current (account-filter, date-range) combination so
     * that filtering happens in SQL rather than in memory.
     *
     * The empty-`accountIds` short-circuit to `flowOf(emptyList())` is load-bearing, not
     * cosmetic: Room expands `IN (:accountIds)` into a comma-separated `?` list, and an empty
     * list produces `WHERE accountId IN ()` — a SQLite syntax error that crashes with
     * `SQLiteException: near ")": syntax error`, NOT a no-match. Any future caller of
     * `getByAccounts*` on the DAO/repository must guard against an empty list the same way.
     */
    private fun expenseSource(
        accountIds: List<String>?,
        dateRange: Pair<Long, Long>?
    ): Flow<List<Expense>> = when {
        accountIds != null && accountIds.isEmpty() -> flowOf(emptyList())
        accountIds != null && dateRange != null ->
            getExpensesUseCase.getByAccountsAndDateRange(accountIds, dateRange.first, dateRange.second)
        accountIds != null -> getExpensesUseCase.getByAccounts(accountIds)
        dateRange != null -> getExpensesUseCase.getByDateRange(dateRange.first, dateRange.second)
        else -> getExpensesUseCase()
    }

    private fun incomeSource(
        accountIds: List<String>?,
        dateRange: Pair<Long, Long>?
    ): Flow<List<Income>> = when {
        accountIds != null && accountIds.isEmpty() -> flowOf(emptyList())
        accountIds != null && dateRange != null ->
            getIncomesUseCase.getByAccountsAndDateRange(accountIds, dateRange.first, dateRange.second)
        accountIds != null -> getIncomesUseCase.getByAccounts(accountIds)
        dateRange != null -> getIncomesUseCase.getByDateRange(dateRange.first, dateRange.second)
        else -> getIncomesUseCase()
    }

    /** Internal accessors for the bucket computations, kept private to avoid leaking shape. */
    private data class DatedAmount(val date: Long, val amount: Double)
    private data class CategorizedAmount(val date: Long, val amount: Double, val categoryId: String)

    /**
     * Scalar (single-category) bucket computation. Returns 6 buckets ordered oldest → newest.
     */
    private fun <T> computeAmountBuckets(
        items: List<T>,
        dateOf: (T) -> Long,
        amountOf: (T) -> Double,
        period: PeriodType,
        count: Int = TREND_BUCKET_COUNT
    ): List<TrendBucket> {
        val barPeriod = period.toTrendBarPeriod()
        return (count - 1 downTo 0).map { i ->
            val (start, end) = DateRangeHelper.getDateRange(period, periodOffset = -i)
            val total = items
                .filter { dateOf(it) in start..end }
                .sumOf { amountOf(it) }
            TrendBucket(
                start = start,
                end = end,
                total = total,
                periodOffset = -i,
                label = trendLabelFor(barPeriod, start)
            )
        }
    }

    /**
     * Per-category stacked bucket computation. Single pass over [items] groups by bucket
     * index (linear scan over `count` ranges per item — count is small, ~6) and category id,
     * then materializes [TrendSegment]s sorted by amount desc for stable visual order.
     */
    private fun <T> computeStackedBuckets(
        items: List<T>,
        dateOf: (T) -> Long,
        amountOf: (T) -> Double,
        categoryIdOf: (T) -> String,
        categoryById: Map<String, Category>,
        period: PeriodType,
        count: Int = TREND_BUCKET_COUNT
    ): List<StackedTrendBucket> {
        val barPeriod = period.toTrendBarPeriod()
        val ranges: List<Pair<Long, Long>> = (count - 1 downTo 0).map { i ->
            DateRangeHelper.getDateRange(period, periodOffset = -i)
        }
        // bucketIdx → categoryId → accumulated amount
        val buckets: Array<MutableMap<String, Double>> = Array(count) { mutableMapOf() }
        for (item in items) {
            val d = dateOf(item)
            val idx = ranges.indexOfFirst { (start, end) -> d in start..end }
            if (idx == -1) continue
            val cid = categoryIdOf(item)
            val amt = amountOf(item)
            buckets[idx].merge(cid, amt) { a, b -> a + b }
        }
        return ranges.mapIndexed { idx, (start, end) ->
            val periodOffset = -(count - 1 - idx)
            val segments = buckets[idx]
                .map { (cid, amount) ->
                    val color = categoryById[cid]?.color?.toSafeColor() ?: Color.Gray
                    TrendSegment(categoryId = cid, color = color, amount = amount)
                }
                // Largest at the bottom keeps the stack visually stable across buckets.
                .sortedWith(compareByDescending<TrendSegment> { it.amount }.thenBy { it.categoryId })
            StackedTrendBucket(
                start = start,
                end = end,
                periodOffset = periodOffset,
                label = trendLabelFor(barPeriod, start),
                segments = segments
            )
        }
    }

    private fun <T> computePairedStackedBuckets(
        expenseItems: List<T>,
        incomeItems: List<T>,
        dateOf: (T) -> Long,
        amountOf: (T) -> Double,
        categoryIdOf: (T) -> String,
        categoryById: Map<String, Category>,
        period: PeriodType,
        count: Int = TREND_BUCKET_COUNT
    ): List<PairedStackedTrendBucket> {
        val expenseStacks = computeStackedBuckets(
            expenseItems, dateOf, amountOf, categoryIdOf, categoryById, period, count
        )
        val incomeStacks = computeStackedBuckets(
            incomeItems, dateOf, amountOf, categoryIdOf, categoryById, period, count
        )
        return expenseStacks.indices.map { i ->
            val e = expenseStacks[i]
            val ic = incomeStacks[i]
            PairedStackedTrendBucket(
                start = e.start,
                end = e.end,
                periodOffset = e.periodOffset,
                label = e.label,
                expenseSegments = e.segments,
                incomeSegments = ic.segments
            )
        }
    }

    private fun PeriodType.toTrendBarPeriod(): TrendBarPeriod = when (this) {
        PeriodType.DAY -> TrendBarPeriod.DAY
        PeriodType.WEEK -> TrendBarPeriod.WEEK
        PeriodType.MONTH -> TrendBarPeriod.MONTH
        PeriodType.YEAR -> TrendBarPeriod.YEAR
        PeriodType.PERIOD -> TrendBarPeriod.MONTH
    }

    private data class TrendInputs(
        val period: PeriodType,
        val limitAmount: Double?,
        val allExpenses: List<Expense>,
        val allIncomes: List<Income>
    )

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            when (transaction) {
                is Transaction.ExpenseTransaction -> deleteExpenseUseCase(transaction.toExpense())
                is Transaction.IncomeTransaction -> deleteIncomeUseCase(transaction.toIncome())
            }
        }
    }

    fun getCategoryById(categoryId: String): Category? {
        return uiState.value.categories.findById(categoryId)
    }
}
