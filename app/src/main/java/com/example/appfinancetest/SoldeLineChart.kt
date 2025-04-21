package com.example.appfinancetest

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

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
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val date = Date(value.toLong())  // Conversion en date
                        val format = SimpleDateFormat("dd/MM/yy", Locale.getDefault())  // Format de la date
                        return format.format(date)  // Retourner la date formatée
                    }
                }
                textColor = Color.WHITE
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
    // Valeurs de dates min et max (à adapter si tu veux dynamiquement)
    val minDate = 44000f // Ex: 01/01/2020
    val maxDate = 46000f // Ex: 31/12/2025

    var range by remember { mutableStateOf(minDate..maxDate) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Plage de dates : ${range.start.roundToInt()} à ${range.endInclusive.roundToInt()}",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        RangeSlider(
            value = range,
            onValueChange = { range = it },
            valueRange = minDate..maxDate,
            steps = ((maxDate - minDate) / 10).toInt(), // Optionnel pour un pas visible
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        // Affichage du graphique avec les dates choisies
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