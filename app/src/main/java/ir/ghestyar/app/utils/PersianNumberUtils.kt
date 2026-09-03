package ir.ghestyar.app.utils

/**
 * ابزار تبدیل اعداد انگلیسی به فارسی و فرمت‌دهی مبالغ.
 * قانون پروژه: در دیتابیس همیشه عدد استاندارد (Long/Int) ذخیره می‌شود؛
 * تبدیل به فارسی فقط در لایه Presentation (نمایش) انجام می‌گیرد.
 */
object PersianNumberUtils {

    private val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** تبدیل رشته حاوی اعداد انگلیسی به معادل فارسی */
    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = enDigits.indexOf(ch)
            sb.append(if (idx >= 0) faDigits[idx] else ch)
        }
        return sb.toString()
    }

    /** تبدیل اعداد فارسی به انگلیسی (برای پردازش ورودی کاربر در فرم‌ها) */
    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = faDigits.indexOf(ch)
            sb.append(if (idx >= 0) enDigits[idx] else ch)
        }
        return sb.toString()
    }

    /** فرمت عدد صحیح با جداکننده هزارگان و اعداد فارسی. مثال: ۸,۵۰۰,۰۰۰ */
    fun formatAmount(amount: Long): String {
        val grouped = groupThousands(amount)
        return toPersianDigits(grouped)
    }

    /** مثل formatAmount اما با پسوند "تومان" */
    fun formatToman(amount: Long): String = "${formatAmount(amount)} تومان"

    /** فرمت یک عدد ساده (بدون جداکننده) به فارسی، مثلاً برای شماره قسط یا تعداد روز */
    fun formatNumber(number: Int): String = toPersianDigits(number.toString())

    private fun groupThousands(amount: Long): String {
        val negative = amount < 0
        val absStr = kotlin.math.abs(amount).toString()
        val sb = StringBuilder()
        var count = 0
        for (i in absStr.length - 1 downTo 0) {
            sb.append(absStr[i])
            count++
            if (count % 3 == 0 && i != 0) sb.append(',')
        }
        val result = sb.reverse().toString()
        return if (negative) "-$result" else result
    }

    /** پارس کردن ورودی کاربر (که ممکن است فارسی یا با جداکننده باشد) به Long خام */
    fun parseAmount(input: String): Long? {
        val cleaned = toEnglishDigits(input).replace(",", "").trim()
        return cleaned.toLongOrNull()
    }
}
