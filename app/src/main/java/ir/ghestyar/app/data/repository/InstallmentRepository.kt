package ir.ghestyar.app.data.repository

import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.entity.InstallmentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class InstallmentRepository(private val db: AppDatabase) {

    fun observeAll(): Flow<List<InstallmentEntity>> = db.installmentDao().observeAll()

    fun observeByLoan(loanId: Long): Flow<List<InstallmentEntity>> = db.installmentDao().observeByLoan(loanId)

    suspend fun getById(id: Long): InstallmentEntity? = db.installmentDao().getById(id)

    /** ویرایش مستقل قسط: مبلغ، تاریخ سررسید، توضیحات (بند ۳۰) - بدون اثر روی سایر اقساط */
    suspend fun update(installment: InstallmentEntity) = db.installmentDao().update(installment)

    /** ثبت پرداخت قسط با تاریخ پرداخت مشخص (بند ۳۱) */
    suspend fun markAsPaid(installmentId: Long, paidDate: LocalDate) =
        db.installmentDao().setPaidDate(installmentId, paidDate.toString())

    /** لغو پرداخت (برگرداندن قسط به حالت پرداخت‌نشده) - بند ۳۲ */
    suspend fun unmarkPaid(installmentId: Long) =
        db.installmentDao().setPaidDate(installmentId, null)
}
