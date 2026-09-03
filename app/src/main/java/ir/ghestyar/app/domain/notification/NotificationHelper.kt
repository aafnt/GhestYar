package ir.ghestyar.app.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.ghestyar.app.R
import ir.ghestyar.app.presentation.MainActivity

/** ساخت کانال اعلان و نمایش اعلان‌های سررسید قسط (بند ۳۵-۳۶) */
object NotificationHelper {

    const val CHANNEL_ID = "installment_due_channel"
    private const val CHANNEL_NAME = "یادآوری اقساط"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان‌های مربوط به سررسید اقساط وام‌ها"
        }
        manager?.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // کاربر مجوز POST_NOTIFICATIONS را نداده؛ بی‌صدا نادیده گرفته می‌شود
        }
    }
}
