package com.example.appfinancetest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.R
import com.example.appfinancetest.calculations.formatCurrency
import com.example.appfinancetest.calculations.formatMovementSubtitle
import com.example.appfinancetest.calculations.getCategoryIconAndBg
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.ui.theme.*
import kotlin.math.abs

@Composable
fun HomeMovementItem(
    item: TransactionDB,
    isVisibilityOff: Boolean,
    onClick: () -> Unit
) {
    val amount = item.amount ?: 0.0
    val isNegative = (item.variation ?: 0.0) < 0 || (item.category == "Charge" || item.category == "Investissement")
    val amountColor = if (isNegative) RedAccent else GreenAccent
    val sign = if (isNegative) "- " else "+ "

    val title = item.label ?: item.item ?: stringResource(id = R.string.purchase)
    val todayStr = stringResource(id = R.string.date_today)
    val yesterdayStr = stringResource(id = R.string.date_yesterday)
    val recentStr = stringResource(id = R.string.date_recent)
    val subtitle = formatMovementSubtitle(item, todayStr, yesterdayStr, recentStr)

    val (icon, iconBg) = remember(item) {
        getCategoryIconAndBg(item)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isVisibilityOff) "**** €" else "$sign${formatCurrency(abs(amount))}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
    }
}