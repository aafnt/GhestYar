package ir.ghestyar.app.presentation.loandetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.data.entity.LoanEntity
import ir.ghestyar.app.data.repository.InstallmentRepository
import ir.ghestyar.app.data.repository.LoanRepository
import ir.ghestyar.app.domain.model.InstallmentStatus
import ir.ghestyar.app.domain.notification.NotificationScheduler
import ir.ghestyar.app.utils.ImageStorageManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LoanDetailUiState(
    val loan: LoanEntity? = null,
    val installments: List<InstallmentEntity> = emptyList(),
    val isLoading: Boolean = true,
    val deleted: Boolean = false
) {
    val overdue get() = installments.filterStatus(InstallmentStatus.OVERDUE)
    val paid get() = installments.filterStatus(InstallmentStatus.PAID)
    val upcoming get() = installments.filterStatus(InstallmentStatus.UPCOMING)
}

private fun List<InstallmentEntity>.filterStatus(status: InstallmentStatus): List<InstallmentEntity> {
    val today = LocalDate.now()
    return filter {
        InstallmentStatus.of(LocalDate.parse(it.dueDate), it.paidDate?.let(LocalDate::parse), today) == status
    }
}

class LoanDetailViewModel(
    private val context: Context,
    private val loanId: Long,
    private val loanRepository: LoanRepository,
    private val installmentRepository: InstallmentRepository,
    private val db: AppDatabase
) : ViewModel() {

    val uiState: StateFlow<LoanDetailUiState> = combine(
        loanRepository.observeLoan(loanId),
        installmentRepository.observeByLoan(loanId)
    ) { loan, installments ->
        LoanDetailUiState(loan = loan, installments = installments, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoanDetailUiState())

    fun markAsPaid(installmentId: Long, paidDate: LocalDate) = viewModelScope.launch {
        installmentRepository.markAsPaid(installmentId, paidDate)
        NotificationScheduler.rescheduleAll(context, db)
    }

    fun unmarkPaid(installmentId: Long) = viewModelScope.launch {
        installmentRepository.unmarkPaid(installmentId)
        NotificationScheduler.rescheduleAll(context, db)
    }

    /** ویرایش مستقل قسط: مبلغ، تاریخ سررسید، توضیحات (بند ۳۰) - بدون اثر روی سایر اقساط */
    fun updateInstallment(installment: InstallmentEntity) = viewModelScope.launch {
        installmentRepository.update(installment)
        NotificationScheduler.rescheduleAll(context, db)
    }

    fun deleteLoan(onDeleted: () -> Unit) = viewModelScope.launch {
        val loan = uiState.value.loan ?: return@launch
        loanRepository.deleteLoan(loan)
        ImageStorageManager.delete(loan.imagePath)
        NotificationScheduler.rescheduleAll(context, db)
        onDeleted()
    }
}
