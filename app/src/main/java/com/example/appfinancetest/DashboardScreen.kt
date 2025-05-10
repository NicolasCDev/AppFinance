package com.example.appfinancetest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, viewModel: DataBase_ViewModel)  {
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), viewModel) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    val validDates = transactions.mapNotNull { it.date }
    if (validDates.isEmpty()) {
        Text("Aucune donnée disponible.")
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

    // Charger les préférences
    LaunchedEffect(Unit) {
        prefs.startDateFlow.combine(prefs.endDateFlow) { start, end ->
            start to end
        }.collect { (savedStart, savedEnd) ->
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
        Text("Chargement de la plage de dates...")
        return
    }

    // Then we can show the content
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Plage de dates : ${
                DateFormattedText(
                    range.start.roundToInt().toDouble()
                )
            } à ${DateFormattedText(range.endInclusive.roundToInt().toDouble())}",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Button(onClick = { showDateRangePicker = true }) {
            Text("Choisir la plage de dates")
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
        // Always show RangeSlider

        RangeSlider(
            value = range,
            onValueChange = { range = it },
            valueRange = minDate..maxDate,
            steps = ((maxDate - minDate) / 10).toInt(),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        SoldeLineChart(
            viewModel = viewModel,
            startDate = range.start.toDouble(),
            endDate = range.endInclusive.toDouble()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Répartition par catégorie",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        SoldePieChart(
            viewModel = viewModel,
            startDate = range.start.toDouble(),
            endDate = range.endInclusive.toDouble()
        )
    }
}
