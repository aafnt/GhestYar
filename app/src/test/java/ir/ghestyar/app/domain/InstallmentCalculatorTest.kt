package ir.ghestyar.app.domain

import ir.ghestyar.app.domain.calculator.InstallmentCalculator
import ir.ghestyar.app.domain.calculator.PersianDateConverter
import ir.ghestyar.app.domain.model.PeriodType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InstallmentCalculatorTest {

    @Test
    fun `تعداد اقساط تولیدشده دقیقا برابر مقدار ورودی است`() {
        val firstDue = PersianDateConverter.toGregorian(PersianDateConverter.JalaliDate(1405, 1, 1))
        val result = InstallmentCalculator.generate(firstDue, PeriodType.MONTHLY, 24, 5_000_000, 8_500_000)
        assertEquals(24, result.size)
    }

    @Test
    fun `قسط اول از مبلغ قسط اول و بقیه از مبلغ سایر اقساط استفاده می‌کنند`() {
        val firstDue = PersianDateConverter.toGregorian(PersianDateConverter.JalaliDate(1405, 1, 1))
        val result = InstallmentCalculator.generate(firstDue, PeriodType.MONTHLY, 5, 5_000_000, 8_500_000)
        assertEquals(5_000_000L, result[0].amount)
        result.drop(1).forEach { assertEquals(8_500_000L, it.amount) }
    }

    @Test
    fun `دوره ماهانه هر سررسید یک ماه بعدی است`() {
        val firstDue = PersianDateConverter.toGregorian(PersianDateConverter.JalaliDate(1405, 1, 10))
        val result = InstallmentCalculator.generate(firstDue, PeriodType.MONTHLY, 3, 1, 1)
        val jalaliDates = result.map { PersianDateConverter.toJalali(it.dueDate) }
        assertEquals(PersianDateConverter.JalaliDate(1405, 1, 10), jalaliDates[0])
        assertEquals(PersianDateConverter.JalaliDate(1405, 2, 10), jalaliDates[1])
        assertEquals(PersianDateConverter.JalaliDate(1405, 3, 10), jalaliDates[2])
    }

    @Test
    fun `دوره سه‌ماهه هر سه ماه یک‌بار تکرار می‌شود`() {
        val firstDue = PersianDateConverter.toGregorian(PersianDateConverter.JalaliDate(1405, 6, 15))
        val result = InstallmentCalculator.generate(firstDue, PeriodType.QUARTERLY, 4, 1, 1)
        val jalaliDates = result.map { PersianDateConverter.toJalali(it.dueDate) }
        assertEquals(PersianDateConverter.JalaliDate(1405, 6, 15), jalaliDates[0])
        assertEquals(PersianDateConverter.JalaliDate(1405, 9, 15), jalaliDates[1])
        assertEquals(PersianDateConverter.JalaliDate(1405, 12, 15), jalaliDates[2])
        assertEquals(PersianDateConverter.JalaliDate(1406, 3, 15), jalaliDates[3])
    }

    @Test
    fun `دوره شش‌ماهه`() {
        val firstDue = PersianDateConverter.toGregorian(PersianDateConverter.JalaliDate(1405, 1, 1))
        val result = InstallmentCalculator.generate(firstDue, PeriodType.SEMIANNUAL, 3, 1, 1)
        val jalaliDates = result.map { PersianDateConverter.toJalali(it.dueDate) }
        assertEquals(PersianDateConverter.JalaliDate(1405, 1, 1), jalaliDates[0])
        assertEquals(PersianDateConverter.JalaliDate(1405, 7, 1), jalaliDates[1])
        assertEquals(PersianDateConverter.JalaliDate(1406, 1, 1), jalaliDates[2])
    }

    @Test
    fun `دوره سالانه`() {
        val firstDue = PersianDateConverter.toGregorian(PersianDateConverter.JalaliDate(1405, 5, 20))
        val result = InstallmentCalculator.generate(firstDue, PeriodType.ANNUAL, 3, 1, 1)
        val jalaliDates = result.map { PersianDateConverter.toJalali(it.dueDate) }
        assertEquals(PersianDateConverter.JalaliDate(1405, 5, 20), jalaliDates[0])
        assertEquals(PersianDateConverter.JalaliDate(1406, 5, 20), jalaliDates[1])
        assertEquals(PersianDateConverter.JalaliDate(1407, 5, 20), jalaliDates[2])
    }

    @Test
    fun `قانون روز ۳۱ در تولید اقساط ماهانه به‌درستی رعایت می‌شود`() {
        val firstDue = PersianDateConverter.toGregorian(PersianDateConverter.JalaliDate(1405, 6, 31))
        val result = InstallmentCalculator.generate(firstDue, PeriodType.MONTHLY, 3, 1, 1)
        val jalaliDates = result.map { PersianDateConverter.toJalali(it.dueDate) }
        assertEquals(PersianDateConverter.JalaliDate(1405, 6, 31), jalaliDates[0])
        assertEquals(PersianDateConverter.JalaliDate(1405, 7, 30), jalaliDates[1])
        assertEquals(PersianDateConverter.JalaliDate(1405, 8, 30), jalaliDates[2])
    }

    @Test
    fun `محاسبه روزهای تاخیر پرداخت`() {
        val due = LocalDate.of(2026, 1, 1)
        val paid = LocalDate.of(2026, 1, 4)
        assertEquals(3, InstallmentCalculator.delayDays(due, paid))
    }

    @Test
    fun `محاسبه روزهای معوق تا امروز`() {
        val due = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2026, 1, 6)
        assertEquals(5, InstallmentCalculator.overdueDays(due, today))
    }

    @Test
    fun `محاسبه روزهای باقیمانده تا سررسید`() {
        val due = LocalDate.of(2026, 1, 10)
        val today = LocalDate.of(2026, 1, 7)
        assertEquals(3, InstallmentCalculator.remainingDays(due, today))
    }
}
