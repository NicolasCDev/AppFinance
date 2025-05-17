package com.example.appfinancetest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    fun getInvestment() {
        viewModelScope.launch(Dispatchers.IO) {
            dbDAO.getAll()
        }
    }
    suspend fun insertInvestment(investmentDB: InvestmentDB) {
        viewModelScope.launch(Dispatchers.IO) {
            dbDAO.insertAll(investmentDB)
        }
    }
    fun deleteAllInvestments() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("InvestmentDB_ViewModel", "Deleted every investment")
            dbDAO.deleteAll()
        }
    }
    suspend fun getCurrentInvestments() {
        viewModelScope.launch(Dispatchers.IO) {
            dbDAO.getCurrentInvestments()
        }
    }
    suspend fun getEndedInvestments() {
        viewModelScope.launch(Dispatchers.IO) {
            dbDAO.getEndedInvestments()
        }
    }
    suspend fun getInvestmentById(idInvest: String): InvestmentDB? {
        return withContext(Dispatchers.IO) {
            dbDAO.getInvestmentById(idInvest)
        }
    }
    suspend fun updateInvestment(investmentDB: InvestmentDB) {
        viewModelScope.launch(Dispatchers.IO) {
            dbDAO.updateInvestment(investmentDB)
        }
    }
}