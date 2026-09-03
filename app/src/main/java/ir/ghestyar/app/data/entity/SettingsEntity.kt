package ir.ghestyar.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** جدول تنظیمات - همیشه فقط یک ردیف با id=1 دارد. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "SYSTEM",       // LIGHT, DARK, SYSTEM
    val notificationsEnabled: Boolean = true
) {
    companion object
}
