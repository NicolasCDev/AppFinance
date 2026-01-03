package com.example.appfinancetest

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.*
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import android.view.MotionEvent
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun PatrimonialLineChart(
    viewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    startDate: Double,
    endDate: Double,
    refreshTrigger: Int = 0,
    hideMarkerTrigger: Int = 0,
    onHideMarkers: (() -> Unit)? = null
) {
    val transactions by produceState(initialValue = emptyList(), viewModel, refreshTrigger) {
        value = viewModel.getTransactionsSortedByDateASC()
    }
    
    val endedInvestments by produceState<List<InvestmentDB>?>(initialValue = null, investmentViewModel, refreshTrigger) {
        value = investmentViewModel.getEndedInvestments()
    }

    // Cache the calculations
    var fullHistory by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var dynamicInvestmentSeries by remember { mutableStateOf<Map<String, List<Entry>>>(emptyMap()) }
    
    var isCalculating by remember { mutableStateOf(false) }

    LaunchedEffect(transactions, endedInvestments, refreshTrigger) {
        if (transactions.isEmpty()) {
            fullHistory = emptyList()
            dynamicInvestmentSeries = emptyMap()
            return@LaunchedEffect
        }
        
        isCalculating = true
        withContext(Dispatchers.Default) {
            val uniqueDates = transactions.mapNotNull { it.date }.distinct().sorted()
            
            // 1. Estate (Sampled for performance)
            val sampledDates = if (uniqueDates.size > 200) {
                val step = uniqueDates.size / 200
                uniqueDates.filterIndexed { index, _ -> index % step == 0 || index == uniqueDates.size - 1 }
            } else {
                uniqueDates
            }

            val entriesEstate = mutableListOf<Entry>()
            sampledDates.forEach { date ->
                val worth = viewModel.getNetWorthAtDateStatic(date)
                val millis = ((date - 25569) * 86400 * 1000).toLong()
                entriesEstate.add(Entry(millis.toFloat(), worth.toFloat()))
            }
            fullHistory = entriesEstate

            // 2. Dynamic Investment breakdown
            val allDatesForCumulative = (uniqueDates.map { ((it - 25569) * 86400 * 1000).toFloat() } + 
                                        (endedInvestments?.mapNotNull { it.dateEnd }?.map { ((it - 25569) * 86400 * 1000).toFloat() } ?: emptyList()))
                                        .toSortedSet().toList()
            
            val itemEntries = mutableMapOf<String, MutableList<Entry>>()

            // Group transactions by item
            transactions.filter { it.category == "Investissement" }.forEach { t ->
                val item = t.item ?: "Other"
                val date = t.date ?: return@forEach
                val amount = t.amount ?: return@forEach
                val millis = ((date - 25569) * 86400 * 1000).toFloat()
                itemEntries.getOrPut(item) { mutableListOf() }.add(Entry(millis, amount.toFloat()))
            }

            // Subtract ended investments by item
            endedInvestments?.forEach { i ->
                val item = i.item ?: "Other"
                val dateEnd = i.dateEnd ?: return@forEach
                val invested = i.invested ?: return@forEach
                val millis = ((dateEnd - 25569) * 86400 * 1000).toFloat()
                itemEntries.getOrPut(item) { mutableListOf() }.add(Entry(millis, -invested.toFloat()))
            }

            // Calculate cumulative for each discovered item
            dynamicInvestmentSeries = itemEntries.mapValues { (_, entries) ->
                makeCumulative(entries, allDatesForCumulative)
            }
        }
        isCalculating = false
    }

    var chartRef by remember { mutableStateOf<LineChart?>(null) }
    
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
            .height(350.dp) // Slightly increased height to accommodate multi-line legend
            .padding(4.dp),
        factory = { context ->
            LineChart(context).apply {
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                chartRef = this
                description.isEnabled = false
                xAxis.textColor = Color.WHITE
                axisLeft.textColor = Color.WHITE
                
                // Configure Legend: centered and multi-line
                legend.apply {
                    textColor = Color.WHITE
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    setDrawInside(false)
                    isWordWrapEnabled = true
                }
                
                xAxis.valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
                
                // Format Y-axis as "XXK €"
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (abs(value) >= 1000) {
                            "%.1fK €".format(value / 1000f).replace(".0K", "K")
                        } else {
                            "%.0f €".format(value)
                        }
                    }
                }
                
                val marker = CustomMarkerPatrimonial(context, R.layout.marker_view)
                marker.chartView = this
                this.marker = marker
                
                this.onChartGestureListener = object : OnChartGestureListener {
                    override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                    override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                    override fun onChartLongPressed(me: MotionEvent?) {}
                    override fun onChartDoubleTapped(me: MotionEvent?) {}
                    override fun onChartSingleTapped(me: MotionEvent?) {
                        me?.let {
                            val h = getHighlightByTouchPoint(it.x, it.y)
                            if (h == null) {
                                highlightValue(null, true)
                                onHideMarkers?.invoke()
                            }
                        }
                    }
                    override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
                }
            }
        },
        update = { chart ->
            val filterLambda = { entry: Entry ->
                val excelDate = (entry.x / (86400 * 1000.0)) + 25569
                excelDate in startDate..endDate
            }

            fun createDataSet(entries: List<Entry>, label: String, color: Int, isMain: Boolean = false): LineDataSet? {
                val filtered = entries.filter(filterLambda)
                if (filtered.isEmpty()) return null
                return LineDataSet(filtered, label).apply {
                    this.color = color
                    setCircleColor(color)
                    lineWidth = if (isMain) 3f else 1.5f
                    circleRadius = if (isMain) 3f else 0f
                    setDrawCircles(isMain)
                    setDrawCircleHole(false)
                    valueTextColor = Color.WHITE
                    setDrawValues(false)
                    setDrawFilled(false)
                }
            }

            val dataSets = mutableListOf<ILineDataSet>()
            
            // Main Estate Curve
            createDataSet(fullHistory, "Global estate (€)", Color.GREEN, true)?.let { dataSets.add(it) }
            
            // Dynamic Investment Series
            val colors = listOf(Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.RED, Color.LTGRAY, Color.GRAY)
            dynamicInvestmentSeries.entries.forEachIndexed { index, entry ->
                val color = colors[index % colors.size]
                createDataSet(entry.value, entry.key, color)?.let { dataSets.add(it) }
            }

            if (dataSets.isNotEmpty()) {
                chart.data = LineData(dataSets)
                (chart.marker as? CustomMarkerPatrimonial)?.dataSets = dataSets
                chart.animateX(500)
                chart.invalidate()
            } else {
                chart.clear()
            }
        }
    )
}

class CustomMarkerPatrimonial(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.marker_date)
    private val tvValue: TextView = findViewById(R.id.marker_value)
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    var dataSets: List<ILineDataSet> = emptyList()

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val date = Date(it.x.toLong())
            tvDate.text = "Date : ${dateFormat.format(date)}"
            
            val sb = StringBuilder()
            for (dataSet in dataSets) {
                val entry = dataSet.getEntryForXValue(it.x, Float.NaN)
                if (entry != null) {
                    sb.append("${dataSet.label}: %.2f €\n".format(entry.y))
                }
            }
            tvValue.text = sb.toString().trim()
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
