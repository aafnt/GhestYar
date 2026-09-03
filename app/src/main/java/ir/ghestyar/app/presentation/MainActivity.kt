package ir.ghestyar.app.presentation

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import ir.ghestyar.app.GhestYarApplication
import ir.ghestyar.app.domain.notification.NotificationScheduler
import ir.ghestyar.app.presentation.navigation.GhestYarNavGraph
import ir.ghestyar.app.ui.theme.AppThemeMode
import ir.ghestyar.app.ui.theme.GhestYarTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app: GhestYarApplication by lazy { application as GhestYarApplication }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* نتیجه لازم نیست */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        maybeRequestExactAlarmPermission()

        lifecycleScope.launch {
            NotificationScheduler.rescheduleAll(this@MainActivity, app.database)
        }

        setContent {
            // کل رابط کاربری همیشه راست‌به‌چپ است (بند ۴ سند طراحی)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }

                LaunchedEffect(Unit) {
                    app.settingsRepository.observe().collect { settings ->
                        themeMode = AppThemeMode.entries.firstOrNull { it.name == settings.themeMode } ?: AppThemeMode.SYSTEM
                    }
                }

                GhestYarTheme(themeMode = themeMode) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        GhestYarNavGraph(app = app)
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun maybeRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                    )
                } catch (_: Exception) {
                    // برخی دستگاه‌ها این Intent را پشتیبانی نمی‌کنند؛ بی‌صدا رد می‌شود
                }
            }
        }
    }
}
