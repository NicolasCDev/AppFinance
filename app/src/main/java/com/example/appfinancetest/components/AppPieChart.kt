package com.example.appfinancetest.components

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appfinancetest.R
import com.example.appfinancetest.calculations.CurrencyTextOnPrimary
import com.example.appfinancetest.calculations.PercentageText
import com.example.appfinancetest.calculations.PercentageTextOnPrimary
import com.example.appfinancetest.calculations.formatCurrency
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.views.PortfolioSlice
import com.example.appfinancetest.views.TransactionEditDialog
import com.example.appfinancetest.views.TransactionsLabelDialog
import com.github.mikephil.charting.charts.PieChart as MPPieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Data slice for the unified [AppPieChart].
 */
data class PieChartSlice(
    val name: String,
    val amount: Double,
    val percentage: Float = 0f,
    val color: Color? = null,
)

/**
 * Extension to convert [PortfolioSlice] to [PieChartSlice].
 */
fun PortfolioSlice.toPieChartSlice(): PieChartSlice = PieChartSlice(
    name = name,
    amount = amount,
    percentage = percentage,
    color = color
)

/**
 * Legend display mode for [AppPieChart].
 */
enum class PieChartLegendStyle {
    NONE,
    COMPACT_RIGHT,
    DETAILED_BOTTOM
}

val DefaultPieChartColors = listOf(
    Color(0xFF2196F3), // Blue
    Color(0xFFF44336), // Red
    Color(0xFF4CAF50), // Green
    Color(0xFFFF9800), // Orange
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFF8BC34A), // Light Green
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF009688), // Teal
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5)  // Indigo
)

/**
 * Unified Pie/Donut Chart component.
 *
 * Can be used in non-clickable mode for overview screens (like [com.example.appfinancetest.views.HomeScreen])
 * or in interactive/clickable mode for detailed breakdown screens (like [com.example.appfinancetest.views.BudgetScreen]).
 *
 * @param slices List of [PieChartSlice] data items to display.
 * @param modifier Modifier for the container.
 * @param isClickable Whether the chart slices are interactive / clickable.
 * @param centerText Optional text to display inside the donut hole.
 * @param holeRadiusRatio Radius ratio for donut hole (default 52%).
 * @param chartHeight Height of the pie chart view.
 * @param legendStyle Legend layout style ([PieChartLegendStyle.NONE], [PieChartLegendStyle.COMPACT_RIGHT], [PieChartLegendStyle.DETAILED_BOTTOM]).
 * @param isVisibilityOff Mask amount values with "**** €" when true.
 * @param onSliceClick Callback triggered when a slice is clicked (if [isClickable] is true).
 */
