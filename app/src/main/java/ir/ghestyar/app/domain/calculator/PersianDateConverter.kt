package ir.ghestyar.app.domain.calculator

import ir.ghestyar.app.utils.PersianNumberUtils
import java.time.LocalDate

/**
 * تبدیل دقیق تاریخ جلالی (شمسی) <-> میلادی.
 *
 * پیاده‌سازی بر پایه الگوریتم مرجع و شناخته‌شده تبدیل تقویم جلالی (مبتنی بر جدول دوره‌های
 * کبیسه‌گیری واقعی، نه تقریب ساده ۳۳ ساله)، که با ده‌ها تاریخ نوروز رسمی تأیید و تست شده است.
 *
 * تمام محاسبات داخلی برنامه (ذخیره‌سازی، مقایسه تاریخ‌ها) بر مبنای [LocalDate] میلادی انجام
 * می‌شود؛ این کلاس فقط برای تبدیل به/از نمایش شمسی به کاربر و همچنین محاسبه صحیح سررسید
 * اقساط (که باید بر اساس طول واقعی ماه‌های شمسی انجام شود) استفاده می‌شود.
 */
object PersianDateConverter {

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    private val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun monthName(month: Int): String = monthNames[month - 1]

    // جدول شکست‌های دوره کبیسه‌گیری (الگوریتم مرجع تقویم جلالی)
    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private fun idiv(a: Int, b: Int): Int {
        val q = kotlin.math.abs(a) / kotlin.math.abs(b)
        return if ((a < 0) != (b < 0)) -q else q
    }

    private fun imod(a: Int, b: Int): Int = a - idiv(a, b) * b

    private data class JalCalResult(val leap: Int, val gy: Int, val march: Int)

    /** leap == 0 یعنی سال شمسی jy کبیسه است (اسفند آن ۳۰ روز دارد) */
    private fun jalCal(jy: Int): JalCalResult {
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        if (jy < jp || jy >= breaks[bl - 1]) {
            throw IllegalArgumentException("سال شمسی نامعتبر: $jy")
        }
        var jump = 0
        var i = 1
        var jm = jp
        while (i < bl) {
            jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += idiv(jump, 33) * 8 + idiv(imod(jump, 33), 4)
            jp = jm
            i++
        }
        var n = jy - jp
        leapJ += idiv(n, 33) * 8 + idiv(imod(n, 33) + 3, 4)
        if (imod(jump, 33) == 4 && (jump - n) == 4) leapJ += 1

        val leapG = idiv(gy, 4) - idiv((idiv(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG

        if ((jump - n) < 6) n = n - jump + idiv(jump, 33) * 33
        var leap = imod(imod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4

        return JalCalResult(leap, gy, march)
    }

    /** آیا سال شمسی jy کبیسه است (اسفند ۳۰ روزه) */
    fun isLeapJalaliYear(jy: Int): Boolean = jalCal(jy).leap == 0

    /** تعداد روزهای ماه jm از سال شمسی jy */
    fun daysInMonth(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeapJalaliYear(jy)) 30 else 29
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = idiv((gy + idiv(gm - 8, 6) + 100100) * 1461, 4) +
                idiv(153 * imod(gm + 9, 12) + 2, 5) + gd - 34840408
        d -= idiv(idiv(gy + 100100 + idiv(gm - 8, 6), 100) * 3, 4) - 752
        return d
    }

    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += idiv(idiv(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = idiv(imod(j, 1461), 4) * 5 + 308
        val gd = idiv(imod(i, 153), 5) + 1
        val gm = imod(idiv(i, 153), 12) + 1
        val gy = idiv(j, 1461) - 100100 + idiv(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - idiv(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): JalaliDate {
        val gy = d2g(jdn).first
        var jy = gy - 621
        var r = jalCal(jy)
        val jdn1f = g2d(r.gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                val jm = 1 + idiv(k, 31)
                val jd = imod(k, 31) + 1
                return JalaliDate(jy, jm, jd)
            } else {
                k -= 186
            }
        } else {
            jy -= 1
            k += 179
            r = jalCal(jy)
            if (r.leap == 0) k += 1
        }
        val jm = 7 + idiv(k, 30)
        val jd = imod(k, 30) + 1
        return JalaliDate(jy, jm, jd)
    }

    /** میلادی -> شمسی */
    fun toJalali(date: LocalDate): JalaliDate =
        d2j(g2d(date.year, date.monthValue, date.dayOfMonth))

    /**
     * شمسی -> میلادی.
     * قانون روز ۳۱ (بند ۲۱ سند طراحی): اگر day > تعداد روزهای واقعی ماه، به آخرین روز همان ماه
     * محدود می‌شود (مثلاً ۳۱ شهریور -> ۳۰ مهر -> ۳۰ آبان و ...).
     */
    fun toGregorian(jalali: JalaliDate): LocalDate {
        val maxDay = daysInMonth(jalali.year, jalali.month)
        val clampedDay = jalali.day.coerceAtMost(maxDay)
        val jdn = j2d(jalali.year, jalali.month, clampedDay)
        val (gy, gm, gd) = d2g(jdn)
        return LocalDate.of(gy, gm, gd)
    }

    /**
     * افزودن n دوره (به ماه) به یک تاریخ شمسی، با رعایت قانون روز ۳۱:
     * - اگر روز مبدأ، آخرین روز ماه مبدأ باشد (مثلاً ۳۱ در ماه ۳۱ روزه)، در نتیجه هم
     *   همیشه «آخرین روز همان ماه مقصد» قرار می‌گیرد.
     * - در غیر این صورت همان روز حفظ می‌شود (و فقط در صورت نیاز به آخرین روز ماه محدود می‌شود).
     */
    fun addMonths(base: JalaliDate, months: Int): JalaliDate {
        val wasLastDayOfMonth = base.day == daysInMonth(base.year, base.month)
        val totalMonths = (base.year * 12 + (base.month - 1)) + months
        val newYear = totalMonths / 12
        val newMonth = totalMonths % 12 + 1
        val newDay = if (wasLastDayOfMonth) {
            daysInMonth(newYear, newMonth)
        } else {
            base.day.coerceAtMost(daysInMonth(newYear, newMonth))
        }
        return JalaliDate(newYear, newMonth, newDay)
    }

    /** فرمت نمایشی کامل با اعداد فارسی، مثل ۱۴۰۵/۰۶/۱۵ */
    fun formatFull(date: LocalDate): String {
        val j = toJalali(date)
        val raw = "%04d/%02d/%02d".format(j.year, j.month, j.day)
        return PersianNumberUtils.toPersianDigits(raw)
    }

    /** فرمت نمایشی کوتاه، مثل «۱۵ شهریور ۱۴۰۵» */
    fun formatLong(date: LocalDate): String {
        val j = toJalali(date)
        return "${PersianNumberUtils.formatNumber(j.day)} ${monthName(j.month)} ${PersianNumberUtils.toPersianDigits(j.year.toString())}"
    }

    /** پارس رشته ورودی کاربر به شکل ۱۴۰۵/۰۶/۱۵ یا 1405-06-15 (فارسی یا انگلیسی) */
    fun parse(input: String): LocalDate? {
        val cleaned = PersianNumberUtils.toEnglishDigits(input).trim().replace("-", "/")
        val parts = cleaned.split("/")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m !in 1..12) return null
        return try {
            toGregorian(JalaliDate(y, m, d))
        } catch (e: Exception) {
            null
        }
    }
}
