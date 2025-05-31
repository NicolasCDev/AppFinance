package com.example.appfinancetest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvestmentDB_ViewModel (application: Application) : AndroidViewModel(application) {
    private val investmentDB = Room.databaseBuilder(
        application.applicationContext,
        InvestmentDatabase::class.java,
        "InvestmentDB"
    )
        .fallbackToDestructiveMigration(false)
        .build()

    private val dbDAO = investmentDB.investmentDao()
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
            Log.d("InvestmentDB_ViewModel", "Deleted every investment")
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
}