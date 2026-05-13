package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.RateSource
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.ui.util.displayName
import com.example.budgetcontrol.ui.util.getCategoryIcon
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shared content for transaction detail screens
 */
@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    category: Category?,
    baseCurrency: String,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatPattern = stringResource(R.string.date_format_full)
    val fullDateFormatter = remember(dateFormatPattern, Locale.getDefault().language) {
        SimpleDateFormat(dateFormatPattern, Locale.getDefault())
    }
    val timeFormatter = remember(Locale.getDefault().language) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick()
                }) {
                    Text(
                        text = stringResource(R.string.delete_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TransactionAmountCard(
            transaction = transaction,
            baseCurrency = baseCurrency
        )

        DetailItem(
            label = stringResource(R.string.category),
            value = category?.displayName() ?: stringResource(R.string.unknown_category),
            icon = getCategoryIcon(category?.iconName),
            iconColor = category?.let {
                Color(it.color.toColorInt())
            } ?: MaterialTheme.colorScheme.primary
        )

        DetailItem(
            label = stringResource(R.string.detail_date),
            value = fullDateFormatter.format(Date(transaction.date))
        )

        DetailItem(
            label = stringResource(R.string.detail_time),
            value = timeFormatter.format(Date(transaction.date))
        )

        val originalCurrency = when (transaction) {
            is Transaction.ExpenseTransaction -> transaction.originalCurrency
            is Transaction.IncomeTransaction -> transaction.originalCurrency
        }
        val originalAmount = when (transaction) {
            is Transaction.ExpenseTransaction -> transaction.originalAmount
            is Transaction.IncomeTransaction -> transaction.originalAmount
        }
        val txBankName = when (transaction) {
            is Transaction.ExpenseTransaction -> transaction.bankName
            is Transaction.IncomeTransaction -> transaction.bankName
        }
        val txBankCommission = when (transaction) {
            is Transaction.ExpenseTransaction -> transaction.bankCommission
            is Transaction.IncomeTransaction -> transaction.bankCommission
        }
        val txRateSource = RateSource.fromStringOrNull(
            when (transaction) {
                is Transaction.ExpenseTransaction -> transaction.rateSource
                is Transaction.IncomeTransaction -> transaction.rateSource
            }
        )

        if (originalCurrency != baseCurrency) {
            val originalFormatted = formatAmount(originalAmount)
            val displayName = try {
                Currency.getInstance(originalCurrency)
                    .getDisplayName(Locale.getDefault())
            } catch (_: IllegalArgumentException) {
                originalCurrency
            }
            DetailItem(
                label = stringResource(R.string.original_amount),
                value = "$originalFormatted ${getCurrencySymbol(originalCurrency)} ($displayName)"
            )

            val isCashExchange = txRateSource == RateSource.CASH_EXCHANGE

            if (isCashExchange) {
                DetailItem(
                    label = stringResource(R.string.payment_method),
                    value = stringResource(R.string.cash)
                )
            }

            txBankName?.let { name ->
                val commissionStr = if (txBankCommission != null) {
                    val trimmed = if (txBankCommission == txBankCommission.toLong().toDouble())
                        txBankCommission.toLong().toString()
                    else
                        txBankCommission.toString()
                    " ($trimmed%)"
                } else ""
                val bankLabel = "$name$commissionStr"
                val isUserCorrected = txRateSource == RateSource.USER_CORRECTED

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.bank),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = bankLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isUserCorrected) {
                            Text(
                                text = "· " + stringResource(R.string.user_corrected),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        transaction.description?.takeIf { it.isNotBlank() }?.let { description ->
            DetailItem(
                label = stringResource(R.string.detail_comment),
                value = description
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.delete_upper),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun TransactionAmountCard(
    transaction: Transaction,
    baseCurrency: String,
    modifier: Modifier = Modifier
) {
    val amountText = when (transaction.type) {
        TransactionType.EXPENSE -> "${formatAmount(transaction.amount)} ${getCurrencySymbol(baseCurrency)}"
        TransactionType.INCOME -> "+${formatAmount(transaction.amount)} ${getCurrencySymbol(baseCurrency)}"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

