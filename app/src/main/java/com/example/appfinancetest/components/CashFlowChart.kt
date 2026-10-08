package com.example.appfinancetest.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.ui.theme.*
import java.util.*
import kotlin.math.abs

@Composable
fun CashFlowChart(
    databaseViewModel: DataBaseViewModel,
    refreshTrigger: Int = 0
) {
    val transactions by produceState(initialValue = emptyList(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    var selectedRange by remember { mutableStateOf("1M") }
    val rangeOptions = listOf("1M", "6M", "1Y", "5Y")

    val filteredTransactions = remember(transactions, selectedRange) {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        
        val startTime = when (selectedRange) {
            "1M" -> { calendar.add(Calendar.MONTH, -1); calendar.timeInMillis }
            "6M" -> { calendar.add(Calendar.MONTH, -6); calendar.timeInMillis }
            "1Y" -> { calendar.add(Calendar.YEAR, -1); calendar.timeInMillis }
            "5Y" -> { calendar.add(Calendar.YEAR, -5); calendar.timeInMillis }
            else -> 0L
        }

        transactions.filter { t ->
            val dateExcel = t.date ?: return@filter false
            val millis = ((dateExcel - 25569) * 86400 * 1000).toLong()
            millis in startTime..today
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            rangeOptions.forEach { option ->
                FilterChip(
                    selected = selectedRange == option,
                    onClick = { selectedRange = option },
                    label = { Text(option) }
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Aucune donnée pour cette période.", color = Color.Gray)
            }
            return@Column
        }

        // 1. Incomes (Sources)
        val incomes = filteredTransactions.filter { 
            it.category == "Revenus" || it.category == "Gain investissement"
        }
            .groupBy { 
                if (it.category == "Gain investissement") it.label ?: it.item ?: "Gains d'investissements"
                else it.item ?: "Salaire"
            }
            .mapValues { it.value.sumOf { t -> t.amount ?: 0.0 } }
        val totalIncome = incomes.values.sum().coerceAtLeast(1.0)

        // 2. Intermediate Categories (Items)
        val intermediateCategories = filteredTransactions.filter { 
            it.category != "Revenus" && it.category != "Gain investissement"
        }
            .groupBy { 
                if (it.category == "Investissement") "Investissements"
                else it.category ?: "Charges"
            }
            .mapValues { it.value.sumOf { t -> abs(t.amount ?: 0.0) } }
        
        // 3. Details (Labels) grouped by their category
        val detailedLabels = filteredTransactions.filter { 
            it.category != "Revenus" && it.category != "Gain investissement"
        }
            .groupBy { 
                if (it.category == "Investissement") "Investissements" else it.category ?: "Charges"
            }
            .mapValues { entry ->
                entry.value.groupBy { it.label ?: it.item ?: "Divers" }
                    .mapValues { labelEntry -> labelEntry.value.sumOf { abs(it.amount ?: 0.0) } }
            }

        val totalNodes = incomes.size + intermediateCategories.size + detailedLabels.values.sumOf { it.size }
        val chartHeight = (totalNodes * 35).dp.coerceAtLeast(600.dp)
        val chartWidth = 900.dp // Plus large pour le scroll horizontal

        Text(
            text = "Flux de Trésorerie (${selectedRange})",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val scrollState = rememberScrollState()
        val textColor = MaterialTheme.colorScheme.onSurface
        val bodySmallStyle = MaterialTheme.typography.bodySmall.copy(color = textColor)
        val bodyMediumStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Prend le reste de l'espace
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(chartHeight)
            ) {
                val width = size.width
                val height = size.height
                val nodeWidth = 8.dp.toPx()
                val colSpacing = width / 4f

                // Couleurs
                val incomeColor = IncomeBlue
                val budgetColor = BudgetYellow
                val detailColors = listOf(
                    RedAccent, GreenAccent, BudgetYellow,
                    IncomeBlue, ChartPurple, ChartCyan,
                    ChartDeepOrange, ChartBlueGrey, ChartLightGreen
                )

                // --- COLONNE 1 : SOURCES (Incomes) ---
                var ySource = height * 0.1f
                val sourceNodes = mutableMapOf<String, Float>()
                incomes.forEach { (name, amount) ->
                    val h = (amount / totalIncome * height * 0.5f).toFloat().coerceAtLeast(20f)
                    drawRect(color = incomeColor, topLeft = Offset(0f, ySource), size = Size(nodeWidth, h))
                    drawText(textMeasurer, "$name\n%.0f€".format(amount), 
                        topLeft = Offset(nodeWidth + 8f, ySource + h/2 - 18f),
                        style = bodySmallStyle.copy(fontWeight = FontWeight.Medium))
                    sourceNodes[name] = ySource
                    ySource += h + 30f
                }

                // --- COLONNE 2 : BUDGET ---
                val budgetX = colSpacing * 0.8f
                val budgetH = height * 0.6f
                val budgetY = height * 0.15f
                drawRect(color = incomeColor.copy(alpha = 0.1f), topLeft = Offset(budgetX, budgetY), size = Size(colSpacing * 0.7f, budgetH))
                drawRect(color = budgetColor, topLeft = Offset(budgetX + colSpacing * 0.7f, budgetY), size = Size(nodeWidth, budgetH))
                drawText(textMeasurer, "BUDGET\n%.0f€".format(totalIncome), 
                    topLeft = Offset(budgetX + 10f, budgetY + budgetH/2 - 20f),
                    style = bodyMediumStyle.copy(fontWeight = FontWeight.ExtraBold))

                // --- COLONNE 3 : INTERMÉDIAIRES (Categories) ---
                val interX = colSpacing * 2.3f
                var yInter = budgetY
                val interNodes = mutableMapOf<String, Float>()
                
                intermediateCategories.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
                    val h = (amount / totalIncome * budgetH).toFloat().coerceAtLeast(15f)
                    drawRect(color = budgetColor, topLeft = Offset(interX, yInter), size = Size(nodeWidth, h))
                    drawText(textMeasurer, "$name\n%.0f€".format(amount), 
                        topLeft = Offset(interX - 110f, yInter + h/2 - 18f),
                        style = bodySmallStyle.copy(textAlign = TextAlign.End))
                    interNodes[name] = yInter
                    yInter += h + 25f
                }

                // --- COLONNE 4 : DÉTAILS (Labels) ---
                val detailX = width - nodeWidth - 120f // Marge pour le texte à droite
                var yDetail = height * 0.05f
                val currentYOffsetInCategory = mutableMapOf<String, Float>()
                
                var globalDetailIndex = 0
                intermediateCategories.entries.sortedByDescending { it.value }.forEach { (catName, _) ->
                    val labels = detailedLabels[catName] ?: emptyMap()
                    labels.entries.sortedByDescending { it.value }.forEach { (labelName, amount) ->
                        val h = (amount / totalIncome * height * 0.7f).toFloat().coerceAtLeast(10f)
                        val color = detailColors[globalDetailIndex % detailColors.size]
                        
                        drawRect(color = color, topLeft = Offset(detailX, yDetail), size = Size(nodeWidth, h))
                        drawText(textMeasurer, "$labelName\n%.0f€".format(amount), 
                            topLeft = Offset(detailX + nodeWidth + 8f, yDetail + h/2 - 15f),
                            style = bodySmallStyle)

                        // Lien Catégorie -> Détail
                        val catStartY = interNodes[catName] ?: budgetY
                        val catOffset = currentYOffsetInCategory.getOrDefault(catName, 0f)
                        val flowStartH = (amount / totalIncome * budgetH).toFloat()

                        drawSankeyLink(
                            startX = interX + nodeWidth, startY = catStartY + catOffset, startWidth = flowStartH,
                            endX = detailX, endY = yDetail, endWidth = h,
                            color = color
                        )
                        
                        currentYOffsetInCategory[catName] = catOffset + flowStartH
                        yDetail += h + 15f
                        globalDetailIndex++
                    }
                }

                // --- LIENS BUDGET -> CATEGORIES ---
                var flowY = budgetY
                intermediateCategories.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
                    val h = (amount / totalIncome * budgetH).toFloat().coerceAtLeast(15f)
                    drawSankeyLink(
                        startX = budgetX + colSpacing * 0.7f + nodeWidth, startY = flowY, startWidth = h,
                        endX = interX, endY = flowY, endWidth = h,
                        color = budgetColor
                    )
                    flowY += h + 25f
                }

                // --- LIENS SOURCE -> BUDGET ---
                var flowSourceY = budgetY
                incomes.forEach { (name, amount) ->
                    val startY = sourceNodes[name] ?: 0f
                    val startH = (amount / totalIncome * height * 0.5f).toFloat().coerceAtLeast(20f)
                    val endH = (amount / totalIncome * budgetH).toFloat()
                    
                    drawSankeyLink(
                        startX = nodeWidth, startY = startY, startWidth = startH,
                        endX = budgetX, endY = flowSourceY, endWidth = endH,
                        color = incomeColor
                    )
                    flowSourceY += endH
                }
            }
        }
    }
}

fun DrawScope.drawSankeyLink(
    startX: Float, startY: Float, startWidth: Float,
    endX: Float, endY: Float, endWidth: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(startX, startY)
        cubicTo(
            (startX + endX) / 2, startY,
            (startX + endX) / 2, endY,
            endX, endY
        )
        lineTo(endX, endY + endWidth)
        cubicTo(
            (startX + endX) / 2, endY + endWidth,
            (startX + endX) / 2, startY + startWidth,
            startX, startY + startWidth
        )
        close()
    }
    drawPath(path, color = color.copy(alpha = 0.2f))
}
