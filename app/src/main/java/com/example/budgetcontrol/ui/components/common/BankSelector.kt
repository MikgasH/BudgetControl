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

    val displayValue = selectedBank?.let {
        "${it.name} (${formatCommission(it.commissionPercent)})"
    } ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (banks.isNotEmpty()) expanded = !expanded
        },
        modifier = modifier
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

