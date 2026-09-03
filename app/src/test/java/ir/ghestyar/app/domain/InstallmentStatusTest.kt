package ir.ghestyar.app.domain

import ir.ghestyar.app.domain.model.InstallmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InstallmentStatusTest {

    private val today = LocalDate.of(2026, 6, 15)

    @Test
    fun `قسط با تاریخ پرداخت همیشه پرداخت‌شده است`() {
        val status = InstallmentStatus.of(
            dueDate = LocalDate.of(2026, 6, 10),
            paidDate = LocalDate.of(2026, 6, 12),
            today = today
        )
        assertEquals(InstallmentStatus.PAID, status)
    }

    @Test
    fun `قسط بدون پرداخت و سررسید گذشته معوق است`() {
        val status = InstallmentStatus.of(
            dueDate = LocalDate.of(2026, 6, 1),
            paidDate = null,
            today = today
        )
        assertEquals(InstallmentStatus.OVERDUE, status)
    }

    @Test
    fun `قسط بدون پرداخت و سررسید امروز معوق است`() {
        val status = InstallmentStatus.of(dueDate = today, paidDate = null, today = today)
        assertEquals(InstallmentStatus.OVERDUE, status)
    }

    @Test
    fun `قسط بدون پرداخت و سررسید آینده در انتظار است`() {
        val status = InstallmentStatus.of(
            dueDate = LocalDate.of(2026, 7, 1),
            paidDate = null,
            today = today
        )
        assertEquals(InstallmentStatus.UPCOMING, status)
    }

    @Test
    fun `پرداخت اولویت دارد حتی اگر سررسید گذشته باشد`() {
        // قسطی که دیر پرداخت شده باید پرداخت‌شده باشد، نه معوق
        val status = InstallmentStatus.of(
            dueDate = LocalDate.of(2026, 1, 1),
            paidDate = LocalDate.of(2026, 3, 1),
            today = today
        )
        assertEquals(InstallmentStatus.PAID, status)
    }
}
