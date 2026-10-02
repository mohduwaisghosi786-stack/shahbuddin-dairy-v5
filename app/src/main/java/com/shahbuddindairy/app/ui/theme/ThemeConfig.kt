package com.shahbuddindairy.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.util.Calendar

enum class ThemeMode(val label: String) {
    AUTO("Auto (Day/Night)"),
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

enum class AccentColor(
    val label: String,
    val previewColor: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val primaryContainerLight: Color,
    val primaryContainerDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color
) {
    GREEN(
        label = "Dairy Green",
        previewColor = Color(0xFF1B5E20),
        primaryLight = Color(0xFF1B5E20),
        primaryDark = Color(0xFF81C784),
        primaryContainerLight = Color(0xFFE8F5E9),
        primaryContainerDark = Color(0xFF1B401E),
        secondaryLight = Color(0xFF2E7D32),
        secondaryDark = Color(0xFFA5D6A7)
    ),
    BLUE(
        label = "Cobalt Blue",
        previewColor = Color(0xFF1565C0),
        primaryLight = Color(0xFF1565C0),
        primaryDark = Color(0xFF90CAF9),
        primaryContainerLight = Color(0xFFE3F2FD),
        primaryContainerDark = Color(0xFF0D3766),
        secondaryLight = Color(0xFF1976D2),
        secondaryDark = Color(0xFF64B5F6)
    ),
    PURPLE(
        label = "Royal Purple",
        previewColor = Color(0xFF6A1B9A),
        primaryLight = Color(0xFF6A1B9A),
        primaryDark = Color(0xFFCE93D8),
        primaryContainerLight = Color(0xFFF3E5F5),
        primaryContainerDark = Color(0xFF420E63),
        secondaryLight = Color(0xFF8E24AA),
        secondaryDark = Color(0xFFBA68C8)
    ),
    ORANGE(
        label = "Sunset Orange",
        previewColor = Color(0xFFE65100),
        primaryLight = Color(0xFFE65100),
        primaryDark = Color(0xFFFFB74D),
        primaryContainerLight = Color(0xFFFFF3E0),
        primaryContainerDark = Color(0xFF5C2200),
        secondaryLight = Color(0xFFF57C00),
        secondaryDark = Color(0xFFFFCC80)
    ),
    TEAL(
        label = "Deep Teal",
        previewColor = Color(0xFF00695C),
        primaryLight = Color(0xFF00695C),
        primaryDark = Color(0xFF80CBC4),
        primaryContainerLight = Color(0xFFE0F2F1),
        primaryContainerDark = Color(0xFF003D35),
        secondaryLight = Color(0xFF00897B),
        secondaryDark = Color(0xFF4DB6AC)
    ),
    RED(
        label = "Ruby Red",
        previewColor = Color(0xFFC62828),
        primaryLight = Color(0xFFC62828),
        primaryDark = Color(0xFFEF9A9A),
        primaryContainerLight = Color(0xFFFFEBEE),
        primaryContainerDark = Color(0xFF4E0E0E),
        secondaryLight = Color(0xFFD32F2F),
        secondaryDark = Color(0xFFE57373)
    ),
    DYNAMIC(
        label = "Material You (Dynamic)",
        previewColor = Color(0xFF436549),
        primaryLight = Color(0xFF1B5E20),
        primaryDark = Color(0xFF81C784),
        primaryContainerLight = Color(0xFFE8F5E9),
        primaryContainerDark = Color(0xFF1B401E),
        secondaryLight = Color(0xFF2E7D32),
        secondaryDark = Color(0xFFA5D6A7)
    )
}

class ThemeController(context: Context) {
    private val prefs = context.getSharedPreferences("sd_theme_prefs", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(loadThemeMode())
        private set

    var accentColor by mutableStateOf(loadAccentColor())
        private set

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString("theme_mode", ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name
        return try {
            ThemeMode.valueOf(saved)
        } catch (e: Exception) {
            ThemeMode.AUTO
        }
    }

    private fun loadAccentColor(): AccentColor {
        val saved = prefs.getString("accent_color", AccentColor.GREEN.name) ?: AccentColor.GREEN.name
        return try {
            AccentColor.valueOf(saved)
        } catch (e: Exception) {
            AccentColor.GREEN
        }
    }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setColor(accent: AccentColor) {
        accentColor = accent
        prefs.edit().putString("accent_color", accent.name).apply()
    }

    fun isDarkTheme(systemIsDark: Boolean): Boolean {
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemIsDark
            ThemeMode.AUTO -> {
                // Daytime (06:00 to 18:30) = Light, Nighttime = Dark
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                val currentMinutes = hour * 60 + minute
                val sunrise = 6 * 60       // 6:00 AM
                val sunset = 18 * 60 + 30   // 6:30 PM
                currentMinutes !in sunrise..sunset
            }
        }
    }
}

val LocalThemeController = compositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}

fun buildColorScheme(
    accent: AccentColor,
    isDark: Boolean,
    context: Context
): ColorScheme {
    if (accent == AccentColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    return if (isDark) {
        darkColorScheme(
            primary = accent.primaryDark,
            onPrimary = Color(0xFF003912),
            primaryContainer = accent.primaryContainerDark,
            onPrimaryContainer = Color(0xFFC7F0C7),
            secondary = accent.secondaryDark,
            onSecondary = Color(0xFF003918),
            secondaryContainer = Color(0xFF1E3821),
            onSecondaryContainer = Color(0xFFCEECCF),
            tertiary = Color(0xFFA0D1D6),
            onTertiary = Color(0xFF00363B),
            tertiaryContainer = Color(0xFF1E4D53),
            onTertiaryContainer = Color(0xFFBCEEF3),
            background = Color(0xFF111411),
            onBackground = Color(0xFFE2E4DE),
            surface = Color(0xFF181C18),
            onSurface = Color(0xFFE2E4DE),
            surfaceVariant = Color(0xFF222722),
            onSurfaceVariant = Color(0xFFC2C8C1),
            surfaceContainer = Color(0xFF1E231E),
            surfaceContainerHigh = Color(0xFF282E28),
            surfaceContainerHighest = Color(0xFF333933),
            outline = Color(0xFF404840),
            outlineVariant = Color(0xFF2B322B),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = accent.primaryLight,
            onPrimary = Color.White,
            primaryContainer = accent.primaryContainerLight,
            onPrimaryContainer = Color(0xFF002206),
            secondary = accent.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0EEDF),
            onSecondaryContainer = Color(0xFF0A200E),
            tertiary = Color(0xFF006874),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFBCEEF3),
            onTertiaryContainer = Color(0xFF001F24),
            background = Color(0xFFF7F9F7),
            onBackground = Color(0xFF181C19),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF181C19),
            surfaceVariant = Color(0xFFEDF2ED),
            onSurfaceVariant = Color(0xFF424941),
            surfaceContainer = Color(0xFFF2F6F2),
            surfaceContainerHigh = Color(0xFFE6EDE6),
            surfaceContainerHighest = Color(0xFFDBE3DB),
            outline = Color(0xFFD0D7D0),
            outlineVariant = Color(0xFFE3E9E3),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}
