package com.example.appfinancetest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataBase_ViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "FinanceDB"
    ).fallbackToDestructiveMigration(false).build()

    private val dbDAO = database.transactionDao()

    private val _netWorthDate = MutableLiveData<Double>(Double.MAX_VALUE)
    val netWorth: LiveData<Double?> = _netWorthDate.switchMap { date ->
        dbDAO.getNetWorthAtDate(date)
    }

    fun setNetWorthDate(date: Double) {
        _netWorthDate.value = date
    }

    fun refreshNetWorth() {
        _netWorthDate.value = _netWorthDate.value
    }

    suspend fun getNetWorthAtDateStatic(date: Double): Double {
        return withContext(Dispatchers.IO) {
            dbDAO.getNetWorthAtDateStatic(date)
        }
    }

    suspend fun getAllInvestments(): List<InvestmentDB> {
        return withContext(Dispatchers.IO) {
            database.investmentDao().getAll()
        }
    }

    suspend fun getAllCategories(): List<String> {
        return withContext(Dispatchers.IO) {
            dbDAO.getAllCategories()
        }
    }

    suspend fun getAllItems(): List<String> {
        return withContext(Dispatchers.IO) {
            dbDAO.getAllItems()
        }
    }

    suspend fun getAllLabels(): List<String> {
        return withContext(Dispatchers.IO) {
            dbDAO.getAllLabels()
        }
    }

    suspend fun insertTransaction(transaction: TransactionDB) {
        withContext(Dispatchers.IO) {
            dbDAO.insertAll(transaction)
        }
    }
    suspend fun deleteAllTransactions() {
        withContext(Dispatchers.IO) {
            Log.d("DataBase_ViewModel", "Deleting every transactions")
            dbDAO.deleteAll()
        }
    }
    suspend fun updateBalance(transactionId: Int, newBalance: Double) {
        withContext(Dispatchers.IO) {
            dbDAO.updateBalance(transactionId, newBalance)
        }
    }
    suspend fun getPagedTransactions(limit: Int, offset: Int): List<TransactionDB> {
        return dbDAO.getTransactionsPaged(limit, offset)
    }
    // Suspend call for gathering every transaction
    suspend fun getTransactionsSortedByDateASC(): List<TransactionDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getTransactionsSortedByDateASC()
        }
    }
    suspend fun getTransactionsSortedByDateDESC(): List<TransactionDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getTransactionsSortedByDate()
        }
    }
    suspend fun getInvestmentTransactions(): List<TransactionDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getInvestmentTransactions()
        }
    }
    suspend fun getInvestmentTransactionsByID(idInvest: String): List<TransactionDB> {
        return withContext(Dispatchers.IO) {
            dbDAO.getInvestmentTransactionsByID(idInvest)
        }
    }
}
