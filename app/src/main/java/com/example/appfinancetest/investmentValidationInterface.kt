package com.example.appfinancetest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InvestmentValidationInterface(
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    val tabTitles = listOf(
        stringResource(id = R.string.investment_current),
        stringResource(id = R.string.investment_closed)
    )
    val pageSize = 100
    var isFirstLoad by remember { mutableStateOf(true) }
    val beforeRefresh = 20
    val ongoingToShow = remember { mutableStateListOf<InvestmentDB>() }
    val finishedToShow = remember { mutableStateListOf<InvestmentDB>() }
    val listState = rememberLazyListState()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var currentPageOngoing by remember { mutableIntStateOf(1) }
    var currentPageFinished by remember { mutableIntStateOf(1) }

    val currentPage = when (selectedTabIndex) {
        0 -> currentPageOngoing
        1 -> currentPageFinished
        else -> 0
    }

    LaunchedEffect(listState, selectedTabIndex) {
        delay(200)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleItemIndex ->
            val currentListSize = when (selectedTabIndex) {
                0 -> ongoingToShow.size
                1 -> finishedToShow.size
                else -> 0
            }
            if (lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= currentListSize - beforeRefresh
            ) {
                when (selectedTabIndex) {
                    0 -> currentPageOngoing += 1
                    1 -> currentPageFinished += 1
                }
            }
        }
    }

    LaunchedEffect(currentPage, refreshTrigger, selectedTabIndex) {
        scope.launch {
            if (refreshTrigger > 0) {
                currentPageOngoing = 1
                currentPageFinished = 1
            }
            val offset = (currentPage - 1) * pageSize
            val newInvestment = investmentViewModel.getPagedInvestments(pageSize, offset)

            val ongoingInvestments = newInvestment.filter { it.dateEnd == null || it.dateEnd == 0.0 }
            val finishedInvestments = newInvestment.filter { it.dateEnd != null && it.dateEnd != 0.0 }

            if (isFirstLoad || refreshTrigger > 0 || currentPage == 1) {
                ongoingToShow.clear()
                finishedToShow.clear()
                isFirstLoad = false
                refreshTrigger = 0
            }
            ongoingToShow.addAll(ongoingInvestments)
            finishedToShow.addAll(finishedInvestments)
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
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
                        stringResource(id = R.string.investment_management_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.close))
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val currentList = if (selectedTabIndex == 0) ongoingToShow else finishedToShow

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentList.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(id = R.string.no_investments_found), color = Color.Gray)
                            }
                        }
                    } else {
                        items(currentList) { investment ->
                            InvestmentItemCard(
                                investment = investment,
                                isOngoing = selectedTabIndex == 0,
                                onAction = {
                                    scope.launch {
                                        if (selectedTabIndex == 0) {
                                            validateInvestments(databaseViewModel, investmentViewModel, investment.idInvest) {
                                                refreshTrigger++
                                                databaseViewModel.refreshNetWorth()
                                                onRefresh()
                                            }
                                        } else {
                                            invalidateInvestments(databaseViewModel, investmentViewModel, investment.idInvest) {
                                                refreshTrigger++
                                                databaseViewModel.refreshNetWorth()
                                                onRefresh()
                                            }
                                        }
                                    }
                                },
                                onDateChange = { newDate ->
                                    scope.launch {
                                        val updated = investment.copy(dateEnd = newDate)
                                        investmentViewModel.updateInvestment(updated)
                                        refreshTrigger++
                                        databaseViewModel.refreshNetWorth()
                                        onRefresh()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvestmentItemCard(
    investment: InvestmentDB,
    isOngoing: Boolean,
    onAction: () -> Unit,
    onDateChange: (Double) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = investment.label ?: stringResource(id = R.string.no_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = investment.item ?: "N/A",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Checkbox(
                    checked = !isOngoing,
                    onCheckedChange = { onAction() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    InfoLabel(stringResource(id = R.string.beginning), dateFormattedText(investment.dateBegin))
                    if (!isOngoing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showDatePicker = true }) {
                            InfoLabel(stringResource(id = R.string.end), dateFormattedText(investment.dateEnd))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modifier",
                                modifier = Modifier.size(14.dp).padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    InfoLabel(stringResource(id = R.string.invested), "%.2f €".format(investment.invested ?: 0.0), true)
                    InfoLabel(stringResource(id = R.string.earned), "%.2f €".format(investment.earned ?: 0.0), true)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = investment.dateEnd ?: (System.currentTimeMillis() / (1000 * 86400.0) + 25569),
            onDismiss = { showDatePicker = false },
            onDateSelected = { 
                onDateChange(it)
                showDatePicker = false
            }
        )
    }
}

@Composable
fun InfoLabel(label: String, value: String, isBoldValue: Boolean = false) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            value, 
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isBoldValue) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DatePickerDialog(
    initialDate: Double,
    onDismiss: () -> Unit,
    onDateSelected: (Double) -> Unit
) {
    var dateString by remember { mutableStateOf(dateFormattedText(initialDate)) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(id = R.string.edit_closing_date), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text(stringResource(id = R.string.date_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.close))
                    }
                    Button(onClick = {
                        try {
                            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                            val date = sdf.parse(dateString)
                            if (date != null) {
                                val excelDate = (date.time / (1000 * 86400.0)) + 25569
                                onDateSelected(excelDate)
                            }
                        } catch (e: Exception) { }
                    }) {
                        Text(stringResource(id = R.string.filter_apply))
                    }
                }
            }
        }
    }
}
