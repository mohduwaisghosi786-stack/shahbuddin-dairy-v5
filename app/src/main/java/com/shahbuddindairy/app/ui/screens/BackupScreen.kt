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
    var upiInput by remember(uiState.upiId) { mutableStateOf(uiState.upiId) }
    var showRevertConfirm by remember { mutableStateOf(false) }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backup & Data Protection (डेटा बैकअप)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Creates verified physical backups with all customers, milk history, payments, and settings in Downloads.",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Last Backup: ${uiState.lastBackupTime}",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    if (uiState.lastBackupSize > 0) {
                        Text(
                            text = "• Size: %,d KB (%d backups taken)".format(uiState.lastBackupSize / 1024, uiState.backupCount),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

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

            // 6. DAIRY SETTINGS CARD
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
                        Icon(Icons.Default.Settings, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dairy Settings (डेयरी सेटिंग्स)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = payeeInput,
                        onValueChange = { payeeInput = it },
                        label = { Text("Dairy Name / Payee") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Dairy Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text("UPI ID (Optional, for WhatsApp receipts)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.saveSettings(payeeInput, phoneInput, upiInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE SETTINGS", fontWeight = FontWeight.Bold)
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
                    text = "Shahbuddin Dairy V7.5",
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
}
