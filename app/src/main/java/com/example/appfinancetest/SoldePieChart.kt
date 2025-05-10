package com.example.appfinancetest

import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.components.Legend

@Composable
fun SoldePieChart(viewModel: DataBase_ViewModel, startDate: Double, endDate: Double) {
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), viewModel) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val filteredTransactions = transactions.filter {
        it.date != null && it.montant != null && it.categorie != null &&
                it.date in startDate..endDate
    }

    val chartEntries = if (selectedCategory == null) {
        // Global view: group by category
        filteredTransactions
            .groupBy { it.categorie }
            .mapValues { entry -> entry.value.sumOf { it.montant ?: 0.0 } }
            .map { (category, total) ->
                PieEntry(total.toFloat(), category)
            }
    } else {
        // Drill-down: group by label within the selected category
        val posteTotals = filteredTransactions
            .filter { it.categorie == selectedCategory && it.poste != null }
            .groupBy { it.poste!! }
            .mapValues { entry -> entry.value.sumOf { it.montant ?: 0.0 } }

        val sortedPostes = posteTotals.entries.sortedByDescending { it.value }
        val top8= sortedPostes.take(8)
        val others = sortedPostes.drop(8)

        val chartEntries = mutableListOf<PieEntry>()

        for ((poste, total) in top8) {
            chartEntries.add(PieEntry(total.toFloat(), poste))
        }

        val othersTotal = others.sumOf { it.value }
        if (othersTotal > 0) {
            chartEntries.add(PieEntry(othersTotal.toFloat(), "Autres"))
        }
        chartEntries
    }
    val customColors = listOf(
        Color.rgb(0,0,255), // Blue
        Color.rgb(255,0,0), // Red
        Color.rgb(0,255,0), // Green
        Color.rgb(255,142,36), // Orange
        Color.rgb(255,0,255), // Pink
        Color.rgb(136, 66, 29), // Acajou
        Color.rgb(192,192,192),  // Grey
        Color.rgb(145, 40, 59), // Amarante
        Color.rgb(16, 52, 166), // Bleu Egyptien
        Color.rgb(255,255,87), // Beige
        Color.rgb(128,0,128), // Purple
        Color.rgb(255, 94, 77), // Rouge capucine
        Color.rgb(255,96,125), // White pink
        Color.rgb(0,255,255), // Cyan
        Color.rgb(255,255,0), // Yellow
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        Text(
            text = selectedCategory?.let { "Répartition par : $it" } ?: "Répartition par : Catégories",
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(0.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(35.dp) // Largeur fixe du "rectangle"
                        .padding(end = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { selectedCategory = null },
                        modifier = Modifier
                            .fillMaxSize(), // Prend toute la hauteur et largeur du box
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent, // Fond transparent
                            contentColor = androidx.compose.ui.graphics.Color.White // Couleur du texte/flèche
                    )
                    ) {
                        Text("<", fontWeight = FontWeight.Bold)
                    }
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
                        legend.isEnabled = true
                        legend.textColor = Color.WHITE
                        legend.textSize = 12f
                        legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                        legend.orientation = Legend.LegendOrientation.VERTICAL
                        legend.setDrawInside(false)
                    }
                },
                update = { chart ->
                    val dataSet = PieDataSet(chartEntries, "").apply {
                        colors = customColors.take(chartEntries.size)
                        valueTextColor = Color.WHITE
                        valueTextSize = 14f
                    }

                    chart.data = PieData(dataSet)
                    // Masquer les labels sur le camembert si une catégorie est sélectionnée
                    chart.setDrawEntryLabels(selectedCategory == null)

                    chart.setOnChartValueSelectedListener(object :
                        com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                        override fun onValueSelected(
                            e: Entry?,
                            h: com.github.mikephil.charting.highlight.Highlight?
                        ) {
                            if (selectedCategory == null && e is PieEntry) {
                                selectedCategory = e.label
                            }
                        }

                        override fun onNothingSelected() {}
                    })
                    chart.invalidate()
                    chart.highlightValues(null)
                }
            )

        }
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val tableEntries = if (selectedCategory == null) {
                // Global view: group by category
                filteredTransactions
                    .groupBy { it.categorie }
                    .mapValues { entry -> entry.value.sumOf { it.montant ?: 0.0 } }
                    .map { (category, total) ->
                        PieEntry(total.toFloat(), category)
                    }
            } else {
                // Drill-down: group by label within the selected category
                val posteTotals = filteredTransactions
                    .filter { it.categorie == selectedCategory && it.poste != null }
                    .groupBy { it.poste!! }
                    .mapValues { entry -> entry.value.sumOf { it.montant ?: 0.0 } }

                val sortedPostes = posteTotals.entries.sortedByDescending { it.value }
                val top5= sortedPostes.take(5)
                val others = sortedPostes.drop(5)

                val tableEntries = mutableListOf<PieEntry>()

                for ((poste, total) in top5) {
                    tableEntries.add(PieEntry(total.toFloat(), poste))
                }

                val othersTotal = others.sumOf { it.value }
                if (othersTotal > 0) {
                    tableEntries.add(PieEntry(othersTotal.toFloat(), "Autres"))
                }
                tableEntries
            }
            val total = tableEntries.sumOf { it.value.toDouble() }

            tableEntries.take(6).forEach { entry -> // Top 5 + "Autres" éventuel
                val label = entry.label
                val amount = entry.value.toDouble()
                val percent = if (total > 0) (amount / total * 100) else 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, fontSize = 12.sp)
                    Text(text = String.format("%.2f € (%.1f%%)", amount, percent), fontSize = 12.sp)
                }
            }
        }
    }

}
