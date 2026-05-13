package com.example.budgetcontrol.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.domain.model.LookupState
import kotlinx.coroutines.launch

@Composable
internal fun BanksSection(
    banks: List<Bank>,
    onManageClick: () -> Unit
) {
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
                    text = pluralStringResource(R.plurals.banks_selected_count, favoriteCount, favoriteCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onManageClick) {
                Text(stringResource(R.string.manage))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BanksBottomSheet(
    banks: List<Bank>,
    onDismiss: () -> Unit,
    onToggleFavorite: (Bank) -> Unit,
    onSetDefault: (Bank) -> Unit,
    onEdit: (Bank) -> Unit,
    onDelete: (Bank) -> Unit,
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
    bank: Bank,
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

internal fun formatCommission(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        "${value.toLong()}%"
    } else {
        "$value%"
    }
}

@Composable
internal fun AddEditBankDialog(
    bank: Bank?,
    initialName: String? = null,
    lookupState: LookupState?,
    onLookup: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(bank?.name ?: initialName ?: "") }
    var commission by remember {
        mutableStateOf(
            bank?.let { formatCommission(it.commissionPercent).removeSuffix("%") } ?: ""
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
                        Text(stringResource(R.string.find_with_ai))
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
