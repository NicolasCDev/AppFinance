import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appfinancetest.DataBase_ViewModel
import com.example.appfinancetest.DateFormattedText
import com.example.appfinancetest.TransactionDB
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.*

@Composable
fun SoldeLineChart(viewModel: DataBase_ViewModel) {
    val transactions by produceState(initialValue = emptyList<TransactionDB>(), viewModel) {
        value = viewModel.getTransactionsSortedByDateASC()
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp),
        factory = { context ->
            LineChart(context).apply {
                description.text = "Évolution du solde"
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
            }
        },
        update = { chart ->
            // Format de date pour affichage (ex: 14 avr)
            val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>()

            transactions.forEachIndexed { index, transaction ->
                val solde = transaction.solde
                val date = transaction.date

                if (solde != null && date != null) {
                    entries.add(Entry(index.toFloat(), solde.toFloat()))
                    val formattedDate = DateFormattedText(date)
                    labels.add(formattedDate)
                }
            }

            val dataSet = LineDataSet(entries, "Solde en €").apply {
                color = Color.BLUE
                valueTextColor = Color.WHITE
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(true)
                setDrawCircles(true)
                setDrawFilled(true)
                fillColor = Color.CYAN
            }
            chart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                textColor = Color.WHITE // <-- ICI
            }

            chart.axisLeft.textColor = Color.WHITE // <-- ICI
            chart.legend.textColor = Color.WHITE   // <-- Légende "Solde en €"
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
