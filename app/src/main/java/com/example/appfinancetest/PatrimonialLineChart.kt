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

@Composable
fun PatrimonialLineChart(
    viewModel: DataBase_ViewModel,
    investmentViewModel: InvestmentDB_ViewModel,
    startDate: Double,
    endDate: Double,
    refreshTrigger: Int = 0,
    hideMarkerTrigger: Int = 0,
    onHideMarkers: (() -> Unit)? = null
) {
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), viewModel, refreshTrigger) {
        value = viewModel.getTransactionsSortedByDateASC()
    }
    
    val endedInvestments by produceState<List<InvestmentDB>?>(initialValue = null, investmentViewModel, refreshTrigger) {
        value = investmentViewModel.getEndedInvestments()
    }

    // Cache the calculations
    var fullHistory by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var cumulativeCrypto by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var cumulativeStockExchange by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var cumulativeCrowdfunding by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var cumulativeRealEstateCrowdfunding by remember { mutableStateOf<List<Entry>>(emptyList()) }
    
    var isCalculating by remember { mutableStateOf(false) }

    LaunchedEffect(transactions, endedInvestments, refreshTrigger) {
        if (transactions.isEmpty()) {
            fullHistory = emptyList()
            return@LaunchedEffect
        }
        
        isCalculating = true
        withContext(Dispatchers.Default) {
            val uniqueDates = transactions.mapNotNull { it.date }.distinct().sorted()
            
            // 1. Patrimoine Global (Sampled for performance)
            val sampledDates = if (uniqueDates.size > 200) {
                val step = uniqueDates.size / 200
                uniqueDates.filterIndexed { index, _ -> index % step == 0 || index == uniqueDates.size - 1 }
            } else {
                uniqueDates
            }

            val entriesPatrimoine = mutableListOf<Entry>()
            sampledDates.forEach { date ->
                val worth = viewModel.getNetWorthAtDateStatic(date)
                val millis = ((date - 25569) * 86400 * 1000).toLong()
                entriesPatrimoine.add(Entry(millis.toFloat(), worth.toFloat()))
            }
            fullHistory = entriesPatrimoine

            // 2. Investment breakdown
            // We need all relevant dates (Transactions + Ends of investments) for accurate cumulative
            val allDatesForCumulative = (uniqueDates.map { ((it - 25569) * 86400 * 1000).toFloat() } + 
                                        (endedInvestments?.mapNotNull { it.dateEnd }?.map { ((it - 25569) * 86400 * 1000).toFloat() } ?: emptyList()))
                                        .toSortedSet().toList()
            
            val entriesCrypto = mutableListOf<Entry>()
            val entriesStockExchange = mutableListOf<Entry>()
            val entriesCrowdfunding = mutableListOf<Entry>()
            val entriesRealEstateCrowdfunding = mutableListOf<Entry>()

            transactions.filter { it.category == "Investissement" }.forEach { t ->
                val date = t.date ?: return@forEach
                val amount = t.amount ?: return@forEach
                val millis = ((date - 25569) * 86400 * 1000).toFloat()
                val entry = Entry(millis, amount.toFloat())
                when (t.item) {
                    "Crypto" -> entriesCrypto.add(entry)
                    "Bourse - PEA", "Bourse - Compte titre" -> entriesStockExchange.add(entry)
                    "Crowdfunding" -> entriesCrowdfunding.add(entry)
                    "Crowdfunding immobilier" -> entriesRealEstateCrowdfunding.add(entry)
                }
            }

            endedInvestments?.forEach { i ->
                val dateEnd = i.dateEnd ?: return@forEach
                val invested = i.invested ?: return@forEach
                val millis = ((dateEnd - 25569) * 86400 * 1000).toFloat()
                // Subtract invested amount at the end date
                val exitEntry = Entry(millis, -invested.toFloat())
                when (i.item) {
                    "Crypto" -> entriesCrypto.add(exitEntry)
                    "Bourse - PEA", "Bourse - Compte titre" -> entriesStockExchange.add(exitEntry)
                    "Crowdfunding" -> entriesCrowdfunding.add(exitEntry)
                    "Crowdfunding immobilier" -> entriesRealEstateCrowdfunding.add(exitEntry)
                }
            }

            cumulativeCrypto = makeCumulative(entriesCrypto, allDatesForCumulative)
            cumulativeStockExchange = makeCumulative(entriesStockExchange, allDatesForCumulative)
            cumulativeCrowdfunding = makeCumulative(entriesCrowdfunding, allDatesForCumulative)
            cumulativeRealEstateCrowdfunding = makeCumulative(entriesRealEstateCrowdfunding, allDatesForCumulative)
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
            .height(300.dp)
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
                legend.textColor = Color.WHITE
                
                xAxis.valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
                
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "%.0f €".format(value)
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
                excelDate >= startDate && excelDate <= endDate
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
                    setDrawFilled(false) // Désactivation du fond rempli
                }
            }

            val dataSets = mutableListOf<ILineDataSet>()
            
            // Principal
            createDataSet(fullHistory, "Patrimoine Global (€)", Color.GREEN, true)?.let { dataSets.add(it) }
            
            // Détails Investissements (Plus fin)
            createDataSet(cumulativeStockExchange, "Bourse", Color.BLUE)?.let { dataSets.add(it) }
            createDataSet(cumulativeCrypto, "Crypto", Color.YELLOW)?.let { dataSets.add(it) }
            createDataSet(cumulativeCrowdfunding, "Crowdfunding", Color.MAGENTA)?.let { dataSets.add(it) }
            createDataSet(cumulativeRealEstateCrowdfunding, "Immobilier", Color.CYAN)?.let { dataSets.add(it) }

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
