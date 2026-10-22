package com.nursetmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nursetmobile.domain.OrderStatus
import java.util.Locale

// Locale.US нужен, чтобы разделитель групп всегда был запятой и заменялся на пробел
fun formatPrice(price: Long): String {
    return String.format(Locale.US, "%,d ₸", price).replace(',', ' ')
}

@Composable
fun StatusChip(status: OrderStatus) {
    val (containerColor, textColor) = when (status) {
        OrderStatus.NEW -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.PROCESSING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Text(
        text = status.displayName,
        fontSize = 12.sp,
        color = textColor,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
