package com.example.appfinancetest

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
    var categoryFilter by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var investedFilter by remember { mutableStateOf("") }
    var earnedFilter by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    var currentPage by remember { mutableIntStateOf(1) }
    val investmentsToShow = remember { mutableStateListOf<InvestmentDB>() }
    val pageSize = 100
    val beforeRefresh = 20
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isFirstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        delay(200)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleItemIndex ->
            if (lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= investmentsToShow.size - beforeRefresh
            ) {
                currentPage += 1
            }
        }
    }

    LaunchedEffect(currentPage, refreshTrigger) {
        scope.launch {
            val offset = (currentPage - 1) * pageSize
            val newInvestment = investmentViewModel.getPagedInvestments(pageSize, offset)

            val filtered = filterInvestments(
                newInvestment,
                dateBeginFilter, dateEndFilter, itemFilter, labelFilter, investedFilter, earnedFilter
            )

            Log.d("InvestmentDebug", "Fetched: ${newInvestment.size} - After filter: ${filtered.size}")
            if (isFirstLoad) {
                investmentsToShow.clear()
                isFirstLoad = false
            }
            investmentsToShow.addAll(filtered)
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
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Validate operations") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "DateBegin",
                            "Item",
                            "Label",
                            "Invested",
                            "Earned"
                        ).forEach {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    LazyColumn(state = listState) {
                        if (investmentsToShow.isEmpty()) {
                            item {
                                Text(
                                    "No investment to print",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(investmentsToShow) { investmentDB ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    val data = listOf(
                                        DateFormattedText(investmentDB.dateBegin),
                                        investmentDB.item?: "N/A",
                                        investmentDB.label ?: "N/A",
                                        "%.2f €".format(investmentDB.invested ?: 0.0),
                                        "%.2f €".format(investmentDB.earned ?: 0.0)
                                    )
                                    data.forEach {
                                        Text(
                                            it,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
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
