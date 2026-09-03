package ir.ghestyar.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ir.ghestyar.app.data.dao.AlertDao
import ir.ghestyar.app.data.dao.InstallmentDao
import ir.ghestyar.app.data.dao.LoanDao
import ir.ghestyar.app.data.dao.SettingsDao
import ir.ghestyar.app.data.entity.AlertEntity
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.data.entity.LoanEntity
import ir.ghestyar.app.data.entity.SettingsEntity

@Database(
    entities = [LoanEntity::class, InstallmentEntity::class, AlertEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loanDao(): LoanDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun alertDao(): AlertDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DB_NAME = "ghestyar.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
