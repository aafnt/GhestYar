package ir.ghestyar.app.domain.calculator

import ir.ghestyar.app.domain.model.PeriodType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** یک قسط تولیدشده، قبل از ذخیره در دیتابیس (برای Preview و برای ساخت واقعی) */
data class GeneratedInstallment(
    val number: Int,
    val dueDate: LocalDate,
    val amount: Long
)

/**
 * منطق اصلی تولید اقساط یک وام (بند ۲۰ تا ۲۲ سند طراحی).
 *
 * نکات کلیدی:
 * - قسط اول از مبلغ قسط اول، بقیه از مبلغ سایر اقساط استفاده می‌کنند.
 * - هر قسط مستقل از دیگری محاسبه می‌شود (نه با جمع کردن پی‌درپی، بلکه با آفست ثابت از
 *   تاریخ سررسید اول)، تا قانون روز ۳۱ به‌درستی و بدون انباشت خطا رعایت شود.
 * - محاسبه در فضای تقویم شمسی انجام می‌شود چون طول ماه‌های شمسی (۳۱/۳۰/۲۹) ملاک قانون
 *   روز ۳۱ است، سپس نتیجه به میلادی (برای ذخیره در دیتابیس) تبدیل می‌شود.
 */
object InstallmentCalculator {

    fun generate(
        firstDueDate: LocalDate,
        periodType: PeriodType,
        installmentCount: Int,
        firstInstallmentAmount: Long,
        otherInstallmentAmount: Long
    ): List<GeneratedInstallment> {
        require(installmentCount > 0) { "تعداد اقساط باید بزرگ‌تر از صفر باشد" }

        val anchor = PersianDateConverter.toJalali(firstDueDate)
        return (0 until installmentCount).map { index ->
            val jalaliDue = if (index == 0) anchor else PersianDateConverter.addMonths(anchor, index * periodType.months)
            val dueDate = if (index == 0) firstDueDate else PersianDateConverter.toGregorian(jalaliDue)
            GeneratedInstallment(
                number = index + 1,
                dueDate = dueDate,
                amount = if (index == 0) firstInstallmentAmount else otherInstallmentAmount
            )
        }
    }

    /** تعداد روزهای تأخیر بین سررسید و تاریخ پرداخت واقعی (بند ۲۳) */
    fun delayDays(dueDate: LocalDate, paidDate: LocalDate): Long =
        ChronoUnit.DAYS.between(dueDate, paidDate).coerceAtLeast(0)

    /** تعداد روزهای گذشته از سررسید تا امروز، برای قسط معوق */
    fun overdueDays(dueDate: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(dueDate, today).coerceAtLeast(0)

    /** تعداد روزهای باقی‌مانده تا سررسید، برای قسط آینده */
    fun remainingDays(dueDate: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, dueDate).coerceAtLeast(0)
}
