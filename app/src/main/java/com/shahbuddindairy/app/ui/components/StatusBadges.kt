package com.shahbuddindairy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahbuddindairy.app.ui.theme.LocalThemeController
import com.shahbuddindairy.app.ui.theme.StatusAmber
import com.shahbuddindairy.app.ui.theme.StatusAmberBg
import com.shahbuddindairy.app.ui.theme.StatusAmberBgDark
import com.shahbuddindairy.app.ui.theme.StatusAmberDark
import com.shahbuddindairy.app.ui.theme.StatusBlue
import com.shahbuddindairy.app.ui.theme.StatusBlueBg
import com.shahbuddindairy.app.ui.theme.StatusBlueBgDark
import com.shahbuddindairy.app.ui.theme.StatusBlueDark
import com.shahbuddindairy.app.ui.theme.StatusGreen
import com.shahbuddindairy.app.ui.theme.StatusGreenBg
import com.shahbuddindairy.app.ui.theme.StatusGreenBgDark
import com.shahbuddindairy.app.ui.theme.StatusGreenDark
import com.shahbuddindairy.app.ui.theme.StatusRed
import com.shahbuddindairy.app.ui.theme.StatusRedBg
import com.shahbuddindairy.app.ui.theme.StatusRedBgDark
import com.shahbuddindairy.app.ui.theme.StatusRedDark

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val themeController = LocalThemeController.current
    val isDark = themeController.isDarkTheme(isSystemInDarkTheme())

    val (bgColor, textColor, text, icon) = when (status.uppercase()) {
        "DONE", "DELIVERED" -> Tuple4(
            if (isDark) StatusGreenBgDark else StatusGreenBg,
            if (isDark) StatusGreenDark else StatusGreen,
            "DONE",
            Icons.Default.Check
        )
        "NAGA", "SKIPPED" -> Tuple4(
            if (isDark) StatusRedBgDark else StatusRedBg,
            if (isDark) StatusRedDark else StatusRed,
            "NAGA (नागा)",
            Icons.Default.Close
        )
        "ALTERNATE" -> Tuple4(
            if (isDark) StatusBlueBgDark else StatusBlueBg,
            if (isDark) StatusBlueDark else StatusBlue,
            "ALTERNATE (कल)",
            Icons.Default.Schedule
        )
        "PAUSED", "INACTIVE" -> Tuple4(
            if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE),
            if (isDark) Color(0xFFAAAAAA) else Color(0xFF616161),
            "PAUSED",
            null
        )
        else -> Tuple4(
            if (isDark) StatusAmberBgDark else StatusAmberBg,
            if (isDark) StatusAmberDark else StatusAmber,
            "PENDING",
            Icons.Default.HourglassEmpty
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun BillingTypeBadge(
    billingType: String,
    modifier: Modifier = Modifier
) {
    val themeController = LocalThemeController.current
    val isDark = themeController.isDarkTheme(isSystemInDarkTheme())
    val isDaily = billingType == "daily_cash"

    val bgColor = if (isDaily) {
        if (isDark) Color(0xFF1B381D) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF1B2844) else Color(0xFFE8EAF6)
    }

    val textColor = if (isDaily) {
        if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
    } else {
        if (isDark) Color(0xFF9FA8DA) else Color(0xFF283593)
    }

    val text = if (isDaily) "Daily Cash" else "Monthly"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RateBadge(
    rate: Double,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colorScheme.primaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = if (rate > 0) "₹%.0f/L".format(rate) else "Std Rate",
            color = colorScheme.onPrimaryContainer,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MilkTypeBadge(
    milkType: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isCow = milkType == "गाय" || milkType.equals("cow", ignoreCase = true)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCow) Color(0xFFFFF3E0) else colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .border(
                0.5.dp,
                if (isCow) Color(0xFFFFB74D) else colorScheme.secondary.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = if (isCow) "गाय (Cow)" else "भैंस (Buffalo)",
            color = if (isCow) Color(0xFFE65100) else colorScheme.onSecondaryContainer,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
