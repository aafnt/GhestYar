package ir.ghestyar.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import ir.ghestyar.app.GhestYarApplication
import ir.ghestyar.app.domain.calculator.PersianDateConverter
import ir.ghestyar.app.domain.model.ReportType
import ir.ghestyar.app.ui.components.LoanCard
import ir.ghestyar.app.utils.PersianNumberUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: GhestYarApplication,
    onAddLoan: () -> Unit,
    onOpenLoan: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(app.loanRepository, app.installmentRepository) }
        }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("وام‌های من", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLoan) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن وام")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.loans.isEmpty()) {
            EmptyHomeState(Modifier.padding(padding).fillMaxSize(), onAddLoan)
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SummarySection(state) }

            if (state.criticalLoanWarnings.isNotEmpty()) {
                item { CriticalWarningBanner(state.criticalLoanWarnings) }
            }

            item { MonthlyReportSection(state, viewModel) }

            item {
                Text("وام‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(state.loans, key = { it.id }) { loan ->
                LoanCard(loan = loan, onClick = { onOpenLoan(loan.id) })
            }

            item { Spacer(Modifier.height(72.dp)) } // فضای FAB
        }
    }
}

@Composable
private fun SummarySection(state: HomeUiState) {
    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("اقساط معوق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "🔴 ${PersianNumberUtils.formatToman(state.summary.overdueTotalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.summary.nearestDueDate != null) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("نزدیک‌ترین سررسید", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val days = ir.ghestyar.app.domain.calculator.InstallmentCalculator.remainingDays(state.summary.nearestDueDate)
                    val daysText = if (days <= 0) "امروز" else "${PersianNumberUtils.formatNumber(days.toInt())} روز دیگر"
                    Text(daysText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        PersianNumberUtils.formatToman(state.summary.nearestDueAmount ?: 0),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CriticalWarningBanner(warnings: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("🚨 هشدار مهم", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            warnings.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyReportSection(state: HomeUiState, viewModel: HomeViewModel) {
    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                YearDropdown(state, viewModel)
                ReportTypeDropdown(state, viewModel)
            }

            Spacer(Modifier.height(12.dp))

            val selectedIndex = state.months.indexOf(state.selectedMonth).coerceAtLeast(0)
            ir.ghestyar.app.ui.components.MonthSelector(
                months = state.months,
                selectedIndex = selectedIndex,
                onSelect = { viewModel.selectMonth(it) }
            )

            Spacer(Modifier.height(16.dp))

            if (state.selectedMonth != null) {
                Text(
                    "${PersianDateConverter.monthName(state.selectedMonth.jalaliMonth)} ${PersianNumberUtils.toPersianDigits(state.selectedMonth.jalaliYear.toString())}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                if (state.monthlyReport.rows.isEmpty()) {
                    Text(
                        "قسطی برای این ماه با این فیلتر وجود ندارد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.monthlyReport.rows.forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.loanName, style = MaterialTheme.typography.bodyMedium)
                            Text(PersianNumberUtils.formatToman(row.amount), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("جمع", fontWeight = FontWeight.Bold)
                        Text(PersianNumberUtils.formatToman(state.monthlyReport.total), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearDropdown(state: HomeUiState, viewModel: HomeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { expanded = true }, label = { Text(PersianNumberUtils.toPersianDigits(state.selectedJalaliYear.toString())) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.availableYears.forEach { year ->
                DropdownMenuItem(
                    text = { Text(PersianNumberUtils.toPersianDigits(year.toString())) },
                    onClick = { viewModel.selectYear(year); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportTypeDropdown(state: HomeUiState, viewModel: HomeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { expanded = true }, label = { Text(state.selectedReportType.displayName) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReportType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = { viewModel.selectReportType(type); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier, onAddLoan: () -> Unit) {
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text("هنوز وامی ثبت نکرده‌اید", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "برای شروع، اولین وام خود را اضافه کنید",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddLoan) { Text("افزودن وام") }
        }
    }
}
