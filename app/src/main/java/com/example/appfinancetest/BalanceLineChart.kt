package com.example.appfinancetest

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import android.view.MotionEvent
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener

@Composable
fun BalanceLineChart(
    viewModel: DataBase_ViewModel, 
    startDate: Double = 0.0, 
    endDate: Double = 2958465.0,
    hideMarkerTrigger: Int = 0,
    onHideMarkers: (() -> Unit)? = null
) {
    // Getting transactions from database
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), viewModel) {
        value = viewModel.getTransactionsSortedByDateASC()
    }
    
    // Keep reference to chart for external marker hiding
    var chartRef by remember { mutableStateOf<LineChart?>(null) }
    
    // Hide marker when trigger changes
    LaunchedEffect(hideMarkerTrigger) {
        if (hideMarkerTrigger > 0) {
            chartRef?.let {
                it.highlightValue(null)
                it.invalidate()
            }
        }
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
                chartRef = this // Store reference for external access
            }
        },
        update = { chart ->
            // Hiding chart description
            chart.description.isEnabled = false

            // Filtering transactions between dates
            val filteredTransactions = transactions.filter {
                it.date != null && it.balance != null && it.date in startDate..endDate
            }
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>()

            // Transformation of transactions
            filteredTransactions.forEach { transaction ->
                val balance = transaction.balance
                val date = transaction.date

                if (balance != null && date != null) {
                    // Conversion milliseconds to date
                    val millis = ((date - 25569) * 86400 * 1000).toLong()
                    entries.add(Entry(millis.toFloat(), balance.toFloat()))

                    // Printing date
                    val formattedDate = dateFormattedText(date)
                    labels.add(formattedDate)
                }
            }

            // Data settings
            val dataSet = LineDataSet(entries, "Balance in €").apply {
                color = Color.BLUE
                valueTextColor = Color.WHITE
                lineWidth = 2f
                circleRadius = 1f
                setDrawValues(false)
                setDrawCircles(true)
                setDrawFilled(true)
                fillColor = Color.CYAN
            }

            // Personalized ValueFormatter for X axis
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

            chart.axisLeft.textColor = Color.WHITE
            chart.legend.textColor = Color.WHITE
            chart.data = LineData(dataSet)

            val marker = CustomMarkerBalance(chart.context, R.layout.marker_view)
            marker.chartView = chart
            chart.marker = marker

            // Handle marker visibility when clicking outside points
            chart.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    // Nothing to do here, marker shows automatically
                }

                override fun onNothingSelected() {
                    // When nothing is selected, hide the marker
                    chart.highlightValue(null)
                    chart.invalidate()
                }
            })

            // Intercept clicks on the chart to detect clicks outside points
            chart.onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {
                    me?.let {
                        val h = chart.getHighlightByTouchPoint(it.x, it.y)
                        if (h == null) {
                            // No point under finger -> clear selection -> hide marker
                            chart.highlightValue(null, true) // Trigger onNothingSelected
                            onHideMarkers?.invoke() // Also trigger external marker hiding
                        }
                    }
                }
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            }

            chart.invalidate()
        }


    )
}

class CustomMarkerBalance(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.marker_date)
    private val tvValue: TextView = findViewById(R.id.marker_value)
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            // Conversion milliseconds to date
            val date = Date(it.x.toLong())
            tvDate.text = "Date : ${dateFormat.format(date)}"
            tvValue.text = "Balance: %.2f €".format(it.y)
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Center on the top of the point
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}