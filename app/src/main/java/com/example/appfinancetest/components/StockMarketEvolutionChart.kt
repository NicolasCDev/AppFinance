package com.example.appfinancetest.components

import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appfinancetest.R
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.services.StockTickerData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun StockMarketEvolutionChart(
    stockTransactions: List<TransactionDB>,
    tickerDataMap: Map<String, StockTickerData>,
    transactionTickerMap: Map<TransactionDB, String>,
    startTimeMillis: Long = 0L,
    isVisibilityOff: Boolean = false,
    modifier: Modifier = Modifier
) {
    val chartData = remember(stockTransactions, tickerDataMap, transactionTickerMap, startTimeMillis) {
        if (stockTransactions.isEmpty() || tickerDataMap.isEmpty()) {
            return@remember null
        }

        val validTxs = stockTransactions
            .filter { it.amount != null && it.amount > 0 && it.date != null }
            .sortedBy { it.date }

        if (validTxs.isEmpty()) return@remember null

        val txWithMillis = validTxs.map { tx ->
            val dateMillis = ((tx.date!! - 25569.0) * 86400.0 * 1000.0).toLong()
            val ticker = transactionTickerMap[tx] ?: "CW8.PA"
            val tickerData = tickerDataMap[ticker]
            val priceHistory = tickerData?.history?.sortedBy { it.timestampMilli } ?: emptyList()
            
            // Find buy price on or closest after transaction date
            val matchPoint = priceHistory.firstOrNull { it.timestampMilli >= dateMillis }
                ?: priceHistory.lastOrNull()
            val buyPrice = if (matchPoint != null && matchPoint.closePrice > 0) matchPoint.closePrice else (tickerData?.currentPrice ?: 1.0)
            val shares = (tx.amount ?: 0.0) / buyPrice

            Triple(dateMillis, tx, shares)
        }

        val firstTxMillis = txWithMillis.first().first

        // Collect all timestamps from price histories on or after firstTxMillis
        val allTimestamps = tickerDataMap.values
            .flatMap { it.history }
            .map { it.timestampMilli }
            .filter { it >= firstTxMillis - (86400 * 1000L) }
            .distinct()
            .sorted()

        if (allTimestamps.isEmpty()) return@remember null

        val investedEntries = mutableListOf<Entry>()
        val valueEntries = mutableListOf<Entry>()

        for (t in allTimestamps) {
            val activeTx = txWithMillis.filter { it.first <= t }
            if (activeTx.isEmpty()) continue

            val cumulativeInvested = activeTx.sumOf { it.second.amount ?: 0.0 }

            var totalMarketValue = 0.0
            for (txTriple in activeTx) {
                val ticker = transactionTickerMap[txTriple.second] ?: "CW8.PA"
                val tickerData = tickerDataMap[ticker]
                val history = tickerData?.history ?: emptyList()
                
                val priceAtT = history.lastOrNull { it.timestampMilli <= t }?.closePrice
                    ?: history.firstOrNull()?.closePrice
                    ?: tickerData?.currentPrice
                    ?: 0.0
                
                totalMarketValue += txTriple.third * priceAtT
            }

            investedEntries.add(Entry(t.toFloat(), cumulativeInvested.toFloat()))
            valueEntries.add(Entry(t.toFloat(), totalMarketValue.toFloat()))
        }

        // Add current point if last timestamp is not today
        val nowMillis = System.currentTimeMillis()
        val lastTs = allTimestamps.lastOrNull() ?: 0L
        if (nowMillis - lastTs > 12 * 3600 * 1000L) {
            val cumulativeInvested = validTxs.sumOf { it.amount ?: 0.0 }
            var currentPortfolioValue = 0.0
            for (txTriple in txWithMillis) {
                val ticker = transactionTickerMap[txTriple.second] ?: "CW8.PA"
                val tickerData = tickerDataMap[ticker]
                val curPrice = tickerData?.currentPrice ?: 0.0
                currentPortfolioValue += txTriple.third * curPrice
            }

            investedEntries.add(Entry(nowMillis.toFloat(), cumulativeInvested.toFloat()))
            valueEntries.add(Entry(nowMillis.toFloat(), currentPortfolioValue.toFloat()))
        }

        // Filter entries based on startTimeMillis
        val filteredInvested = if (startTimeMillis > 0L) {
            val res = investedEntries.filter { it.x >= startTimeMillis }
            if (res.isEmpty()) investedEntries else res
        } else {
            investedEntries
        }

        val filteredValue = if (startTimeMillis > 0L) {
            val res = valueEntries.filter { it.x >= startTimeMillis }
            if (res.isEmpty()) valueEntries else res
        } else {
            valueEntries
        }

        Pair(filteredInvested, filteredValue)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(4.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 86400000f // 1 day
                xAxis.textColor = Color.WHITE
                axisLeft.textColor = Color.WHITE
                legend.textColor = Color.WHITE
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
            }
        },
        update = { chart ->
            if (chartData == null || chartData.first.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }

            val (investedEntries, valueEntries) = chartData

            val lastValue = valueEntries.lastOrNull()?.y ?: 0f
            val lastInvested = investedEntries.lastOrNull()?.y ?: 0f
            val isPositive = lastValue >= lastInvested
            val valueLineColor = if (isPositive) Color.parseColor("#00E676") else Color.parseColor("#FF5252")

            val investedDataSet = LineDataSet(investedEntries, "Cumul Investi").apply {
                color = Color.parseColor("#90CAF9")
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 6f, 0f)
            }

            val valueDataSet = LineDataSet(valueEntries, "Valeur Estimée (€)").apply {
                color = valueLineColor
                lineWidth = 3f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = valueLineColor
                fillAlpha = 35
            }

            chart.data = LineData(investedDataSet, valueDataSet)

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                private val formatter = SimpleDateFormat("MMM yy", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    return formatter.format(Date(value.toLong()))
                }
            }

            chart.axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "%.0f €".format(value)
                }
            }

            val marker = CustomStockMarker(chart.context, R.layout.marker_view, isVisibilityOff)
            marker.chartView = chart
            chart.marker = marker

            chart.invalidate()
        }
    )
}

class CustomStockMarker(
    context: Context,
    layoutResource: Int,
    private val isVisibilityOff: Boolean
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.marker_date)
    private val tvValue: TextView = findViewById(R.id.marker_value)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val date = Date(entry.x.toLong())
            tvDate.text = "Date : ${dateFormat.format(date)}"

            val chart = chartView as? LineChart
            val data = chart?.data
            val investedSet = data?.getDataSetByIndex(0)
            val valueSet = data?.getDataSetByIndex(1)

            val invested = investedSet?.getEntryForXValue(entry.x, Float.NaN)?.y ?: entry.y
            val marketVal = valueSet?.getEntryForXValue(entry.x, Float.NaN)?.y ?: entry.y

            val profit = marketVal - invested
            val profitPercent = if (invested > 0) (profit / invested) * 100 else 0f

            if (isVisibilityOff) {
                tvValue.text = "Investi : **** €\nValeur : **** €\nPlus-value : ****"
            } else {
                val sign = if (profit >= 0) "+" else "-"
                tvValue.text = String.format(
                    Locale.getDefault(),
                    "Investi : %,.2f €\nValeur : %,.2f €\nPlus-value : %s%,.2f € (%s%.1f%%)",
                    invested,
                    marketVal,
                    sign,
                    abs(profit),
                    sign,
                    abs(profitPercent)
                )
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
