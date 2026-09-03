package ir.ghestyar.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * هشدار اعلان سطح وام (بند ۳۳-۳۴): برای هر وام حداکثر دو هشدار، متعلق به کل وام
 * (نه اقساط جداگانه). alertIndex فقط ۱ یا ۲ است.
 */
@Entity(
    tableName = "alerts",
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
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val alertIndex: Int,       // 1 یا 2
    val enabled: Boolean,
    val daysBefore: Int,       // 0, 1, 2, 3, 7
    val hour: Int,
    val minute: Int
) {
    companion object
}
