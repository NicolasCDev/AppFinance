package com.example.appfinancetest

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity
data class InvestmentDB (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "idInvest") val idInvest: String?,
    @ColumnInfo(name = "dateBegin") val dateBegin: Double?,
    @ColumnInfo(name = "dateEnd") val dateEnd: Double?,
    @ColumnInfo(name = "transactionList") val transactionList: List<Int>,
    @ColumnInfo(name = "invested") val invested: Double?,
    @ColumnInfo(name = "earned") val earned: Double?,
    @ColumnInfo(name = "profitability") val profitability: Double?,
    @ColumnInfo(name = "annualProfitability") val annualProfitability: Double?,
    @ColumnInfo(name = "item") val item: String?,
    @ColumnInfo(name = "label") val label: String?
)

@Dao
interface InvestmentDao{
    @Query("SELECT * FROM InvestmentDB")
    fun getAll(): List<InvestmentDB>

    @Insert
    suspend fun insertAll(vararg investments: InvestmentDB)

    @Query("DELETE FROM InvestmentDB")
    fun deleteAll()

    @Query("SELECT * FROM InvestmentDB WHERE dateEnd IS NULL")
    suspend fun getCurrentInvestments(): List<InvestmentDB>?

    @Query("SELECT * FROM InvestmentDB WHERE dateEnd IS NOT NULL")
    suspend fun getEndedInvestments(): List<InvestmentDB>?

    @Query("SELECT * FROM InvestmentDB WHERE idInvest = :idInvest LIMIT 1")
    suspend fun getInvestmentById(idInvest: String): InvestmentDB?

    @Query("SELECT * FROM InvestmentDB ORDER BY idInvest DESC LIMIT :limit OFFSET :offset")
    suspend fun getInvestmentsPaged(limit: Int, offset: Int): List<InvestmentDB>

    @Update
    suspend fun updateInvestment(investment: InvestmentDB)

}

@Database(entities = [InvestmentDB::class], version = 1, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class InvestmentDatabase : RoomDatabase() {
    abstract fun investmentDao(): InvestmentDao
}
