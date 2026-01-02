package com.example.appfinancetest

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Composable
fun PatrimonialScreen(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBase_ViewModel,
    investmentViewModel: InvestmentDB_ViewModel
) {
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var hideMarkerTrigger by remember { mutableIntStateOf(0) }
    val netWorth by databaseViewModel.netWorth.observeAsState(0.0)
    
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    var showValidation by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    if (showValidation) {
        InvestmentValidationInterface(
            databaseViewModel = databaseViewModel,
            investmentViewModel = investmentViewModel,
            onDismiss = { showValidation = false },
            onRefresh = { refreshTrigger++ }
        )
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
    if (showImportExport) {
        ImportExportInterface(
            databaseViewModel = databaseViewModel,
            investmentViewModel = investmentViewModel,
            onDismiss = { showImportExport = false },
            onRefresh = {
                refreshTrigger++
                databaseViewModel.refreshNetWorth()
            }
        )
    }

    val validDates = transactions.mapNotNull { it.date }
    val minDate = validDates.minOrNull()?.toDouble() ?: 0.0
    val todayExcel = (System.currentTimeMillis() / (1000 * 86400.0)) + 25569

    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    
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
                onValidateClick = { showValidation = true },
                onSettingsClick = { showSettings = true },
                onImportExportClick = { showImportExport = true },
                name = "Patrimoine"
            )
        }
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
            Text(
                text = "Patrimoine Global",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${"%.2f".format(netWorth ?: 0.0)} €",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (isPrefsLoaded) {
                // Utilisation du composant partagé DateRangeSelector
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
                    text = "Évolution du patrimoine",
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
                    text = "Répartition par catégorie",
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
