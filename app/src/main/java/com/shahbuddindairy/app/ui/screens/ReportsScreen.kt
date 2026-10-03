package com.shahbuddindairy.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.ui.components.AppTopHeader
import com.shahbuddindairy.app.ui.components.EmptyState
import com.shahbuddindairy.app.ui.components.StatCard
import com.shahbuddindairy.app.ui.theme.DairyGreenDark
import com.shahbuddindairy.app.ui.theme.StatusAmber
import com.shahbuddindairy.app.ui.theme.StatusRed

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToCustomerKhata: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var listFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showWhatsAppDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // TOP APP HEADER
        AppTopHeader(
            title = "Reports & Analytics",
            subtitle = "${uiState.periodLabel} • ${CalculationEngine.formatDateDisplay(uiState.selectedDate)}"
        )

        // PERIOD SELECTOR (DAILY / WEEKLY / MONTHLY)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedPeriod == ReportPeriod.DAILY,
                onClick = { viewModel.onPeriodSelected(ReportPeriod.DAILY) },
                label = { Text("Daily (दैनिक)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary,
                    selectedLabelColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.selectedPeriod == ReportPeriod.WEEKLY,
                onClick = { viewModel.onPeriodSelected(ReportPeriod.WEEKLY) },
                label = { Text("7 Days (साप्ताहिक)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary,
                    selectedLabelColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.selectedPeriod == ReportPeriod.MONTHLY,
                onClick = { viewModel.onPeriodSelected(ReportPeriod.MONTHLY) },
                label = { Text("30 Days (मासिक)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary,
                    selectedLabelColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
        }

        // DATE SELECTOR (ONLY SHOWN FOR DAILY)
        if (uiState.selectedPeriod == ReportPeriod.DAILY) {
            Surface(
                color = colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.onPrevDay() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Day", tint = colorScheme.primary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onDateSelected(CalculationEngine.todayStr()) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CalculationEngine.formatDateDisplay(uiState.selectedDate),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { viewModel.onNextDay() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = colorScheme.primary)
                    }
                }
            }
        }

        // FACTUAL TREND BANNER (IF AVAILABLE)
        if (uiState.trendVsYesterdayPercent != null) {
            val trend = uiState.trendVsYesterdayPercent!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (trend >= 0) colorScheme.primaryContainer.copy(alpha = 0.6f) else StatusAmber.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (trend >= 0) "📈" else "📉",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "कल की तुलना में ${if (trend >= 0) "+$trend%" else "$trend%"} दूध वितरण (${CalculationEngine.formatLiters(uiState.totalSoldLiters)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
            }
        }

        // KEY METRIC STAT CARDS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Total Milk",
                value = CalculationEngine.formatLiters(uiState.totalSoldLiters),
                subValue = "All Types",
                valueColor = colorScheme.primary,
                icon = Icons.Default.LocalDrink,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Bill",
                value = CalculationEngine.formatMoney(uiState.totalBill),
                subValue = "Cash: ${CalculationEngine.formatMoney(uiState.totalCashCollected)} | Online: ${CalculationEngine.formatMoney(uiState.totalOnlineCollected)}",
                valueColor = colorScheme.primary,
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1.2f)
            )
            StatCard(
                label = "Pending Due",
                value = CalculationEngine.formatMoney(uiState.pendingDue),
                subValue = "Route Balance",
                valueColor = if (uiState.pendingDue > 0) StatusRed else colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        // MILK BREAKDOWN & ROUTE STATUS CARDS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Milk Type Breakdown Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "MILK BREAKDOWN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("भैंस: %.1f L".format(uiState.buffaloLiters), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        Text("गाय: %.1f L".format(uiState.cowLiters), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Lightweight Horizontal Progress Bar
                    val totalL = (uiState.buffaloLiters + uiState.cowLiters).coerceAtLeast(0.01)
                    val buffaloWeight = (uiState.buffaloLiters / totalL).toFloat().coerceIn(0.05f, 0.95f)
                    val cowWeight = 1f - buffaloWeight

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(if (uiState.buffaloLiters > 0) buffaloWeight else 0.001f)
                                .fillMaxHeight()
                                .background(colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .weight(if (uiState.cowLiters > 0) cowWeight else 0.001f)
                                .fillMaxHeight()
                                .background(Color(0xFFFFB74D))
                        )
                    }
                }
            }

            // Customer Route Status Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "DELIVERY STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Done: ${uiState.servedCount}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        Text("Naaga: ${uiState.nagaCount}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                        Text("Pend: ${uiState.pendingCount}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val totalCust = (uiState.customerRows.size).coerceAtLeast(1).toFloat()
                    val doneW = (uiState.servedCount / totalCust).coerceIn(0f, 1f)
                    val nagaW = (uiState.nagaCount / totalCust).coerceIn(0f, 1f)
                    val pendW = (uiState.pendingCount / totalCust).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        if (doneW > 0) Box(modifier = Modifier.weight(doneW).fillMaxHeight().background(colorScheme.primary))
                        if (nagaW > 0) Box(modifier = Modifier.weight(nagaW).fillMaxHeight().background(StatusRed))
                        if (pendW > 0) Box(modifier = Modifier.weight(pendW).fillMaxHeight().background(StatusAmber))
                    }
                }
            }
        }

        // EXPORT PDF & WHATSAPP REPORT BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.exportDailyPdf(context) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("DAILY PDF", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }

            Button(
                onClick = { showWhatsAppDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("WHATSAPP REPORT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        }


        // SEARCH & FILTER CHIPS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter table...", fontSize = 12.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ALL" to "All", "SERVED" to "Served", "NAGA" to "Naaga", "PENDING" to "Pending").forEach { (key, label) ->
                    val isSel = listFilter == key
                    FilterChip(
                        selected = isSel,
                        onClick = { listFilter = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // FILTERED CUSTOMER ROWS
        val filteredRows = uiState.customerRows.filter { r ->
            val matchesFilter = when (listFilter) {
                "SERVED" -> r.isServed
                "NAGA" -> r.isNaga
                "PENDING" -> !r.isServed && !r.isNaga
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    r.customer.name.contains(searchQuery, ignoreCase = true) ||
                    r.customer.id.toString() == searchQuery.trim() ||
                    r.customer.mohalla.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }

        if (filteredRows.isEmpty()) {
            EmptyState(
                title = "No Customers Found",
                message = "No records match the current filter or search."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredRows, key = { it.customer.id }) { row ->
                    DailyReportRowCard(
                        row = row,
                        onClick = { onNavigateToCustomerKhata(row.customer.id) }
                    )
                }
            }
        }
    }

    // PDF RESULT DIALOG
    uiState.pdfResult?.let { res ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPdfResult() },
            title = {
                Text(
                    text = if (res.success) "Daily Report PDF Ready ✓" else "PDF Generation Failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(res.message) },
            confirmButton = {
                if (res.success && res.uri != null) {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, res.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Daily Report PDF"))
                            viewModel.clearPdfResult()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE PDF")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.clearPdfResult() }, shape = RoundedCornerShape(8.dp)) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // WHATSAPP SALES REPORT DIALOG
    if (showWhatsAppDialog) {
        val reportText = remember(uiState) { viewModel.generateWhatsAppReportText(uiState) }
        AlertDialog(
            onDismissRequest = { showWhatsAppDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp Daily Sales Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Preview (Ready to Share / Copy):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = reportText,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Daily Sales Report", reportText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Report copied to clipboard! ✓", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                }, "Share Daily Sales Report")
                                context.startActivity(chooser)
                            }
                            showWhatsAppDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }
}


@Composable
fun DailyReportRowCard(
    row: DailyCustomerReportRow,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${row.customer.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = row.customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "${row.customer.mohalla} (${if (row.customer.billingType == "daily_cash") "Cash" else "Monthly"})",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (row.isServed) {
                    Text(
                        text = "%.2f L • ₹%.0f".format(row.liters, row.bill),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "Cash: ₹%.0f".format(row.cashPaid),
                        fontSize = 10.5.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                } else if (row.isNaga) {
                    Text(
                        text = "NAAGA (नागा)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = StatusRed
                    )
                } else {
                    Text(
                        text = "Pending",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusAmber
                    )
                }
            }
        }
    }
}
