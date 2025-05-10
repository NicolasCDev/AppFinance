package com.example.appfinancetest

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.*
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
            .height(230.dp)
            .padding(4.dp),
        factory = { context ->
            LineChart(context).apply {
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
                circleRadius = 1f
                setDrawValues(false)
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
            chart.axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "%.0f €".format(value)
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