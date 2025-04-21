package com.example.appfinancetest

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream


fun readExcelFile(inputStream: InputStream): List<Transaction> {
    val transactions = mutableListOf<Transaction>()
    try {
        val workbook: Workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0) // Choose the first sheet of the Excel file

        for (row in sheet) {
            if (row.rowNum == 0) continue // Ignore the first row (titles)

            val date = row.getCell(0).numericCellValue.toDouble()
            val category = row.getCell(1).stringCellValue.toString()
            val poste = row.getCell(2).stringCellValue.toString()
            val label = row.getCell(3).stringCellValue.toString()
            val amount = row.getCell(4).numericCellValue.toDouble()
            val variation = when (category) {
                "Investissement", "Charge" -> -amount
                "Revenus", "Gain investissement" -> amount
                else -> 0.0
            }

            // Ajouter une nouvelle transaction à la liste
            transactions.add(Transaction(date, category, poste, label, amount, variation, solde = 0.0))
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
    headerRow.createCell(5).setCellValue("Variation")

    // Données des transactions
    for ((index, transaction) in transactions.withIndex()) {
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction.date)
        row.createCell(1).setCellValue(transaction.categorie)
        row.createCell(2).setCellValue(transaction.poste)
        row.createCell(3).setCellValue(transaction.label)
        row.createCell(4).setCellValue(transaction.montant)
        row.createCell(5).setCellValue(transaction.variation)
    }

    // Écriture dans le OutputStream
    workbook.write(outputStream) // Cette ligne permet de sauvegarder le contenu du workbook dans le OutputStream

    // Libération des ressources
    workbook.close() // Fermer le workbook pour libérer les ressources
}

suspend fun addTransaction(listTransactions: List<Transaction>, viewModel: DataBase_ViewModel) {
    viewModel.viewModelScope.launch {
        // Utiliser `async` pour insérer chaque transaction dans une coroutine séparée
        val insertJobs =
            listTransactions.map { transaction: Transaction ->
                // Lancer une coroutine pour insérer chaque transaction
                async {
                    val transactionDB = TransactionDB(
                        date = transaction.date,
                        categorie = transaction.categorie,
                        poste = transaction.poste,
                        label = transaction.label,
                        montant = transaction.montant,
                        variation = transaction.variation,
                        solde = transaction.solde
                    )
                    // Insérer la transaction dans la base de données
                    viewModel.insertTransaction(transactionDB)
                }
            }

        // Attendre que toutes les insertions soient terminées
        insertJobs.awaitAll()
        delay(2000)
        // Une fois toutes les transactions insérées, on calcule les soldes
        calculateRunningBalance(viewModel)
    }
}

suspend fun calculateRunningBalance(viewModel: DataBase_ViewModel) {
    // Récupérer toutes les transactions triées par date
    val existingTransactions = viewModel.getTransactionsSortedByDateASC()
    var solde = 0.0
    existingTransactions.forEach { transaction ->
        solde += transaction.variation ?: 0.0
        // Mise à jour du solde pour chaque transaction
        viewModel.updateSolde(transaction.id, solde)
    }
}