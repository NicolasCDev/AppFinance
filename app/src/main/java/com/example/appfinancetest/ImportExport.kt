package com.example.appfinancetest

import android.util.Log
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
            val date = row.getCell(1).numericCellValue
            val category = row.getCell(2).stringCellValue.toString()
            val item = row.getCell(3).stringCellValue.toString()
            val label = row.getCell(4).stringCellValue.toString()
            val amount = row.getCell(5).numericCellValue
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

    // Write the workbook in OutputStream
    workbook.write(outputStream)

    // Close workbook -> free resources
    workbook.close()
}

suspend fun addTransaction(listTransactions: List<Transaction>, databaseViewModel: DataBaseViewModel) {
    Log.d("Import/Export", "Adding transactions")
    coroutineScope {
        val insertJobs = listTransactions.map { transaction ->
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
                databaseViewModel.insertTransaction(transactionDB)
            }
        }

        // Waiting for all insertions to finish
        insertJobs.awaitAll()

        // Then calculate the balance
        calculateRunningBalance(databaseViewModel)
    }
}

suspend fun calculateRunningBalance(databaseViewModel: DataBaseViewModel) {
    Log.d("Import/Export", "Calculating running balance")
    // Gather every transactions order by date
    val existingTransactions = databaseViewModel.getTransactionsSortedByDateASC()
    var balance = 0.0
    existingTransactions.forEach { transaction ->
        balance += transaction.variation ?: 0.0
        // Update balance of each transaction
        databaseViewModel.updateBalance(transaction.id, balance)
    }
}

suspend fun addInvestments(databaseViewModel: DataBaseViewModel, investmentViewModel: InvestmentDB_ViewModel) {
    Log.d("Import/Export", "Adding investments")
    val investmentList = databaseViewModel.getInvestmentTransactions()
    Log.d("Import/Export", "Investment transaction list size: ${investmentList.size}")

    val investmentListGrouped = investmentList.groupBy { it.idInvest }
        .map { (idInvest, transactions) ->
            val investedTransactions = transactions.filter { it.category == "Investissement" }
            val earnedTransactions = transactions.filter { it.category == "Gain investissement" }

            val dateBegin = transactions.minOfOrNull { it.date as Double } ?: 0.0
            val transactionList = transactions.mapNotNull { it.id }
            val invested = investedTransactions.sumOf { it.amount as Double }
            val earned = earnedTransactions.sumOf { it.amount as Double }

            val item = transactions.firstOrNull()?.item ?: ""
            val label = transactions.firstOrNull()?.label ?: ""

            Investment(idInvest, dateBegin, null, transactionList, invested, earned, 0.0, 0.0, item, label)
        }

    Log.d("Import/Export", "Investment list size: ${investmentListGrouped.size}")

    coroutineScope {
        val jobs = investmentListGrouped.map { investment ->
            async {
                val existing = investmentViewModel.getInvestmentById(investment.idInvest ?: "")
                val investmentDB = InvestmentDB(
                    idInvest = investment.idInvest ?: "",
                    dateBegin = investment.dateBegin,
                    dateEnd = investment.dateEnd,
                    transactionList = investment.transactionList,
                    invested = investment.invested,
                    earned = investment.earned,
                    profitability = investment.profitability,
                    annualProfitability = investment.annualProfitability,
                    item = investment.item,
                    label = investment.label
                )
                if (existing == null) {
                    investmentViewModel.insertInvestment(investmentDB)
                    Log.d("Import/Export", "Investment inserted")
                } else {
                    investmentViewModel.updateInvestment(investmentDB)
                    Log.d("Import/Export", "Investment updated")
                }
            }
        }
        jobs.awaitAll()
    }
}

suspend fun validateInvestments(databaseViewModel: DataBaseViewModel, investmentViewModel: InvestmentDB_ViewModel, idInvest: String?, onValidated: () -> Unit) {
    Log.d("Import/Export", "Validating investments: $idInvest")
    if (idInvest == null) return
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


    val dateBegin = investmentTransactions.minOfOrNull { it.date as Double } ?: 0.0
    val dateEnd = investmentTransactions.maxOfOrNull { it.date as Double } ?: 0.0
    Log.d("Import/Export", "Date begin: $dateBegin, date end: $dateEnd")

    if (investmentRow != null) {
        val updatedInvestment = InvestmentDB(
            id = investmentRow.id,
            idInvest = investmentRow.idInvest,
            dateBegin = dateBegin,
            dateEnd = dateEnd,
            transactionList = investmentRow.transactionList,
            invested = invested,
            earned = earned,
            profitability = profitability,
            annualProfitability = annualProfitability,
            item = investmentRow.item,
            label = investmentRow.label
        )
        Log.d("Import/Export", "updatedInvestment.dateEnd = ${updatedInvestment.dateEnd}")
        investmentViewModel.updateInvestment(updatedInvestment)
        Log.d("Import/Export", "Investment updated")
        onValidated()
    }
}

suspend fun invalidateInvestments(databaseViewModel: DataBaseViewModel, investmentViewModel: InvestmentDB_ViewModel, idInvest: String?, onValidated: () -> Unit) {
    Log.d("Import/Export", "Invalidating investments: $idInvest")
    if (idInvest == null) return
    val investmentTransactions = databaseViewModel.getInvestmentTransactionsByID(idInvest)
    val investmentRow = investmentViewModel.getInvestmentById(idInvest)

    val invested = investmentRow?.invested ?: 0.0
    val earned = investmentRow?.earned ?: 0.0
    val dateBegin = investmentTransactions.minOfOrNull { it.date as Double } ?: 0.0

    if (investmentRow != null) {
        val updatedInvestment = InvestmentDB(
            id = investmentRow.id,
            idInvest = investmentRow.idInvest,
            dateBegin = dateBegin,
            dateEnd = null,
            transactionList = investmentRow.transactionList,
            invested = invested,
            earned = earned,
            profitability = 0.0,
            annualProfitability = 0.0,
            item = investmentRow.item,
            label = investmentRow.label
        )
        investmentViewModel.updateInvestment(updatedInvestment)
        onValidated()
    }
}
