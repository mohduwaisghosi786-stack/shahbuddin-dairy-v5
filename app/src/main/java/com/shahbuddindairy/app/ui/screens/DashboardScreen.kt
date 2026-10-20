package com.shahbuddindairy.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.ui.components.AppTopHeader
import com.shahbuddindairy.app.ui.theme.StatusAmber
import com.shahbuddindairy.app.ui.theme.StatusRed

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToDailyTask: () -> Unit,
    onNavigateToKhata: () -> Unit,
    onNavigateToReport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // TOP APP HEADER
        AppTopHeader(
            title = "Shahbuddin Dairy",
            subtitle = "डैशबोर्ड • ${CalculationEngine.formatDateDisplay(uiState.selectedDate)}",
            marketDue = uiState.totalMarketDue
        )

        // DATE SELECTOR BAR WITH QUICK PRESETS
        Surface(
            color = colorScheme.surface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.onPrevDay() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = colorScheme.primary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isToday = uiState.selectedDate == CalculationEngine.todayStr()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isToday) colorScheme.primary else colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { viewModel.onDateSelected(CalculationEngine.todayStr()) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = if (isToday) Color.White else colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isToday) "आज (Today) • ${CalculationEngine.formatDateDisplay(uiState.selectedDate)}" else CalculationEngine.formatDateDisplay(uiState.selectedDate),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White else colorScheme.primary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.onNextDay() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = colorScheme.primary)
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. EXECUTIVE 4-KPI SUMMARY GRID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMetricCard(
                        title = "दूध वितरण (Milk)",
                        value = CalculationEngine.formatLiters(uiState.totalMilkDispatched),
                        subValue = "आज का कुल दूध",
                        icon = Icons.Default.LocalDrink,
                        accentColor = Color(0xFF15803D),
                        bgColor = Color(0xFFF0FDF4),
                        modifier = Modifier.weight(1f)
                    )

                    DashboardMetricCard(
                        title = "कुल बिक्री (Sales)",
                        value = CalculationEngine.formatMoney(uiState.totalBillToday),
                        subValue = "आज का बिल",
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFF0F766E),
                        bgColor = Color(0xFFF0FDFA),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalCollected = uiState.totalCashCollectedToday + uiState.totalOnlineCollectedToday
                    DashboardMetricCard(
                        title = "कुल वसूली (Collected)",
                        value = CalculationEngine.formatMoney(totalCollected),
                        subValue = "कैश: ₹%,.0f • ऑन: ₹%,.0f".format(uiState.totalCashCollectedToday, uiState.totalOnlineCollectedToday),
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFF166534),
                        bgColor = Color(0xFFDCFCE7),
                        modifier = Modifier.weight(1f)
                    )

                    DashboardMetricCard(
                        title = "बाजार उधारी (Due)",
                        value = CalculationEngine.formatMoney(uiState.totalMarketDue),
                        subValue = "कुल बाकी उधारी (PBS सहित)",
                        icon = Icons.Default.TrendingUp,
                        accentColor = Color(0xFFDC2626),
                        bgColor = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. LIVE ROUTE DELIVERY PROGRESS CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val totalScheduledCount = uiState.deliveredCount + uiState.pendingCount + uiState.nagaCount
                        val pct = if (totalScheduledCount > 0) (uiState.deliveredCount * 100) / totalScheduledCount else 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (pct == 100) Color(0xFF16A34A) else Color(0xFFD97706))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "डिलीवरी प्रगति (Delivery Progress)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${uiState.deliveredCount} में से $totalScheduledCount शेड्यूल पूर्ण • ${uiState.altDayCount} अल्टरनेट (ऑफ)",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (pct == 100) Color(0xFFDCFCE7) else colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (pct == 100) Color(0xFF86EFAC) else colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "$pct% पूर्ण",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (pct == 100) Color(0xFF166534) else colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF15803D),
                            trackColor = colorScheme.surfaceContainerHigh
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DeliveryHealthPill(label = "डिलीवर", count = uiState.deliveredCount, color = Color(0xFF16A34A), bgColor = Color(0xFFDCFCE7), modifier = Modifier.weight(1f))
                            DeliveryHealthPill(label = "बाकी", count = uiState.pendingCount, color = Color(0xFFD97706), bgColor = Color(0xFFFEF3C7), modifier = Modifier.weight(1f))
                            DeliveryHealthPill(label = "नागा", count = uiState.nagaCount, color = Color(0xFFDC2626), bgColor = Color(0xFFFEE2E2), modifier = Modifier.weight(1f))
                            DeliveryHealthPill(label = "अल्टरनेट", count = uiState.altDayCount, color = Color(0xFF2563EB), bgColor = Color(0xFFDBEAFE), modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 3. FAST-LAUNCH ACTION TILES (4 TILES)
                Text(
                    text = "त्वरित कार्य (FAST ACTIONS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickLaunchTile(
                        title = "रनर मोड",
                        subtitle = "1-क्लिक डिलीवरी",
                        icon = Icons.Default.Today,
                        badgeColor = Color(0xFF15803D),
                        onClick = onNavigateToDailyTask,
                        modifier = Modifier.weight(1f)
                    )

                    QuickLaunchTile(
                        title = "दैनिक रिपोर्ट",
                        subtitle = "PDF व WhatsApp",
                        icon = Icons.Default.Assessment,
                        badgeColor = Color(0xFF0284C7),
                        onClick = onNavigateToReport,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickLaunchTile(
                        title = "ग्राहक खाता",
                        subtitle = "खाता बही (Khata)",
                        icon = Icons.Default.People,
                        badgeColor = Color(0xFFD97706),
                        onClick = onNavigateToKhata,
                        modifier = Modifier.weight(1f)
                    )

                    QuickLaunchTile(
                        title = "कुल हिसाब",
                        subtitle = "व्यापार सारांश",
                        icon = Icons.Default.TrendingUp,
                        badgeColor = Color(0xFF7C3AED),
                        onClick = onNavigateToKhata,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. ROUTE BREAKDOWN LIST
                if (uiState.routeProgressStats.isNotEmpty()) {
                    Text(
                        text = "रूट प्रगति (ROUTE BREAKDOWN)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.routeProgressStats.forEach { stat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToDailyTask() },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stat.routeName, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${stat.deliveredCount}/${stat.totalCount} डिलीवर • ${stat.pendingCount} बाकी • ${stat.nagaCount} नागा",
                                            fontSize = 11.sp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (stat.percent == 100) Color(0xFFDCFCE7) else colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${stat.percent}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stat.percent == 100) Color(0xFF166534) else colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLaunchTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text(text = subtitle, fontSize = 10.5.sp, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (bgColor != Color.Transparent) bgColor else colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 10.5.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeliveryHealthPill(label: String, count: Int, color: Color, bgColor: Color = Color.Transparent, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (bgColor != Color.Transparent) bgColor else color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label: $count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 10.5.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeliveryHealthPill(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label: $count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
