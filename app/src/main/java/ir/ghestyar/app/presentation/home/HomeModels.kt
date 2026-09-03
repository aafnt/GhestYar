package ir.ghestyar.app.presentation.home

import ir.ghestyar.app.domain.model.ReportType
import java.time.LocalDate

data class LoanCardUiModel(
    val id: Long,
    val name: String,
    val imagePath: String?,
    val totalAmount: Long,
    val installmentCount: Int,
    val paidCount: Int,
    val overdueCount: Int,
    val upcomingCount: Int,
    val nextInstallmentAmount: Long?,
    val nextInstallmentDueDate: LocalDate?
)

data class HomeSummary(
    val overdueTotalAmount: Long,
    val nearestDueDate: LocalDate?,
    val nearestDueAmount: Long?
)

/** یک ماه قابل انتخاب در گزارش ماهانه صفحه اصلی */
data class SelectableMonth(
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val isCurrentMonth: Boolean
)

/** ردیف یک وام در گزارش ماه انتخاب‌شده */
data class MonthlyReportRow(
    val loanName: String,
    val amount: Long
)

data class MonthlyReport(
    val rows: List<MonthlyReportRow>,
    val total: Long
)

data class HomeUiState(
    val loans: List<LoanCardUiModel> = emptyList(),
    val summary: HomeSummary = HomeSummary(0, null, null),
    val selectedJalaliYear: Int = 0,
    val availableYears: List<Int> = emptyList(),
    val selectedReportType: ReportType = ReportType.OVERDUE,
    val months: List<SelectableMonth> = emptyList(),
    val selectedMonth: SelectableMonth? = null,
    val monthlyReport: MonthlyReport = MonthlyReport(emptyList(), 0),
    val criticalLoanWarnings: List<String> = emptyList(), // وام‌هایی با ۳+ قسط معوق
    val isLoading: Boolean = true
)
