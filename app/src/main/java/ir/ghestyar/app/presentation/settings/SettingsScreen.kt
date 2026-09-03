package ir.ghestyar.app.presentation.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import ir.ghestyar.app.BuildConfig
import ir.ghestyar.app.GhestYarApplication
import ir.ghestyar.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: GhestYarApplication, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(context.applicationContext, app.settingsRepository, app.database) }
        }
    )
    val state by viewModel.uiState.collectAsState()
    var showRestoreConfirm by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) showRestoreConfirm = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            SettingsSection(title = "ظاهر") {
                val options = listOf("LIGHT" to "روشن", "DARK" to "تاریک", "SYSTEM" to "مطابق سیستم")
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(label)
                        RadioButton(selected = state.themeMode == value, onClick = { viewModel.setThemeMode(value) })
                    }
                }
            }

            SettingsSection(title = "اعلان‌ها") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("فعال‌سازی اعلان‌های برنامه")
                    Switch(checked = state.notificationsEnabled, onCheckedChange = { viewModel.setNotificationsEnabled(it) })
                }
            }

            SettingsSection(title = "پشتیبان‌گیری") {
                Button(
                    onClick = { exportLauncher.launch("ghestyar_backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isWorking
                ) { Text("تهیه نسخه پشتیبان") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isWorking
                ) { Text("بازیابی نسخه پشتیبان") }

                if (state.backupMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.backupMessage!!, style = MaterialTheme.typography.bodySmall)
                }
            }

            SettingsSection(title = "درباره برنامه") {
                Text(stringResourceAppName())
                Text("نسخه ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    showRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text("بازیابی اطلاعات") },
            text = { Text("با بازیابی، تمام اطلاعات فعلی برنامه با اطلاعات فایل انتخاب‌شده جایگزین می‌شود. آیا ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreBackup(uri)
                    showRestoreConfirm = null
                }) { Text("بازیابی", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun stringResourceAppName(): String = androidx.compose.ui.res.stringResource(R.string.app_name)

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card { Column(Modifier.padding(16.dp), content = content) }
    }
}
