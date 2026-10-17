package com.shahbuddindairy.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.graphics.Bitmap
import com.shahbuddindairy.app.receipt.UpiQrGenerator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.shahbuddindairy.app.ai.PrivateAiApiClient
import com.shahbuddindairy.app.ai.ConnectionTestResult
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
import com.shahbuddindairy.app.ui.components.StatCard
import com.shahbuddindairy.app.ui.theme.AccentColor
import com.shahbuddindairy.app.ui.theme.DairyGreenDark
import com.shahbuddindairy.app.ui.theme.LocalThemeController
import com.shahbuddindairy.app.ui.theme.StatusAmber
import com.shahbuddindairy.app.ui.theme.StatusRed
import com.shahbuddindairy.app.ui.theme.ThemeMode

@Composable
fun TotalHisabScreen(viewModel: BackupViewModel) {
    BackupScreen(viewModel = viewModel)
}

@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val themeController = LocalThemeController.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onFileSelectedForRestore(context, uri)
        }
    }

    var payeeInput by remember(uiState.payeeName) { mutableStateOf(uiState.payeeName) }
    var phoneInput by remember(uiState.dairyPhone) { mutableStateOf(uiState.dairyPhone) }
    var whatsappInput by remember(uiState.whatsappPhone) { mutableStateOf(uiState.whatsappPhone) }
    var upiInput by remember(uiState.upiId) { mutableStateOf(uiState.upiId) }
    var showRevertConfirm by remember { mutableStateOf(false) }

    // AI Munshi & Private Server Configuration States
    val coroutineScope = rememberCoroutineScope()
    val aiClient = remember { PrivateAiApiClient(context) }
    var aiEnabled by remember { mutableStateOf(aiClient.isAiEnabled) }
    var aiServerUrlInput by remember { mutableStateOf(aiClient.serverUrl) }
    var aiModelInput by remember { mutableStateOf(aiClient.configuredModel) }
    var aiTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isTestingAi by remember { mutableStateOf(false) }

    // Security & App Lock Dialog States
    var showEnablePinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showQrPreviewDialog by remember { mutableStateOf(false) }
    var pinSetupInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }

    // Module 5: 30-Second Delayed Factory Reset States
    var showFormatConfirmDialog by remember { mutableStateOf(false) }
    var formatConfirmInput by remember { mutableStateOf("") }
    var formatTimerSeconds by remember { mutableIntStateOf(30) }

    LaunchedEffect(showFormatConfirmDialog) {
        if (showFormatConfirmDialog) {
            formatTimerSeconds = 30
            formatConfirmInput = ""
            while (formatTimerSeconds > 0) {
                delay(1000L)
                formatTimerSeconds--
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // TOP APP HEADER
        AppTopHeader(
            title = "Total Hisab",
            subtitle = "कुल हिसाब, डेटा सुरक्षा व सेटिंग्स"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TOTAL FINANCIAL HISAB OVERVIEW CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TOTAL HISAB (कुल हिसाब)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Active ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Customers",
                            value = "${uiState.totalCustomers}",
                            valueColor = colorScheme.onSurface,
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Deliveries",
                            value = "${uiState.totalDeliveries}",
                            valueColor = colorScheme.onSurface,
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Payments",
                            value = "${uiState.totalPayments}",
                            subValue = "Online: ${uiState.totalOnlinePayments}",
                            valueColor = colorScheme.primary,
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }

            // 2. APPEARANCE & THEME SETTINGS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance & Theme (दिखावट व थीम)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "DAY / NIGHT MODE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    // Theme Mode Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val isSel = themeController.themeMode == mode
                            FilterChip(
                                selected = isSel,
                                onClick = { themeController.setTheme(mode) },
                                label = { Text(mode.label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    val icon = when (mode) {
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.AUTO -> Icons.Default.Settings
                                        ThemeMode.SYSTEM -> Icons.Default.Info
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primary,
                                    selectedLabelColor = colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Text(
                        text = "PRIMARY ACCENT COLOR (थीम रंग)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    // Accent Color Palette Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AccentColor.values().forEach { accent ->
                            val isSelected = themeController.accentColor == accent
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { themeController.setColor(accent) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent.previewColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = accent.label.split(" ").first(),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. DAILY TASK NOTIFICATION WORKFLOW CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Daily Task Notification (वितरण सूचना)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Native Android notification with quick delivery actions",
                                    fontSize = 11.5.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = uiState.notifEnabled,
                            onCheckedChange = { viewModel.setNotifEnabled(it, context) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.onPrimary, checkedTrackColor = colorScheme.primary)
                        )
                    }

                    if (uiState.notifEnabled) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = "LOCK SCREEN NOTIFICATION PRIVACY",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isFull = uiState.notifPrivacy == "full"
                            FilterChip(
                                selected = isFull,
                                onClick = { viewModel.setNotifPrivacy("full", context) },
                                label = { Text("Full Details (Milk, Bill, Due)", fontSize = 11.5.sp, fontWeight = if (isFull) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorScheme.primary, selectedLabelColor = colorScheme.onPrimary),
                                shape = RoundedCornerShape(8.dp)
                            )

                            val isPrivate = uiState.notifPrivacy == "private"
                            FilterChip(
                                selected = isPrivate,
                                onClick = { viewModel.setNotifPrivacy("private", context) },
                                label = { Text("Private (Hide Money)", fontSize = 11.5.sp, fontWeight = if (isPrivate) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colorScheme.primary, selectedLabelColor = colorScheme.onPrimary),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // 4. ZERO DATA LOSS & BACKUP CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BACKUP & DATA PROTECTION",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3-WAY HEALTH STATUS GRID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. LOCAL BACKUP
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("LOCAL BACKUP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                Text("✓ Healthy", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DairyGreenDark)
                                Text(uiState.lastBackupTime ?: "Pending", fontSize = 9.5.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }

                        // 2. CLOUD BACKUP
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surfaceContainer,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("GOOGLE CLOUD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                                Text("✓ Enabled", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                Text("Drive Export", fontSize = 9.5.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }

                        // 3. DATABASE
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surfaceContainer,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("DATABASE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                                Text("✓ Healthy", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DairyGreenDark)
                                Text("SQLite v2", fontSize = 9.5.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Creates verified physical backups with all customers, milk history, payments, and settings in Downloads.",
                        fontSize = 11.5.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.backupDataNow(context) },
                        enabled = !uiState.isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        if (uiState.isBackingUp) {
                            CircularProgressIndicator(color = colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creating Physical Backup...")
                        } else {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BACKUP DATA NOW", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESTORE BACKUP FILE", fontWeight = FontWeight.Bold)
                    }

                    if (uiState.hasSafetySnapshot) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showRevertConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Revert to Pre-Restore Safety Snapshot")
                        }
                    }
                }
            }

            // 5. GOOGLE DRIVE & CLOUD BACKUP CARD (OPTIONAL)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google Drive & Cloud (वैकल्पिक)",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Optional", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "स्थानीय डिवाइस डेटा ही मुख्य स्रोत (Source of Truth) है। आप बैकअप पैकेज को सुरक्षित रूप से Google Drive, Gmail या अन्य क्लाउड स्टोरेज पर भेज सकते हैं।",
                        fontSize = 11.5.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.shareBackupToCloud(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE / UPLOAD TO GOOGLE DRIVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESTORE FROM DRIVE / FILES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // 6. SECURITY & APP LOCK CARD (V8.0 Native PIN Lock)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (uiState.isAppLockEnabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "App Lock (ऐप लॉक सुरक्षा)",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (uiState.isAppLockEnabled) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (uiState.isAppLockEnabled) "ON ✓" else "OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isAppLockEnabled) colorScheme.primary else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "स्थानीय 4-अंकीय पिन कोड सुरक्षा। जब यह ऑन होता है, तो ऐप खोलने पर सुरक्षित पिन पूछा जाता है। (डिफ़ॉल्ट रूप से बंद है)",
                        fontSize = 11.5.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (!uiState.isAppLockEnabled) {
                        Button(
                            onClick = {
                                pinSetupInput = ""
                                pinConfirmInput = ""
                                pinErrorText = null
                                showEnablePinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ENABLE APP LOCK (SET 4-DIGIT PIN)", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    currentPinInput = ""
                                    newPinInput = ""
                                    pinConfirmInput = ""
                                    pinErrorText = null
                                    showChangePinDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CHANGE PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    currentPinInput = ""
                                    pinErrorText = null
                                    showDisablePinDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("DISABLE LOCK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. BUSINESS UPI & CONTACT SETTINGS (V8.0)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Business & UPI Profile (डेयरी प्रोफाइल)",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                    }

                    OutlinedTextField(
                        value = payeeInput,
                        onValueChange = { payeeInput = it },
                        label = { Text("Dairy Name / Business Payee") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Dairy Phone") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = whatsappInput,
                            onValueChange = { whatsappInput = it },
                            label = { Text("WhatsApp Phone") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it.trim() },
                        label = { Text("UPI ID (e.g. 9897259461@upi)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showQrPreviewDialog = true },
                            enabled = upiInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR PREVIEW", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.saveUpiSettings(payeeInput, phoneInput, upiInput, whatsappInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 8. AI MUNSHI & PRIVATE SERVER SETTINGS (DEBIAN 13 / TAILSCALE)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "AI Munshi & Private Server",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Debian 13 Tailscale • MiniCPM-V 1B",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = aiEnabled,
                            onCheckedChange = {
                                aiEnabled = it
                                aiClient.isAiEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF15803D))
                        )
                    }

                    // Provider Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isDebian = aiServerUrlInput == PrivateAiApiClient.DEFAULT_SERVER_URL
                        FilterChip(
                            selected = isDebian,
                            onClick = {
                                aiServerUrlInput = PrivateAiApiClient.DEFAULT_SERVER_URL
                                aiModelInput = PrivateAiApiClient.DEFAULT_MODEL
                            },
                            label = { Text("Debian 13 Tailscale", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = !isDebian,
                            onClick = { /* keep custom */ },
                            label = { Text("कस्टम API", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = aiServerUrlInput,
                        onValueChange = { aiServerUrlInput = it },
                        label = { Text("Server Base URL (Tailscale HTTPS)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = aiModelInput,
                        onValueChange = { aiModelInput = it },
                        label = { Text("Loaded Model Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Test Result Display
                    aiTestResult?.let { res ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (res.isConnected) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (res.isConnected) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (res.isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (res.isConnected) Color(0xFF15803D) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = res.message,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.isConnected) Color(0xFF15803D) else Color(0xFFDC2626)
                                    )
                                    if (res.modelName != null) {
                                        Text(
                                            text = "Model: ${res.modelName}",
                                            fontSize = 11.sp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingAi = true
                                aiTestResult = null
                                coroutineScope.launch {
                                    // Temporarily set url to test
                                    aiClient.serverUrl = aiServerUrlInput
                                    aiClient.configuredModel = aiModelInput
                                    val result = aiClient.testConnection()
                                    aiTestResult = result
                                    if (result.modelName != null) {
                                        aiModelInput = result.modelName
                                    }
                                    isTestingAi = false
                                }
                            },
                            enabled = !isTestingAi && aiEnabled,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (isTestingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TESTING...", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD97706))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AUTO-DETECT & TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                aiClient.isAiEnabled = aiEnabled
                                aiClient.serverUrl = aiServerUrlInput
                                aiClient.configuredModel = aiModelInput
                                android.widget.Toast.makeText(context, "AI सेटिंग्स सुरक्षित सहेजी गईं ✓", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 9. DANGER ZONE: FACTORY RESET / FORMAT APP (V9.0)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer.copy(alpha = 0.25f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Factory Reset / Format App (डेटा रीसेट)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = "ऐप को नए उपयोगकर्ता के लिए साफ़ करने हेतु सभी लेनदेन रीसेट करें। रीसेट करने से पहले स्वचालित सुरक्षा बैकअप अनिवार्य रूप से सुरक्षित किया जाएगा।",
                        fontSize = 11.5.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            showFormatConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FORMAT ALL APP DATA (30s SAFETY)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. APP VERSION FOOTER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Shahbuddin Dairy V8.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colorScheme.primary
                )
                Text(
                    text = "Native Android Material 3 Edition • Zero Data Loss Protection",
                    fontSize = 11.5.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // BACKUP SUCCESS DIALOG
    uiState.backupResult?.let { res ->
        AlertDialog(
            onDismissRequest = { viewModel.clearBackupResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (res.success) colorScheme.primary else colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (res.success) "Backup Verified ✓" else "Backup Error", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(res.message)
                    if (res.success && res.stats != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• File: ${res.fileName}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text("• Size: %,d bytes".format(res.sizeBytes), fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text("• Customers: ${res.stats.customers}", fontSize = 12.sp)
                        Text("• Milk Deliveries: ${res.stats.deliveries} (%.2f L)".format(res.stats.totalLiters), fontSize = 12.sp)
                        Text("• Payments: ${res.stats.payments} (${res.stats.onlinePaymentsCount} Online)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                if (res.success && res.uri != null) {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, res.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Backup File"))
                            viewModel.clearBackupResult()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE BACKUP")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.clearBackupResult() }, shape = RoundedCornerShape(8.dp)) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // RESTORE CONFIRMATION DIALOG
    uiState.pendingRestoreValidation?.let { valRes ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            title = {
                Text(
                    text = if (valRes.isValid) "Validate & Confirm Restore" else "Invalid Backup File",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (!valRes.isValid) {
                    Text(valRes.error ?: "File could not be validated", color = colorScheme.error)
                } else {
                    val stats = valRes.stats!!
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Backup file verified valid ✓", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Customers: ${stats.customers}", fontSize = 12.5.sp)
                        Text("• Milk Deliveries: ${stats.deliveries} (%.2f L)".format(stats.totalLiters), fontSize = 12.5.sp)
                        Text("• Payments: ${stats.payments} (Total: ₹%,d)".format(stats.totalPaid.toLong()), fontSize = 12.5.sp)
                        if (stats.routesCount > 0) Text("• Routes: ${stats.routesCount}", fontSize = 12.5.sp)
                        if (stats.notesCount > 0) Text("• Customer Notes: ${stats.notesCount}", fontSize = 12.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "An automatic safety snapshot of current data will be saved before restoring.",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (valRes.isValid) {
                    Button(
                        onClick = { viewModel.executeRestore(context) },
                        enabled = !uiState.isRestoring,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        if (uiState.isRestoring) {
                            CircularProgressIndicator(color = colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restoring...")
                        } else {
                            Text("RESTORE NOW", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissRestoreDialog() }, shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // RESTORE RESULT DIALOG
    uiState.restoreResult?.let { res ->
        AlertDialog(
            onDismissRequest = { viewModel.clearRestoreResult() },
            title = { Text(if (res.success) "Restore Finished" else "Restore Failed", fontWeight = FontWeight.Bold) },
            text = { Text(res.message) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearRestoreResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // REVERT SAFETY SNAPSHOT CONFIRMATION DIALOG
    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { showRevertConfirm = false },
            title = { Text("Revert to Safety Snapshot?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will rollback the database to the snapshot taken before the last restore.\n\n" +
                            "Are you sure you want to revert?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revertSafetyBackup(context)
                        showRevertConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAmber)
                ) {
                    Text("REVERT NOW", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRevertConfirm = false }, shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // 1. ENABLE PIN DIALOG
    if (showEnablePinDialog) {
        AlertDialog(
            onDismissRequest = { showEnablePinDialog = false },
            title = { Text("Set 4-Digit App Lock PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a 4-digit numeric PIN to secure the app.", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = pinSetupInput,
                        onValueChange = { if (it.length <= 4) pinSetupInput = it },
                        label = { Text("Enter 4-Digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = { if (it.length <= 4) pinConfirmInput = it },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    pinErrorText?.let {
                        Text(it, color = colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinSetupInput.length != 4) {
                            pinErrorText = "PIN must be exactly 4 digits"
                        } else if (pinSetupInput != pinConfirmInput) {
                            pinErrorText = "PINs do not match"
                        } else {
                            val ok = viewModel.setAppLock(true, pin = pinSetupInput)
                            if (ok) {
                                showEnablePinDialog = false
                            } else {
                                pinErrorText = "Failed to set PIN"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("ENABLE LOCK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEnablePinDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // 2. CHANGE PIN DIALOG
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change App Lock PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4) currentPinInput = it },
                        label = { Text("Current 4-Digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4) newPinInput = it },
                        label = { Text("New 4-Digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = { if (it.length <= 4) pinConfirmInput = it },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    pinErrorText?.let {
                        Text(it, color = colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPinInput.length != 4) {
                            pinErrorText = "Enter current 4-digit PIN"
                        } else if (newPinInput.length != 4) {
                            pinErrorText = "New PIN must be exactly 4 digits"
                        } else if (newPinInput != pinConfirmInput) {
                            pinErrorText = "New PINs do not match"
                        } else {
                            val ok = viewModel.changeAppLockPin(currentPinInput, newPinInput)
                            if (ok) {
                                showChangePinDialog = false
                            } else {
                                pinErrorText = "Current PIN is incorrect"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("SAVE NEW PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // 3. DISABLE PIN DIALOG
    if (showDisablePinDialog) {
        AlertDialog(
            onDismissRequest = { showDisablePinDialog = false },
            title = { Text("Disable App Lock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter current PIN to confirm disabling App Lock.", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4) currentPinInput = it },
                        label = { Text("Current 4-Digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    pinErrorText?.let {
                        Text(it, color = colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = viewModel.setAppLock(false, currentPin = currentPinInput)
                        if (ok) {
                            showDisablePinDialog = false
                        } else {
                            pinErrorText = "Incorrect PIN"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) {
                    Text("DISABLE LOCK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDisablePinDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // 4. UPI QR PREVIEW DIALOG
    if (showQrPreviewDialog) {
        val upiUri = remember(upiInput, payeeInput) {
            UpiQrGenerator.buildUpiUri(upiId = upiInput, payeeName = payeeInput)
        }
        val qrBitmap = remember(upiUri) {
            UpiQrGenerator.generateUpiQrBitmap(upiUri, size = 480)
        }

        AlertDialog(
            onDismissRequest = { showQrPreviewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Business UPI QR", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = payeeInput.ifBlank { "Shahbuddin Dairy" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "UPI: $upiInput",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI QR Code",
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(8.dp)
                            )
                        }
                    } else {
                        Text("QR code could not be rendered", color = colorScheme.error, fontSize = 12.sp)
                    }

                    Text(
                        text = "Offline UPI QR • All UPI Apps Supported (PhonePe, GPay, Paytm, BHIM)",
                        fontSize = 10.5.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Payment to ${payeeInput.ifBlank { "Shahbuddin Dairy" }}\nUPI ID: $upiInput\nPay Link: $upiUri"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share UPI Details"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SHARE UPI")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showQrPreviewDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }

    // FACTORY RESET 30s COUNTDOWN DIALOG (V9.0)
    if (showFormatConfirmDialog) {
        val canConfirm = formatTimerSeconds == 0 && formatConfirmInput.trim().equals("FORMAT", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { showFormatConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚠️ Format App & Factory Reset", color = colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "चेतावनी: यह क्रिया सभी दूध बिक्री, भुगतान व लेनदेन का डेटा पूरी तरह साफ़ कर देगी!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.error
                    )
                    Text(
                        text = "चिंता न करें: रीसेट से पूर्व एक स्वचालित पूर्ण बैकअप 'Downloads/ShahbuddinDairy_Backup_*.json' में स्वतः सहेज लिया जाएगा।",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "पुष्टि के लिए नीचे 'FORMAT' लिखें:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = formatConfirmInput,
                        onValueChange = { formatConfirmInput = it },
                        placeholder = { Text("FORMAT टाइप करें") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.error,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (formatTimerSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⏳ सुरक्षा प्रतीक्षा: ${formatTimerSeconds} सेकंड शेष...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.formatAllData(context) { success ->
                            showFormatConfirmDialog = false
                        }
                    },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) {
                    Text(
                        text = if (formatTimerSeconds > 0) "WAIT (${formatTimerSeconds}s)" else "CONFIRM RESET",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFormatConfirmDialog = false }) {
                    Text("रद्द करें (Cancel)")
                }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = colorScheme.surface
        )
    }
}
