package ir.ghestyar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** رنگ‌های وضعیت اقساط، جدا از پالت Material، برای استفاده در کارت‌های قسط */
data class InstallmentStatusColors(
    val paid: Color,
    val paidBg: Color,
    val overdue: Color,
    val overdueBg: Color,
    val upcoming: Color,
    val upcomingBg: Color
)

val LocalInstallmentStatusColors = staticCompositionLocalOf {
    InstallmentStatusColors(
        paid = StatusPaidLight, paidBg = StatusPaidBgLight,
        overdue = StatusOverdueLight, overdueBg = StatusOverdueBgLight,
        upcoming = StatusUpcomingLight, upcomingBg = StatusUpcomingBgLight
    )
}

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = Color(0xFF00201C),
    background = BackgroundLight,
    surface = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF00382E),
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealPrimaryContainerLight,
    background = BackgroundDark,
    surface = SurfaceDark
)

enum class AppThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun GhestYarTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors
    val statusColors = if (darkTheme) {
        InstallmentStatusColors(
            paid = StatusPaidDark, paidBg = StatusPaidBgDark,
            overdue = StatusOverdueDark, overdueBg = StatusOverdueBgDark,
            upcoming = StatusUpcomingDark, upcomingBg = StatusUpcomingBgDark
        )
    } else {
        InstallmentStatusColors(
            paid = StatusPaidLight, paidBg = StatusPaidBgLight,
            overdue = StatusOverdueLight, overdueBg = StatusOverdueBgLight,
            upcoming = StatusUpcomingLight, upcomingBg = StatusUpcomingBgLight
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalInstallmentStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GhestYarTypography,
            content = content
        )
    }
}
