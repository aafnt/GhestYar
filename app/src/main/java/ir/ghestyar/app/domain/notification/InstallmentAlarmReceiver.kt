package ir.ghestyar.app.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.utils.PersianNumberUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * وقتی زمان یک هشدار فرا می‌رسد، این Receiver اجرا می‌شود.
 * قبل از نمایش اعلان، دوباره از دیتابیس چک می‌کند که آیا قسط قبلاً پرداخت شده یا حذف شده
 * است (بند ۳۵: «اگر قسط قبل از زمان اعلان پرداخت شده باشد، اعلان ارسال نشود»).
 */
class InstallmentAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val installmentId = intent.getLongExtra(EXTRA_INSTALLMENT_ID, -1L)
        if (installmentId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context, installmentId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, installmentId: Long) {
        val db = AppDatabase.getInstance(context)
        val installment = db.installmentDao().getById(installmentId) ?: return
        if (installment.paidDate != null) return // قبلاً پرداخت شده، اعلان لازم نیست

        val loan = db.loanDao().getById(installment.loanId) ?: return
        val today = LocalDate.now()
        val dueDate = LocalDate.parse(installment.dueDate)

        val allLoanInstallments = db.installmentDao().getAllOnce().filter { it.loanId == loan.id }
        val unpaidOverdueCount = allLoanInstallments.count {
            it.paidDate == null && !LocalDate.parse(it.dueDate).isAfter(today)
        }

        val amountText = PersianNumberUtils.formatToman(installment.amount)
        val numberText = PersianNumberUtils.formatNumber(installment.installmentNumber)

        val isOverdueNow = !dueDate.isAfter(today)
        val title = if (isOverdueNow) "🔴 سررسید قسط ${loan.name}" else "🔔 سررسید قسط ${loan.name}"

        val whenText = when {
            dueDate.isEqual(today) -> "امروز سررسید می‌شود"
            dueDate.isEqual(today.plusDays(1)) -> "فردا سررسید می‌شود"
            isOverdueNow -> "سررسید آن گذشته است"
            else -> "به‌زودی سررسید می‌شود"
        }

        val bodyBuilder = StringBuilder("قسط شماره $numberText به مبلغ $amountText، $whenText.")
        if (unpaidOverdueCount >= 2) {
            bodyBuilder.append("\n⚠️ ${PersianNumberUtils.formatNumber(unpaidOverdueCount)} قسط پرداخت‌نشده دارید.")
        }

        NotificationHelper.show(
            context = context,
            notificationId = installment.id.toInt(),
            title = title,
            body = bodyBuilder.toString()
        )

        // هشدار ویژه سه قسط معوق یا بیشتر (بند ۳۶)
        if (unpaidOverdueCount >= 3) {
            NotificationHelper.show(
                context = context,
                notificationId = (loan.id * -1).toInt(),
                title = "🚨 هشدار مهم",
                body = "وام ${loan.name} دارای ${PersianNumberUtils.formatNumber(unpaidOverdueCount)} قسط پرداخت‌نشده است."
            )
        }
    }
}
