package ir.ghestyar.app.data.dao

import androidx.room.*
import ir.ghestyar.app.data.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts WHERE loanId = :loanId ORDER BY alertIndex ASC")
    fun observeByLoan(loanId: Long): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts")
    suspend fun getAllOnce(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: AlertEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(alerts: List<AlertEntity>)

    @Delete
    suspend fun delete(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE loanId = :loanId")
    suspend fun deleteByLoan(loanId: Long)

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()
}
