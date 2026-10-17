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

        // DATE SELECTOR BAR
        Surface(
            color = colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.onPrevDay() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { viewModel.onDateSelected(CalculationEngine.todayStr()) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CalculationEngine.formatDateDisplay(uiState.selectedDate),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.onNextDay() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
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
                    .padding(14.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. HERO OPERATIONAL METRICS
                Text(
                    text = "TODAY'S OVERVIEW (आज का हिसाब)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMetricCard(
                        title = "Milk Dispatched",
                        value = CalculationEngine.formatLiters(uiState.totalMilkDispatched),
                        subValue = "Bill: ${CalculationEngine.formatMoney(uiState.totalBillToday)}",
                        icon = Icons.Default.LocalDrink,
                        accentColor = colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    DashboardMetricCard(
                        title = "Cash Collected",
                        value = CalculationEngine.formatMoney(uiState.totalCashCollectedToday),
                        subValue = "Due: ${CalculationEngine.formatMoney(uiState.totalMarketDue)}",
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. DELIVERY HEALTH GAUGE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val totalDeliveryCount = uiState.deliveredCount + uiState.pendingCount + uiState.nagaCount
                        val pct = if (totalDeliveryCount > 0) (uiState.deliveredCount * 100) / totalDeliveryCount else 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Route Progress",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.deliveredCount} of $totalDeliveryCount Delivered",
                                    fontSize = 11.5.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (pct == 100) Color(0xFFDCFCE7) else colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pct == 100) Color(0xFF166534) else colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceContainerHigh
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DeliveryHealthPill(label = "Delivered", count = uiState.deliveredCount, color = Color(0xFF16A34A))
                            DeliveryHealthPill(label = "Pending", count = uiState.pendingCount, color = Color(0xFFD97706))
                            DeliveryHealthPill(label = "Naga (Skip)", count = uiState.nagaCount, color = StatusRed)
                        }
                    }
                }

                // 3. FINANCIAL SUMMARY (MARKET DUE VS ADVANCE)
                Text(
                    text = "FINANCIAL SUMMARY (बाजार की उधारी एवं एडवांस)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Market Due", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.error)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CalculationEngine.formatMoney(uiState.totalMarketDue),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.error
                            )
                            Text("बाकी उधारी", fontSize = 9.5.sp, color = colorScheme.error.copy(alpha = 0.8f))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Advance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CalculationEngine.formatMoney(uiState.totalAdvanceBalance),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                            Text("ग्राहकों का एडवांस", fontSize = 9.5.sp, color = colorScheme.primary.copy(alpha = 0.8f))
                        }
                    }
                }

                // 4. ROUTE BREAKDOWN
                if (uiState.routeProgressStats.isNotEmpty()) {
                    Text(
                        text = "ROUTES BREAKDOWN (रूट के अनुसार)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.routeProgressStats.forEach { stat ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stat.routeName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                        Text(
                                            text = "${stat.deliveredCount}/${stat.totalCount} Delivered • ${stat.pendingCount} Pending • ${stat.nagaCount} Naga",
                                            fontSize = 10.5.sp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${stat.percent}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. QUICK JUMP ACTIONS
                Text(
                    text = "QUICK ACTIONS (तुरंत कार्य)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToDailyTask,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Delivery", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToReport,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Daily Report", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
