package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Bank

private fun formatCommission(percent: Double): String {
    return if (percent == percent.toLong().toDouble()) {
        "${percent.toLong()}%"
    } else {
        "$percent%"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelector(
    banks: List<Bank>,
    selectedBank: Bank?,
    onBankSelect: (Bank) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showCommissionInfo by remember { mutableStateOf(false) }

    val displayValue = selectedBank?.let {
        "${it.name} (${formatCommission(it.commissionPercent)})"
    } ?: ""

    if (showCommissionInfo) {
        AlertDialog(
            onDismissRequest = { showCommissionInfo = false },
            confirmButton = {
                TextButton(onClick = { showCommissionInfo = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.commission_info_tooltip),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (banks.isNotEmpty()) expanded = !expanded
            },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.bank_payment_method)) },
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
                banks.forEach { bank ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = bank.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = formatCommission(bank.commissionPercent),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onBankSelect(bank)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        IconButton(
            onClick = { showCommissionInfo = true },
            modifier = Modifier
                .padding(start = 4.dp, bottom = 4.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ConversionPreview(
    preview: String,
    modifier: Modifier = Modifier
) {
    if (preview.isNotBlank()) {
        Text(
            text = preview,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

