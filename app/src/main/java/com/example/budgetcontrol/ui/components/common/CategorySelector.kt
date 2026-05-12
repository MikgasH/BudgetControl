package com.example.budgetcontrol.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryLimitProgress
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.ui.util.displayName
import com.example.budgetcontrol.ui.util.getCategoryIcon
import com.example.budgetcontrol.ui.util.toSafeColor

private const val GRID_COLUMNS = 4
private const val COMPACT_COUNT = 7

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit,
    transactionType: TransactionType,
    modifier: Modifier = Modifier,
    onCreateCategory: ((name: String, iconName: String, color: String, type: CategoryType, limitAmount: Double?) -> Unit)? = null,
    onUpdateCategoryColor: (Category, String) -> Unit = { _, _ -> },
    onUpdateCategory: (Category) -> Unit = {},
    onDeleteCategory: (Category) -> Unit = {},
    limitProgressMap: Map<String, CategoryLimitProgress> = emptyMap(),
    baseCurrency: String = "EUR",
    categoryLimits: Map<String, com.example.budgetcontrol.core.domain.model.CategoryLimit> = emptyMap(),
    onSetCategoryLimit: (categoryId: String, amount: Double) -> Unit = { _, _ -> },
    onClearCategoryLimit: (categoryId: String) -> Unit = { _ -> }
) {
    val title = when (transactionType) {
        TransactionType.EXPENSE -> stringResource(R.string.expense_categories)
        TransactionType.INCOME -> stringResource(R.string.income_categories)
    }

    val categoryType = when (transactionType) {
        TransactionType.EXPENSE -> CategoryType.EXPENSE
        TransactionType.INCOME -> CategoryType.INCOME
    }

    val visibleCategories = remember(categories) {
        categories.filter { it.iconName != "more_horiz" }
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var createSheetInitialName by remember { mutableStateOf("") }
    var settingsCategory by remember { mutableStateOf<Category?>(null) }

    // Sheets
    if (showCreateSheet && onCreateCategory != null) {
        CreateCategoryBottomSheet(
            categoryType = categoryType,
            onDismiss = {
                showCreateSheet = false
                createSheetInitialName = ""
            },
            onSave = { name, iconName, color, type, limitAmount ->
                onCreateCategory(name, iconName, color, type, limitAmount)
            },
            initialName = createSheetInitialName,
            baseCurrency = baseCurrency
        )
    }

    if (showMoreSheet) {
        AllCategoriesBottomSheet(
            categories = visibleCategories,
            selectedCategory = selectedCategory,
            onCategorySelect = { cat ->
                onCategorySelect(cat)
                showMoreSheet = false
            },
            onDismiss = { showMoreSheet = false },
            onCreateCategory = if (onCreateCategory != null) {
                { showCreateSheet = true }
            } else null,
            onCreateCategoryWithName = if (onCreateCategory != null) {
                { name ->
                    createSheetInitialName = name
                    showCreateSheet = true
                }
            } else null,
            onLongPress = { settingsCategory = it },
            limitProgressMap = limitProgressMap
        )
    }

    settingsCategory?.let { cat ->
        CategorySettingsSheet(
            category = cat,
            existingLimit = categoryLimits[cat.id],
            baseCurrency = baseCurrency,
            onDismiss = { settingsCategory = null },
            onUpdateCategoryColor = { category, color ->
                onUpdateCategoryColor(category, color)
                settingsCategory = null
            },
            onUpdateCategory = { updated ->
                onUpdateCategory(updated)
                settingsCategory = null
            },
            onDeleteCategory = { deleted ->
                onDeleteCategory(deleted)
                settingsCategory = null
            },
            onSetCategoryLimit = onSetCategoryLimit,
            onClearCategoryLimit = onClearCategoryLimit
        )
    }

    // Top 7 by usageCount
    val compactItems = remember(visibleCategories) {
        visibleCategories.sortedByDescending { it.usageCount }.take(COMPACT_COUNT)
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Row 1: items 0-3
        val row1 = compactItems.take(GRID_COLUMNS)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            row1.forEach { category ->
                CompactCategoryItem(
                    category = category,
                    isSelected = category.id == selectedCategory?.id,
                    limitFraction = limitProgressMap[category.id]?.fraction,
                    onClick = { onCategorySelect(category) },
                    onLongClick = { settingsCategory = category },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(GRID_COLUMNS - row1.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: items 4-6 + "More" button
        val row2 = compactItems.drop(GRID_COLUMNS)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            row2.forEach { category ->
                CompactCategoryItem(
                    category = category,
                    isSelected = category.id == selectedCategory?.id,
                    limitFraction = limitProgressMap[category.id]?.fraction,
                    onClick = { onCategorySelect(category) },
                    onLongClick = { settingsCategory = category },
                    modifier = Modifier.weight(1f)
                )
            }

            // "More" button — always the 8th slot
            MoreButtonItem(
                onClick = { showMoreSheet = true },
                modifier = Modifier.weight(1f)
            )

            // Fill remaining if row2 has fewer than 3 items
            repeat(GRID_COLUMNS - row2.size - 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Compact grid item
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactCategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    limitFraction: Float? = null
) {
    val backgroundColor = if (isSelected) {
        category.color.toSafeColor(MaterialTheme.colorScheme.primary)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) Color.White
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        if (limitFraction != null) {
            CategoryIconWithLimitRing(
                iconName = category.iconName,
                iconColor = backgroundColor,
                sizeDp = 52.dp,
                progress = limitFraction,
                contentDescription = category.displayName(),
                // Match the non-limit branch: white over the vibrant selected color,
                // onSurfaceVariant over the muted surfaceVariant when unselected.
                // Without this, the icon was rendered white over light gray and looked faded.
                contentColor = contentColor
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = category.displayName(),
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category.displayName(),
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

@Composable
private fun MoreButtonItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// "All categories" BottomSheet (opened by "More")
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AllCategoriesBottomSheet(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit,
    onDismiss: () -> Unit,
    onCreateCategory: (() -> Unit)?,
    onCreateCategoryWithName: ((String) -> Unit)? = null,
    onLongPress: (Category) -> Unit,
    limitProgressMap: Map<String, CategoryLimitProgress> = emptyMap()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    // Build a map of category id -> localized display name so filtering
    // matches what the user actually sees (not the internal name/key).
    val displayNames = categories.associateBy({ it.id }, { it.displayName() })

    val filtered = remember(categories, searchQuery, displayNames) {
        if (searchQuery.isBlank()) categories
        else categories.filter { (displayNames[it.id] ?: it.name).contains(searchQuery, ignoreCase = true) }
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
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = stringResource(R.string.long_press_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search field
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
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fixed-height container: grid and empty state overlap so
            // toggling between them never changes the layout height.
            // 2 rows of items: (52 icon + 4 spacer + 36 text) × 2 + 12 gap = 196dp
            val showEmptyCreate = filtered.isEmpty() && searchQuery.isNotBlank() && onCreateCategoryWithName != null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 196.dp)
            ) {
                // Category grid — always in layout, invisible when empty create is shown
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .alpha(if (showEmptyCreate) 0f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    userScrollEnabled = !showEmptyCreate
                ) {
                    items(
                        items = filtered,
                        key = { it.id }
                    ) { category ->
                        CompactCategoryItem(
                            category = category,
                            isSelected = category.id == selectedCategory?.id,
                            limitFraction = limitProgressMap[category.id]?.fraction,
                            onClick = { onCategorySelect(category) },
                            onLongClick = { onLongPress(category) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    if (onCreateCategory != null && searchQuery.isBlank()) {
                        item(key = "add_category") {
                            AddCategoryItem(
                                onClick = onCreateCategory,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                // Empty state — overlaid on top, same reserved space
                if (showEmptyCreate) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 196.dp)
                            .clickable { onCreateCategoryWithName?.invoke(searchQuery.trim()) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.create_category_suggestion, searchQuery.trim()),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCategoryItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_category),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.add_category),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CategorySettingsSheet — long press on any category
// ═══════════════════════════════════════════════════════════════════════

private val settingsPresetColors = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#3F51B5",
    "#2196F3", "#00BCD4", "#4CAF50", "#8BC34A",
    "#FF9800", "#FF5722", "#795548", "#607D8B"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySettingsSheet(
    category: Category,
    existingLimit: com.example.budgetcontrol.core.domain.model.CategoryLimit?,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onUpdateCategoryColor: (Category, String) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onSetCategoryLimit: (categoryId: String, amount: Double) -> Unit,
    onClearCategoryLimit: (categoryId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (category.isSystem) {
            SystemCategorySettings(
                category = category,
                existingLimit = existingLimit,
                baseCurrency = baseCurrency,
                onSave = { newColor -> onUpdateCategoryColor(category, newColor) },
                onSetLimit = { amount -> onSetCategoryLimit(category.id, amount) },
                onClearLimit = { onClearCategoryLimit(category.id) },
                onDismiss = onDismiss
            )
        } else {
            CustomCategorySettings(
                category = category,
                existingLimit = existingLimit,
                baseCurrency = baseCurrency,
                onSave = { updated -> onUpdateCategory(updated) },
                onDelete = { onDeleteCategory(category) },
                onSetLimit = { amount -> onSetCategoryLimit(category.id, amount) },
                onClearLimit = { onClearCategoryLimit(category.id) },
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Monthly-limit editor block, shared between system and custom category settings.
 * Only renders for EXPENSE categories. Stateless — `limitText` is hoisted to the
 * parent so the parent's single Save button can persist the limit alongside
 * the rest of the category in one operation.
 */
@Composable
private fun MonthlyLimitSection(
    category: Category,
    baseCurrency: String,
    limitText: String,
    onLimitTextChange: (String) -> Unit
) {
    if (category.type != CategoryType.EXPENSE) return

    HorizontalDivider()

    Text(
        text = stringResource(R.string.set_monthly_limit),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp)
    )

    OutlinedTextField(
        value = limitText,
        onValueChange = { input ->
            onLimitTextChange(input.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })
        },
        label = { Text(stringResource(R.string.monthly_limit_label)) },
        suffix = { Text(baseCurrency) },
        trailingIcon = if (limitText.isNotBlank()) {
            {
                IconButton(onClick = { onLimitTextChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.remove_limit),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

private fun formatLimitInputCs(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else String.format(java.util.Locale.US, "%.2f", amount)

@Composable
private fun SystemCategorySettings(
    category: Category,
    existingLimit: com.example.budgetcontrol.core.domain.model.CategoryLimit?,
    baseCurrency: String,
    onSave: (String) -> Unit,
    onSetLimit: (Double) -> Unit,
    onClearLimit: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(category.color) }
    var limitText by remember(existingLimit) {
        mutableStateOf(existingLimit?.amount?.let { formatLimitInputCs(it) } ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large icon circle with category color
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    selectedColor.toSafeColor(MaterialTheme.colorScheme.primary)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category.iconName),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.system_category_color_only),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Color.Gray
        )

        HorizontalDivider()

        // Change color section
        Text(
            text = stringResource(R.string.select_color),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )

        // 2 rows of 6 preset colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            settingsPresetColors.take(6).forEach { hex ->
                SettingsColorCircle(
                    hex = hex,
                    isSelected = selectedColor.equals(hex, ignoreCase = true),
                    onClick = { selectedColor = hex }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            settingsPresetColors.drop(6).forEach { hex ->
                SettingsColorCircle(
                    hex = hex,
                    isSelected = selectedColor.equals(hex, ignoreCase = true),
                    onClick = { selectedColor = hex }
                )
            }
        }

        MonthlyLimitSection(
            category = category,
            baseCurrency = baseCurrency,
            limitText = limitText,
            onLimitTextChange = { limitText = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save button — persists color AND the limit in one operation
        Button(
            onClick = {
                onSave(selectedColor)
                if (category.type == CategoryType.EXPENSE) {
                    val parsed = limitText.replace(',', '.').toDoubleOrNull()
                    if (parsed != null && parsed > 0.0) {
                        onSetLimit(parsed)
                    } else if (existingLimit != null) {
                        onClearLimit()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(R.string.save),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CustomCategorySettings(
    category: Category,
    existingLimit: com.example.budgetcontrol.core.domain.model.CategoryLimit?,
    baseCurrency: String,
    onSave: (Category) -> Unit,
    onDelete: () -> Unit,
    onSetLimit: (Double) -> Unit,
    onClearLimit: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialName = category.displayName()
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(category.iconName) }
    var selectedColor by remember { mutableStateOf(category.color) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMoreIcons by remember { mutableStateOf(false) }
    var showCustomColor by remember { mutableStateOf(false) }
    // RGB sliders state
    val initialRgb = remember { parseHexColor(category.color) }
    var red by remember { mutableIntStateOf(initialRgb.first) }
    var green by remember { mutableIntStateOf(initialRgb.second) }
    var blue by remember { mutableIntStateOf(initialRgb.third) }
    var hexDraft by remember { mutableStateOf(category.color.removePrefix("#")) }
    var limitText by remember(existingLimit) {
        mutableStateOf(existingLimit?.amount?.let { formatLimitInputCs(it) } ?: "")
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_category)) },
            text = { Text(stringResource(R.string.delete_category_confirm, category.displayName())) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.yes_upper))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.no_upper))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large icon circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    selectedColor.toSafeColor(MaterialTheme.colorScheme.primary)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(selectedIcon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.category_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        // Icon picker
        Text(
            text = stringResource(R.string.select_icon),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )

        // Quick icons row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(quickIcons) { entry ->
                SettingsIconCircle(
                    icon = entry.icon,
                    isSelected = selectedIcon == entry.key,
                    selectedColor = selectedColor,
                    onClick = { selectedIcon = entry.key }
                )
            }
        }

        // More icons expandable
        Row(
            modifier = Modifier
                .clickable { showMoreIcons = !showMoreIcons }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (showMoreIcons) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.more_icons),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = showMoreIcons) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                iconGroups.forEach { group ->
                    Text(
                        text = stringResource(group.nameResId),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier.heightIn(max = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        userScrollEnabled = false
                    ) {
                        items(group.icons) { entry ->
                            SettingsIconCircle(
                                icon = entry.icon,
                                isSelected = selectedIcon == entry.key,
                                selectedColor = selectedColor,
                                onClick = { selectedIcon = entry.key },
                                size = 40
                            )
                        }
                    }
                }
            }
        }

        // Color picker
        Text(
            text = stringResource(R.string.select_color),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            settingsPresetColors.take(6).forEach { hex ->
                SettingsColorCircle(
                    hex = hex,
                    isSelected = selectedColor.equals(hex, ignoreCase = true),
                    onClick = {
                        selectedColor = hex
                        showCustomColor = false
                    }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            settingsPresetColors.drop(6).forEach { hex ->
                SettingsColorCircle(
                    hex = hex,
                    isSelected = selectedColor.equals(hex, ignoreCase = true),
                    onClick = {
                        selectedColor = hex
                        showCustomColor = false
                    }
                )
            }
        }

        // Custom color toggle
        TextButton(
            onClick = { showCustomColor = !showCustomColor },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.custom_color),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        // Custom color picker with RGB sliders + HEX input
        AnimatedVisibility(visible = showCustomColor) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(red, green, blue)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(selectedIcon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // HEX input
                OutlinedTextField(
                    value = hexDraft,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetterOrDigit() }.take(6)
                        hexDraft = filtered
                        if (filtered.length == 6) {
                            val (r, g, b) = parseHexColor("#$filtered")
                            red = r; green = g; blue = b
                            selectedColor = "#${filtered.uppercase()}"
                        }
                    },
                    label = { Text(stringResource(R.string.hex_color_label)) },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // R slider
                SettingsColorSliderRow(
                    label = "R",
                    value = red,
                    color = Color.Red,
                    onValueChange = {
                        red = it
                        val hex = buildHex(red, green, blue)
                        selectedColor = hex
                        hexDraft = hex.removePrefix("#")
                    }
                )

                // G slider
                SettingsColorSliderRow(
                    label = "G",
                    value = green,
                    color = Color(0xFF4CAF50),
                    onValueChange = {
                        green = it
                        val hex = buildHex(red, green, blue)
                        selectedColor = hex
                        hexDraft = hex.removePrefix("#")
                    }
                )

                // B slider
                SettingsColorSliderRow(
                    label = "B",
                    value = blue,
                    color = Color.Blue,
                    onValueChange = {
                        blue = it
                        val hex = buildHex(red, green, blue)
                        selectedColor = hex
                        hexDraft = hex.removePrefix("#")
                    }
                )
            }
        }

        MonthlyLimitSection(
            category = category,
            baseCurrency = baseCurrency,
            limitText = limitText,
            onLimitTextChange = { limitText = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save button — persists name/icon/color AND the limit in one operation
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(category.copy(
                        name = name.trim(),
                        iconName = selectedIcon,
                        color = selectedColor
                    ))
                    if (category.type == CategoryType.EXPENSE) {
                        val parsed = limitText.replace(',', '.').toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            onSetLimit(parsed)
                        } else if (existingLimit != null) {
                            onClearLimit()
                        }
                    }
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(R.string.save),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Delete button
        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.delete_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Helper composables
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsColorCircle(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = hex.toSafeColor()

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            )
            .clickable { onClick() }
    )
}

@Composable
private fun SettingsIconCircle(
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: String,
    onClick: () -> Unit,
    size: Int = 44
) {
    val bgColor = if (isSelected) {
        selectedColor.toSafeColor(MaterialTheme.colorScheme.primary)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

@Composable
private fun SettingsColorSliderRow(
    label: String,
    value: Int,
    color: Color,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(30.dp),
            fontSize = 12.sp
        )
    }
}

private fun parseHexColor(hex: String): Triple<Int, Int, Int> {
    return try {
        val c = hex.toColorInt()
        Triple(
            android.graphics.Color.red(c),
            android.graphics.Color.green(c),
            android.graphics.Color.blue(c)
        )
    } catch (_: Exception) {
        Triple(33, 150, 243)
    }
}

private fun buildHex(r: Int, g: Int, b: Int): String =
    "#%02X%02X%02X".format(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

// ═══════════════════════════════════════════════════════════════════════
// Icon & color data (reused from CreateCategoryBottomSheet)
// ═══════════════════════════════════════════════════════════════════════

private val quickIcons = listOf(
    IconEntry("shopping_cart", Icons.Default.ShoppingCart),
    IconEntry("restaurant", Icons.Default.Restaurant),
    IconEntry("directions_car", Icons.Default.DirectionsCar),
    IconEntry("home", Icons.Default.Home),
    IconEntry("work", Icons.Default.Work),
    IconEntry("movie", Icons.Default.Movie),
    IconEntry("fitness_center", Icons.Default.FitnessCenter),
    IconEntry("card_giftcard", Icons.Default.CardGiftcard),
    IconEntry("flight", Icons.Default.Flight),
    IconEntry("devices", Icons.Default.Devices)
)

private val iconGroups = listOf(
    IconGroup(R.string.icon_group_finance, listOf(
        IconEntry("work", Icons.Default.Work),
        IconEntry("trending_up", Icons.AutoMirrored.Filled.TrendingUp),
        IconEntry("account_balance", Icons.Default.AccountBalance),
        IconEntry("savings", Icons.Default.Savings),
        IconEntry("credit_card", Icons.Default.CreditCard),
        IconEntry("payments", Icons.Default.Payments),
        IconEntry("attach_money", Icons.Default.AttachMoney),
        IconEntry("percent", Icons.Default.Percent),
        IconEntry("sell", Icons.Default.Sell),
        IconEntry("replay", Icons.Default.Replay)
    )),
    IconGroup(R.string.icon_group_transport, listOf(
        IconEntry("directions_car", Icons.Default.DirectionsCar),
        IconEntry("flight", Icons.Default.Flight),
        IconEntry("train", Icons.Default.Train),
        IconEntry("directions_bus", Icons.Default.DirectionsBus),
        IconEntry("two_wheeler", Icons.Default.TwoWheeler),
        IconEntry("directions_bike", Icons.AutoMirrored.Filled.DirectionsBike),
        IconEntry("local_shipping", Icons.Default.LocalShipping),
        IconEntry("sailing", Icons.Default.Sailing)
    )),
    IconGroup(R.string.icon_group_food, listOf(
        IconEntry("shopping_cart", Icons.Default.ShoppingCart),
        IconEntry("restaurant", Icons.Default.Restaurant),
        IconEntry("local_cafe", Icons.Default.LocalCafe),
        IconEntry("local_bar", Icons.Default.LocalBar),
        IconEntry("bakery_dining", Icons.Default.BakeryDining),
        IconEntry("fastfood", Icons.Default.Fastfood),
        IconEntry("liquor", Icons.Default.Liquor),
        IconEntry("cake", Icons.Default.Cake)
    )),
    IconGroup(R.string.icon_group_home, listOf(
        IconEntry("home", Icons.Default.Home),
        IconEntry("apartment", Icons.Default.Apartment),
        IconEntry("fitness_center", Icons.Default.FitnessCenter),
        IconEntry("spa", Icons.Default.Spa),
        IconEntry("local_hospital", Icons.Default.LocalHospital),
        IconEntry("school", Icons.Default.School),
        IconEntry("church", Icons.Default.Church),
        IconEntry("park", Icons.Default.Park)
    )),
    IconGroup(R.string.icon_group_entertainment, listOf(
        IconEntry("movie", Icons.Default.Movie),
        IconEntry("sports_esports", Icons.Default.SportsEsports),
        IconEntry("music_note", Icons.Default.MusicNote),
        IconEntry("sports_soccer", Icons.Default.SportsSoccer),
        IconEntry("casino", Icons.Default.Casino),
        IconEntry("theater_comedy", Icons.Default.TheaterComedy),
        IconEntry("headphones", Icons.Default.Headphones)
    )),
    IconGroup(R.string.icon_group_other, listOf(
        IconEntry("pets", Icons.Default.Pets),
        IconEntry("checkroom", Icons.Default.Checkroom),
        IconEntry("devices", Icons.Default.Devices),
        IconEntry("more_horiz", Icons.Default.MoreHoriz),
        IconEntry("card_giftcard", Icons.Default.CardGiftcard),
        IconEntry("star", Icons.Default.Star),
        IconEntry("favorite", Icons.Default.Favorite),
        IconEntry("category", Icons.Default.Category)
    ))
)

