package com.example.appfinancetest.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.components.TransactionRow

@Composable
fun TransactionsLabelDialog(
    selectedLabel: String?,
    othersTransactions: List<TransactionDB>? = null,
    filteredTransactions: List<TransactionDB> = emptyList(),
    selectedCategory: String? = null,
    selectedItem: String? = null,
    isVisibilityOff: Boolean,
    onDismiss: () -> Unit,
    onTransactionClick: (TransactionDB) -> Unit
) {
    if (selectedLabel != null || othersTransactions != null) {
        val labelTransactions = othersTransactions ?: filteredTransactions.filter { 
            it.category == selectedCategory && 
            it.item == selectedItem && 
            it.label == selectedLabel 
        }

        val sortedTransactions = labelTransactions.sortedByDescending { it.date }

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
                            text = selectedLabel ?: "Others",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
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
