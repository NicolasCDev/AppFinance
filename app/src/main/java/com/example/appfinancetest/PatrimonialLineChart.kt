package com.example.appfinancetest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.pow

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
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    val transactions by produceState(initialValue = emptyList(), viewModel, refreshTrigger) {
        value = viewModel.getTransactionsSortedByDateASC()
    }
    
    val endedInvestments by produceState<List<InvestmentDB>?>(initialValue = null, investmentViewModel, refreshTrigger) {
        value = investmentViewModel.getEndedInvestments()
    }

    // Cache the calculations
    var fullHistory by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var dynamicInvestmentSeries by remember { mutableStateOf<Map<String, List<Entry>>>(emptyMap()) }
    var goalEntries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    
    var isCalculating by remember { mutableStateOf(false) }

    LaunchedEffect(transactions, endedInvestments, refreshTrigger) {
        if (transactions.isEmpty()) {
            fullHistory = emptyList()
            dynamicInvestmentSeries = emptyMap()
            goalEntries = emptyList()
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

            // 3. Goal Sequence Calculation (Monthly Exponential Growth)
            val showGoals = sharedPreferences.getBoolean("show_goals", true)
            if (showGoals && uniqueDates.isNotEmpty()) {
                val birthDateExcel = sharedPreferences.getFloat("user_birth_date", 0.0f).toDouble()
                val annualRate = sharedPreferences.getFloat("goal_annual_interest_rate", 5.0f).toDouble() / 100.0
                val annualInvestment = sharedPreferences.getFloat("goal_annual_invested_amount", 1200.0f).toDouble()
                val savedGoalStartDate = sharedPreferences.getFloat("goal_start_date", 0.0f).toDouble()
                
                // Monthly conversions
                val monthlyRate = (1.0 + annualRate).pow(1.0 / 12.0) - 1.0
                val monthlyInvestment = annualInvestment / 12.0
                
                if (birthDateExcel > 0) {
                    val goalFirstDate = if (savedGoalStartDate == 0.0) uniqueDates.first() else savedGoalStartDate
                    val startWorth = viewModel.getNetWorthAtDateStatic(goalFirstDate)
                    
                    val entries = mutableListOf<Entry>()
                    var currentWorth = startWorth
                    var currentDate = goalFirstDate
                    
                    // Add start point
                    entries.add(Entry(((currentDate - 25569) * 86400 * 1000).toFloat(), currentWorth.toFloat()))
                    
                    // Simulate month by month until millionaire goal is reached or 100 years
                    var months = 0
                    while (currentWorth < 1100000 && months < 1200) {
                        currentWorth = currentWorth * (1.0 + monthlyRate) + monthlyInvestment
                        currentDate += 30.4375 // Average month in days
                        months++
                        entries.add(Entry(((currentDate - 25569) * 86400 * 1000).toFloat(), currentWorth.toFloat()))
                    }
                    goalEntries = entries
                } else {
                    goalEntries = emptyList()
                }
            } else {
                goalEntries = emptyList()
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
            .height(350.dp)
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
                
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        if (isVisibilityOff) return "****"
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
            // Update visibility state in marker
            (chart.marker as? CustomMarkerPatrimonial)?.isVisibilityOff = isVisibilityOff
            
            // Use exact startDate/endDate to avoid stretching the X axis into the future
            val filterLambda = { entry: Entry ->
                val excelDate = (entry.x / (86400 * 1000.0)) + 25569
                excelDate in startDate..endDate
            }

            fun createDataSet(entries: List<Entry>, label: String, color: Int, isMain: Boolean = false, isDashed: Boolean = false): LineDataSet? {
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
                    if (isDashed) {
                        enableDashedLine(10f, 10f, 0f)
                    }
                }
            }

            val dataSets = mutableListOf<ILineDataSet>()
            
            // Main Estate Curve
            createDataSet(fullHistory, "Global estate (€)", Color.GREEN, true)?.let { dataSets.add(it) }
            
            // Goal Line (Monthly Geometric progression)
            if (goalEntries.isNotEmpty()) {
                createDataSet(goalEntries, "Millionaire Goal", Color.WHITE, isMain = false, isDashed = true)?.let { dataSets.add(it) }
            }
            
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

@SuppressLint("ViewConstructor")
class CustomMarkerPatrimonial(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.marker_date)
    private val tvValue: TextView = findViewById(R.id.marker_value)
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    var dataSets: List<ILineDataSet> = emptyList()
    var isVisibilityOff: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val date = Date(it.x.toLong())
            tvDate.text = "Date : ${dateFormat.format(date)}"
            
            val sb = StringBuilder()
            for (dataSet in dataSets) {
                if (dataSet.label == "Millionaire Goal") continue
                
                val entry = dataSet.getEntryForXValue(it.x, Float.NaN)
                if (entry != null) {
                    val valueText = if (isVisibilityOff) "****" else "%.2f €".format(entry.y)
                    sb.append("${dataSet.label}: $valueText\n")
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
