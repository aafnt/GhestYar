package ir.ghestyar.app.data.dao

import androidx.room.*
import ir.ghestyar.app.data.entity.InstallmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {

    @Query("SELECT * FROM installments ORDER BY dueDate ASC")
    fun observeAll(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun observeByLoan(loanId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments ORDER BY installmentNumber ASC")
    suspend fun getAllOnce(): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getById(id: Long): InstallmentEntity?

    @Insert
    suspend fun insertAll(installments: List<InstallmentEntity>)

    @Insert
    suspend fun insert(installment: InstallmentEntity): Long

    @Update
    suspend fun update(installment: InstallmentEntity)

    @Query("UPDATE installments SET paidDate = :paidDate WHERE id = :id")
    suspend fun setPaidDate(id: Long, paidDate: String?)

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteByLoan(loanId: Long)

    @Query("DELETE FROM installments")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM installments WHERE loanId = :loanId AND paidDate IS NULL AND dueDate <= :today")
    suspend fun countOverdue(loanId: Long, today: String): Int
}
