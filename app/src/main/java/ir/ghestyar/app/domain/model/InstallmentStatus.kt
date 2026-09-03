package ir.ghestyar.app.domain.model

import java.time.LocalDate

/**
 * وضعیت قسط. طبق بند ۲۴ سند طراحی، این وضعیت هرگز در دیتابیس ذخیره نمی‌شود؛
 * همیشه به‌صورت پویا از روی dueDate و paidDate محاسبه می‌شود تا هیچ‌وقت با واقعیت
 * ناهماهنگ نشود (مثلاً وقتی کاربر فقط تاریخ سیستم را عوض می‌کند).
 */
enum class InstallmentStatus {
    PAID,       // پرداخت‌شده (paidDate پر است)
    OVERDUE,    // معوق (paidDate خالی و dueDate <= امروز)
    UPCOMING;   // آینده (paidDate خالی و dueDate > امروز)

    companion object {
        fun of(dueDate: LocalDate, paidDate: LocalDate?, today: LocalDate = LocalDate.now()): InstallmentStatus {
            if (paidDate != null) return PAID
            return if (!dueDate.isAfter(today)) OVERDUE else UPCOMING
        }
    }
}

/** نوع گزارش قابل انتخاب در صفحه اصلی (بند ۱۳ سند طراحی) */
enum class ReportType(val displayName: String) {
    OVERDUE("سررسید شده و پرداخت نشده"),
    PAID("پرداخت شده"),
    UPCOMING("سررسید نشده"),
    ALL("همه");
}
