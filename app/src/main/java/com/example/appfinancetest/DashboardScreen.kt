package com.example.appfinancetest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier, databaseViewModel: DataBase_ViewModel, investmentViewModel: InvestmentDB_ViewModel)  {

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    var showValidation by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    val validDates = transactions.mapNotNull { it.date }
    val fallbackMin = 0f
    val fallbackMax = 100f
    val minDate = validDates.minOrNull()?.toFloat() ?: fallbackMin
    val maxDate = validDates.maxOrNull()?.toFloat() ?: fallbackMax
    var isFirstLoad by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }
    var hideMarkerTrigger by remember { mutableIntStateOf(0) }

    if (showValidation) {
        InvestmentValidationInterface(databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel, onDismiss = { showValidation = false })
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
    if (showImportExport) {
        ImportExportInterface(databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel, onDismiss = { showImportExport = false }, onRefresh = {
            refreshTrigger++
            isFirstLoad = true
            currentPage = 1
        })
    }
    if (validDates.isEmpty()) {
        Scaffold(
            topBar = {
                TopBar(
                    onValidateClick = { showValidation = true },
                    onSettingsClick = { showSettings = true },
                    onImportExportClick = { showImportExport = true },
                    name = "Dashboard"
                )
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(paddingValues) // add padding for top bar space
                ) {
                    Text("No data available")
                }
            }
        )
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember { DataStorage(context) }

    // State showing preferences are loaded
    var showDateRangePicker by remember { mutableStateOf(false) }
    var isPrefsLoaded by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(minDate..maxDate) }

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
        Text("Date range loading...")
        return
    }

    // Scaffold to contain TopAppBar and the body content
    Scaffold(
        topBar = {
            TopBar(
                onValidateClick = { showValidation = true },
                onSettingsClick = { showSettings = true },
                onImportExportClick = { showImportExport = true },
                name = "Dashboard"
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(paddingValues) // add padding for top bar space
                    .pointerInput(Unit) {
                        detectTapGestures { _ ->
                            // Hide markers on any tap - let the charts handle their own interactions
                            hideMarkerTrigger++
                        }
                    }
            ) {
                if (validDates.isEmpty()) {
                    Text("No data available")
                } else {
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
                                text = "Plage de dates : ${
                                    dateFormattedText(range.start.roundToInt().toDouble())
                                } à ${dateFormattedText(range.endInclusive.roundToInt().toDouble())}",
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )

                            // Box around RangeSlider to control its size
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 0.dp)  // Padding autour de la Box
                                    .fillMaxWidth()  // Remplir toute la largeur disponible
                                    .height(20.dp)  // Définir une hauteur spécifique pour le RangeSlider
                            ) {
                                // RangeSlider inside the Box
                                RangeSlider(
                                    value = range,
                                    onValueChange = { range = it },
                                    valueRange = minDate..maxDate,
                                    steps = ((maxDate - minDate) / 10).toInt(),
                                    modifier = Modifier
                                        .align(Alignment.Center) // Aligner le RangeSlider au centre de la Box
                                        .fillMaxWidth(), // Rendre le RangeSlider aussi large que la Box
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Blue, // Couleur du curseur
                                        activeTrackColor = Color.Green, // Couleur de la piste active
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f), // Couleur de la piste inactive
                                        activeTickColor = Color.Transparent, // Masquer les "ticks" actifs
                                        inactiveTickColor = Color.Transparent // Masquer les "ticks" inactifs
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
                    val lastTransaction = transactions.filter {
                        it.date != null && it.date in range.start..range.endInclusive
                    }.maxByOrNull { it.date ?: Double.MIN_VALUE }

                    // Print balance of last transaction if it exists
                    val lastBalance = lastTransaction?.balance ?: 0.0

                    // Print balance on the top of the LineChart
                    Text(
                        text = "Balance at the end of the period: ${"%.2f".format(lastBalance)} €",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 0.dp)  // Padding around the Box
                            .fillMaxWidth()  // Fill all the width available
                            .height(230.dp)  // Define a specific height for graphics
                    ) {
                        LineChartPager(
                            databaseViewModel = databaseViewModel,
                            investmentViewModel = investmentViewModel,
                            range = range,
                            hideMarkerTrigger = hideMarkerTrigger,
                            onHideMarkers = { hideMarkerTrigger++ }
                        )
                    }

                    BalancePieChart(
                        viewModel = databaseViewModel,
                        startDate = range.start.toDouble(),
                        endDate = range.endInclusive.toDouble()
                    )
                }
            }
        }
    )
}