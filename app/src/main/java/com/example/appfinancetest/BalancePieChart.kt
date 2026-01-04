package com.example.appfinancetest

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@Composable
fun BalancePieChart(viewModel: DataBaseViewModel, startDate: Double, endDate: Double) {
    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    val transactions by produceState(initialValue = emptyList(), viewModel) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var selectedLabelForTransactions by remember { mutableStateOf<String?>(null) }

    val filteredTransactions = transactions.filter {
        it.date != null && it.amount != null && it.category != null &&
                it.date in startDate..endDate
    }

    val chartEntries = if (selectedCategory == null && selectedItem == null) {
        filteredTransactions
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
            .map { (category, total) ->
                PieEntry(total.toFloat(), category)
            }
    } else {
        if (selectedCategory != null && selectedItem == null) {
            val itemTotals = filteredTransactions
                .filter { it.category == selectedCategory && it.item != null }
                .groupBy { it.item!! }
                .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }

            createPieEntries(itemTotals, topN = 8, othersLabel = "Others")
        } else {
            val labelTotal = filteredTransactions
                .filter { it.category == selectedCategory && it.item == selectedItem && it.label != null }
                .groupBy { it.label!! }
                .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }

            createPieEntries(labelTotal, topN = 8, othersLabel = "Others")
        }
    }

    val customColors = listOf(
        Color.rgb(0, 0, 255), Color.rgb(255, 0, 0), Color.rgb(0, 255, 0),
        Color.rgb(255, 142, 36), Color.rgb(255, 0, 255), Color.rgb(136, 66, 29),
        Color.rgb(192, 192, 192), Color.rgb(145, 40, 59), Color.rgb(16, 52, 166),
        Color.rgb(255, 255, 87), Color.rgb(128, 0, 128), Color.rgb(255, 94, 77),
        Color.rgb(255, 96, 125), Color.rgb(0, 255, 255), Color.rgb(255, 255, 0)
    )

    if (selectedLabelForTransactions != null) {
        val labelTransactions = filteredTransactions.filter { 
            it.category == selectedCategory && 
            it.item == selectedItem && 
            it.label == selectedLabelForTransactions 
        }.sortedByDescending { it.date }

        Dialog(onDismissRequest = { selectedLabelForTransactions = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedLabelForTransactions ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedLabelForTransactions = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(labelTransactions) { transaction ->
                            TransactionRow(transaction, isVisibilityOff) // Reuse Dashboard component
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = when {
                selectedCategory == null -> stringResource(id = R.string.category)
                selectedItem == null -> "$selectedCategory"
                else -> "$selectedCategory : $selectedItem"
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory != null) {
                TextButton(
                    onClick = {
                        if (selectedItem != null) selectedItem = null
                        else selectedCategory = null
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("<", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                }
            }

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        isRotationEnabled = true
                        setUsePercentValues(true)
                        setEntryLabelColor(Color.WHITE)
                        setEntryLabelTextSize(12f)
                        legend.isEnabled = false
                        setHoleColor(Color.TRANSPARENT)
                        minOffset = 0f
                    }
                },
                update = { chart ->
                    val dataSet = PieDataSet(chartEntries, "").apply {
                        colors = customColors.take(chartEntries.size)
                        valueTextColor = Color.WHITE
                        valueTextSize = 14f
                    }
                    chart.data = PieData(dataSet)
                    chart.setDrawEntryLabels(selectedCategory == null)
                    chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            if (e is PieEntry) {
                                when {
                                    selectedCategory == null -> selectedCategory = e.label
                                    selectedItem == null -> {
                                        if (e.label != "Others") selectedItem = e.label
                                    }
                                    else -> {
                                        if (e.label != "Others") selectedLabelForTransactions = e.label
                                    }
                                }
                            }
                        }
                        override fun onNothingSelected() {}
                    })
                    chart.invalidate()
                    chart.highlightValues(null)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val periodDuration = endDate - startDate
        val previousStart = startDate - periodDuration
        val previousEnd = startDate - 1
        val previousTransactions = transactions.filter {
            it.date != null && it.amount != null && it.category != null &&
                    it.date in previousStart..previousEnd
        }

        val previousMap: Map<String, Double> = when {
            selectedCategory == null -> {
                previousTransactions.groupBy { it.category ?: "Inconnu" }.mapValues { it.value.sumOf { t -> t.amount ?: 0.0 } }
            }
            selectedItem == null -> {
                previousTransactions.filter { it.category == selectedCategory }.groupBy { it.item ?: "Inconnu" }.mapValues { it.value.sumOf { t -> t.amount ?: 0.0 } }
            }
            else -> {
                previousTransactions.filter { it.category == selectedCategory && it.item == selectedItem }.groupBy { it.label ?: "Inconnu" }.mapValues { it.value.sumOf { t -> t.amount ?: 0.0 } }
            }
        }

        val total = chartEntries.sumOf { it.value.toDouble() }

        chartEntries.forEachIndexed { index, entry ->
            val label = entry.label
            val amount = entry.value.toDouble()
            val percent = if (total > 0) (amount / total * 100) else 0.0
            val color = androidx.compose.ui.graphics.Color(customColors[index % customColors.size])
            val previousAmount = previousMap[label] ?: 0.0
            val evolution = if (previousAmount != 0.0) ((amount - previousAmount) / previousAmount * 100) else null

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(12.dp).background(color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                
                val evolutionText = if (evolution == null) "N/A" else (if (evolution >= 0) "+" else "") + "%.1f%%".format(evolution)
                Text(
                    text = if (isVisibilityOff) "**** € (%.1f%%) %s".format(percent, evolutionText)
                           else "%.2f € (%.1f%%) %s".format(amount, percent, evolutionText),
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    color = if (evolution != null && evolution < 0) androidx.compose.ui.graphics.Color.Red 
                            else if (evolution != null && evolution > 0) androidx.compose.ui.graphics.Color.Green 
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun createPieEntries(dataMap: Map<String, Double>, topN: Int = 8, othersLabel: String = "Others"): List<PieEntry> {
    val sortedEntries = dataMap.entries.sortedByDescending { it.value }
    val topEntries = sortedEntries.take(topN)
    val others = sortedEntries.drop(topN)
    val entries = topEntries.map { PieEntry(it.value.toFloat(), it.key) }.toMutableList()
    val othersTotal = others.sumOf { it.value }
    if (othersTotal > 0) entries.add(PieEntry(othersTotal.toFloat(), othersLabel))
    return entries
}
