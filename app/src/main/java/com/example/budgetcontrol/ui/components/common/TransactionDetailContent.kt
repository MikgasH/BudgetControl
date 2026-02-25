package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.theme.AppBlue
import java.text.SimpleDateFormat
import java.util.*

/**
 * Общий контент для экранов детального просмотра транзакций
 */
@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    category: Category?,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Карточка с суммой
        TransactionAmountCard(
            transaction = transaction
        )

        // Категория
        DetailItem(
            label = stringResource(R.string.category),
            value = category?.name ?: stringResource(R.string.unknown_category),
            icon = getCategoryIcon(category?.iconName),
            iconColor = category?.let {
                Color(android.graphics.Color.parseColor(it.color))
            } ?: AppBlue
        )

        // Дата
        DetailItem(
            label = stringResource(R.string.detail_date),
            value = SimpleDateFormat(stringResource(R.string.date_format_full), Locale.getDefault())
                .format(Date(transaction.date))
        )

        // Время
        DetailItem(
            label = stringResource(R.string.detail_time),
            value = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(transaction.date))
        )

        // Оригинальная сумма и банк (только для конвертированных расходов)
        if (transaction is Transaction.ExpenseTransaction &&
            transaction.originalCurrency != "EUR"
        ) {
            val originalFormatted = String.format("%.2f", transaction.originalAmount)
                .trimEnd('0').trimEnd('.', ',')
            val displayName = try {
                Currency.getInstance(transaction.originalCurrency)
                    .getDisplayName(Locale.getDefault())
            } catch (_: IllegalArgumentException) {
                transaction.originalCurrency
            }
            DetailItem(
                label = stringResource(R.string.original_amount),
                value = "$originalFormatted ${transaction.originalCurrency} ($displayName)"
            )

            transaction.bankName?.let { name ->
                val commission = transaction.bankCommission
                val commissionStr = if (commission != null) {
                    val trimmed = if (commission == commission.toLong().toDouble())
                        commission.toLong().toString()
                    else
                        commission.toString()
                    " ($trimmed%)"
                } else ""
                val bankLabel = "$name$commissionStr"
                val isUserCorrected = transaction.rateSource == "USER_CORRECTED"

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

        // Комментарий
        transaction.description?.takeIf { it.isNotBlank() }?.let { description ->
            DetailItem(
                label = stringResource(R.string.detail_comment),
                value = description
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка удаления
        Button(
            onClick = onDeleteClick,
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
    modifier: Modifier = Modifier
) {
    val amountText = when (transaction.type) {
        TransactionType.EXPENSE -> "${String.format("%.2f", transaction.amount)} €"
        TransactionType.INCOME -> "+${String.format("%.2f", transaction.amount)} €"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppBlue
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
    icon: ImageVector? = null,
    iconColor: Color = AppBlue,
    modifier: Modifier = Modifier
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

/**
 * Получение иконки для категории
 */
@Composable
private fun getCategoryIcon(iconName: String?): ImageVector {
    return when (iconName) {
        // Иконки для расходов
        "shopping_cart" -> Icons.Default.ShoppingCart
        "directions_car" -> Icons.Default.DirectionsCar
        "movie" -> Icons.Default.Movie
        "local_hospital" -> Icons.Default.LocalHospital
        "home" -> Icons.Default.Home
        "subscriptions" -> Icons.Default.Subscriptions

        // Иконки для доходов
        "work" -> Icons.Default.Work
        "computer" -> Icons.Default.Computer
        "trending_up" -> Icons.Default.TrendingUp
        "card_giftcard" -> Icons.Default.CardGiftcard
        "sell" -> Icons.Default.Sell

        // По умолчанию
        else -> Icons.Default.Category
    }
}