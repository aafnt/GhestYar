package ir.ghestyar.app.data.dao

import androidx.room.*
import ir.ghestyar.app.data.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    fun observeById(loanId: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getById(loanId: Long): LoanEntity?

    @Query("SELECT * FROM loans ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<LoanEntity>

    @Insert
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Delete
    suspend fun delete(loan: LoanEntity)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()
}
