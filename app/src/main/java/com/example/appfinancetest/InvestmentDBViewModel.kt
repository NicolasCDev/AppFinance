package com.example.appfinancetest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvestmentDBViewModel (application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "FinanceDB"
    )
        .fallbackToDestructiveMigration(false)
        .build()

    private val dbDAO = database.investmentDao()

    suspend fun getInvestment(): List<InvestmentDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getAll()
        }
    }
    suspend fun insertInvestment(investmentDB: InvestmentDB) {
        withContext(Dispatchers.IO) {
            dbDAO.insertAll(investmentDB)
        }
    }
    suspend fun deleteAllInvestments() {
        withContext(Dispatchers.IO) {
            dbDAO.deleteAll()
        }
    }
    suspend fun getCurrentInvestments(): List<InvestmentDB>? {
        return withContext(Dispatchers.IO) {
            dbDAO.getCurrentInvestments()
        }
    }
    suspend fun getEndedInvestments(): List<InvestmentDB>? {
        return withContext(Dispatchers.IO) {
            dbDAO.getEndedInvestments()
        }
    }
    suspend fun getInvestmentById(idInvest: String): InvestmentDB? {
        return withContext(Dispatchers.IO) {
            dbDAO.getInvestmentById(idInvest)
        }
    }
    suspend fun updateInvestment(investmentDB: InvestmentDB) {
        withContext(Dispatchers.IO) {
            dbDAO.updateInvestment(investmentDB)
        }
    }
    suspend fun getPagedInvestments(limit: Int, offset: Int): List<InvestmentDB> {
        return dbDAO.getInvestmentsPaged(limit, offset)
    }

    suspend fun updateInvestmentEndDate(idInvest: String, dateEnd: Double) {
        withContext(Dispatchers.IO) {
            val investment = dbDAO.getInvestmentById(idInvest)
            if (investment != null) {
                dbDAO.updateInvestment(investment.copy(dateEnd = dateEnd))
            }
        }
    }
}