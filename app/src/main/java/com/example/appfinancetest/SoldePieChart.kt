package com.example.appfinancetest

import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.foundation.layout.*
import java.util.*

@Composable
fun SoldePieChart(viewModel: DataBase_ViewModel, startDate: Double, endDate: Double) {
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), viewModel) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    val filteredTransactions = transactions.filter {
        it.date != null && it.montant != null && it.categorie != null &&
                it.date in startDate..endDate
    }

    val groupedByCategory = filteredTransactions
        .groupBy { it.categorie }
        .mapValues { entry -> entry.value.sumOf { it.montant ?: 0.0 } }

    val entries = groupedByCategory.map { (category, total) ->
        PieEntry(total.toFloat(), category)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isRotationEnabled = true
                legend.textColor = Color.WHITE
                setUsePercentValues(true)
                setEntryLabelColor(Color.WHITE)
                setEntryLabelTextSize(12f)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "Répartition par catégorie").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextColor = Color.WHITE
                valueTextSize = 14f
            }

            chart.data = PieData(dataSet)
            chart.invalidate()
        }
    )
}
