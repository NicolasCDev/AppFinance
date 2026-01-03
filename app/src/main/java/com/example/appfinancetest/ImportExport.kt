package com.example.appfinancetest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.pow
import kotlin.math.abs


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

    withContext(Dispatchers.IO) {

        val transactionsDB = listTransactions.map { transaction ->
            TransactionDB(
                date = transaction.date,
                category = transaction.category,
                item = transaction.item,
                label = transaction.label,
                amount = transaction.amount,
                variation = transaction.variation,
                balance = transaction.balance,
                idInvest = transaction.idInvest
            )
        }
        // Option A : If you have an insertAll in your DAO (recommended)

        /*
        databaseViewModel.insertAll(transactionsDB)
        */

        // Option B : SIf you don't have any, do a simple loop

        transactionsDB.forEach { transactionDB ->
             databaseViewModel.insertTransaction(transactionDB)
        }


        // Then we calculate the balance
        calculateRunningBalance(databaseViewModel)
    }
}


suspend fun calculateRunningBalance(databaseViewModel: DataBaseViewModel) {
    withContext(Dispatchers.IO) {
        val existingTransactions = databaseViewModel.getTransactionsSortedByDateASC()
        var balance = 0.0
        val updateList = existingTransactions.map { transaction ->
            balance += (transaction.variation ?: 0.0)
            Pair(transaction.id, balance)
        }
        if (updateList.isNotEmpty()) {
            databaseViewModel.updateAllBalances(updateList)
        }
    }
}


suspend fun addInvestments(databaseViewModel: DataBaseViewModel, investmentViewModel: InvestmentDBViewModel) {
    val investmentList = databaseViewModel.getInvestmentTransactions()

    // Filter to only take idInvest starting with 'I'
    val filteredInvestmentList = investmentList.filter { it.idInvest?.startsWith("I", ignoreCase = true) == true }

    val investmentListGrouped = filteredInvestmentList.groupBy { it.idInvest }
        .map { (idInvest, transactions) ->
            val investedTransactions = transactions.filter { it.category == "Investissement" }
            val earnedTransactions = transactions.filter { it.category == "Gain investissement" }

            val dateBegin = transactions.minOfOrNull { it.date as Double } ?: 0.0
            val transactionList = transactions.map { it.id }
            val invested = investedTransactions.sumOf { it.amount as Double }
            val earned = earnedTransactions.sumOf { it.amount as Double }

            val item = transactions.firstOrNull()?.item ?: ""
            val label = transactions.firstOrNull()?.label ?: ""

            Investment(idInvest, dateBegin, null, transactionList, invested, earned, 0.0, 0.0, item, label)
        }

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
                } else {
                    investmentViewModel.updateInvestment(investmentDB)
                }
            }
        }
        jobs.awaitAll()
    }
}

suspend fun addCredits(databaseViewModel: DataBaseViewModel, creditViewModel: CreditDBViewModel) {
    val allTransactions = databaseViewModel.getInvestmentTransactions()
    
    // Filter to only take idInvest starting with 'C'
    val creditTransactions = allTransactions.filter { it.idInvest?.startsWith("C", ignoreCase = true) == true }
    
    val creditGroups = creditTransactions.groupBy { it.idInvest }
    
    coroutineScope {
        val jobs = creditGroups.map { (idInvest, transactions) ->
            async {
                val dateBegin = transactions.minOfOrNull { it.date ?: 0.0 } ?: 0.0
                
                // totalAmount = Sum of amounts with positive variation
                // reimbursedAmount = Sum of amounts with negative variations
                // Variation is negative for 'Charge' and 'Investissement'
                
                val borrowed = transactions.filter { (it.variation ?: 0.0) > 0 }.sumOf { it.amount ?: 0.0 }
                val paid = transactions.filter { (it.variation ?: 0.0) < 0 }.sumOf { it.amount ?: 0.0 }
                
                // Payed amount is saved in absolute value for reimbursedAmount
                val reimbursed = abs(paid)
                val remaining = borrowed - reimbursed
                
                val label = transactions.firstOrNull()?.label ?: "Crédit $idInvest"

                val existingCredits = creditViewModel.getAllCredits()
                val existing = existingCredits.find { it.idInvest == idInvest }

                val creditDB = CreditDB(
                    id = existing?.id ?: 0,
                    label = label,
                    dateBegin = dateBegin,
                    totalAmount = borrowed,
                    reimbursedAmount = reimbursed,
                    remainingAmount = remaining,
                    monthlyPayment = existing?.monthlyPayment ?: 0.0,
                    interestRate = existing?.interestRate ?: 0.0,
                    idInvest = idInvest
                )

                if (existing == null) {
                    creditViewModel.insertCredit(creditDB)
                } else {
                    creditViewModel.updateCredit(creditDB)
                }
            }
        }
        jobs.awaitAll()
    }
}

suspend fun validateInvestments(databaseViewModel: DataBaseViewModel, investmentViewModel: InvestmentDBViewModel, idInvest: String?, onValidated: () -> Unit) {
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
        investmentViewModel.updateInvestment(updatedInvestment)
        onValidated()
    }
}

suspend fun invalidateInvestments(databaseViewModel: DataBaseViewModel, investmentViewModel: InvestmentDBViewModel, idInvest: String?, onValidated: () -> Unit) {
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
