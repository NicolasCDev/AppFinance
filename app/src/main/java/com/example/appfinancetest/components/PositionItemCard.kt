package com.example.appfinancetest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.R
import com.example.appfinancetest.calculations.CurrencyText
import com.example.appfinancetest.calculations.CurrencyTextOnPrimary
import com.example.appfinancetest.calculations.PercentageText
import com.example.appfinancetest.classes.InvestmentDB

@Composable
fun PositionItemCard(investment: InvestmentDB, isVisibilityOff: Boolean) {
    val invested = investment.invested ?: 0.0
    val earned = investment.earned ?: 0.0
    val profitEuro = earned - invested
    val profitPercent = if (invested > 0) (profitEuro / invested) * 100 else 0.0
    val annual = investment.annualProfitability ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = investment.label ?: stringResource(id = R.string.no_label),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            amount = profitEuro,
                            isVisibilityOff = isVisibilityOff,
                            showSign = true,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        PercentageText(
                            amount = profitPercent,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PercentageText(
                            amount = annual,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = " (${stringResource(id = R.string.annual)})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoLabelSmall(label = stringResource(id = R.string.invested), amount = invested, isVisibilityOff = isVisibilityOff)
                InfoLabelSmall(label = stringResource(id = R.string.earned), amount = earned, isVisibilityOff = isVisibilityOff)
            }
        }
    }
}

@Composable
fun InfoLabelSmall(label: String, amount: Double, isVisibilityOff: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall)
        CurrencyTextOnPrimary(
            amount = amount,
            isVisibilityOff = isVisibilityOff,
            style = MaterialTheme.typography.bodySmall
        )
    }
}