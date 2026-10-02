package com.shahbuddindairy.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
                                text = "#${it.id} • ${it.mohalla}${if (it.landmark.isNotBlank()) " • ${it.landmark}" else ""}",
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

                // 2. QUICK ACTIONS BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ PAYMENT", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.exportKhataPdf(context) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 12.5.sp)
                    }

                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
