package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.ui.util.getCategoryIcon
import com.example.budgetcontrol.ui.util.toSafeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelector(
    accounts: List<AccountWithBalance>,
    selectedAccountId: String?,
    onAccountSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.account.id == selectedAccountId }
    val displayValue = selectedAccount?.let {
        "${it.account.name} (${formatBalance(it.currentBalance, it.account.currency)})"
    } ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (accounts.isNotEmpty()) expanded = !expanded
        },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.account_label)) },
            leadingIcon = selectedAccount?.let {
                {
                    Icon(
                        imageVector = getCategoryIcon(it.account.iconName),
                        contentDescription = null,
                        tint = it.account.color.toSafeColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
        ) {
            accounts.forEach { accountWithBalance ->
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(accountWithBalance.account.iconName),
                            contentDescription = null,
                            tint = accountWithBalance.account.color.toSafeColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = accountWithBalance.account.name,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                text = formatBalance(accountWithBalance.currentBalance, accountWithBalance.account.currency),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onAccountSelect(accountWithBalance.account.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

private fun formatBalance(balance: Double, currency: String): String {
    return if (balance == balance.toLong().toDouble()) {
        "${balance.toLong()} $currency"
    } else {
        String.format(java.util.Locale.US, "%.2f %s", balance, currency)
    }
}
