package com.example.appfinancetest.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.appfinancetest.R
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.InvestmentDB
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.components.PositionItemCard
import com.example.appfinancetest.components.invalidateInvestments
import com.example.appfinancetest.components.saveTransactionAndSyncInvestments
import com.example.appfinancetest.components.validateInvestments
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailDialog(
    category: String,
    investments: List<InvestmentDB>,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    isVisibilityOff: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.investment_current),
        stringResource(id = R.string.investment_closed)
    )

    var selectedInvestmentTransactions by remember { mutableStateOf<List<TransactionDB>?>(null) }
    var selectedInvestmentLabel by remember { mutableStateOf<String?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionDB?>(null) }
    var innerRefreshTrigger by remember { mutableIntStateOf(0) }

    val allTransactionsList by produceState<List<TransactionDB>>(initialValue = emptyList(), databaseViewModel, innerRefreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    TransactionsLabelDialog(
        selectedLabel = selectedInvestmentLabel,
        othersTransactions = selectedInvestmentTransactions,
        isVisibilityOff = isVisibilityOff,
        onDismiss = {
            selectedInvestmentLabel = null
            selectedInvestmentTransactions = null
        },
        onTransactionClick = { transactionToEdit = it }
    )

    val innerScope = rememberCoroutineScope()

    if (transactionToEdit != null) {
        TransactionEditDialog(
            transaction = transactionToEdit!!,
            onDismiss = { transactionToEdit = null },
            onSave = { updated ->
                innerScope.launch {
                    val prevInvest = transactionToEdit?.idInvest
                    saveTransactionAndSyncInvestments(
                        updated = updated,
                        previousIdInvest = prevInvest,
                        databaseViewModel = databaseViewModel,
                        investmentViewModel = investmentViewModel
                    )
                    innerRefreshTrigger++
                    onRefresh()
                    transactionToEdit = null
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredInvestments = remember(investments, selectedTabIndex) {
                    if (selectedTabIndex == 0) {
                        investments.filter { it.dateEnd == null || it.dateEnd == 0.0 }
                    } else {
                        investments.filter { it.dateEnd != null && it.dateEnd != 0.0 }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredInvestments.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(id = R.string.no_investment),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        // FIX: Key is modified to include selectedTabIndex to prevent state reuse between tabs
                        items(filteredInvestments, key = { "${it.id}_$selectedTabIndex" }) { investment ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd && selectedTabIndex == 0) true
                                    else value == SwipeToDismissBoxValue.EndToStart && selectedTabIndex == 1
                                }
                            )

                            // Effect to handle the actual database update after the swipe is confirmed
                            LaunchedEffect(dismissState.currentValue) {
                                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd && selectedTabIndex == 0) {
                                    validateInvestments(
                                        databaseViewModel,
                                        investmentViewModel,
                                        investment.idInvest
                                    ) {
                                        databaseViewModel.refreshNetWorth()
                                        onRefresh()
                                    }
                                } else if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && selectedTabIndex == 1) {
                                    invalidateInvestments(
                                        databaseViewModel,
                                        investmentViewModel,
                                        investment.idInvest
                                    ) {
                                        databaseViewModel.refreshNetWorth()
                                        onRefresh()
                                    }
                                }
                            }

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val isDismissingToEnd = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
                                    val isDismissingToStart = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

                                    val color = if (isDismissingToEnd) Color(0xFF4CAF50)
                                    else if (isDismissingToStart) Color.Gray
                                    else Color.Transparent

                                    val alignment = if (isDismissingToEnd) Alignment.CenterStart
                                    else if (isDismissingToStart) Alignment.CenterEnd
                                    else Alignment.Center

                                    val icon = if (isDismissingToEnd) Icons.Default.Done
                                    else if (isDismissingToStart) Icons.Default.Refresh
                                    else null

                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(color, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment
                                    ) {
                                        icon?.let { Icon(it, contentDescription = null, tint = Color.White) }
                                    }
                                },
                                enableDismissFromStartToEnd = selectedTabIndex == 0,
                                enableDismissFromEndToStart = selectedTabIndex == 1
                            ) {
                                Box(modifier = Modifier.clickable {
                                    selectedInvestmentLabel = investment.label ?: "Investment"
                                    selectedInvestmentTransactions = allTransactionsList.filter { it.idInvest == investment.idInvest }
                                }) {
                                    PositionItemCard(investment, isVisibilityOff)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}