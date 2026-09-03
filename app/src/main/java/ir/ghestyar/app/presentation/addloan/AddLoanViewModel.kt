package ir.ghestyar.app.presentation.addloan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ghestyar.app.data.entity.AlertEntity
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.data.entity.LoanEntity
import ir.ghestyar.app.data.repository.LoanRepository
import ir.ghestyar.app.domain.calculator.InstallmentCalculator
import ir.ghestyar.app.domain.calculator.PersianDateConverter
import ir.ghestyar.app.utils.ImageStorageManager
import ir.ghestyar.app.utils.PersianNumberUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddLoanViewModel(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddLoanFormState())
    val state: StateFlow<AddLoanFormState> = _state

    fun onNameChange(v: String) = update { it.copy(name = v, showPreview = false) }
    fun onTotalAmountChange(v: String) = update { it.copy(totalAmountText = v, showPreview = false) }
    fun onReceivedDateChange(v: String) = update { it.copy(receivedDateText = v, showPreview = false) }
    fun onInstallmentCountChange(v: String) = update { it.copy(installmentCountText = v, showPreview = false) }
    fun onPeriodTypeChange(v: ir.ghestyar.app.domain.model.PeriodType) = update { it.copy(periodType = v, showPreview = false) }
    fun onFirstDueDateChange(v: String) = update { it.copy(firstDueDateText = v, showPreview = false) }
    fun onFirstInstallmentAmountChange(v: String) = update { it.copy(firstInstallmentAmountText = v, showPreview = false) }
    fun onOtherInstallmentAmountChange(v: String) = update { it.copy(otherInstallmentAmountText = v, showPreview = false) }
    fun onAlert1Change(v: AlertFormState) = update { it.copy(alert1 = v) }
    fun onAlert2Change(v: AlertFormState) = update { it.copy(alert2 = v) }

    fun onImagePicked(context: Context, uri: Uri) {
        val path = ImageStorageManager.copyToInternalStorage(context, uri)
        if (path != null) update { it.copy(imagePath = path) }
    }

    private fun update(block: (AddLoanFormState) -> AddLoanFormState) {
        _state.value = block(_state.value)
    }

    /** اعتبارسنجی و ساخت پیش‌نمایش اقساط (بند ۱۹ و ۴۶) */
    fun buildPreview() {
        val s = _state.value
        val errors = mutableMapOf<String, String>()

        if (s.name.isBlank()) errors["name"] = "نام وام نباید خالی باشد"

        val totalAmount = PersianNumberUtils.parseAmount(s.totalAmountText)
        if (totalAmount == null || totalAmount <= 0) errors["totalAmount"] = "مبلغ وام معتبر نیست"

        val receivedDate = PersianDateConverter.parse(s.receivedDateText)
        if (receivedDate == null) errors["receivedDate"] = "تاریخ دریافت معتبر نیست (مثال: ۱۴۰۵/۰۱/۰۱)"

        val count = s.installmentCountText.let { PersianNumberUtils.toEnglishDigits(it).toIntOrNull() }
        if (count == null || count <= 0) errors["installmentCount"] = "تعداد اقساط باید بزرگ‌تر از صفر باشد"

        val firstDueDate = PersianDateConverter.parse(s.firstDueDateText)
        if (firstDueDate == null) errors["firstDueDate"] = "تاریخ اولین سررسید معتبر نیست (مثال: ۱۴۰۵/۰۶/۱۵)"

        val firstAmount = PersianNumberUtils.parseAmount(s.firstInstallmentAmountText)
        if (firstAmount == null || firstAmount <= 0) errors["firstInstallmentAmount"] = "مبلغ قسط اول معتبر نیست"

        val otherAmount = PersianNumberUtils.parseAmount(s.otherInstallmentAmountText)
        if (otherAmount == null || otherAmount <= 0) errors["otherInstallmentAmount"] = "مبلغ سایر اقساط معتبر نیست"

        if (errors.isNotEmpty()) {
            update { it.copy(errors = errors, showPreview = false) }
            return
        }

        val preview = InstallmentCalculator.generate(
            firstDueDate = firstDueDate!!,
            periodType = s.periodType,
            installmentCount = count!!,
            firstInstallmentAmount = firstAmount!!,
            otherInstallmentAmount = otherAmount!!
        )

        update { it.copy(errors = emptyMap(), preview = preview, showPreview = true) }
    }

    fun save(onDone: (Long) -> Unit) {
        val s = _state.value
        if (!s.showPreview || s.preview.isEmpty()) return

        viewModelScope.launch {
            update { it.copy(isSaving = true) }

            val totalAmount = PersianNumberUtils.parseAmount(s.totalAmountText)!!
            val receivedDate = PersianDateConverter.parse(s.receivedDateText)!!
            val firstDueDate = PersianDateConverter.parse(s.firstDueDateText)!!
            val firstAmount = PersianNumberUtils.parseAmount(s.firstInstallmentAmountText)!!
            val otherAmount = PersianNumberUtils.parseAmount(s.otherInstallmentAmountText)!!

            val loan = LoanEntity(
                name = s.name.trim(),
                imagePath = s.imagePath,
                totalAmount = totalAmount,
                receivedDate = receivedDate.toString(),
                installmentCount = s.preview.size,
                periodType = s.periodType.name,
                firstDueDate = firstDueDate.toString(),
                firstInstallmentAmount = firstAmount,
                otherInstallmentAmount = otherAmount
            )

            val installments = s.preview.map {
                InstallmentEntity(
                    loanId = 0, // در Repository جایگزین می‌شود
                    installmentNumber = it.number,
                    amount = it.amount,
                    dueDate = it.dueDate.toString(),
                    paidDate = null
                )
            }

            val alerts = buildList {
                if (s.alert1.enabled) add(s.alert1.toEntity(1))
                if (s.alert2.enabled) add(s.alert2.toEntity(2))
            }

            val loanId = loanRepository.createLoan(loan, installments, alerts)
            update { it.copy(isSaving = false, saved = true) }
            onDone(loanId)
        }
    }

    private fun AlertFormState.toEntity(index: Int) = AlertEntity(
        loanId = 0,
        alertIndex = index,
        enabled = enabled,
        daysBefore = daysBefore,
        hour = hour,
        minute = minute
    )
}
