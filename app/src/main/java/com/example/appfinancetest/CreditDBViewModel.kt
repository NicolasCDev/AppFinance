package com.example.appfinancetest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreditDBViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "FinanceDB"
    )
        .fallbackToDestructiveMigration(false)
        .build()

    private val creditDao = database.creditDao()

    suspend fun getAllCredits(): List<CreditDB> {
        return withContext(Dispatchers.IO) {
            creditDao.getAll()
        }
    }

    suspend fun insertCredit(credit: CreditDB) {
        withContext(Dispatchers.IO) {
            creditDao.insertAll(credit)
        }
    }

    suspend fun updateCredit(credit: CreditDB) {
        withContext(Dispatchers.IO) {
            creditDao.updateCredit(credit)
        }
    }

    suspend fun deleteCreditById(id: Int) {
        withContext(Dispatchers.IO) {
            creditDao.deleteById(id)
        }
    }

    suspend fun deleteAllCredits() {
        withContext(Dispatchers.IO) {
            creditDao.deleteAll()
        }
    }

    suspend fun getCreditById(id: Int): CreditDB? {
        return withContext(Dispatchers.IO) {
            creditDao.getCreditById(id)
        }
    }
}
