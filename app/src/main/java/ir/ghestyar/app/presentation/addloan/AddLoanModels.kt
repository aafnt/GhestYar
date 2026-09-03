package ir.ghestyar.app.presentation.addloan

import ir.ghestyar.app.domain.calculator.GeneratedInstallment
import ir.ghestyar.app.domain.model.PeriodType

data class AlertFormState(
    val enabled: Boolean = false,
    val daysBefore: Int = 1,
    val hour: Int = 9,
    val minute: Int = 0
)

data class AddLoanFormState(
    val name: String = "",
    val imagePath: String? = null,
    val totalAmountText: String = "",
    val receivedDateText: String = "",
    val installmentCountText: String = "",
    val periodType: PeriodType = PeriodType.MONTHLY,
    val firstDueDateText: String = "",
    val firstInstallmentAmountText: String = "",
    val otherInstallmentAmountText: String = "",
    val alert1: AlertFormState = AlertFormState(),
    val alert2: AlertFormState = AlertFormState(),
    val errors: Map<String, String> = emptyMap(),
    val preview: List<GeneratedInstallment> = emptyList(),
    val showPreview: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

val ALERT_DAYS_BEFORE_OPTIONS = listOf(0, 1, 2, 3, 7)
fun alertDaysBeforeLabel(days: Int): String = when (days) {
    0 -> "همان روز"
    1 -> "۱ روز قبل"
    2 -> "۲ روز قبل"
    3 -> "۳ روز قبل"
    7 -> "۷ روز قبل"
    else -> "$days روز قبل"
}
