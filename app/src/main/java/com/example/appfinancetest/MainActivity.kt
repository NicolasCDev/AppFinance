package com.example.appfinancetest

import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.appfinancetest.ui.theme.AppFinanceTestTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Settings
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.io.InputStream
import java.io.OutputStream


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFinanceTestTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val items = listOf("Tableau de bord", "Investment", "Patrimonial", "DataBase")
    val icons = listOf(R.drawable.ic_dashboard, R.drawable.ic_investment, R.drawable.ic_patrimoine, R.drawable.ic_database)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Parameters")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = icons[index]),
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        modifier = if(index == 0){
                             Modifier.weight(1f)
                        }else{
                            Modifier
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedItem) {
            0 -> DashboardScreen(modifier = Modifier.padding(innerPadding))
            1 -> InvestissementScreen(modifier = Modifier.padding(innerPadding))
            2 -> PatrimonialScreen(modifier = Modifier.padding(innerPadding))
            3 -> DataBaseScreen(modifier = Modifier.padding(innerPadding))
            else -> {
                ErrorScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

fun readExcelFile(inputStream: InputStream): List<Transaction> {
    val transactions = mutableListOf<Transaction>()
    try {
        val workbook: Workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0) // Choose the first sheet of the Excel file

        for (row in sheet) {
            if (row.rowNum == 0) continue // Ignore the first row (titles)

            val dateCell = row.getCell(0).dateCellValue
            val dateFormat = SimpleDateFormat("dd/MM/yy")
            val date = dateFormat.format(dateCell)
            val category = row.getCell(1).stringCellValue.toString()
            val poste = row.getCell(2).stringCellValue.toString()
            val label = row.getCell(3).stringCellValue.toString()
            val amount = row.getCell(4).numericCellValue.toDouble()

            // Ajouter une nouvelle transaction à la liste
            transactions.add(Transaction(date, category, poste, label, amount))
        }
    } catch (e: Exception) {
        e.printStackTrace() // Gérer les erreurs de lecture de fichier
    }

    // Ajouter un log pour vérifier les transactions lues
    Log.d("ExcelReader", "Transactions lues : ${transactions.size}")
    return transactions
}

fun writeExcelFile(outputStream: OutputStream, transactions: List<Transaction>) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Transactions")

    // En-têtes
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("Date")
    headerRow.createCell(1).setCellValue("Catégorie")
    headerRow.createCell(2).setCellValue("Poste")
    headerRow.createCell(3).setCellValue("Libellé")
    headerRow.createCell(4).setCellValue("Montant")

    // Données des transactions
    for ((index, transaction) in transactions.withIndex()) {
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction.date)
        row.createCell(1).setCellValue(transaction.categorie)
        row.createCell(2).setCellValue(transaction.poste)
        row.createCell(3).setCellValue(transaction.label)
        row.createCell(4).setCellValue(transaction.montant)
    }

    // Écriture dans le OutputStream
    workbook.write(outputStream) // Cette ligne permet de sauvegarder le contenu du workbook dans le OutputStream

    // Libération des ressources
    workbook.close() // Fermer le workbook pour libérer les ressources
}

fun addTransaction(listTransactions: List<Transaction>, viewModel: DataBase_ViewModel){
    for (transaction in listTransactions) {
        val transactionDB = Transaction_DB(
            date = transaction.date,
            categorie = transaction.categorie,
            poste = transaction.poste,
            label = transaction.label,
            montant = transaction.montant
        )
        viewModel.insertTransaction(transactionDB)
    }
}