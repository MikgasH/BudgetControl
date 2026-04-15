package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.TransactionType
import java.util.Currency

@Composable
fun AmountInputCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    transactionType: TransactionType,
    currency: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    hint: String? = null
) {
    val title = when (transactionType) {
        TransactionType.EXPENSE -> stringResource(R.string.expense_amount)
        TransactionType.INCOME -> stringResource(R.string.income_amount)
    }

    val currencySymbol = try {
        Currency.getInstance(currency).symbol
    } catch (_: IllegalArgumentException) {
        currency
    }

    val accent = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                readOnly = readOnly,
                placeholder = {
                    Text(
                        "0",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                suffix = {
                    Text(
                        currencySymbol,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = accent
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = accent.copy(alpha = 0.5f),
                    focusedTextColor = accent,
                    unfocusedTextColor = accent,
                    focusedLabelColor = accent,
                    cursorColor = accent,
                    disabledTextColor = accent,
                    disabledBorderColor = accent.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    if (hint != null) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}
