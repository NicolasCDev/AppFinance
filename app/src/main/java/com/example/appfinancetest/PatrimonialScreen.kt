package com.example.appfinancetest

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PatrimonialScreen(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    creditViewModel: CreditDBViewModel
) {
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var hideMarkerTrigger by remember { mutableIntStateOf(0) }
    val netWorth by databaseViewModel.netWorth.observeAsState(0.0)
    
    val transactions by produceState(initialValue = emptyList(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }
    var showCreditScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    if (showSettings) {
        SettingsScreen(onDismiss = { 
            showSettings = false 
            refreshTrigger++ // Force refresh chart when settings are closed
        })
    }
    if (showImportExport) {
        ImportExportInterface(
            databaseViewModel = databaseViewModel,
            investmentViewModel = investmentViewModel,
            creditViewModel = creditViewModel,
            onDismiss = { showImportExport = false },
            onRefresh = {
                refreshTrigger++
                databaseViewModel.refreshNetWorth()
            }
        )
    }
    if (showCreditScreen) {
        CreditScreen(
            creditViewModel = creditViewModel,
            onDismiss = { showCreditScreen = false }
        )
    }

    val validDates = transactions.mapNotNull { it.date }
    val minDate = validDates.minOrNull()?: 0.0
    val todayExcel = (System.currentTimeMillis() / (1000 * 86400.0)) + 25569
    
    var isPrefsLoaded by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(minDate.toFloat()..todayExcel.toFloat()) }
    var selectedOption by remember { mutableStateOf(DateRangeOption.ALL_TIME) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Load preferences ONCE at startup to avoid feedback loop during saves
    LaunchedEffect(Unit) {
        try {
            val savedData = prefs.patrimonialStartDateFlow.combine(prefs.patrimonialEndDateFlow) { start, end -> start to end }
                .combine(prefs.patrimonialOptionFlow) { dates, option -> Triple(dates.first, dates.second, option) }
                .first() // Only take the current state from DataStore

            val (savedStart, savedEnd, savedOption) = savedData
            if (savedStart != null && savedEnd != null) {
                range = savedStart..savedEnd
            }
            if (savedOption != null) {
                try {
                    selectedOption = DateRangeOption.valueOf(savedOption)
                } catch (e: Exception) {
                    selectedOption = DateRangeOption.ALL_TIME
                }
            }
        } catch (e: Exception) {
            // Default values already set
        } finally {
            isPrefsLoaded = true
        }
    }

    // Save preferences when range or option changes
    LaunchedEffect(range, selectedOption, isPrefsLoaded) {
        if (isPrefsLoaded) {
            prefs.savePatrimonialStartDate(range.start)
            prefs.savePatrimonialEndDate(range.endInclusive)
            prefs.savePatrimonialOption(selectedOption.name)
            databaseViewModel.setNetWorthDate(range.endInclusive.toDouble())
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onSettingsClick = { showSettings = true },
                onImportExportClick = { showImportExport = true },
                onVisibilityClick = {
                    scope.launch { prefs.saveVisibilityState(!isVisibilityOff) }
                },
                isVisibilityOff = isVisibilityOff,
                name = stringResource(id = R.string.patrimonial_title)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        hideMarkerTrigger++
                    })
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.net_worth_simple),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isVisibilityOff) "**** €" else "${"%.2f".format(netWorth ?: 0.0)} €",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if ((netWorth ?: 0.0) >= 0) Color.Green else Color.Red
                )
            }

            if (isPrefsLoaded) {
                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            stringResource(id = R.string.no_transactions),
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        ImportActionCard(
                            databaseViewModel = databaseViewModel,
                            investmentViewModel = investmentViewModel,
                            creditViewModel = creditViewModel,
                            onRefresh = {
                                refreshTrigger++
                                databaseViewModel.refreshNetWorth()
                            }
                        )
                    }
                } else {
                    // Use of DateRangeSelector
                    DateRangeSelector(
                        selectedOption = selectedOption,
                        onOptionChange = { option ->
                            selectedOption = option
                            range = calculateRangeFromOption(option, minDate)
                        },
                        onCalendarClick = { showDateRangePicker = true }
                    )

                    if (showDateRangePicker) {
                        LegacyMaterialDateRangePicker(
                            onDismiss = { showDateRangePicker = false },
                            onDateSelected = { start, end ->
                                range = start..end
                                showDateRangePicker = false
                            }
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.estate_evolution),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    
                    PatrimonialLineChart(
                        viewModel = databaseViewModel,
                        investmentViewModel = investmentViewModel,
                        startDate = range.start.toDouble(),
                        endDate = range.endInclusive.toDouble(),
                        refreshTrigger = refreshTrigger,
                        hideMarkerTrigger = hideMarkerTrigger,
                        onHideMarkers = { hideMarkerTrigger++ }
                    )

                    Text(
                        text = stringResource(id = R.string.breakdown_by_category),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    BalancePieChart(
                        viewModel = databaseViewModel,
                        startDate = range.start.toDouble(),
                        endDate = range.endInclusive.toDouble()
                    )
                }
            }
        }
    }
}
