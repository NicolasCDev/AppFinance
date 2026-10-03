package com.example.appfinancetest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TransactionRow(
    transaction: TransactionDB,
    isVisibilityOff: Boolean,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.label ?: stringResource(id = R.string.no_label),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = "${dateFormattedText(transaction.date)} • ${transaction.category} • ${transaction.item}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val isNegative =
            transaction.category == "Charge" || transaction.category == "Investissement"

        // Use of CurrencyText for transactions too
        CurrencyText(
            amount = transaction.amount ?: 0.0,
            isNegative = isNegative,
            isVisibilityOff = isVisibilityOff,
            showSign = true,
            textAlign = TextAlign.End
        )
    }
}