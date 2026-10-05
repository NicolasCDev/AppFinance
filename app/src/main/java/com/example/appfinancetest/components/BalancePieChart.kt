package com.example.appfinancetest.components

import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import com.example.appfinancetest.calculations.CurrencyTextOnPrimary
import com.example.appfinancetest.calculations.formatCurrency
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.calculations.PercentageText
import com.example.appfinancetest.calculations.PercentageTextOnPrimary
import com.example.appfinancetest.R
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.views.TransactionEditDialog
import com.example.appfinancetest.views.TransactionsLabelDialog
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun BalancePieChart(
    viewModel: DataBaseViewModel,
    startDate: Double,
    endDate: Double,
    initialCategory: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val transactions by produceState(initialValue = emptyList(), viewModel, refreshTrigger) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    var selectedCategory by remember(initialCategory) { mutableStateOf<String?>(initialCategory) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var isViewingOthersCategories by remember { mutableStateOf(false) }
    var isViewingOthersItems by remember { mutableStateOf(false) }
    var isViewingOthersLabels by remember { mutableStateOf(false) }

    var selectedLabelForTransactions by remember { mutableStateOf<String?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionDB?>(null) }
    var selectedOthersTransactions by remember { mutableStateOf<List<TransactionDB>?>(null) }

    val filteredTransactions = transactions.filter {
        it.date != null && it.amount != null && it.category != null &&
                it.date in startDate..endDate
    }

    val chartEntries = when {
        initialCategory == null && selectedCategory == null && !isViewingOthersCategories -> {
            val catTotals = filteredTransactions
                .groupBy { it.category ?: "Inconnu" }
                .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
            createPieEntries(catTotals, topN = 8, othersLabel = "Others")
        }
        initialCategory == null && selectedCategory == null && isViewingOthersCategories -> {
            val catTotals = filteredTransactions
                .groupBy { it.category ?: "Inconnu" }
                .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
            val sorted = catTotals.entries.sortedByDescending { it.value }
            val topCats = sorted.take(8).map { it.key }.toSet()
            catTotals.filterKeys { it !in topCats }.map { (cat, total) -> PieEntry(total.toFloat(), cat) }
        }
        else -> {
            val currentCat = selectedCategory ?: initialCategory
            if (currentCat != null) {
                if (selectedItem == null && !isViewingOthersItems) {
                    val itemTotals = filteredTransactions
                        .filter { it.category == currentCat && it.item != null }
                        .groupBy { it.item!! }
                        .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
                    createPieEntries(itemTotals, topN = 8, othersLabel = "Others")
                } else if (selectedItem == null && isViewingOthersItems) {
                    val itemTotals = filteredTransactions
                        .filter { it.category == currentCat && it.item != null }
                        .groupBy { it.item!! }
                        .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
                    val sorted = itemTotals.entries.sortedByDescending { it.value }
                    val topItems = sorted.take(8).map { it.key }.toSet()
                    itemTotals.filterKeys { it !in topItems }.map { (item, total) -> PieEntry(total.toFloat(), item) }
                } else {
                    val currentItem = selectedItem
                    val labelTotal = filteredTransactions
                        .filter { it.category == currentCat && it.item == currentItem && it.label != null }
                        .groupBy { it.label!! }
                        .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
                    if (!isViewingOthersLabels) {
                        createPieEntries(labelTotal, topN = 8, othersLabel = "Others")
                    } else {
                        val sorted = labelTotal.entries.sortedByDescending { it.value }
                        val topLabels = sorted.take(8).map { it.key }.toSet()
                        labelTotal.filterKeys { it !in topLabels }.map { (label, total) -> PieEntry(total.toFloat(), label) }
                    }
                }
            } else {
                emptyList()
            }
        }
    }

    val customColors = listOf(
        Color.rgb(0, 0, 255), Color.rgb(255, 0, 0), Color.rgb(0, 255, 0),
        Color.rgb(255, 142, 36), Color.rgb(255, 0, 255), Color.rgb(136, 66, 29),
        Color.rgb(192, 192, 192), Color.rgb(145, 40, 59), Color.rgb(16, 52, 166),
        Color.rgb(255, 255, 87), Color.rgb(128, 0, 128), Color.rgb(255, 94, 77),
        Color.rgb(255, 96, 125), Color.rgb(0, 255, 255), Color.rgb(255, 255, 0)
    )

    TransactionsLabelDialog(
        selectedLabel = selectedLabelForTransactions,
        othersTransactions = selectedOthersTransactions,
        filteredTransactions = filteredTransactions,
        selectedCategory = selectedCategory ?: initialCategory,
        selectedItem = selectedItem,
        isVisibilityOff = isVisibilityOff,
        onDismiss = {
            selectedLabelForTransactions = null
            selectedOthersTransactions = null
        },
        onTransactionClick = { transactionToEdit = it }
    )

    if (transactionToEdit != null) {
        TransactionEditDialog(
            transaction = transactionToEdit!!,
            onDismiss = { transactionToEdit = null },
            onSave = { updated ->
                scope.launch {
                    val prevInvest = transactionToEdit?.idInvest
                    saveTransactionAndSyncInvestments(
                        updated = updated,
                        previousIdInvest = prevInvest,
                        databaseViewModel = viewModel,
                        investmentViewModel = null
                    )
                    refreshTrigger++
                    transactionToEdit = null
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = when {
                isViewingOthersCategories -> "Others (Catégories)"
                isViewingOthersItems -> "${selectedCategory ?: initialCategory} : Others (Items)"
                isViewingOthersLabels -> "${selectedCategory ?: initialCategory} : $selectedItem : Others (Labels)"
                initialCategory != null && selectedItem == null -> initialCategory
                initialCategory != null -> "$initialCategory : $selectedItem"
                selectedCategory == null -> stringResource(id = R.string.category)
                selectedItem == null -> "$selectedCategory"
                else -> "$selectedCategory : $selectedItem"
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val canGoBack = if (initialCategory != null) {
                selectedItem != null || isViewingOthersItems
            } else {
                selectedCategory != null || isViewingOthersCategories || isViewingOthersItems || isViewingOthersLabels
            }

            if (canGoBack) {
                TextButton(
                    onClick = {
                        when {
                            isViewingOthersLabels -> isViewingOthersLabels = false
                            selectedItem != null -> {
                                selectedItem = null
                                isViewingOthersLabels = false
                            }
                            isViewingOthersItems -> isViewingOthersItems = false
                            selectedCategory != null -> selectedCategory = null
                            isViewingOthersCategories -> isViewingOthersCategories = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("<", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                }
            }

            val activeCategory = selectedCategory ?: initialCategory
            val totalDisplayAmount = remember(
                filteredTransactions,
                activeCategory,
                selectedItem,
                isViewingOthersCategories,
                isViewingOthersItems,
                isViewingOthersLabels,
                chartEntries
            ) {
                when {
                    isViewingOthersCategories || isViewingOthersItems || isViewingOthersLabels -> {
                        chartEntries.sumOf { it.value.toDouble() }
                    }
                    activeCategory != null -> {
                        if (selectedItem == null) {
                            filteredTransactions.filter { it.category == activeCategory }.sumOf { it.amount ?: 0.0 }
                        } else {
                            filteredTransactions.filter { it.category == activeCategory && it.item == selectedItem }.sumOf { it.amount ?: 0.0 }
                        }
                    }
                    else -> {
                        filteredTransactions.sumOf { it.amount ?: 0.0 }
                    }
                }
            }

            val centerTextFormatted = if (isVisibilityOff) {
                "**** €"
            } else {
                formatCurrency(totalDisplayAmount)
            }

            val centerTextColorArgb = MaterialTheme.colorScheme.onSurface.toArgb()

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        isRotationEnabled = false
                        setUsePercentValues(true)
                        setEntryLabelColor(Color.WHITE)
                        setEntryLabelTextSize(10f)
                        setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
                        legend.isEnabled = false
                        isDrawHoleEnabled = true
                        setHoleColor(Color.TRANSPARENT)
                        setHoleRadius(52f)
                        setTransparentCircleRadius(55f)
                        minOffset = 0f
                    }
                },
                update = { chart ->
                    chart.centerText = centerTextFormatted
                    chart.setCenterTextColor(centerTextColorArgb)
                    chart.setCenterTextSize(13f)
                    chart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)

                    val dataSet = PieDataSet(chartEntries, "").apply {
                        colors = customColors.take(chartEntries.size)
                        valueTextColor = Color.WHITE
                        valueTextSize = 10f
                        valueTypeface = Typeface.DEFAULT_BOLD
                    }
                    chart.data = PieData(dataSet)
                    chart.setDrawEntryLabels(selectedCategory == null || initialCategory != null)
                    chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            if (e is PieEntry) {
                                val label = e.label
                                when {
                                    initialCategory == null && selectedCategory == null && !isViewingOthersCategories -> {
                                        if (label != "Others") {
                                            selectedCategory = label
                                        } else {
                                            isViewingOthersCategories = true
                                        }
                                    }
                                    initialCategory == null && selectedCategory == null && isViewingOthersCategories -> {
                                        selectedCategory = label
                                        isViewingOthersCategories = false
                                    }
                                    selectedItem == null && !isViewingOthersItems -> {
                                        if (label != "Others") {
                                            selectedItem = label
                                        } else {
                                            isViewingOthersItems = true
                                        }
                                    }
                                    selectedItem == null && isViewingOthersItems -> {
                                        selectedItem = label
                                        isViewingOthersItems = false
                                    }
                                    selectedItem != null && !isViewingOthersLabels -> {
                                        if (label != "Others") {
                                            selectedLabelForTransactions = label
                                        } else {
                                            isViewingOthersLabels = true
                                        }
                                    }
                                    selectedItem != null && isViewingOthersLabels -> {
                                        selectedLabelForTransactions = label
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

        val periodDays = endDate - startDate + 1.0
        val previousEnd = startDate - 1.0
        val previousStart = startDate - periodDays
        val previousTransactions = transactions.filter {
            it.date != null && it.amount != null && it.category != null &&
                    it.date in previousStart..previousEnd
        }

        val activeCategory = selectedCategory ?: initialCategory
        val previousMap: Map<String, Double> = when {
            activeCategory == null -> {
                previousTransactions.groupBy { it.category ?: "Inconnu" }.mapValues { entry -> entry.value.sumOf { t -> t.amount ?: 0.0 } }
            }
            selectedItem == null -> {
                previousTransactions.filter { it.category == activeCategory }.groupBy { it.item ?: "Inconnu" }.mapValues { entry -> entry.value.sumOf { t -> t.amount ?: 0.0 } }
            }
            else -> {
                previousTransactions.filter { it.category == activeCategory && it.item == selectedItem }.groupBy { it.label ?: "Inconnu" }.mapValues { entry -> entry.value.sumOf { t -> t.amount ?: 0.0 } }
            }
        }

        val total = chartEntries.sumOf { it.value.toDouble() }
        val topLabels = chartEntries.map { val l = it.label; if (l != null) l else "" }.filter { it != "Others" }.toSet()

        chartEntries.forEachIndexed { index, entry ->
            val label = entry.label ?: ""
            val amount = entry.value.toDouble()
            val percent = if (total > 0) (amount / total * 100) else 0.0
            val color = androidx.compose.ui.graphics.Color(customColors[index % customColors.size])
            val previousAmount = if (label == "Others") {
                previousMap.filterKeys { it !in topLabels }.values.sum()
            } else {
                previousMap[label] ?: 0.0
            }
            val evolution = if (previousAmount != 0.0) ((amount - previousAmount) / abs(previousAmount) * 100) else null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier
                    .size(12.dp)
                    .background(color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyTextOnPrimary(
                        amount = amount,
                        isVisibilityOff = isVisibilityOff,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = " (",
                        style = MaterialTheme.typography.bodySmall
                    )
                    PercentageTextOnPrimary(
                        amount = percent,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = ") ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (evolution == null) {
                        Text(
                            text = "N/A",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        PercentageText(
                            amount = evolution,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
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
