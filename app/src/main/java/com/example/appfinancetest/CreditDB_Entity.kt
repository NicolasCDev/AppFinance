package com.example.appfinancetest

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity
data class CreditDB (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "dateBegin") val dateBegin: Double?,
    @ColumnInfo(name = "totalAmount") val totalAmount: Double?,
    @ColumnInfo(name = "monthlyPayment") val monthlyPayment: Double?,
    @ColumnInfo(name = "interestRate") val interestRate: Double?,
    @ColumnInfo(name = "idInvest") val idInvest: String?
)

@Dao
interface CreditDao {
    @Query("SELECT * FROM CreditDB")
    suspend fun getAll(): List<CreditDB>

    @Insert
    suspend fun insertAll(vararg credits: CreditDB)

    @Query("DELETE FROM CreditDB")
    suspend fun deleteAll()

    @Query("SELECT * FROM CreditDB WHERE id = :id LIMIT 1")
    suspend fun getCreditById(id: Int): CreditDB?

    @Update
    suspend fun updateCredit(credit: CreditDB)

    @Query("DELETE FROM CreditDB WHERE id = :id")
    suspend fun deleteById(id: Int)
}
