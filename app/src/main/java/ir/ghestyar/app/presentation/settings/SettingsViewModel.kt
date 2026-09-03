package ir.ghestyar.app.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ghestyar.app.data.backup.BackupManager
import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.repository.SettingsRepository
import ir.ghestyar.app.domain.notification.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val notificationsEnabled: Boolean = true,
    val backupMessage: String? = null,
    val isWorking: Boolean = false
)

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val db: AppDatabase
) : ViewModel() {

    private val _extra = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = kotlinx.coroutines.flow.combine(
        settingsRepository.observe(), _extra
    ) { settings, extra ->
        extra.copy(themeMode = settings.themeMode, notificationsEnabled = settings.notificationsEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: String) = viewModelScope.launch {
        val current = settingsRepository.get()
        settingsRepository.update(current.copy(themeMode = mode))
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        val current = settingsRepository.get()
        settingsRepository.update(current.copy(notificationsEnabled = enabled))
        if (enabled) NotificationScheduler.rescheduleAll(context, db) else NotificationScheduler.cancelAll(context, db)
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        _extra.value = _extra.value.copy(isWorking = true, backupMessage = null)
        val result = BackupManager.export(context, db, uri)
        _extra.value = _extra.value.copy(isWorking = false, backupMessage = result.message)
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        _extra.value = _extra.value.copy(isWorking = true, backupMessage = null)
        val result = BackupManager.restore(context, db, uri)
        if (result.success) NotificationScheduler.rescheduleAll(context, db)
        _extra.value = _extra.value.copy(isWorking = false, backupMessage = result.message)
    }
}
