package ir.ghestyar.app.data.repository

import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.entity.AlertEntity
import ir.ghestyar.app.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val db: AppDatabase) {

    fun observe(): Flow<SettingsEntity> = db.settingsDao().observe().map { it ?: SettingsEntity() }

    suspend fun get(): SettingsEntity = db.settingsDao().get() ?: SettingsEntity()

    suspend fun update(settings: SettingsEntity) = db.settingsDao().upsert(settings)

    fun observeAlerts(loanId: Long): Flow<List<AlertEntity>> = db.alertDao().observeByLoan(loanId)

    /** ذخیره حداکثر دو هشدار برای یک وام (بند ۳۳) */
    suspend fun saveAlerts(loanId: Long, alerts: List<AlertEntity>) {
        require(alerts.size <= 2) { "حداکثر دو هشدار برای هر وام مجاز است" }
        db.alertDao().deleteByLoan(loanId)
        if (alerts.isNotEmpty()) {
            db.alertDao().upsertAll(alerts.map { it.copy(loanId = loanId) })
        }
    }
}
