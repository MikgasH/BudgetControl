package com.example.budgetcontrol.ui.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.usecase.AccountGroupWithBalance
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.ui.util.getCategoryIcon
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountsBottomSheet(
    accounts: List<AccountWithBalance>,
    groups: List<AccountGroupWithBalance> = emptyList(),
    selectedAccountId: String?,
    selectedGroupId: String? = null,
    totalBalance: Double,
    baseCurrency: String,
    onAccountSelect: (String?) -> Unit,
    onGroupSelect: (String) -> Unit = {},
    onCreateAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onCreateGroup: () -> Unit = {},
    onEditGroup: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var contextMenuAccountId by remember { mutableStateOf<String?>(null) }
    var contextMenuGroupId by remember { mutableStateOf<String?>(null) }

    val filteredAccounts = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts
        else accounts.filter { it.account.name.contains(searchQuery, ignoreCase = true) }
    }

    val filteredGroups = remember(groups, searchQuery) {
        if (searchQuery.isBlank()) groups
        else groups.filter { it.group.name.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_accounts)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // "All accounts" item
                if (searchQuery.isBlank()) {
                    item {
                        val isSelected = selectedAccountId == null && selectedGroupId == null
                        val bgColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            label = "allBg"
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        onAccountSelect(null)
                                        onDismiss()
                                    }
                                ),
                            color = bgColor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.all_accounts),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = formatAccountBalance(totalBalance, baseCurrency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // Groups section
                if (filteredGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.account_groups_header),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(filteredGroups, key = { "group_${it.group.id}" }) { groupWithBalance ->
                        val group = groupWithBalance.group
                        val isSelected = selectedGroupId == group.id
                        val bgColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            label = "groupBg"
                        )

                        Box {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = {
                                            contextMenuGroupId = null
                                            onGroupSelect(group.id)
                                            onDismiss()
                                        },
                                        onLongClick = {
                                            contextMenuGroupId = group.id
                                        }
                                    ),
                                color = bgColor,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.group_member_count,
                                                groupWithBalance.memberCount
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatAccountBalance(
                                            groupWithBalance.combinedBalance,
                                            baseCurrency
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (groupWithBalance.combinedBalance >= 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = contextMenuGroupId == group.id,
                                onDismissRequest = { contextMenuGroupId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_group)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        contextMenuGroupId = null
                                        onEditGroup(group.id)
                                    }
                                )
                            }
                        }
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // Accounts section header (only when groups are present)
                if (filteredGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.accounts_header),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                // Account items
                items(filteredAccounts, key = { it.account.id }) { accountWithBalance ->
                    val account = accountWithBalance.account
                    val isSelected = selectedAccountId == account.id && selectedGroupId == null
                    val bgColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                        label = "accountBg"
                    )
                    val accountColor = try {
                        Color(android.graphics.Color.parseColor(account.color))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        contextMenuAccountId = null
                                        onAccountSelect(account.id)
                                        onDismiss()
                                    },
                                    onLongClick = {
                                        contextMenuAccountId = account.id
                                    }
                                ),
                            color = bgColor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accountColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(account.iconName),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (account.currency != baseCurrency) {
                                        Text(
                                            text = account.currency,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = formatAccountBalance(
                                        accountWithBalance.currentBalance,
                                        account.currency
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (accountWithBalance.currentBalance >= 0) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = contextMenuAccountId == account.id,
                            onDismissRequest = { contextMenuAccountId = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_account)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    contextMenuAccountId = null
                                    onEditAccount(account.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_to_group)) },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                onClick = {
                                    contextMenuAccountId = null
                                    onCreateGroup()
                                }
                            )
                        }
                    }
                }

                // Empty search state
                if (filteredAccounts.isEmpty() && filteredGroups.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_accounts_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreateAccount,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.new_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = onCreateGroup,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.new_group),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatAccountBalance(balance: Double, currency: String): String {
    return if (balance == balance.toLong().toDouble()) {
        "${balance.toLong()} $currency"
    } else {
        String.format(Locale.US, "%.2f %s", balance, currency)
    }
}
