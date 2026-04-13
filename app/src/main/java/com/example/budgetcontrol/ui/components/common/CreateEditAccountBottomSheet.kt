package com.example.budgetcontrol.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import androidx.core.graphics.toColorInt

private data class AccountIconEntry(val key: String, val icon: ImageVector)

private val accountIcons = listOf(
    AccountIconEntry("account_balance", Icons.Default.AccountBalance),
    AccountIconEntry("credit_card", Icons.Default.CreditCard),
    AccountIconEntry("savings", Icons.Default.Savings),
    AccountIconEntry("payments", Icons.Default.Payments),
    AccountIconEntry("attach_money", Icons.Default.AttachMoney),
    AccountIconEntry("work", Icons.Default.Work),
    AccountIconEntry("home", Icons.Default.Home),
    AccountIconEntry("shopping_cart", Icons.Default.ShoppingCart),
    AccountIconEntry("flight", Icons.Default.Flight),
    AccountIconEntry("star", Icons.Default.Star),
    AccountIconEntry("favorite", Icons.Default.Favorite),
    AccountIconEntry("wallet", Icons.Default.AccountBalanceWallet)
)

private val accountPresetColors = listOf(
    "#4CAF50", "#2196F3", "#F44336", "#E91E63",
    "#9C27B0", "#3F51B5", "#00BCD4", "#FF9800",
    "#8BC34A", "#FF5722", "#795548", "#607D8B"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditAccountBottomSheet(
    isEditMode: Boolean = false,
    account: Account? = null,
    baseCurrency: String = DEFAULT_BASE_CURRENCY,
    transactionCount: Int = 0,
    availableCurrencies: List<String> = emptyList(),
    favoriteCurrencies: Set<String> = emptySet(),
    isCurrenciesLoading: Boolean = false,
    onSave: (name: String, iconName: String, color: String, initialBalance: Double, currency: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val defaultIcon = "account_balance"
    val defaultColor = accountPresetColors[0]

    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(account?.iconName ?: defaultIcon) }
    var selectedColor by remember { mutableStateOf(account?.color ?: defaultColor) }
    var initialBalance by remember { mutableStateOf(
        if (account != null) {
            if (account.initialBalance == account.initialBalance.toLong().toDouble()) {
                account.initialBalance.toLong().toString()
            } else {
                account.initialBalance.toString()
            }
        } else ""
    ) }
    var currency by remember { mutableStateOf(account?.currency ?: baseCurrency) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDeleteConfirm && onDelete != null && account != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_account_confirm, account.name))
                    if (transactionCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.transactions_will_be_moved,
                                transactionCount,
                                stringResource(R.string.main_account)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(
                        stringResource(R.string.delete_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(if (isEditMode) R.string.edit_account else R.string.new_account),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            // Preview
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val previewColor = try {
                    Color(selectedColor.toColorInt())
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(previewColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveAccountIcon(selectedIcon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.account_name_label)) },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.account_name_required)) }
                } else null,
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
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(accountIcons) { entry ->
                    val isSelected = selectedIcon == entry.key
                    val iconColor = try {
                        Color(selectedColor.toColorInt())
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) iconColor else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedIcon = entry.key },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Color picker
            Text(
                text = stringResource(R.string.select_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                accountPresetColors.take(6).forEach { hex ->
                    AccountColorCircle(
                        hex = hex,
                        isSelected = selectedColor == hex,
                        onClick = { selectedColor = hex }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                accountPresetColors.drop(6).forEach { hex ->
                    AccountColorCircle(
                        hex = hex,
                        isSelected = selectedColor == hex,
                        onClick = { selectedColor = hex }
                    )
                }
            }

            // Currency selector
            val currencyChangeBlocked = isEditMode && transactionCount > 0
            if (availableCurrencies.isNotEmpty()) {
                Column {
                    CurrencySelector(
                        currencies = availableCurrencies,
                        selectedCurrency = currency,
                        onCurrencySelect = { if (!currencyChangeBlocked) currency = it },
                        baseCurrency = baseCurrency,
                        favoriteCurrencies = favoriteCurrencies,
                        isLoading = isCurrenciesLoading,
                        enabled = !currencyChangeBlocked
                    )
                    if (currencyChangeBlocked) {
                        Text(
                            text = stringResource(R.string.currency_change_blocked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }
            }

            // Initial balance
            OutlinedTextField(
                value = initialBalance,
                onValueChange = { input ->
                    // Allow digits, one dot/comma, optional leading minus
                    val filtered = input.replace(',', '.')
                    if (filtered.isEmpty() || filtered == "-" || filtered.toDoubleOrNull() != null
                        || (filtered.endsWith('.') && filtered.count { it == '.' } == 1)) {
                        initialBalance = filtered
                    }
                },
                label = { Text(stringResource(R.string.initial_balance_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text(currency) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // Save button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val balanceValue = initialBalance.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), selectedIcon, selectedColor, balanceValue, currency)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Delete button (edit mode only, not for default account)
            if (isEditMode && onDelete != null && account?.isDefault != true) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.delete_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountColorCircle(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try { Color(hex.toColorInt()) } catch (_: Exception) { Color.Gray }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun resolveAccountIcon(key: String): ImageVector {
    return accountIcons.find { it.key == key }?.icon ?: Icons.Default.AccountBalance
}
