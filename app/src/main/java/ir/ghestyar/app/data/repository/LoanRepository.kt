package ir.ghestyar.app.data.repository

import androidx.room.withTransaction
import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.entity.AlertEntity
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.data.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

class LoanRepository(private val db: AppDatabase) {

    fun observeLoans(): Flow<List<LoanEntity>> = db.loanDao().observeAll()

    fun observeLoan(loanId: Long): Flow<LoanEntity?> = db.loanDao().observeById(loanId)

    suspend fun getLoan(loanId: Long): LoanEntity? = db.loanDao().getById(loanId)

    /**
     * ایجاد وام جدید به‌همراه اقساط تولیدشده و هشدارها، به‌صورت یک تراکنش واحد
     * (یا همه ذخیره می‌شوند یا هیچ‌کدام - جلوگیری از داده ناقص).
     */
    suspend fun createLoan(
        loan: LoanEntity,
        installments: List<InstallmentEntity>,
        alerts: List<AlertEntity>
    ): Long = db.withTransaction {
        val loanId = db.loanDao().insert(loan)
        db.installmentDao().insertAll(installments.map { it.copy(loanId = loanId) })
        if (alerts.isNotEmpty()) {
            db.alertDao().upsertAll(alerts.map { it.copy(loanId = loanId) })
        }
        loanId
    }

    suspend fun updateLoan(loan: LoanEntity) = db.loanDao().update(loan)

    suspend fun deleteLoan(loan: LoanEntity) = db.withTransaction {
        // ForeignKey CASCADE هم اقساط و هم هشدارها را حذف می‌کند، اما صریح هم پاک می‌کنیم
        db.installmentDao().deleteByLoan(loan.id)
        db.alertDao().deleteByLoan(loan.id)
        db.loanDao().delete(loan)
    }
}
