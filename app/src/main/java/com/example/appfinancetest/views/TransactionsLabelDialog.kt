package com.example.appfinancetest.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.components.TransactionRow
import com.example.appfinancetest.ui.theme.BluePill
import java.util.Locale

@Composable
fun TransactionsLabelDialog(
    selectedLabel: String?,
    othersTransactions: List<TransactionDB>? = null,
    filteredTransactions: List<TransactionDB> = emptyList(),
    selectedCategory: String? = null,
    selectedItem: String? = null,
    isVisibilityOff: Boolean,
    databaseViewModel: DataBaseViewModel? = null,
    onDismiss: () -> Unit,
    onTransactionClick: (TransactionDB) -> Unit
) {
    var showStockMarketDialog by remember { mutableStateOf(false) }

    if (selectedLabel != null || othersTransactions != null) {
        val labelTransactions = othersTransactions ?: filteredTransactions.filter { 
            it.category == selectedCategory && 
            it.item == selectedItem && 
            it.label == selectedLabel 
        }

        val sortedTransactions = labelTransactions.sortedByDescending { it.date }

        val isSupportedRealtime = labelTransactions.any { tx ->
            val itemText = (tx.item ?: "").lowercase(Locale.ROOT)
            val labelText = (tx.label ?: "").lowercase(Locale.ROOT)
            val catText = (tx.category ?: "").lowercase(Locale.ROOT)

            !itemText.contains("crowd") && !labelText.contains("crowd") &&
            (itemText.contains("bourse") || itemText.contains("pea") || itemText.contains("compte") || itemText.contains("crypto") ||
             labelText.contains("bourse") || labelText.contains("pea") || labelText.contains("crypto") ||
             catText.contains("bourse") || catText.contains("crypto"))
        }

        if (showStockMarketDialog && databaseViewModel != null) {
            StockMarketRealtimeDialog(
                databaseViewModel = databaseViewModel,
                isVisibilityOff = isVisibilityOff,
                initialCategory = selectedItem ?: selectedCategory ?: "",
                targetTransactions = labelTransactions,
                targetLabel = selectedLabel,
                onDismiss = { showStockMarketDialog = false }
            )
        }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedLabel ?: "Autres",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSupportedRealtime && databaseViewModel != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BluePill)
                                        .clickable { showStockMarketDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "📈 Temps Réel",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sortedTransactions) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                isVisibilityOff = isVisibilityOff,
                                onClick = { onTransactionClick(transaction) }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}
