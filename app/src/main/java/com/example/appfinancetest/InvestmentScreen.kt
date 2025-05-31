package com.example.appfinancetest

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(modifier: Modifier = Modifier, databaseViewModel: DataBase_ViewModel, investmentViewModel: InvestmentDB_ViewModel) {
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), databaseViewModel) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    val validDates = transactions.mapNotNull { it.date }
    if (validDates.isEmpty()) {
        Text("No data available")
        return
    }

    val minDate = validDates.minOrNull()!!.toFloat()
    val maxDate = validDates.maxOrNull()!!.toFloat()
    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }

    // State showing preferences are loaded
    var showDateRangePicker by remember { mutableStateOf(false) }
    var isPrefsLoaded by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(minDate..maxDate) }

    // Printing investments
    val scope = rememberCoroutineScope()
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

    val listState = rememberLazyListState()
    var currentPageOngoing by remember { mutableIntStateOf(1) }
    var currentPageFinished by remember { mutableIntStateOf(1) }
    val ongoingToShow = remember { mutableStateListOf<InvestmentDB>() }
    val finishedToShow = remember { mutableStateListOf<InvestmentDB>() }
    val pageSize = 100
    val beforeRefresh = 20
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isFirstLoad by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
    var currentPage = when(selectedTabIndex) {
        0 -> currentPageOngoing
        1 -> currentPageFinished
        else -> 0
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

    // Load preferences
    LaunchedEffect(Unit) {
        prefs.startDateFlow.combine(prefs.endDateFlow) { start, end -> start to end }
            .collect { (savedStart, savedEnd) ->
                if (savedStart != null && savedEnd != null) {
                    range = savedStart..savedEnd
                } else {
                    range = minDate..maxDate
                }
                isPrefsLoaded = true
            }
    }

    // Only saving if preferences are loaded
    LaunchedEffect(range, isPrefsLoaded) {
        if (isPrefsLoaded) {
            prefs.saveStartDate(range.start)
            prefs.saveEndDate(range.endInclusive)
        }
    }

    // Doesn't show until prefs are loaded
    if (!isPrefsLoaded) {
        Text("Loading of data range...")
        return
    }
    var showDialog by remember { mutableStateOf(false) }

    val tabTitles = listOf("Ongoing", "Validated")

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
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
                            onClick = { showDialog = false },
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
                            0 -> listOf("Began","Item", "Label", "Invested", "Earned", "Validated").forEach {
                                Text(
                                    it,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp
                                )
                            }
                            1 -> listOf("End", "Item", "Label", "Invested", "Earned", "Validated").forEach {
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
                                            DateFormattedText(investmentDB.dateBegin),
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
                                            DateFormattedText(investmentDB.dateEnd),
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
                                        checked = checkedItems[investmentDB.idInvest.orEmpty()] ?: isValidatedTab,
                                        onCheckedChange = {isChecked ->
                                            investmentDB.idInvest?.let { id ->
                                                checkedItems[id] = isChecked
                                                scope.launch {
                                                    if (isValidatedTab) {
                                                        if (!isChecked) {
                                                            invalidateInvestments(databaseViewModel, investmentViewModel, id) {
                                                                refreshTrigger++
                                                            }
                                                        }
                                                    } else {
                                                        if (isChecked) {
                                                            validateInvestments(databaseViewModel, investmentViewModel, id) {
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


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Investment",
                        color = Color.White, // Text color
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top =18.dp)
                    )
                },
                modifier = Modifier,
                navigationIcon = {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        FloatingActionButton(
                            onClick = {showDialog = true},
                            modifier = Modifier.size(40.dp),
                            containerColor = Color.White,
                            contentColor = Color.Blue,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(
                                painterResource(id = R.drawable.ic_validate_investment),
                                contentDescription = "Validate Investment",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(paddingValues) // add padding for top bar space
            ) {
                // Display Date Range and controls in Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date picker button with calendar image
                    IconButton(
                        onClick = { showDateRangePicker = true },
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp)
                            .padding(end = 16.dp),
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_calendar),
                            contentDescription = "Choose dates",
                        )
                    }

                    // Column for displaying date range and RangeSlider
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Range of dates : ${
                                DateFormattedText(range.start.roundToInt().toDouble())
                            } à ${DateFormattedText(range.endInclusive.roundToInt().toDouble())}",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )

                        // Box around RangeSlider to control its size
                        Box(
                            modifier = Modifier
                                .padding(vertical = 0.dp)
                                .fillMaxWidth()
                                .height(20.dp)
                        ) {
                            // RangeSlider inside the Box
                            RangeSlider(
                                value = range,
                                onValueChange = { range = it },
                                valueRange = minDate..maxDate,
                                steps = ((maxDate - minDate) / 10).toInt(),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Blue, // Cursor color
                                    activeTrackColor = Color.Green, // Active track color
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f), // Inactive track color
                                    activeTickColor = Color.Transparent, // Hide active "ticks"
                                    inactiveTickColor = Color.Transparent // Hide inactive "ticks"
                                )
                            )
                        }
                    }
                }

                // Show DateRangePicker only if showDateRangePicker is true
                if (showDateRangePicker) {
                    DateRangePickerDialog(
                        onDismiss = { showDateRangePicker = false },
                        onDateSelected = { start, end ->
                            range = start..end
                            showDateRangePicker = false
                        }
                    )
                }
                BalanceLineChart(
                    viewModel = databaseViewModel,
                    startDate = range.start.toDouble(),
                    endDate = range.endInclusive.toDouble()
                )
            }
        }
    )
}
