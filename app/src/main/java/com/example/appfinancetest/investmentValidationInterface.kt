package com.example.appfinancetest

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.set

@Composable
fun InvestmentValidationInterface (
    databaseViewModel: DataBase_ViewModel,
    investmentViewModel: InvestmentDB_ViewModel,
    onDismiss: () -> Unit
) {
    val tabTitles = listOf("Ongoing", "Validated")
    val pageSize = 100
    var dateBeginFilter by remember { mutableStateOf("") }
    var dateEndFilter by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var investedFilter by remember { mutableStateOf("") }
    var earnedFilter by remember { mutableStateOf("") }

    var dateBeginFilterValidated by remember { mutableStateOf("") }
    var dateEndFilterValidated by remember { mutableStateOf("") }
    var itemFilterValidated by remember { mutableStateOf("") }
    var labelFilterValidated by remember { mutableStateOf("") }
    var investedFilterValidated by remember { mutableStateOf("") }
    var earnedFilterValidated by remember { mutableStateOf("") }
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

    var currentPage = when(selectedTabIndex) {
        0 -> currentPageOngoing
        1 -> currentPageFinished
        else -> 0
    }

    LaunchedEffect(listState, selectedTabIndex) {
        delay(200)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleItemIndex ->
            val currentListSize = when(selectedTabIndex) {
                0 -> ongoingToShow.size
                1 -> finishedToShow.size
                else -> 0
            }
            if (lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= currentListSize - beforeRefresh
            ) {
                when(selectedTabIndex) {
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

            val ongoingFiltered = filterInvestments(
                ongoingInvestments,
                dateBeginFilter, dateEndFilter, itemFilter, labelFilter, investedFilter, earnedFilter
            )
            val finishedFiltered = filterInvestments(
                finishedInvestments,
                dateBeginFilterValidated, dateEndFilterValidated, itemFilterValidated, labelFilterValidated, investedFilterValidated, earnedFilterValidated
            )

            Log.d("InvestmentScreen", "OngoingFetched: ${ongoingInvestments.size} - After filter: ${ongoingFiltered.size}")
            Log.d("InvestmentScreen", "ValidatedFetched: ${finishedInvestments.size} - After filter: ${finishedFiltered.size}")

            if (isFirstLoad || refreshTrigger > 0 || currentPage == 1) {
                ongoingToShow.clear()
                finishedToShow.clear()
                isFirstLoad = false
                refreshTrigger = 0
            }
            ongoingToShow.addAll(ongoingFiltered)
            finishedToShow.addAll(finishedFiltered)
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 600.dp)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Validate operations",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    when (selectedTabIndex) {
                        0 -> listOf(
                            "Began",
                            "Item",
                            "Label",
                            "Invested",
                            "Earned",
                            "Validated"
                        ).forEach {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }

                        1 -> listOf("Ended", "Item", "Label", "Invested", "Earned", "Validated").forEach {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }

                        else -> {}
                    }
                }

                val currentList = if (selectedTabIndex == 0) ongoingToShow else finishedToShow

                LazyColumn(state = listState) {
                    if (currentList.isEmpty()) {
                        item {
                            Text(
                                "No investment to print",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(currentList) { investmentDB ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp, vertical = 0.dp)
                            ) {
                                val data = when (selectedTabIndex) {
                                    0 -> listOf(
                                        dateFormattedText(investmentDB.dateBegin),
                                        when (investmentDB.item) {
                                            "Bourse - PEA" -> "PEA"
                                            "Bourse - Compte titre" -> "Compte titre"
                                            else -> investmentDB.item ?: "N/A"
                                        },
                                        investmentDB.label ?: "N/A",
                                        "%.0f €".format(investmentDB.invested ?: 0.0),
                                        "%.0f €".format(investmentDB.earned ?: 0.0)
                                    )

                                    1 -> listOf(
                                        dateFormattedText(investmentDB.dateEnd),
                                        when (investmentDB.item) {
                                            "Bourse - PEA" -> "PEA"
                                            "Bourse - Compte titre" -> "Compte titre"
                                            else -> investmentDB.item ?: "N/A"
                                        },
                                        investmentDB.label ?: "N/A",
                                        "%.0f €".format(investmentDB.invested ?: 0.0),
                                        "%.0f €".format(investmentDB.earned ?: 0.0)
                                    )

                                    else -> emptyList()
                                }
                                data.forEach {
                                    Text(
                                        it,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val isValidatedTab = (selectedTabIndex == 1)
                                val checkedItems = remember { mutableStateMapOf<String, Boolean>() }
                                Checkbox(
                                    checked = checkedItems[investmentDB.idInvest.orEmpty()]
                                        ?: isValidatedTab,
                                    onCheckedChange = { isChecked ->
                                        investmentDB.idInvest?.let { id ->
                                            checkedItems[id] = isChecked
                                            scope.launch {
                                                if (isValidatedTab) {
                                                    if (!isChecked) {
                                                        invalidateInvestments(
                                                            databaseViewModel,
                                                            investmentViewModel,
                                                            id
                                                        ) {
                                                            refreshTrigger++
                                                        }
                                                    }
                                                } else {
                                                    if (isChecked) {
                                                        validateInvestments(
                                                            databaseViewModel,
                                                            investmentViewModel,
                                                            id
                                                        ) {
                                                            refreshTrigger++
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