@Composable
fun AppPieChart(
    slices: List<PieChartSlice>,
    modifier: Modifier = Modifier,
    isClickable: Boolean = false,
    centerText: String? = null,
    holeRadiusRatio: Float = 52f,
    chartHeight: Dp = 200.dp,
    legendStyle: PieChartLegendStyle = PieChartLegendStyle.NONE,
    isVisibilityOff: Boolean = false,
    onSliceClick: ((PieChartSlice) -> Unit)? = null
) {
    val centerTextColorArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val textPrimary = MaterialTheme.colorScheme.onSurface

    val chartEntries = remember(slices) {
        slices.map { PieEntry(it.amount.toFloat(), it.name) }
    }

    val chartColors = remember(slices) {
        slices.mapIndexed { index, slice ->
            (slice.color ?: DefaultPieChartColors[index % DefaultPieChartColors.size]).toArgb()
        }
    }

    val chartContent = @Composable {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            factory = { ctx ->
                MPPieChart(ctx).apply {
                    description.isEnabled = false
                    isRotationEnabled = isClickable
                    setTouchEnabled(isClickable)
                    setUsePercentValues(true)
                    setEntryLabelColor(AndroidColor.WHITE)
                    setEntryLabelTextSize(10f)
                    setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
                    legend.isEnabled = false
                    isDrawHoleEnabled = true
                    setHoleColor(AndroidColor.TRANSPARENT)
                    holeRadius = holeRadiusRatio
                    transparentCircleRadius = holeRadiusRatio + 3f
                    setTransparentCircleColor(AndroidColor.TRANSPARENT)
                    minOffset = 0f
                }
            },
            update = { chart ->
                chart.setTouchEnabled(isClickable)
                chart.isRotationEnabled = isClickable
                chart.holeRadius = holeRadiusRatio
                chart.transparentCircleRadius = holeRadiusRatio + 3f

                if (!centerText.isNullOrEmpty()) {
                    chart.centerText = centerText
                    chart.setCenterTextColor(centerTextColorArgb)
                    chart.setCenterTextSize(if (chartHeight < 160.dp) 11f else 13f)
                    chart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)
                } else {
                    chart.centerText = ""
                }

                val dataSet = PieDataSet(chartEntries, "").apply {
                    colors = chartColors
                    valueTextColor = AndroidColor.WHITE
                    valueTextSize = 10f
                    valueTypeface = Typeface.DEFAULT_BOLD
                    sliceSpace = 2.5f
                    selectionShift = if (isClickable) 5f else 0f
                }

                chart.data = PieData(dataSet).apply {
                    setDrawValues(false)
                }
                chart.setDrawEntryLabels(false)

                if (isClickable && onSliceClick != null) {
                    chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            if (e is PieEntry) {
                                val clickedSlice = slices.find { it.name == e.label }
                                if (clickedSlice != null) {
                                    onSliceClick(clickedSlice)
                                }
                            }
                        }
                        override fun onNothingSelected() {}
                    })
                } else {
                    chart.setOnChartValueSelectedListener(null)
                }

                chart.highlightValues(null)
                chart.invalidate()
            }
        )
    }

    when (legendStyle) {
        PieChartLegendStyle.NONE -> {
            Box(
                modifier = modifier.height(chartHeight),
                contentAlignment = Alignment.Center
            ) {
                chartContent()
            }
        }

        PieChartLegendStyle.COMPACT_RIGHT -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(chartHeight),
                    contentAlignment = Alignment.Center
                ) {
                    chartContent()
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slices.forEachIndexed { index, slice ->
                        val color = slice.color ?: DefaultPieChartColors[index % DefaultPieChartColors.size]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isClickable && onSliceClick != null) Modifier.clickable { onSliceClick(slice) } else Modifier),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slice.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "%.1f%%".format(slice.percentage).replace('.', ','),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }
                    }
                }
            }
        }

        PieChartLegendStyle.DETAILED_BOTTOM -> {
            Column(modifier = modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    contentAlignment = Alignment.Center
                ) {
                    chartContent()
                }

                Spacer(modifier = Modifier.height(16.dp))

                val total = slices.sumOf { it.amount }
                slices.forEachIndexed { index, slice ->
                    val color = slice.color ?: DefaultPieChartColors[index % DefaultPieChartColors.size]
                    val pct = if (total > 0) (slice.amount / total * 100) else slice.percentage.toDouble()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .then(if (isClickable && onSliceClick != null) Modifier.clickable { onSliceClick(slice) } else Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = slice.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyTextOnPrimary(
                                amount = slice.amount,
                                isVisibilityOff = isVisibilityOff,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = " (",
                                style = MaterialTheme.typography.bodySmall
                            )
                            PercentageTextOnPrimary(
                                amount = pct,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = ")",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stateful [AppPieChart] for transaction breakdown with optional drill-down interactivity.
 *
 * @param viewModel [DataBaseViewModel] to fetch transaction data.
 * @param startDate Start date in Excel date format.
 * @param endDate End date in Excel date format.
 * @param modifier Container modifier.
 * @param initialCategory Optional category filter.
 * @param isClickable Whether clicking on chart slices allows drill-down into sub-categories/items/labels.
 */
@Composable
fun AppPieChart(
    viewModel: DataBaseViewModel,
    startDate: Double,
    endDate: Double,
    modifier: Modifier = Modifier,
    initialCategory: String? = null,
    isClickable: Boolean = true,
    holeRadiusRatio: Float = 52f
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val transactions by produceState(initialValue = emptyList(), viewModel, refreshTrigger) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var isViewingOthersCategories by remember { mutableStateOf(false) }
    var isViewingOthersItems by remember { mutableStateOf(false) }
    var isViewingOthersLabels by remember { mutableStateOf(false) }

    LaunchedEffect(initialCategory, startDate, endDate) {
        selectedCategory = initialCategory
        selectedItem = null
        isViewingOthersCategories = false
        isViewingOthersItems = false
        isViewingOthersLabels = false
    }

    var selectedLabelForTransactions by remember { mutableStateOf<String?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionDB?>(null) }
    var selectedOthersTransactions by remember { mutableStateOf<List<TransactionDB>?>(null) }

    val filteredTransactions = transactions.filter {
        it.date != null && it.amount != null && it.category != null &&
                it.date in startDate..endDate
    }

    val chartEntries = when (initialCategory) {
        null if selectedCategory == null && !isViewingOthersCategories -> {
            val catTotals = filteredTransactions
                .groupBy { it.category ?: "Inconnu" }
                .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
            createPieEntries(catTotals, topN = 8, othersLabel = "Others")
        }
        null if selectedCategory == null && isViewingOthersCategories -> {
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

    val slices = remember(chartEntries) {
        val total = chartEntries.sumOf { it.value.toDouble() }
        chartEntries.mapIndexed { index, entry ->
            val amt = entry.value.toDouble()
            val pct = if (total > 0) ((amt / total) * 100).toFloat() else 0f
            val color = DefaultPieChartColors[index % DefaultPieChartColors.size]
            PieChartSlice(name = entry.label ?: "", amount = amt, percentage = pct, color = color)
        }
    }

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
        modifier = modifier
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

            if (isClickable && canGoBack) {
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

            val handleSliceClick: (PieChartSlice) -> Unit = { slice ->
                if (isClickable) {
                    val label = slice.name
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AppPieChart(
                    slices = slices,
                    isClickable = isClickable,
                    centerText = centerTextFormatted,
                    holeRadiusRatio = holeRadiusRatio,
                    chartHeight = 250.dp,
                    legendStyle = PieChartLegendStyle.NONE,
                    isVisibilityOff = isVisibilityOff,
                    onSliceClick = handleSliceClick
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed legend with evolution comparison
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
        val topLabels = chartEntries.map { val l = it.label; l ?: "" }.filter { it != "Others" }.toSet()

        slices.forEachIndexed { index, slice ->
            val label = slice.name
            val amount = slice.amount
            val percent = if (total > 0) (amount / total * 100) else 0.0
            val color = slice.color ?: DefaultPieChartColors[index % DefaultPieChartColors.size]
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
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
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
