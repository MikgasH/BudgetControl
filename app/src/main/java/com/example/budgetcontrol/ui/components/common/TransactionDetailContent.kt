package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Карточка с суммой
        TransactionAmountCard(
            transaction = transaction
        )

        // Категория
        DetailItem(
            label = "Категория",
            value = category?.name ?: "Неизвестная категория",
            icon = getCategoryIcon(category?.iconName),
            iconColor = category?.let {
                Color(android.graphics.Color.parseColor(it.color))
            } ?: AppBlue
        )

        // Дата
        DetailItem(
            label = "Дата",
            value = SimpleDateFormat("d MMMM yyyy 'г.'", Locale.getDefault())
                .format(Date(transaction.date))
        )

        // Время
        DetailItem(
            label = "Время",
            value = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(transaction.date))
        )

        // Комментарий
        transaction.description?.takeIf { it.isNotBlank() }?.let { description ->
            DetailItem(
                label = "Комментарий",
                value = description
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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
                text = "УДАЛИТЬ",
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