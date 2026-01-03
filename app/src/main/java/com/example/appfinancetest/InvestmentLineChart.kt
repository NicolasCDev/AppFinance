package com.example.appfinancetest

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Date
import java.util.Locale
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF
import android.view.MotionEvent
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener

@Composable
fun InvestmentLineChart(
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    startDate: Double = 0.0,
    endDate: Double = 2958465.0,
    hideMarkerTrigger: Int = 0,
    onHideMarkers: (() -> Unit)? = null
) {

    val transactions by produceState(initialValue = emptyList(), databaseViewModel) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
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

    val filteredTransactions = transactions.filter {
        it.category == "Investissement" && it.date != null && it.amount != null && it.date in startDate..endDate
    }

    val endedInvestments by produceState<List<InvestmentDB>?>(initialValue = null, investmentViewModel) {
        value = investmentViewModel.getEndedInvestments()
    }

    val filteredInvestments = endedInvestments?.filter {
        it.dateEnd != null && it.invested != null && it.dateEnd in startDate..endDate
    }

    val entriesCrypto = mutableListOf<Entry>()
    val entriesStockExchange = mutableListOf<Entry>()
    val entriesCrowdfunding = mutableListOf<Entry>()
    val entriesRealEstateCrowdfunding = mutableListOf<Entry>()
    val entriesTotal = mutableListOf<Entry>()

    filteredTransactions.forEach { transaction ->
        val amount = transaction.amount
        val date = transaction.date
        val item = transaction.item

        if (amount != null && date != null) {
            val millis = ((date - 25569) * 86400 * 1000).toLong()
            val entry = Entry(millis.toFloat(), amount.toFloat())
            when (item) {
                "Bourse - PEA", "Bourse - Compte titre" -> {
                    entriesStockExchange.add(entry)
                }
                "Crypto" -> {
                    entriesCrypto.add(entry)
                }
                "Crowdfunding" -> {
                    entriesCrowdfunding.add(entry)
                }
                "Crowdfunding immobilier" -> {
                    entriesRealEstateCrowdfunding.add(entry)
                }
            }
            entriesTotal.add(entry)
        }
    }

    filteredInvestments?.forEach { investment ->
        val dateEnd = investment.dateEnd
        val invested = investment.invested
        val item = investment.item

        if (dateEnd != null && invested != null) {
            val millisEnd = ((dateEnd - 25569) * 86400 * 1000).toLong()
            val exitEntry = Entry(millisEnd.toFloat(), -invested.toFloat())
            when (item) {
                "Bourse - PEA", "Bourse - Compte titre" -> {
                    entriesStockExchange.add(exitEntry)
                }
                "Crypto" -> {
                    entriesCrypto.add(exitEntry)
                }
                "Crowdfunding" -> {
                    entriesCrowdfunding.add(exitEntry)
                }
                "Crowdfunding immobilier" -> {
                    entriesRealEstateCrowdfunding.add(exitEntry)
                }
            }
            entriesTotal.add(exitEntry)
        }
    }
    val allDates = (entriesCrypto + entriesStockExchange + entriesCrowdfunding + entriesRealEstateCrowdfunding + entriesTotal)
        .map { it.x }
        .toSortedSet()
        .toList()

    val cumulativeCrypto = makeCumulative(entriesCrypto, allDates)
    val cumulativeStockExchange = makeCumulative(entriesStockExchange, allDates)
    val cumulativeCrowdfunding = makeCumulative(entriesCrowdfunding, allDates)
    val cumulativeRealEstateCrowdfunding = makeCumulative(entriesRealEstateCrowdfunding, allDates)
    val cumulativeTotal = makeCumulative(entriesTotal, allDates)

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
            chart.description.isEnabled = false

            fun makeDataSet(entries: List<Entry>, label: String, color: Int): LineDataSet {
                return LineDataSet(entries, label).apply {
                    this.color = color
                    valueTextColor = Color.WHITE
                    lineWidth = 2f
                    setDrawValues(false)
                    setDrawCircles(false)
                    setDrawFilled(false)
                }
            }

            val dataSets = listOfNotNull(
                if (cumulativeStockExchange.isNotEmpty()) makeDataSet(cumulativeStockExchange, "Bourse", Color.BLUE) else null,
                if (cumulativeCrypto.isNotEmpty()) makeDataSet(cumulativeCrypto, "Crypto", Color.GREEN) else null,
                if (cumulativeCrowdfunding.isNotEmpty()) makeDataSet(cumulativeCrowdfunding, "Crowdfunding", Color.MAGENTA) else null,
                if (cumulativeRealEstateCrowdfunding.isNotEmpty()) makeDataSet(cumulativeRealEstateCrowdfunding, "Immobilier", Color.CYAN) else null,
                if (cumulativeTotal.isNotEmpty()) makeDataSet(cumulativeTotal, "Total", Color.RED) else null
            )

            chart.data = LineData(dataSets)

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                private val formatter = if (endDate - startDate > 365) {
                    SimpleDateFormat("MMM yy", Locale.getDefault())
                } else {
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                }
                override fun getFormattedValue(value: Float): String {
                    return formatter.format(Date(value.toLong()))
                }
            }

            chart.axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "%.0f €".format(value)
                }
            }

            chart.axisLeft.textColor = Color.WHITE
            chart.legend.textColor = Color.WHITE

            val marker = CustomMarkerInvestments(chart.context, R.layout.marker_view)
            marker.dataSets = dataSets
            marker.chartView = chart
            chart.marker = marker

            // Select/Unselect management
            chart.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    // Nothing to do here, marker shows automatically
                }

                override fun onNothingSelected() {
                    // When nothing selected we hide the crossbar
                    chart.highlightValue(null)
                    chart.invalidate()
                }
            })

            // Detect the click on the chart
            chart.onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {
                    me?.let {
                        val h = chart.getHighlightByTouchPoint(it.x, it.y)
                        if (h == null) {
                            // No point under the finger -> we clear the selection -> hide crossbar
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

class CustomMarkerInvestments(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.marker_date)
    private val tvValue: TextView = findViewById(R.id.marker_value)
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    var dataSets: List<ILineDataSet> = emptyList()

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            // Conversion milliseconds to date
            val date = Date(it.x.toLong())
            tvDate.text = "Date : ${dateFormat.format(date)}"
            val sb = StringBuilder()
            for (dataSet in dataSets) {
                val y = dataSet.getEntryForXValue(it.x, Float.NaN)?.y
                if (y != null) {
                    sb.append("${dataSet.label}: %.2f €\n".format(y))
                }
            }
            tvValue.text = sb.toString().trim()
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Center on the top of the point
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}