package com.example.appfinancetest.calculations

import com.example.appfinancetest.classes.InvestmentDB
import com.example.appfinancetest.classes.TransactionDB
import kotlin.math.pow

/**
 * Calculates the profit percentage given the invested amount and earned amount.
 * Formula: ((earned - invested) / invested) * 100
 */
fun calculateProfitPercent(invested: Double, earned: Double): Double {
    return if (invested > 0.0) {
        ((earned - invested) / invested) * 100.0
    } else {
        0.0
    }
}

/**
 * Calculates the weighted annual profitability for a list of closed investments based on their transactions.
 */
fun calculateWeightedAnnualProfitability(
    filteredInvestments: List<InvestmentDB>,
    allTransactions: List<TransactionDB>
): Double {
    val totalInvested = filteredInvestments.sumOf { it.invested ?: 0.0 }
    val sumFinishedEarned = filteredInvestments.sumOf { it.earned ?: 0.0 }

    return if (totalInvested > 0.0 && sumFinishedEarned > 0.0) {
        val finishedIdInvests = filteredInvestments.mapNotNull { it.idInvest }.toSet()
        val catTransactions = allTransactions.filter { it.idInvest in finishedIdInvests }
        val investedTransactions = catTransactions.filter { it.category == "Investissement" }
        val earnedTransactions = catTransactions.filter { it.category == "Gain investissement" }

        val avgInvestDate = if (totalInvested > 0.0) {
            investedTransactions.sumOf { (it.date ?: 0.0) * (it.amount ?: 0.0) } / totalInvested
        } else 0.0

        val avgEarnDate = if (sumFinishedEarned > 0.0) {
            earnedTransactions.sumOf { (it.date ?: 0.0) * (it.amount ?: 0.0) } / sumFinishedEarned
        } else 0.0

        val weightedDays = (avgEarnDate - avgInvestDate).coerceAtLeast(1.0)
        
        ((sumFinishedEarned / totalInvested).pow(365.0 / weightedDays) - 1.0) * 100.0
    } else if (totalInvested > 0.0 && sumFinishedEarned <= 0.0) {
        -100.0
    } else {
        0.0
    }
}
