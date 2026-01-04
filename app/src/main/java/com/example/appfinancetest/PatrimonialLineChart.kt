package com.example.appfinancetest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.ui.graphics.toArgb
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow

@OptIn(ExperimentalLayoutApi::class)
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

    // State to track hidden data sets by their labels
    val hiddenLabels = remember { mutableStateListOf<String>() }

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

    // Get translated labels at the composable level
    val globalEstateLabel = stringResource(R.string.net_worth_simple)
    val millionaireGoalLabel = stringResource(R.string.millionaire_goal_label)

    // Pre-calculate ALL possible datasets info for the custom legend
    val allLegendInfos = remember(fullHistory, dynamicInvestmentSeries, goalEntries, globalEstateLabel, millionaireGoalLabel) {
        val infos = mutableListOf<Pair<String, Int>>()
        infos.add(globalEstateLabel to Color.GREEN)
        if (goalEntries.isNotEmpty()) {
            infos.add(millionaireGoalLabel to ComposeColor.White.toArgb())
        }
        val colors = listOf(Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.RED, Color.LTGRAY, Color.GRAY)
        dynamicInvestmentSeries.keys.forEachIndexed { index, label ->
            infos.add(label to colors[index % colors.size])
        }
        infos
    }

    // Pre-calculate LineData to contain ONLY VISIBLE datasets so the Y-axis adapts
    val lineData = remember(fullHistory, dynamicInvestmentSeries, goalEntries, startDate, endDate, globalEstateLabel, millionaireGoalLabel, hiddenLabels.toList()) {
        val filterLambda = { entry: Entry ->
            val excelDate = (entry.x / (86400 * 1000.0)) + 25569
            excelDate in startDate..endDate
        }

        fun createDataSet(entries: List<Entry>, label: String, color: Int, isMain: Boolean = false, isDashed: Boolean = false): LineDataSet? {
            // If hidden, don't even add it to LineData to force axis recalculation
            if (hiddenLabels.contains(label)) return null
            
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
        
        createDataSet(fullHistory, globalEstateLabel, Color.GREEN, true)?.let { dataSets.add(it) }
        
        if (goalEntries.isNotEmpty()) {
            createDataSet(goalEntries, millionaireGoalLabel, ComposeColor.White.toArgb(), isMain = false, isDashed = true)?.let { dataSets.add(it) }
        }
        
        val colors = listOf(Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.RED, Color.LTGRAY, Color.GRAY)
        dynamicInvestmentSeries.entries.forEachIndexed { index, entry ->
            val color = colors[index % colors.size]
            createDataSet(entry.value, entry.key, color)?.let { dataSets.add(it) }
        }
        
        if (dataSets.isNotEmpty()) LineData(dataSets) else null
    }

    var chartRef by remember { mutableStateOf<LineChart?>(null) }

    val bodyMediumStyle = MaterialTheme.typography.bodyMedium
    val bodyMediumSize = bodyMediumStyle.fontSize.value // Extrait la taille (ex: 12f)


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
            .height(250.dp)
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

                xAxis.apply {
                    textColor = Color.WHITE
                    textSize = bodyMediumSize
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    position = XAxis.XAxisPosition.BOTTOM
                }
                axisLeft.apply {
                    textColor = Color.WHITE
                    textSize = bodyMediumSize
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                // Set extra top offset to accommodate the marker at the top
                setExtraOffsets(0f, 0f, 0f, 0f)
                
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(true)
                setPinchZoom(false)
                isHighlightPerDragEnabled = true // Crucial for smooth sliding
                
                // Disable native legend as we use a custom Compose one
                legend.isEnabled = false
                
                xAxis.valueFormatter = object : ValueFormatter() {
                    private val dateFormat = android.text.format.DateFormat.getDateFormat(context)
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
                
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val currentMarker = marker as? CustomMarkerPatrimonial
                        if (currentMarker?.isVisibilityOff == true) return "****"
                        
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
                    override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
                        this@apply.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
                        this@apply.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    override fun onChartLongPressed(me: MotionEvent?) {}
                    override fun onChartDoubleTapped(me: MotionEvent?) {}
                    override fun onChartSingleTapped(me: MotionEvent?) {
                        me?.let { event ->
                            val h = getHighlightByTouchPoint(event.x, event.y)
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
            val marker = chart.marker as? CustomMarkerPatrimonial
            marker?.isVisibilityOff = isVisibilityOff
            
            // Re-set data every time to ensure axis recalculation from the new LineData object
            chart.data = lineData
            marker?.dataSets = lineData?.dataSets ?: emptyList()

            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )

    // Use allLegendInfos for the legend to keep hidden items visible and clickable
    if (allLegendInfos.isNotEmpty()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            allLegendInfos.forEach { (label, color) ->
                val isHidden = hiddenLabels.contains(label)
                Row(
                    modifier = Modifier
                        .clickable {
                            if (isHidden) hiddenLabels.remove(label) else hiddenLabels.add(label)
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isHidden) ComposeColor.Gray else ComposeColor(color),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isHidden) ComposeColor.Gray else MaterialTheme.typography.bodyMedium.color,
                        textDecoration = if (isHidden) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

@SuppressLint("ViewConstructor")
class CustomMarkerPatrimonial(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.marker_date)
    private val tvValue: TextView = findViewById(R.id.marker_value)
    private val dateFormat = android.text.format.DateFormat.getDateFormat(context)
    var dataSets: List<ILineDataSet> = emptyList()
    var isVisibilityOff: Boolean = false

    init {
        this.isClickable = false
        this.isFocusable = false
    }

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val date = Date(it.x.toLong())
            tvDate.text = "Date : ${dateFormat.format(date)}"
            
            val sb = StringBuilder()
            val millionaireGoalLabel = context.getString(R.string.millionaire_goal_label)
            
            for (dataSet in dataSets) {
                if (dataSet.label == millionaireGoalLabel) continue
                
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
        return MPPointF(-(width / 2f), -height.toFloat() - 20f)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val offset = MPPointF(-(width / 2f), -posY + 100f)
        
        val chart = chartView ?: return offset
        
        // Prevent popup from going off-screen to the left
        if (posX + offset.x < 0) {
            offset.x = -posX
        }
        
        // Prevent popup from going off-screen to the right
        if (posX + offset.x + width > chart.width) {
            offset.x = chart.width - posX - width
        }
        
        return offset
    }
}
