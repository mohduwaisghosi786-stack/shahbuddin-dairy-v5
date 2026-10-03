package com.shahbuddindairy.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.core.content.FileProvider
import com.shahbuddindairy.app.receipt.ReceiptImageGenerator
import com.shahbuddindairy.app.receipt.UpiQrGenerator
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.ui.components.BillingTypeBadge
import com.shahbuddindairy.app.ui.components.EmptyState
import com.shahbuddindairy.app.ui.components.MilkTypeBadge
import com.shahbuddindairy.app.ui.components.RateBadge
import com.shahbuddindairy.app.ui.theme.DairyGreenDark
import com.shahbuddindairy.app.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: CustomerDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var selectedTab by remember { mutableIntStateOf(0) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPbsDialog by remember { mutableStateOf(false) }
    var editingPbsText by remember { mutableStateOf("") }
    var itemToVoid by remember { mutableStateOf<KhataTimelineItem?>(null) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showStatementDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.customer?.name ?: "Customer Khata",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        uiState.customer?.let {
                            Text(
                                text = "#${it.id} • 📍 ${uiState.routeName} • ${it.mohalla}${if (it.landmark.isNotBlank()) " • ${it.landmark}" else ""}",
                                fontSize = 11.5.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportKhataPdf(context) }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = colorScheme.primary
                        )
                    }
                    if (!uiState.customer?.phone.isNullOrBlank()) {
                        IconButton(onClick = {
                            uiState.customer?.let { cust ->
                                shareWhatsAppPaymentReminder(
                                    context = context,
                                    customer = cust,
                                    balance = uiState.currentBalance,
                                    upiId = uiState.upiId,
                                    payeeName = uiState.payeeName
                                )
                            }
                        }) {
                            Text("💬", fontSize = 16.sp)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${uiState.customer?.phone}"))
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading || uiState.customer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else {
            val customer = uiState.customer!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(colorScheme.background)
            ) {
                // 1. HERO BALANCE CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.currentBalance > 0)
                            colorScheme.errorContainer.copy(alpha = 0.5f)
                        else
                            colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.currentBalance >= 0) "APP OUTSTANDING DUE" else "APP ADVANCE BALANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.currentBalance > 0) colorScheme.error else colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = CalculationEngine.formatMoney(if (uiState.currentBalance >= 0) uiState.currentBalance else -uiState.currentBalance),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.currentBalance > 0) colorScheme.error else colorScheme.primary
                                )
                            }

                            // External PBS / Manual Book Pending Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.surface.copy(alpha = 0.9f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    editingPbsText = if (uiState.pbsPending > 0) "%.0f".format(java.util.Locale.US, uiState.pbsPending) else ""
                                    showPbsDialog = true
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "BOOK / PBS",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC2410C)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit PBS",
                                            tint = Color(0xFFC2410C),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Text(
                                        text = CalculationEngine.formatMoney(uiState.pbsPending),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.pbsPending > 0) Color(0xFFC2410C) else colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "बाहरी कॉपी बकाया",
                                        fontSize = 8.5.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Delivered: %.2f L".format(uiState.totalLitersDelivered),
                                fontSize = 12.sp,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Billed: ₹%,d".format(uiState.totalBilled.toLong()),
                                fontSize = 12.sp,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Paid: ₹%,d".format(uiState.totalPaid.toLong()),
                                fontSize = 12.sp,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // PINNED NOTE BANNER
                uiState.pinnedNote?.let { pNote ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clickable { selectedTab = 3 }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📌", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${pNote.category}: ${pNote.content}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ONE-TAP QUICK PAYMENT RECONCILIATION
                if (uiState.currentBalance > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ One-Tap Pay:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.clickable {
                                viewModel.recordOneTapPayment(uiState.currentBalance)
                            }
                        ) {
                            Text(
                                text = "Pay Full ₹%.0f".format(uiState.currentBalance),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (uiState.currentBalance >= 100.0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable {
                                    viewModel.recordOneTapPayment(100.0)
                                }
                            ) {
                                Text(
                                    text = "Pay ₹100",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (uiState.currentBalance >= 500.0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable {
                                    viewModel.recordOneTapPayment(500.0)
                                }
                            ) {
                                Text(
                                    text = "Pay ₹500",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 2. CUSTOMER 360° ACTIONS BAR (Section 10)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ PAY", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showUpiQrDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(15.dp), tint = colorScheme.primary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("UPI QR", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = { showReceiptDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp), tint = colorScheme.primary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("RECEIPT", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showStatementDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp), tint = colorScheme.primary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("MONTH", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                    }
                }

                // SMART CUSTOMER TAGS BAR (Section 12)
                val tagsScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tagsScroll)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { showTagsDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏷️ Tags (${uiState.tags.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                    }

                    if (uiState.tags.isEmpty()) {
                        Text(
                            text = "Tap to add Monthly, Buffalo, VIP tags...",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colorScheme.primaryContainer,
                                modifier = Modifier.clickable { viewModel.toggleCustomerTag(tag) }
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // 3. TABS: KHATA TIMELINE / MILK / PAYMENTS
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Khata Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Milk (${uiState.milkDeliveries.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Payments (${uiState.payments.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Notes (${uiState.customerNotes.size})", fontSize = 12.sp) }
                    )
                }

                // 4. TAB CONTENT
                when (selectedTab) {
                    0 -> {
                        // Chronological Ledger Timeline
                        if (uiState.timeline.isEmpty()) {
                            EmptyState(
                                title = "No Transactions",
                                message = "No deliveries or payments recorded for this customer yet."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.timeline, key = { it.id }) { item ->
                                    KhataTimelineCard(
                                        item = item,
                                        onVoid = { itemToVoid = item }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Deliveries history
                        if (uiState.milkDeliveries.isEmpty()) {
                            EmptyState(
                                title = "No Milk Deliveries",
                                message = "No milk delivery history recorded for this customer."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.milkDeliveries, key = { it.id }) { d ->
                                    DeliveryHistoryCard(delivery = d)
                                }
                            }
                        }
                    }
                    2 -> {
                        // Payments history
                        if (uiState.payments.isEmpty()) {
                            EmptyState(
                                title = "No Payments",
                                message = "No payment records found for this customer."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.payments, key = { it.id }) { p ->
                                    PaymentHistoryCard(payment = p)
                                }
                            }
                        }
                    }
                    3 -> {
                        // Customer Notes Tab
                        CustomerNotesTabContent(
                            notes = uiState.customerNotes,
                            onAddNote = { content, cat, isPinned ->
                                viewModel.saveNote(content, cat, isPinned)
                            },
                            onDeleteNote = { id -> viewModel.deleteNote(id) },
                            onTogglePin = { note -> viewModel.togglePinNote(note) }
                        )
                    }
                }
            }
        }

        // PDF EXPORT RESULT DIALOG
        uiState.pdfExportResult?.let { res ->
            AlertDialog(
                onDismissRequest = { viewModel.clearPdfResult() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (res.success) colorScheme.primary else colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (res.success) "PDF Generated" else "PDF Error", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(res.message)
                        if (res.success && res.fileName != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Saved to Downloads/${res.fileName}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            Text("Size: %,d KB".format(res.sizeBytes / 1024), fontSize = 11.5.sp, color = colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    if (res.success && res.uri != null) {
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, res.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Khata PDF"))
                                viewModel.clearPdfResult()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
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

        // RECORD PAYMENT DIALOG
        if (showPaymentDialog) {
            RecordPaymentDialog(
                onDismiss = { showPaymentDialog = false },
                onSave = { amount, mode, note ->
                    viewModel.recordDirectPayment(amount, mode, note)
                    showPaymentDialog = false
                    Toast.makeText(context, "Payment ₹$amount recorded ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // EDIT CUSTOMER DIALOG
        if (showEditDialog && uiState.customer != null) {
            val c = uiState.customer!!
            EditCustomerDialog(
                customer = c,
                onDismiss = { showEditDialog = false },
                onSave = { name, phone, mohalla, landmark, liters, rate, bType, freq ->
                    viewModel.updateCustomerDetails(name, phone, mohalla, landmark, liters, rate, bType, freq)
                    showEditDialog = false
                    Toast.makeText(context, "Customer details updated ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // EDIT BOOK / PBS PENDING DIALOG
        if (showPbsDialog && uiState.customer != null) {
            val cust = uiState.customer!!
            AlertDialog(
                onDismissRequest = { showPbsDialog = false },
                title = {
                    Text(
                        text = "Manual Book / PBS Pending",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Customer #${cust.id}: ${cust.name}\n(बाहरी कॉपी का बकाया — डिजिटल खाते से अलग रहेगा)",
                            fontSize = 12.5.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editingPbsText,
                            onValueChange = { editingPbsText = it },
                            label = { Text("Manual Copy Pending (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colorScheme.surfaceContainer,
                                unfocusedContainerColor = colorScheme.surfaceContainer,
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = editingPbsText.toDoubleOrNull() ?: 0.0
                            viewModel.updatePbsPending(amount)
                            showPbsDialog = false
                            Toast.makeText(context, "PBS Pending updated ✓", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Save (सुरक्षित करें)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPbsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // CONFIRM VOID TRANSACTION DIALOG
        itemToVoid?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToVoid = null },
                title = { Text("Void / Reverse Transaction?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to void this ${item.type.lowercase()} transaction on ${CalculationEngine.formatDateDisplay(item.date)}?\n\n" +
                                "This will mark it as voided without deleting history."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.voidTransaction(item)
                            itemToVoid = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("VOID TRANSACTION", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { itemToVoid = null }, shape = RoundedCornerShape(8.dp)) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                containerColor = colorScheme.surface
            )
        }

        // CUSTOMER 360° UPI QR DIALOG (Section 35 & 38)
        if (showUpiQrDialog && uiState.customer != null) {
            CustomerDueQrDialog(
                customer = uiState.customer!!,
                balance = uiState.currentBalance,
                upiId = uiState.upiId,
                payeeName = uiState.payeeName,
                onDismiss = { showUpiQrDialog = false }
            )
        }

        // CUSTOMER 360° RECEIPT OPTIONS DIALOG (Section 36, 37, 39)
        if (showReceiptDialog && uiState.customer != null) {
            val receiptData = viewModel.getReceiptData()
            CustomerReceiptOptionsDialog(
                receiptData = receiptData,
                upiId = uiState.upiId,
                onDismiss = { showReceiptDialog = false },
                onExportPdf = { viewModel.exportSingleReceiptPdf(context, receiptData) }
            )
        }

        // MONTHLY CUSTOMER STATEMENT DIALOG (Section 40)
        if (showStatementDialog && uiState.customer != null) {
            CustomerMonthlyStatementDialog(
                customer = uiState.customer!!,
                onDismiss = { showStatementDialog = false },
                onExportPdf = { month -> viewModel.exportMonthlyStatementPdf(context, month) }
            )
        }

        // SMART CUSTOMER TAGS DIALOG (Section 12)
        if (showTagsDialog) {
            CustomerTagsDialog(
                activeTags = uiState.tags,
                onToggleTag = { tag -> viewModel.toggleCustomerTag(tag) },
                onDismiss = { showTagsDialog = false }
            )
        }
    }
}

@Composable
fun KhataTimelineCard(
    item: KhataTimelineItem,
    onVoid: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (item.isVoid) colorScheme.error.copy(alpha = 0.5f) else colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isVoid) colorScheme.errorContainer.copy(alpha = 0.2f) else colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header Row: Date, Badge, Void flag, Running Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CalculationEngine.formatDateDisplay(item.date),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (item.isVoid) colorScheme.onSurfaceVariant else colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (item.type == "PAYMENT") colorScheme.primaryContainer else colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.type == "PAYMENT") "PAYMENT" else "DELIVERY",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.type == "PAYMENT") colorScheme.primary else colorScheme.secondary
                        )
                    }
                    if (item.isVoid) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[VOIDED]",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.error
                        )
                    }
                }

                if (!item.isVoid) {
                    Text(
                        text = "Bal: ${CalculationEngine.formatMoney(item.runningBalance)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (item.runningBalance > 0) colorScheme.error else colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body Row: Description & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.description,
                        fontSize = 12.5.sp,
                        color = if (item.isVoid) colorScheme.onSurfaceVariant else colorScheme.onSurface
                    )
                    if (item.note.isNotBlank() && item.note != "Daily Delivery" && item.note != "Payment") {
                        Text(
                            text = "Note: ${item.note}",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (item.billAmount > 0) {
                        Text(
                            text = "Bill: ₹%.0f".format(item.billAmount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isVoid) colorScheme.onSurfaceVariant else colorScheme.onSurface
                        )
                    }
                    if (item.paidAmount > 0) {
                        Text(
                            text = "Paid: ₹%.0f".format(item.paidAmount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isVoid) colorScheme.onSurfaceVariant else colorScheme.primary
                        )
                    }
                }
            }

            // Void button if not yet voided
            if (!item.isVoid) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "Void / Reverse",
                        fontSize = 10.5.sp,
                        color = colorScheme.error,
                        modifier = Modifier
                            .clickable { onVoid() }
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryHistoryCard(delivery: DeliveryEntity) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = CalculationEngine.formatDateDisplay(delivery.date),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = if (delivery.status == "skipped") "NAGA (Skipped)" else "%.2f L (${delivery.milkType}) @ ₹%.0f/L".format(delivery.liters, delivery.rate),
                    fontSize = 12.sp,
                    color = if (delivery.status == "skipped") colorScheme.error else colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Bill: ${CalculationEngine.formatMoney(delivery.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = colorScheme.onSurface
                )
                if (delivery.totalPaid > 0) {
                    Text(
                        text = "Paid: ${CalculationEngine.formatMoney(delivery.totalPaid)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(payment: PaymentEntity) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = CalculationEngine.formatDateDisplay(payment.date),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Mode: ${payment.paymentMode} ${if (payment.note.isNotBlank()) "• ${payment.note}" else ""}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = CalculationEngine.formatMoney(payment.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colorScheme.primary
            )
        }
    }
}

@Composable
fun RecordPaymentDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Customer Payment", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Payment Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "Cash",
                        onClick = { mode = "Cash" },
                        label = { Text("Cash") }
                    )
                    FilterChip(
                        selected = mode == "Online",
                        onClick = { mode = "Online" },
                        label = { Text("Online / UPI") }
                    )
                    FilterChip(
                        selected = mode == "Adjustment",
                        onClick = { mode = "Adjustment" },
                        label = { Text("Adjustment") }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Remark (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(amt, mode, note)
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RECORD PAYMENT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}

@Composable
fun EditCustomerDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var mohalla by remember { mutableStateOf(customer.mohalla) }
    var landmark by remember { mutableStateOf(customer.landmark) }
    var defaultLitersStr by remember { mutableStateOf(customer.defaultLiters.toString()) }
    var rateStr by remember { mutableStateOf(customer.ratePerLiter.toString()) }
    var billingType by remember { mutableStateOf(customer.billingType) }
    var deliveryFrequency by remember { mutableStateOf(customer.deliveryFrequency) }
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Customer Details", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mohalla,
                        onValueChange = { mohalla = it },
                        label = { Text("Mohalla") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = landmark,
                        onValueChange = { landmark = it },
                        label = { Text("Landmark") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultLitersStr,
                        onValueChange = { defaultLitersStr = it },
                        label = { Text("Default (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Rate (₹/L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = billingType == "daily_cash",
                        onClick = { billingType = "daily_cash" },
                        label = { Text("Daily Cash") }
                    )
                    FilterChip(
                        selected = billingType == "monthly",
                        onClick = { billingType = "monthly" },
                        label = { Text("Monthly") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = deliveryFrequency == "daily",
                        onClick = { deliveryFrequency = "daily" },
                        label = { Text("Everyday") }
                    )
                    FilterChip(
                        selected = deliveryFrequency == "alternate",
                        onClick = { deliveryFrequency = "alternate" },
                        label = { Text("Alternate Day") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val l = defaultLitersStr.toDoubleOrNull() ?: customer.defaultLiters
                        val r = rateStr.toDoubleOrNull() ?: customer.ratePerLiter
                        onSave(name.trim(), phone.trim(), mohalla.trim(), landmark.trim(), l, r, billingType, deliveryFrequency)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}

@Composable
private fun CustomerNotesTabContent(
    notes: List<CustomerNoteEntity>,
    onAddNote: (String, String, Boolean) -> Unit,
    onDeleteNote: (String) -> Unit,
    onTogglePin: (CustomerNoteEntity) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var newNoteText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    var isPinned by remember { mutableStateOf(false) }
    val categories = listOf("General", "Delivery", "Payment", "Timing")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Add Note Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Add Note for Customer (टिप्पणी जोड़ें)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    placeholder = { Text("e.g. सुबह 6 बजे milk देना है, gate के पीछे...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surfaceContainer,
                        unfocusedContainerColor = colorScheme.surfaceContainer
                    )
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isPinned = !isPinned }
                    ) {
                        Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                        Text("Pin note to top (पिन करें)", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            if (newNoteText.isNotBlank()) {
                                onAddNote(newNoteText.trim(), selectedCategory, isPinned)
                                newNoteText = ""
                                isPinned = false
                            }
                        },
                        enabled = newNoteText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Note", fontSize = 12.sp)
                    }
                }
            }
        }

        // List Notes
        if (notes.isEmpty()) {
            EmptyState(
                title = "No Notes",
                message = "No special notes or delivery instructions added for this customer yet."
            )
        } else {
            notes.forEach { note ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (note.isPinned) Color(0xFFFEF3C7) else colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (note.isPinned) Color(0xFFF59E0B) else colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (note.isPinned) {
                                    Text("📌 ", fontSize = 11.sp)
                                }
                                Text(
                                    text = note.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (note.isPinned) Color(0xFFB45309) else colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = note.createdAt.take(10),
                                    fontSize = 10.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = note.content,
                                fontSize = 13.5.sp,
                                color = if (note.isPinned) Color(0xFF78350F) else colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onTogglePin(note) }) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (note.isPinned) Color(0xFFD97706) else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteNote(note.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerReceiptOptionsDialog(
    receiptData: com.shahbuddindairy.app.receipt.ReceiptData,
    upiId: String,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Generate Payment Receipt", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("#${receiptData.customerId} ${receiptData.customerName} • ${CalculationEngine.formatDateDisplay(receiptData.date)}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Digital Image Receipt
                Button(
                    onClick = {
                        val upiUri = if (upiId.isNotBlank()) UpiQrGenerator.buildUpiUri(upiId, receiptData.businessName, receiptData.remainingDue) else null
                        val qrBitmap = upiUri?.let { UpiQrGenerator.generateUpiQrBitmap(it, 250) }
                        val bmp = ReceiptImageGenerator.createReceiptBitmap(receiptData, qrBitmap)
                        val file = ReceiptImageGenerator.saveReceiptImage(context, bmp, receiptData.customerId)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri as android.os.Parcelable)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt Image"))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DIGITAL IMAGE RECEIPT (With QR)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // PDF Receipt
                OutlinedButton(
                    onClick = {
                        onExportPdf()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SINGLE RECEIPT PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp Text Receipt
                OutlinedButton(
                    onClick = {
                        val dueStr = if (receiptData.remainingDue > 0) "₹${CalculationEngine.formatMoney(receiptData.remainingDue)}" else "₹0 (खाता चुकता)"
                        val text = "🧾 *शाहबुद्दीन डेयरी (Shahbuddin Dairy)*\n" +
                                "ग्राहक: #${receiptData.customerId} ${receiptData.customerName}\n" +
                                "दिनांक: ${receiptData.date}\n" +
                                "दूध: ${receiptData.milkLiters}L (${receiptData.milkType}) @ ₹${receiptData.rate}\n" +
                                "आज का बिल: ₹${receiptData.billAmount}\n" +
                                "पिछला बकाया: ₹${receiptData.previousDue}\n" +
                                "जमा: ₹${receiptData.totalPaid} (${if (receiptData.onlinePaid > 0) "ऑनलाइन" else "नकद"})\n" +
                                "*कुल शेष बकाया: $dueStr*\n" +
                                (if (upiId.isNotBlank()) "UPI ID: $upiId\n" else "") +
                                "धन्यवाद।"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        shareIntent.setPackage("com.whatsapp")
                        try {
                            context.startActivity(shareIntent)
                        } catch (_: Exception) {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }, "Share Receipt via WhatsApp"))
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("💬 WHATSAPP TEXT RECEIPT", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun CustomerMonthlyStatementDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onExportPdf: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val currentMonth = CalculationEngine.todayStr().take(7)
    val parts = currentMonth.split("-")
    val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 10
    val prevMonth = if (m <= 1) "%04d-12".format(y - 1) else "%04d-%02d".format(y, m - 1)
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Monthly Customer Statement", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("#${customer.id} ${customer.name}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Billing Month (माह चुनें):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMonth == currentMonth,
                        onClick = { selectedMonth = currentMonth },
                        label = { Text("Current ($currentMonth)", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedMonth == prevMonth,
                        onClick = { selectedMonth = prevMonth },
                        label = { Text("Previous ($prevMonth)", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        onExportPdf(selectedMonth)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GENERATE MONTHLY PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun CustomerTagsDialog(
    activeTags: Set<String>,
    onToggleTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val predefinedTags = listOf("Monthly", "Cash", "Due", "Advance", "Buffalo", "Cow", "Morning", "Evening", "VIP")
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Customer Tags", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "टैग से ग्राहकों को आसानी से पहचानें (वित्तीय रिकॉर्ड अप्रभावित रहेगा):",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    predefinedTags.chunked(3).forEach { rowTags ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowTags.forEach { tag ->
                                val isSelected = activeTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleTag(tag) },
                                    label = { Text(tag, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colorScheme.primary,
                                        selectedLabelColor = colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Done")
            }
        },
        shape = RoundedCornerShape(14.dp)
    )
}

