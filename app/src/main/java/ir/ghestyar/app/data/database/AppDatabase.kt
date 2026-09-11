package ir.ghestyar.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loanDao(): LoanDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun alertDao(): AlertDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DB_NAME = "ghestyar.db"

        /** افزودن ستون توضیحات به وام - داده‌های قبلی کاملاً حفظ می‌شوند */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN description TEXT")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
