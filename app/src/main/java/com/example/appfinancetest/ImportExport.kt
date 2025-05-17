package com.example.appfinancetest

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.pow


fun readExcelFile(inputStream: InputStream): List<Transaction> {
    val transactions = mutableListOf<Transaction>()
    try {
        val workbook: Workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0) // Choose the first sheet of the Excel file

        for (row in sheet) {
            if (row.rowNum == 0) continue // Ignore the first row (titles)

            val cell = row.getCell(0)
            val idInvest = if (cell != null && cell.cellType == CellType.STRING) {
                cell.stringCellValue
            } else {
                ""
            }
            val date = row.getCell(1).numericCellValue.toDouble()
            val category = row.getCell(2).stringCellValue.toString()
            val item = row.getCell(3).stringCellValue.toString()
            val label = row.getCell(4).stringCellValue.toString()
            val amount = row.getCell(5).numericCellValue.toDouble()
            val variation = when (category) {
                "Investissement", "Charge" -> -amount
                "Revenus", "Gain investissement" -> amount
                else -> 0.0
            }

            // Add a new transaction to the list
            transactions.add(Transaction(date, category, item, label, amount, variation, balance = 0.0, idInvest))
        }
    } catch (e: Exception) {
        e.printStackTrace() // Manage file reading errors
    }

    // Add a log in order to verify number of transactions read
    Log.d("ExcelReader", "Transactions read : ${transactions.size}")
    return transactions
}

fun writeExcelFile(outputStream: OutputStream, transactions: List<Transaction>) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Transactions")

    // En-têtes
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("ID Invest")
    headerRow.createCell(1).setCellValue("Date")
    headerRow.createCell(2).setCellValue("Category")
    headerRow.createCell(3).setCellValue("Item")
    headerRow.createCell(4).setCellValue("Label")
    headerRow.createCell(5).setCellValue("Amount")
    headerRow.createCell(6).setCellValue("Variation")

    // Transactions data
    for ((index, transaction) in transactions.withIndex()) {
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction.idInvest)
        row.createCell(1).setCellValue(transaction.date)
        row.createCell(2).setCellValue(transaction.category)
        row.createCell(3).setCellValue(transaction.item)
        row.createCell(4).setCellValue(transaction.label)
        row.createCell(5).setCellValue(transaction.amount)
        row.createCell(6).setCellValue(transaction.variation)

    }

    // Écriture dans le OutputStream
    workbook.write(outputStream) // Cette ligne permet de sauvegarder le contenu du workbook dans le OutputStream

    // Libération des ressources
    workbook.close() // Close workbook -> free ressources
}

suspend fun addTransaction(listTransactions: List<Transaction>, databaseViewModel: DataBase_ViewModel) {
    databaseViewModel.viewModelScope.launch {
        val insertJobs =
            listTransactions.map { transaction: Transaction ->
                // Launch a coroutine for transaction insertion
                async {
                    val transactionDB = TransactionDB(
                        date = transaction.date,
                        category = transaction.category,
                        item = transaction.item,
                        label = transaction.label,
                        amount = transaction.amount,
                        variation = transaction.variation,
                        balance = transaction.balance,
                        idInvest = transaction.idInvest
                    )
                    // Insertion of the transaction inside the DB
                    databaseViewModel.insertTransaction(transactionDB)
                }
            }

        // Waiting end of insertion
        insertJobs.awaitAll()
        delay(2000)
        // Then we can calculate the balances
        calculateRunningBalance(databaseViewModel)
    }
}

suspend fun calculateRunningBalance(databaseViewModel: DataBase_ViewModel) {
    // Récupérer toutes les transactions triées par date
    val existingTransactions = databaseViewModel.getTransactionsSortedByDateASC()
    var solde = 0.0
    existingTransactions.forEach { transaction ->
        solde += transaction.variation ?: 0.0
        // Mise à jour du solde pour chaque transaction
        databaseViewModel.updateSolde(transaction.id, solde)
    }
}

suspend fun addInvestments(databaseViewModel: DataBase_ViewModel, investmentViewModel: InvestmentDB_ViewModel) {
    val investmentList = databaseViewModel.getInvestmentTransactions()

    val investmentListGrouped = investmentList.groupBy { it.idInvest }
        .map { (idInvest, transactions) ->
            val investedTransactions = transactions.filter { it.category == "Investissement" }
            val earnedTransactions = transactions.filter { it.category == "Gain investissement" }

            val invested = investedTransactions.sumOf { (it.amount as Double) }
            val earned = earnedTransactions.sumOf { (it.amount as Double) }


            val category = transactions.firstOrNull()?.category ?: ""
            val item = transactions.firstOrNull()?.item ?: ""
            val description = transactions.firstOrNull()?.label ?: ""

            Investment(idInvest, 0.0, invested, earned, 0.0, 0.0, category, item, description)
        }

    investmentViewModel.viewModelScope.launch {
        investmentListGrouped.map { investment: Investment ->
            async {
                val existing = investmentViewModel.getInvestmentById(investment.idInvest ?: "")
                val investmentDB = InvestmentDB(
                    idInvest = investment.idInvest ?: "",
                    dateEnd = investment.dateEnd,
                    invested = investment.invested,
                    earned = investment.earned,
                    profitability = investment.profitability,
                    annualProfitability = investment.annualProfitability,
                    category = investment.category,
                    item = investment.item,
                    description = investment.label
                )
                if (existing == null) {
                    investmentViewModel.insertInvestment(investmentDB)
                } else {
                    investmentViewModel.updateInvestment(investmentDB)
                }
            }
        }
    }
}


suspend fun validateInvestments(databaseViewModel: DataBase_ViewModel, investmentViewModel: InvestmentDB_ViewModel, idInvest: String) {
    val investmentTransactions = databaseViewModel.getInvestmentTransactionsByID(idInvest)
    val investmentRow = investmentViewModel.getInvestmentById(idInvest)

    val investedTransactions = investmentTransactions.filter { it.category == "Investissement" }
    val earnedTransactions = investmentTransactions.filter { it.category == "Gain investissement" }

    val invested = investmentRow?.invested ?: 0.0
    val earned = investmentRow?.earned ?: 0.0

    // Weighted dates
    val avgInvestDate = if (invested != 0.0) {
        investedTransactions.sumOf { (it.date as Double) * (it.amount as Double) } / invested
    } else 0.0

    val avgEarnDate = if (earned != 0.0) {
        earnedTransactions.sumOf { (it.date as Double) * (it.amount as Double) } / earned
    } else 0.0

    val days = ((avgEarnDate - avgInvestDate).coerceAtLeast(1.0))

    val profitability = if (invested != 0.0) (earned - invested) / invested * 100 else 0.0

    val annualProfitability = if (invested != 0.0 && days > 0.0) {
        ((earned / invested).pow(365.0 / days) - 1) * 100
    } else {
        0.0
    }

    val dateEnd = investmentTransactions.maxOfOrNull { it.date as Double } ?: 0.0

    if (investmentRow != null) {
        val updatedInvestment = InvestmentDB(
            idInvest = investmentRow.idInvest,
            dateEnd = dateEnd,
            invested = invested,
            earned = earned,
            profitability = profitability,
            annualProfitability = annualProfitability,
            category = investmentRow.category,
            item = investmentRow.item,
            description = investmentRow.description
        )
        investmentViewModel.updateInvestment(updatedInvestment)
    }
}