package ir.ghestyar.app.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.entity.AlertEntity
import ir.ghestyar.app.data.entity.InstallmentEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

const val EXTRA_INSTALLMENT_ID = "extra_installment_id"
const val EXTRA_ALERT_INDEX = "extra_alert_index"

/**
 * زمان‌بند اعلان‌های سررسید (بند ۳۳-۳۵).
 *
 * راهبرد: بعد از هر تغییر مؤثر (افزودن/ویرایش/حذف وام، ثبت/لغو پرداخت قسط، تغییر هشدار)،
 * تمام اعلان‌های آینده لغو و دوباره از روی وضعیت فعلی دیتابیس زمان‌بندی می‌شوند. این کار
 * ساده‌ترین راه برای همیشه‌درست‌بودن اعلان‌هاست (چون قسط ممکن است ویرایش/حذف/پرداخت شود).
 */
object NotificationScheduler {

    /** بازسازی کامل زمان‌بندی تمام اعلان‌ها بر اساس وضعیت فعلی دیتابیس */
    suspend fun rescheduleAll(context: Context, db: AppDatabase) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!canScheduleExact(alarmManager)) return

        val settings = db.settingsDao().get()
        if (settings?.notificationsEnabled == false) {
            cancelAll(context, db)
            return
        }

        val loans = db.loanDao().getAllOnce()
        val today = LocalDate.now()

        val installments = db.installmentDao().getAllOnce().filter { it.paidDate == null }

        for (loan in loans) {
            val loanAlerts = db.alertDao().getAllOnce().filter { it.loanId == loan.id && it.enabled }
            val loanInstallments = installments.filter { it.loanId == loan.id }
            for (installment in loanInstallments) {
                val dueDate = LocalDate.parse(installment.dueDate)
                for (alert in loanAlerts) {
                    scheduleOne(context, alarmManager, installment, alert, dueDate, today)
                }
            }
        }
    }

    private fun scheduleOne(
        context: Context,
        alarmManager: AlarmManager,
        installment: InstallmentEntity,
        alert: AlertEntity,
        dueDate: LocalDate,
        today: LocalDate
    ) {
        val triggerDate = dueDate.minusDays(alert.daysBefore.toLong())
        if (triggerDate.isBefore(today)) return // این هشدار برای این قسط دیگر معنا ندارد

        val triggerDateTime = LocalDateTime.of(triggerDate, java.time.LocalTime.of(alert.hour, alert.minute))
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(context, installment.id, alert.alertIndex)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (_: SecurityException) {
            // مجوز زمان‌بندی دقیق داده نشده؛ بی‌صدا رد می‌شود (کاربر باید از تنظیمات فعال کند)
        }
    }

    suspend fun cancelAll(context: Context, db: AppDatabase) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val installments = db.installmentDao().getAllOnce()
        for (installment in installments) {
            for (alertIndex in 1..2) {
                alarmManager.cancel(buildPendingIntent(context, installment.id, alertIndex))
            }
        }
    }

    private fun buildPendingIntent(context: Context, installmentId: Long, alertIndex: Int): PendingIntent {
        val intent = Intent(context, InstallmentAlarmReceiver::class.java).apply {
            putExtra(EXTRA_INSTALLMENT_ID, installmentId)
            putExtra(EXTRA_ALERT_INDEX, alertIndex)
        }
        val requestCode = (installmentId * 10 + alertIndex).toInt()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        alarmManager.canScheduleExactAlarms()
}
