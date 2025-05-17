package com.example.appfinancetest

import androidx.lifecycle.LiveData
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
class InvestmentDB (
    @PrimaryKey(autoGenerate = false)
    val idInvest: String,
    @ColumnInfo(name = "dateEnd") val dateEnd: Double?,
    @ColumnInfo(name = "invested") val invested: Double?,
    @ColumnInfo(name = "earned") val earned: Double?,
    @ColumnInfo(name = "profitability") val profitability: Double?,
    @ColumnInfo(name = "annualProfitability") val annualProfitability: Double?,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "item") val item: String?,
    @ColumnInfo(name = "label") val description: String?
)

@Dao
interface InvestmentDao{
    @Query("SELECT * FROM InvestmentDB")
    fun getAll(): LiveData<List<InvestmentDB>>

    @Insert
    suspend fun insertAll(vararg investments: InvestmentDB)

    @Query("DELETE FROM InvestmentDB")
    fun deleteAll()

    @Query("SELECT * FROM InvestmentDB WHERE dateEnd IS NULL OR dateEnd = ''")
    suspend fun getCurrentInvestments(): List<InvestmentDB>?

    @Query("SELECT * FROM InvestmentDB WHERE dateEnd IS NOT NULL OR dateEnd != ''")
    suspend fun getEndedInvestments(): List<InvestmentDB>?

    @Query("SELECT * FROM InvestmentDB WHERE idInvest = :idInvest LIMIT 1")
    suspend fun getInvestmentById(idInvest: String): InvestmentDB?

    @Update
    suspend fun updateInvestment(investment: InvestmentDB)

}

@Database(entities = [InvestmentDB::class], version = 1, exportSchema = false)
abstract class InvestmentDatabase : RoomDatabase() {
    abstract fun investmentDao(): InvestmentDao
}