package com.example.appfinancetest

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.widget.TextView
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.flow.combine

@Composable
fun SoldeLineChart(viewModel: DataBase_ViewModel, startDate: Double = 0.0, endDate: Double = 2958465.0) {
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), viewModel) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp),
        factory = { context ->
            LineChart(context).apply {
                description.text = "Évolution du solde"
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
            }
        },
        update = { chart ->
            val filteredTransactions = transactions.filter {
                it.date != null && it.solde != null && it.date in startDate..endDate
            }
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>()

            // Transformation des transactions
            filteredTransactions.forEach { transaction ->
                val solde = transaction.solde
                val date = transaction.date

                if (solde != null && date != null) {
                    // Conversion du timestamp Excel (en jour depuis 1900) en millisecondes
                    val millis = ((date - 25569) * 86400 * 1000).toLong()
                    entries.add(Entry(millis.toFloat(), solde.toFloat()))

                    // Formatage de la date
                    val formattedDate = DateFormattedText(date)
                    labels.add(formattedDate)
                }
            }

            val dataSet = LineDataSet(entries, "Solde en €").apply {
                color = Color.BLUE
                valueTextColor = Color.WHITE
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(true)
                setDrawCircles(true)
                setDrawFilled(true)
                fillColor = Color.CYAN
            }

            // Utilisation d'un ValueFormatter personnalisé pour l'axe X
            chart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textColor = Color.WHITE
            }
            val dateRangeInDays = endDate - startDate
            val useMonthFormat = dateRangeInDays > 365

            val dateFormatter = if (useMonthFormat) {
                SimpleDateFormat("MMM yy", Locale.getDefault())
            } else {
                SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            }

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val date = Date(value.toLong())
                    return dateFormatter.format(date)
                }
            }

            // Mise à jour de l'axe Y et de la légende
            chart.axisLeft.textColor = Color.WHITE
            chart.legend.textColor = Color.WHITE
            chart.data = LineData(dataSet)

            val marker = CustomMarkerView(chart.context, R.layout.marker_view)
            marker.chartView = chart // important !
            chart.marker = marker

            chart.invalidate()
        }

    )
}
@Composable
fun SoldeChartWithSlider(viewModel: DataBase_ViewModel) {
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

    // Nouvel état pour signaler que les prefs sont chargées
    var isPrefsLoaded by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(minDate..maxDate) }

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

    // Sauvegarde uniquement si prefs sont chargées
    LaunchedEffect(range, isPrefsLoaded) {
        if (isPrefsLoaded) {
            prefs.saveStartDate(range.start)
            prefs.saveEndDate(range.endInclusive)
        }
    }

    // Ne pas afficher tant que les prefs ne sont pas prêtes
    if (!isPrefsLoaded) {
        Text("Chargement de la plage de dates...")
        return
    }

    // Ensuite on peut afficher normalement
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Plage de dates : ${DateFormattedText(range.start.roundToInt().toDouble())} à ${DateFormattedText(range.endInclusive.roundToInt().toDouble())}",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Le reste ne change pas
        Button(onClick = {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = ((range.start - 25569) * 86400 * 1000).toLong()
            }

            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    val selectedExcelDate = (selectedCalendar.timeInMillis / 86400000f) + 25569f
                    if (selectedExcelDate < range.endInclusive) {
                        range = selectedExcelDate..range.endInclusive
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }) {
            Text("Choisir date de début")
        }

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
    }
}


class CustomMarkerView(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.marker_date)
    private val tvValue: TextView = findViewById(R.id.marker_value)
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            // Convertir la valeur X (millis) en date
            val date = Date(it.x.toLong())
            tvDate.text = "Date : ${dateFormat.format(date)}"
            tvValue.text = "Solde : %.2f €".format(it.y)
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Pour centrer au-dessus du point
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}