package ir.ghestyar.app.presentation.loandetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import ir.ghestyar.app.GhestYarApplication
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.domain.calculator.PersianDateConverter
import ir.ghestyar.app.ui.components.InstallmentRow
import ir.ghestyar.app.ui.components.LoanImage
import ir.ghestyar.app.utils.PersianNumberUtils
import java.time.LocalDate

private enum class DetailTab(val title: String) { OVERDUE("معوق"), PAID("پرداخت‌شده"), UPCOMING("آینده"), ALL("همه") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    app: GhestYarApplication,
    loanId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LoanDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LoanDetailViewModel(context.applicationContext, loanId, app.loanRepository, app.installmentRepository, app.database)
            }
        }
    )
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var installmentForPayment by remember { mutableStateOf<InstallmentEntity?>(null) }
    var installmentForEdit by remember { mutableStateOf<InstallmentEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.loan?.name ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت") } },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف وام")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loan == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator()
            }
            return@Scaffold
        }
        val loan = state.loan!!

        Column(Modifier.padding(padding).fillMaxSize()) {
            LoanHeader(loan.name, loan.imagePath, loan.totalAmount, loan.receivedDate, loan.installmentCount,
                state.paid.size, state.overdue.size, state.upcoming.size)

            val tabs = DetailTab.entries
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tab ->
                    val count = when (tab) {
                        DetailTab.OVERDUE -> state.overdue.size
                        DetailTab.PAID -> state.paid.size
                        DetailTab.UPCOMING -> state.upcoming.size
                        DetailTab.ALL -> state.installments.size
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text("${tab.title} (${PersianNumberUtils.formatNumber(count)})") }
                    )
                }
            }

            val list = when (tabs[selectedTab]) {
                DetailTab.OVERDUE -> state.overdue
                DetailTab.PAID -> state.paid
                DetailTab.UPCOMING -> state.upcoming
                DetailTab.ALL -> state.installments.sortedBy { it.installmentNumber }
            }

            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("قسطی در این دسته وجود ندارد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.id }) { installment ->
                        InstallmentRow(
                            installment = installment,
                            onDoubleTap = {
                                if (installment.paidDate == null) installmentForPayment = installment
                            },
                            onLongPress = { installmentForEdit = installment }
                        )
                    }
                }
            }
        }
    }

    installmentForPayment?.let { inst ->
        PaymentConfirmDialog(
            installment = inst,
            onDismiss = { installmentForPayment = null },
            onConfirm = { date ->
                viewModel.markAsPaid(inst.id, date)
                installmentForPayment = null
            }
        )
    }

    installmentForEdit?.let { inst ->
        InstallmentActionSheet(
            installment = inst,
            onDismiss = { installmentForEdit = null },
            onEdit = { updated -> viewModel.updateInstallment(updated); installmentForEdit = null },
            onMarkPaid = { installmentForEdit = null; installmentForPayment = inst },
            onUnmarkPaid = { viewModel.unmarkPaid(inst.id); installmentForEdit = null }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("آیا مطمئن هستید؟") },
            text = { Text("تمام اطلاعات این وام و اقساط آن حذف خواهد شد.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteLoan {} }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun LoanHeader(
    name: String, imagePath: String?, totalAmount: Long, receivedDate: String, installmentCount: Int,
    paidCount: Int, overdueCount: Int, upcomingCount: Int
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LoanImage(imagePath, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(PersianNumberUtils.formatToman(totalAmount), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "دریافت: ${PersianDateConverter.formatFull(LocalDate.parse(receivedDate))} — ${PersianNumberUtils.formatNumber(installmentCount)} قسط",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🟢 پرداخت‌شده: ${PersianNumberUtils.formatNumber(paidCount)}", style = MaterialTheme.typography.bodySmall)
            Text("🔴 معوق: ${PersianNumberUtils.formatNumber(overdueCount)}", style = MaterialTheme.typography.bodySmall)
            Text("⚪ آینده: ${PersianNumberUtils.formatNumber(upcomingCount)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PaymentConfirmDialog(
    installment: InstallmentEntity,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var dateText by remember { mutableStateOf(PersianDateConverter.formatFull(LocalDate.now())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت پرداخت قسط") },
        text = {
            Column {
                Text("قسط شماره ${PersianNumberUtils.formatNumber(installment.installmentNumber)}")
                Text("مبلغ: ${PersianNumberUtils.formatToman(installment.amount)}")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("تاریخ پرداخت") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = PersianDateConverter.parse(dateText) ?: LocalDate.now()
                onConfirm(date)
            }) { Text("تأیید پرداخت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
    )
}

@Composable
private fun InstallmentActionSheet(
    installment: InstallmentEntity,
    onDismiss: () -> Unit,
    onEdit: (InstallmentEntity) -> Unit,
    onMarkPaid: () -> Unit,
    onUnmarkPaid: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    if (editing) {
        EditInstallmentDialog(
            installment = installment,
            onDismiss = onDismiss,
            onSave = onEdit
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قسط شماره ${PersianNumberUtils.formatNumber(installment.installmentNumber)}") },
        text = {
            Column {
                TextButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) { Text("ویرایش قسط") }
                if (installment.paidDate == null) {
                    TextButton(onClick = onMarkPaid, modifier = Modifier.fillMaxWidth()) { Text("ثبت پرداخت") }
                } else {
                    TextButton(onClick = onUnmarkPaid, modifier = Modifier.fillMaxWidth()) { Text("لغو پرداخت") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun EditInstallmentDialog(
    installment: InstallmentEntity,
    onDismiss: () -> Unit,
    onSave: (InstallmentEntity) -> Unit
) {
    var amountText by remember { mutableStateOf(PersianNumberUtils.toPersianDigits(installment.amount.toString())) }
    var dueDateText by remember { mutableStateOf(PersianDateConverter.formatFull(LocalDate.parse(installment.dueDate))) }
    var note by remember { mutableStateOf(installment.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش قسط") },
        text = {
            Column {
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("مبلغ") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = dueDateText, onValueChange = { dueDateText = it }, label = { Text("تاریخ سررسید") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("توضیحات") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = PersianNumberUtils.parseAmount(amountText) ?: installment.amount
                val due = PersianDateConverter.parse(dueDateText)?.toString() ?: installment.dueDate
                onSave(installment.copy(amount = amount, dueDate = due, note = note.ifBlank { null }))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
