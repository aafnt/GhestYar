package ir.ghestyar.app.presentation.addloan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import ir.ghestyar.app.GhestYarApplication
import ir.ghestyar.app.domain.model.PeriodType
import ir.ghestyar.app.ui.components.LoanImage
import ir.ghestyar.app.utils.PersianNumberUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    app: GhestYarApplication,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel: AddLoanViewModel = viewModel(
        factory = viewModelFactory { initializer { AddLoanViewModel(app.loanRepository) } }
    )
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImagePicked(context, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت وام جدید", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(contentAlignment = Alignment.BottomEnd) {
                    LoanImage(state.imagePath, size = 84.dp)
                    IconButton(onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "انتخاب تصویر")
                    }
                }
            }

            item {
                LabeledField("نام وام", state.name, viewModel::onNameChange, state.errors["name"], placeholder = "مثلاً بانک رفاه مرکز")
            }
            item {
                LabeledField(
                    "مبلغ وام (تومان)", state.totalAmountText, viewModel::onTotalAmountChange,
                    state.errors["totalAmount"], keyboardType = KeyboardType.Number
                )
            }
            item {
                LabeledField(
                    "تاریخ دریافت وام", state.receivedDateText, viewModel::onReceivedDateChange,
                    state.errors["receivedDate"], placeholder = "۱۴۰۵/۰۱/۰۱"
                )
            }
            item {
                LabeledField(
                    "تعداد اقساط", state.installmentCountText, viewModel::onInstallmentCountChange,
                    state.errors["installmentCount"], keyboardType = KeyboardType.Number
                )
            }
            item { PeriodTypeSelector(state.periodType, viewModel::onPeriodTypeChange) }
            item {
                LabeledField(
                    "تاریخ اولین سررسید", state.firstDueDateText, viewModel::onFirstDueDateChange,
                    state.errors["firstDueDate"], placeholder = "۱۴۰۵/۰۶/۱۵"
                )
            }
            item {
                LabeledField(
                    "مبلغ قسط اول (تومان)", state.firstInstallmentAmountText, viewModel::onFirstInstallmentAmountChange,
                    state.errors["firstInstallmentAmount"], keyboardType = KeyboardType.Number
                )
            }
            item {
                LabeledField(
                    "مبلغ سایر اقساط (تومان)", state.otherInstallmentAmountText, viewModel::onOtherInstallmentAmountChange,
                    state.errors["otherInstallmentAmount"], keyboardType = KeyboardType.Number
                )
            }

            item {
                Text("هشدارها (حداکثر دو مورد)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            item { AlertEditor("هشدار اول", state.alert1, viewModel::onAlert1Change) }
            item { AlertEditor("هشدار دوم", state.alert2, viewModel::onAlert2Change) }

            item {
                Button(onClick = { viewModel.buildPreview() }, modifier = Modifier.fillMaxWidth()) {
                    Text("پیش‌نمایش اقساط")
                }
            }

            if (state.showPreview) {
                item { PreviewSection(state) }
                item {
                    Button(
                        onClick = { viewModel.save(onSaved) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    ) {
                        Text(if (state.isSaving) "در حال ذخیره..." else "ذخیره وام")
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = error != null,
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodTypeSelector(selected: PeriodType, onSelect: (PeriodType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("دوره پرداخت") },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PeriodType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.displayName) }, onClick = { onSelect(type); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertEditor(title: String, alert: AlertFormState, onChange: (AlertFormState) -> Unit) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold)
                Switch(checked = alert.enabled, onCheckedChange = { onChange(alert.copy(enabled = it)) })
            }
            if (alert.enabled) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(onClick = { expanded = true }, label = { Text(alertDaysBeforeLabel(alert.daysBefore)) })
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ALERT_DAYS_BEFORE_OPTIONS.forEach { d ->
                                DropdownMenuItem(text = { Text(alertDaysBeforeLabel(d)) }, onClick = { onChange(alert.copy(daysBefore = d)); expanded = false })
                            }
                        }
                    }
                    var hourExpanded by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(onClick = { hourExpanded = true }, label = {
                            Text(PersianNumberUtils.toPersianDigits("%02d:%02d".format(alert.hour, alert.minute)))
                        })
                        DropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                            (0..23).forEach { h ->
                                DropdownMenuItem(
                                    text = { Text(PersianNumberUtils.toPersianDigits("%02d:00".format(h))) },
                                    onClick = { onChange(alert.copy(hour = h, minute = 0)); hourExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(state: AddLoanFormState) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text("پیش‌نمایش اقساط", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val list = state.preview
            val headCount = 3
            val tailCount = 3
            val showAll = list.size <= headCount + tailCount + 1

            val itemsToShow = if (showAll) list else list.take(headCount)
            itemsToShow.forEach { PreviewRow(it) }
            if (!showAll) {
                Text("...", modifier = Modifier.padding(vertical = 4.dp))
                list.takeLast(tailCount).forEach { PreviewRow(it) }
            }
        }
    }
}

@Composable
private fun PreviewRow(item: ir.ghestyar.app.domain.calculator.GeneratedInstallment) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("قسط ${PersianNumberUtils.formatNumber(item.number)}", style = MaterialTheme.typography.bodySmall)
        Text(
            "${ir.ghestyar.app.domain.calculator.PersianDateConverter.formatFull(item.dueDate)} — ${PersianNumberUtils.formatToman(item.amount)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
