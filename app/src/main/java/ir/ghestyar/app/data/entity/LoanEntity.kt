package ir.ghestyar.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول وام‌ها.
 * تمام تاریخ‌ها به‌صورت ISO-8601 میلادی (yyyy-MM-dd) ذخیره می‌شوند (بند ۷ و ۴۴ سند طراحی)
 * و مبالغ به‌صورت Long (تومان، بدون اعشار) - هرگز Float/Double.
 */
@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imagePath: String?,
    val totalAmount: Long,
    val receivedDate: String,
    val installmentCount: Int,
    val periodType: String,
    val firstDueDate: String,
    val firstInstallmentAmount: Long,
    val otherInstallmentAmount: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object
}
