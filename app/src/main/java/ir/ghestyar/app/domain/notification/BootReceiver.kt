package ir.ghestyar.app.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.ghestyar.app.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager با RTC_WAKEUP بعد از ری‌استارت گوشی پاک می‌شود، پس باید همه اعلان‌ها را
 * دوباره زمان‌بندی کنیم (بند ۳۳: قابلیت اطمینان اعلان‌ها).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                NotificationScheduler.rescheduleAll(context, db)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
