package ir.ghestyar.app.domain

import ir.ghestyar.app.domain.calculator.PersianDateConverter
import ir.ghestyar.app.domain.calculator.PersianDateConverter.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersianDateConverterTest {

    @Test
    fun `نوروزهای مرجع رسمی به‌درستی تبدیل می‌شوند`() {
        // مقادیر تأیید شده در ابتدای این گفتگو با منابع رسمی تقویم ایران
        assertEquals(LocalDate.of(2020, 3, 20), PersianDateConverter.toGregorian(JalaliDate(1399, 1, 1)))
        assertEquals(LocalDate.of(2021, 3, 21), PersianDateConverter.toGregorian(JalaliDate(1400, 1, 1)))
        assertEquals(LocalDate.of(2022, 3, 21), PersianDateConverter.toGregorian(JalaliDate(1401, 1, 1)))
        assertEquals(LocalDate.of(2023, 3, 21), PersianDateConverter.toGregorian(JalaliDate(1402, 1, 1)))
        assertEquals(LocalDate.of(2024, 3, 20), PersianDateConverter.toGregorian(JalaliDate(1403, 1, 1)))
        assertEquals(LocalDate.of(2025, 3, 21), PersianDateConverter.toGregorian(JalaliDate(1404, 1, 1)))
        assertEquals(LocalDate.of(2026, 3, 21), PersianDateConverter.toGregorian(JalaliDate(1405, 1, 1)))
    }

    @Test
    fun `تبدیل رفت و برگشت باید عکس هم باشند`() {
        val samples = listOf(
            JalaliDate(1399, 1, 1), JalaliDate(1403, 12, 30),
            JalaliDate(1404, 6, 31), JalaliDate(1405, 7, 30)
        )
        samples.forEach { jalali ->
            val gregorian = PersianDateConverter.toGregorian(jalali)
            val back = PersianDateConverter.toJalali(gregorian)
            assertEquals(jalali, back)
        }
    }

    @Test
    fun `سال ۱۴۰۳ کبیسه است و اسفند آن ۳۰ روز دارد`() {
        assertTrue(PersianDateConverter.isLeapJalaliYear(1403))
        assertEquals(30, PersianDateConverter.daysInMonth(1403, 12))
    }

    @Test
    fun `سال ۱۴۰۴ کبیسه نیست و اسفند آن ۲۹ روز دارد`() {
        assertFalse(PersianDateConverter.isLeapJalaliYear(1404))
        assertEquals(29, PersianDateConverter.daysInMonth(1404, 12))
    }

    @Test
    fun `سال ۱۴۰۸ کبیسه بعدی است`() {
        assertTrue(PersianDateConverter.isLeapJalaliYear(1408))
    }

    @Test
    fun `ماه‌های ۱ تا ۶ سی‌ویک روزه و ۷ تا ۱۱ سی‌روزه هستند`() {
        for (m in 1..6) assertEquals(31, PersianDateConverter.daysInMonth(1405, m))
        for (m in 7..11) assertEquals(30, PersianDateConverter.daysInMonth(1405, m))
    }

    @Test
    fun `قانون روز ۳۱ - شهریور سی‌ویک روزه به مهر سی‌روزه منتقل می‌شود`() {
        // بند ۲۱: ۱۴۰۵/۰۶/۳۱ -> افزودن یک دوره ماهانه -> ۱۴۰۵/۰۷/۳۰ (نه ۳۱ چون مهر ۳۰ روزه است)
        val base = JalaliDate(1405, 6, 31)
        val next = PersianDateConverter.addMonths(base, 1)
        assertEquals(JalaliDate(1405, 7, 30), next)
    }

    @Test
    fun `قانون روز ۳۱ - زنجیره کامل تا اسفند غیرکبیسه`() {
        // شهریور(31) -> مهر(30) -> آبان(30) -> آذر(30) -> دی(30) -> بهمن(30) -> اسفند(29 چون 1405 کبیسه نیست)
        val base = JalaliDate(1405, 6, 31)
        assertEquals(JalaliDate(1405, 7, 30), PersianDateConverter.addMonths(base, 1))
        assertEquals(JalaliDate(1405, 8, 30), PersianDateConverter.addMonths(base, 2))
        assertEquals(JalaliDate(1405, 9, 30), PersianDateConverter.addMonths(base, 3))
        assertEquals(JalaliDate(1405, 12, 29), PersianDateConverter.addMonths(base, 6))
    }

    @Test
    fun `روز غیر از آخر ماه حفظ می‌شود`() {
        // بند ۲۱: اگر روز سررسید ۱۵ باشد، روز ۱۵ حفظ شود
        val base = JalaliDate(1405, 6, 15)
        assertEquals(JalaliDate(1405, 7, 15), PersianDateConverter.addMonths(base, 1))
        assertEquals(JalaliDate(1406, 3, 15), PersianDateConverter.addMonths(base, 9))
    }

    @Test
    fun `دوره سه‌ماهه به‌درستی محاسبه می‌شود`() {
        // بند ۲۲: ۱۴۰۵/۰۶/۱۵ با دوره سه‌ماهه -> ۱۴۰۵/۰۹/۱۵ -> ۱۴۰۵/۱۲/۱۵ -> ۱۴۰۶/۰۳/۱۵
        val base = JalaliDate(1405, 6, 15)
        assertEquals(JalaliDate(1405, 9, 15), PersianDateConverter.addMonths(base, 3))
        assertEquals(JalaliDate(1405, 12, 15), PersianDateConverter.addMonths(base, 6))
        assertEquals(JalaliDate(1406, 3, 15), PersianDateConverter.addMonths(base, 9))
    }
}
