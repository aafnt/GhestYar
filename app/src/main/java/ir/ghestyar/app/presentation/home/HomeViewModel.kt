package ir.ghestyar.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.data.entity.LoanEntity
import ir.ghestyar.app.data.repository.InstallmentRepository
import ir.ghestyar.app.data.repository.LoanRepository
import ir.ghestyar.app.domain.calculator.PersianDateConverter
import ir.ghestyar.app.domain.model.InstallmentStatus
import ir.ghestyar.app.domain.model.ReportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

private data class UserSelection(
    val year: Int?,
    val reportType: ReportType,
    val selectedMonthIndex: Int?
)

class HomeViewModel(
    private val loanRepository: LoanRepository,
    private val installmentRepository: InstallmentRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val todayJalali = PersianDateConverter.toJalali(today)

    private val selection = MutableStateFlow(UserSelection(null, ReportType.OVERDUE, null))

    val uiState: StateFlow<HomeUiState> = combine(
        loanRepository.observeLoans(),
        installmentRepository.observeAll(),
        selection
    ) { loans, installments, sel ->
        buildState(loans, installments, sel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState(isLoading = true))

    fun selectYear(year: Int) {
        selection.value = selection.value.copy(year = year, selectedMonthIndex = null)
    }

    fun selectReportType(type: ReportType) {
        selection.value = selection.value.copy(reportType = type)
    }

    fun selectMonth(index: Int) {
        selection.value = selection.value.copy(selectedMonthIndex = index)
    }

    private fun buildState(
        loans: List<LoanEntity>,
        installments: List<InstallmentEntity>,
        sel: UserSelection
    ): HomeUiState {
        val installmentsByLoan = installments.groupBy { it.loanId }

        // --- کارت‌های وام ---
        val loanCards = loans.map { loan ->
            val loanInstallments = installmentsByLoan[loan.id].orEmpty()
            val paid = loanInstallments.count { it.paidDate != null }
            val overdue = loanInstallments.count {
                InstallmentStatus.of(LocalDate.parse(it.dueDate), it.paidDate?.let(LocalDate::parse), today) == InstallmentStatus.OVERDUE
            }
            val upcoming = loanInstallments.count {
                InstallmentStatus.of(LocalDate.parse(it.dueDate), it.paidDate?.let(LocalDate::parse), today) == InstallmentStatus.UPCOMING
            }
            val next = loanInstallments
                .filter { it.paidDate == null }
                .minByOrNull { it.dueDate }

            LoanCardUiModel(
                id = loan.id,
                name = loan.name,
                imagePath = loan.imagePath,
                totalAmount = loan.totalAmount,
                installmentCount = loan.installmentCount,
                paidCount = paid,
                overdueCount = overdue,
                upcomingCount = upcoming,
                nextInstallmentAmount = next?.amount,
                nextInstallmentDueDate = next?.dueDate?.let(LocalDate::parse)
            )
        }

        // --- خلاصه بالای صفحه ---
        val allUnpaid = installments.filter { it.paidDate == null }
        val overdueInstallments = allUnpaid.filter { !LocalDate.parse(it.dueDate).isAfter(today) }
        val overdueTotal = overdueInstallments.sumOf { it.amount }
        val nearest = allUnpaid.minByOrNull { it.dueDate }
        val summary = HomeSummary(
            overdueTotalAmount = overdueTotal,
            nearestDueDate = nearest?.dueDate?.let(LocalDate::parse),
            nearestDueAmount = nearest?.amount
        )

        // --- هشدار وام‌های با ۳+ قسط معوق (بند ۳۶) ---
        val criticalWarnings = loans.mapNotNull { loan ->
            val overdueCount = installmentsByLoan[loan.id].orEmpty().count {
                InstallmentStatus.of(LocalDate.parse(it.dueDate), it.paidDate?.let(LocalDate::parse), today) == InstallmentStatus.OVERDUE
            }
            if (overdueCount >= 3) "وام ${loan.name} دارای $overdueCount قسط پرداخت‌نشده است" else null
        }

        // --- سال‌های قابل انتخاب: از کوچک‌ترین تا بزرگ‌ترین سال موجود در اقساط، به‌علاوه سال جاری ---
        val allJalaliYears = installments.map { PersianDateConverter.toJalali(LocalDate.parse(it.dueDate)).year }
        val minYear = (allJalaliYears + todayJalali.year).min()
        val maxYear = (allJalaliYears + todayJalali.year).max()
        val availableYears = (minYear..maxYear).toList()
        val selectedYear = sel.year ?: todayJalali.year

        // --- ماه‌های قابل نمایش (بند ۱۴) ---
        val months = computeSelectableMonths(selectedYear, installments)
        val selectedMonth = sel.selectedMonthIndex
            ?.let { idx -> months.getOrNull(idx) }
            ?: months.firstOrNull { it.isCurrentMonth }
            ?: months.firstOrNull()

        // --- گزارش ماه انتخاب‌شده (بند ۱۵) ---
        val monthlyReport = selectedMonth?.let { month ->
            buildMonthlyReport(loans, installments, month, sel.reportType, today)
        } ?: MonthlyReport(emptyList(), 0)

        return HomeUiState(
            loans = loanCards,
            summary = summary,
            selectedJalaliYear = selectedYear,
            availableYears = availableYears,
            selectedReportType = sel.reportType,
            months = months,
            selectedMonth = selectedMonth,
            monthlyReport = monthlyReport,
            criticalLoanWarnings = criticalWarnings,
            isLoading = false
        )
    }

    /**
     * منطق نمایش ماه‌ها (بند ۱۴): ماه‌های گذشته‌ای که هیچ قسط معوقی ندارند حذف می‌شوند؛
     * نمایش از آخرین ماه گذشته دارای معوق (اگر باشد) شروع و تا ماه جاری و آینده ادامه دارد.
     */
    private fun computeSelectableMonths(
        year: Int,
        installments: List<InstallmentEntity>
    ): List<SelectableMonth> {
        val isCurrentYearOrLater = year >= todayJalali.year
        if (!isCurrentYearOrLater) {
            // سال‌های گذشته: همه ماه‌ها برای مرور آزاد نمایش داده می‌شوند
            return (1..12).map { SelectableMonth(year, it, false) }
        }
        if (year > todayJalali.year) {
            return (1..12).map { SelectableMonth(year, it, false) }
        }

        // سال جاری: پیدا کردن آخرین ماه گذشته‌ای که هنوز قسط معوق دارد
        val overdueMonths = installments
            .filter { it.paidDate == null }
            .map { PersianDateConverter.toJalali(LocalDate.parse(it.dueDate)) }
            .filter { InstallmentStatus.of(PersianDateConverter.toGregorian(it), null, today) == InstallmentStatus.OVERDUE }
            .filter { it.year == year && it.month < todayJalali.month }
            .map { it.month }

        val startMonth = overdueMonths.minOrNull() ?: todayJalali.month
        return (startMonth..12).map { SelectableMonth(year, it, it == todayJalali.month) }
    }

    private fun buildMonthlyReport(
        loans: List<LoanEntity>,
        installments: List<InstallmentEntity>,
        month: SelectableMonth,
        reportType: ReportType,
        today: LocalDate
    ): MonthlyReport {
        val loanNameById = loans.associateBy { it.id }
        val relevant = installments.filter { inst ->
            val jalali = PersianDateConverter.toJalali(LocalDate.parse(inst.dueDate))
            if (jalali.year != month.jalaliYear || jalali.month != month.jalaliMonth) return@filter false
            val status = InstallmentStatus.of(LocalDate.parse(inst.dueDate), inst.paidDate?.let(LocalDate::parse), today)
            when (reportType) {
                ReportType.OVERDUE -> status == InstallmentStatus.OVERDUE
                ReportType.PAID -> status == InstallmentStatus.PAID
                ReportType.UPCOMING -> status == InstallmentStatus.UPCOMING
                ReportType.ALL -> true
            }
        }

        val rows = relevant.groupBy { it.loanId }.map { (loanId, list) ->
            MonthlyReportRow(
                loanName = loanNameById[loanId]?.name ?: "—",
                amount = list.sumOf { it.amount }
            )
        }.sortedByDescending { it.amount }

        return MonthlyReport(rows = rows, total = rows.sumOf { it.amount })
    }
}
