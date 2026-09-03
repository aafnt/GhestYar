package ir.ghestyar.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول اقساط. هر قسط کاملاً مستقل است (بند ۳۰): ویرایش یک قسط تأثیری روی بقیه ندارد.
 * وضعیت (پرداخت‌شده/معوق/آینده) هرگز اینجا ذخیره نمی‌شود؛ همیشه محاسبه‌شده است (بند ۲۴).
 */
@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("loanId")]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val installmentNumber: Int,
    val amount: Long,
    val dueDate: String,      // ISO-8601 میلادی
    val paidDate: String?,    // ISO-8601 میلادی، یا null اگر پرداخت نشده
    val note: String? = null
) {
    companion object
}
