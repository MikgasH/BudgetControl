package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.PendingCurrencyChange

@Composable
fun CurrencyChangeConfirmDialog(
    pending: PendingCurrencyChange,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_change_confirm_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.currency_change_confirm_message,
                        pending.fromCurrency,
                        pending.toCurrency
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.currency_change_conversion_line,
                        pending.oldInitialBalance,
                        pending.fromCurrency,
                        pending.newInitialBalance,
                        pending.toCurrency
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (pending.exchangeRate != null && pending.exchangeRate > 0.0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // exchangeRate from ConvertCurrencyUseCase is "how many FROM per 1 TO"
                        text = stringResource(
                            R.string.currency_change_rate_line,
                            pending.toCurrency,
                            pending.exchangeRate,
                            pending.fromCurrency
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.currency_change_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
