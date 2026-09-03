package ir.ghestyar.app

import android.app.Application
import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.repository.InstallmentRepository
import ir.ghestyar.app.data.repository.LoanRepository
import ir.ghestyar.app.data.repository.SettingsRepository
import ir.ghestyar.app.domain.notification.NotificationHelper

/**
 * به‌جای Hilt/Koin (که وابستگی اضافه‌ای هستند و برای این پروژه شخصی و کوچک ضروری نیستند -
 * بند ۴۸: از کتابخانه‌های غیرضروری استفاده نشود)، یک Container دستی و ساده استفاده می‌شود.
 */
class GhestYarApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var loanRepository: LoanRepository
        private set
    lateinit var installmentRepository: InstallmentRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        loanRepository = LoanRepository(database)
        installmentRepository = InstallmentRepository(database)
        settingsRepository = SettingsRepository(database)
        NotificationHelper.ensureChannel(this)
    }
}
