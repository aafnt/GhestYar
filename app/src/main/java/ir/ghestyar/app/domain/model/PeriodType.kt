package ir.ghestyar.app.domain.model

/** دوره پرداخت اقساط. مقدار months تعداد ماه‌های شمسی بین هر دو سررسید متوالی است. */
enum class PeriodType(val months: Int, val displayName: String) {
    MONTHLY(1, "ماهانه"),
    BIMONTHLY(2, "دوماهه"),
    QUARTERLY(3, "سه‌ماهه"),
    SEMIANNUAL(6, "شش‌ماهه"),
    ANNUAL(12, "سالانه");

    companion object {
        fun fromName(name: String): PeriodType = entries.firstOrNull { it.name == name } ?: MONTHLY
    }
}
