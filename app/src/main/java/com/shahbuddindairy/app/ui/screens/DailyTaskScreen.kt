package com.shahbuddindairy.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import java.net.URLEncoder
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.shahbuddindairy.app.ai.VoiceFeedbackManager
import com.shahbuddindairy.app.ai.PrivateAiApiClient
import com.shahbuddindairy.app.whatsapp.WhatsAppReportManager
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Chat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.shahbuddindairy.app.receipt.UpiQrGenerator
import com.shahbuddindairy.app.receipt.ReceiptData
import com.shahbuddindairy.app.receipt.ReceiptImageGenerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import com.shahbuddindairy.app.data.model.RouteEntity
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.notification.DailyTaskNotificationManager
import com.shahbuddindairy.app.ui.components.AppTopHeader
import com.shahbuddindairy.app.ui.components.EmptyState
import com.shahbuddindairy.app.ui.components.MilkTypeBadge
import com.shahbuddindairy.app.ui.components.RateBadge
import com.shahbuddindairy.app.ui.components.StatCard
import com.shahbuddindairy.app.ui.components.StatusBadge
import com.shahbuddindairy.app.ui.components.TransactionConfirmDialog
import com.shahbuddindairy.app.ui.theme.StatusRed
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DailyTaskScreen(
    viewModel: DailyTaskViewModel,
    onNavigateToCustomerKhata: (Long) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingConfirmItem by viewModel.pendingConfirmItem.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    // Immersive Fullscreen Mode: hide system status bar in Runner Mode, restore on exit
    val activity = context as? Activity
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    var activeCustomerId by remember { mutableStateOf<Long?>(null) }
    var showCustomerListSheet by remember { mutableStateOf(false) }
    var showCashCustomerDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showQuickCustomerDialog by remember { mutableStateOf(false) }
    var customerForNotes by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showRouteManagerDialog by remember { mutableStateOf(false) }

    val savedRunnerCustomerId by viewModel.activeRunnerCustomerId.collectAsState()
    LaunchedEffect(savedRunnerCustomerId) {
        if (savedRunnerCustomerId != null && activeCustomerId == null) {
            activeCustomerId = savedRunnerCustomerId
        }
    }

    var runnerMoreCustomerItem by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var runnerQrCustomerItem by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var runnerReceiptCustomerItem by remember { mutableStateOf<DailyCustomerCardItem?>(null) }

    var editingDueCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var editingAdvanceCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var editingMasterCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var overridingBillCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var editingMilkCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var pbsManagerCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var runnerCashConfirmItem by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var showAiMunshiDialog by remember { mutableStateOf(false) }
    var customRateDialogCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var deliveredAnimCustomer by remember { mutableStateOf<String?>(null) }
    var aiDeliveryNoteCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var aiCustomerCleanCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }
    var faceToFaceReminderCustomer by remember { mutableStateOf<DailyCustomerCardItem?>(null) }

    LaunchedEffect(deliveredAnimCustomer) {
        if (deliveredAnimCustomer != null) {
            kotlinx.coroutines.delay(900)
            deliveredAnimCustomer = null
        }
    }

    // Synchronize ongoing delivery route notification
    LaunchedEffect(uiState.selectedDate, uiState.completedCount) {
        DailyTaskNotificationManager.updateNotification(context)
    }

    // Live clock for Header
    var currentTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeStr = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
            kotlinx.coroutines.delay(10000)
        }
    }

    // Voice Feedback & AI Clients
    val voiceManager = remember { VoiceFeedbackManager.getInstance(context) }
    val aiClient = remember { PrivateAiApiClient(context) }
    var aiPingLatency by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        aiPingLatency = aiClient.checkPingLatency()
    }

    // Runner 2.0 Visual Navigation States (Panels 1, 2, 3, 4)
    var showSideTray by remember { mutableStateOf(false) }
    var showQuickJumpDialog by remember { mutableStateOf(false) }
    var showGuidelineDialog by remember { mutableStateOf(false) }
    var scrubberBubbleText by remember { mutableStateOf<String?>(null) }
    var skippedCustomerIds by remember { mutableStateOf(listOf<Long>()) }

    // Runner Queue Filtering (Exclude Alternate Off, move skipped to end of queue)
    val allCards = uiState.customerCards
    val runnerQueueCards = remember(allCards) { allCards.filter { !it.isAlternateOff } }
    val baseCards = if (runnerQueueCards.isNotEmpty()) runnerQueueCards else allCards
    val cards = remember(baseCards, skippedCustomerIds) {
        if (skippedCustomerIds.isEmpty()) baseCards
        else {
            val nonSkipped = baseCards.filter { it.customer.id !in skippedCustomerIds }
            val skipped = baseCards.filter { it.customer.id in skippedCustomerIds }
            nonSkipped + skipped
        }
    }
    val currentItem = cards.find { it.customer.id == activeCustomerId } ?: allCards.find { it.customer.id == activeCustomerId } ?: cards.firstOrNull()
    val currentIndex = if (currentItem != null) cards.indexOf(currentItem).coerceAtLeast(0) else 0
    if (activeCustomerId != currentItem?.customer?.id && currentItem != null) {
        activeCustomerId = currentItem.customer.id
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // 1. RUNNER MODE TOP HEADER (Date, Time, Title, [📋 ग्राहक सूची], [+ नकद ग्राहक])
        Surface(
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "वापस (Back)",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "रनर मोड",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            // Live AI Ping Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (aiPingLatency != null) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, if (aiPingLatency != null) Color(0xFF86EFAC) else Color(0xFFFCD34D)),
                                modifier = Modifier.clickable {
                                    coroutineScope.launch {
                                        aiPingLatency = aiClient.checkPingLatency()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (aiPingLatency != null) Color(0xFF15803D) else Color(0xFFD97706))
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (aiPingLatency != null) "Debian • ${aiPingLatency}ms" else "Local AI",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (aiPingLatency != null) Color(0xFF15803D) else Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onPreviousDay()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Day", tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${CalculationEngine.formatDateDisplay(uiState.selectedDate)} • $currentTimeStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onNextDay()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [⚡ जंप (Quick Jump Keypad)]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showQuickJumpDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("जंप", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                    }

                    // [📋 ग्राहक सूची (Side Tray)]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.5.dp, colorScheme.outlineVariant),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSideTray = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, tint = colorScheme.onSurface, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("सूची", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        }
                    }

                    // [+ नकद ग्राहक]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF15803D),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showCashCustomerDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("नकद", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // [❓ नियम / गाइड]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showGuidelineDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("गाइड", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // 2. LIVE MILK CAN TRACKER STRIP
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, if (uiState.isCanShortageWarning) Color(0xFFDC2626) else colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "गाड़ी/कैन: %.1fL बचा".format(uiState.remainingCanLiters),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isCanShortageWarning) Color(0xFFDC2626) else Color(0xFF1E40AF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "वितरित: %.1fL".format(uiState.totalMilk),
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val totalCards = uiState.customerCards.size
                        Text(
                            text = "${uiState.completedCount}/$totalCards पूर्ण",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₹%,d".format(uiState.totalCashCollected.toLong()),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                val canProgress = if (uiState.loadedCanLiters > 0) ((uiState.totalMilk / uiState.loadedCanLiters).toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { canProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (uiState.isCanShortageWarning) Color(0xFFDC2626) else Color(0xFF15803D),
                    trackColor = colorScheme.outlineVariant
                )
            }
        }

        // 3. QUEUE PROGRESS & MUNSHI AI ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentIndex > 0) {
                            val prevId = cards[currentIndex - 1].customer.id
                            activeCustomerId = prevId
                            viewModel.setActiveRunnerCustomerId(prevId)
                        }
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = if (currentIndex > 0) colorScheme.primary else colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "ग्राहक ${if (cards.isNotEmpty()) currentIndex + 1 else 0} / ${cards.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentIndex < cards.size - 1) {
                            val nextId = cards[currentIndex + 1].customer.id
                            activeCustomerId = nextId
                            viewModel.setActiveRunnerCustomerId(nextId)
                        }
                    },
                    enabled = currentIndex < cards.size - 1,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = if (currentIndex < cards.size - 1) colorScheme.primary else colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${uiState.pendingCount} बाकी • ${uiState.nagaCount} नागा",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAiMunshiDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("मुंशी AI", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(5.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF25D366),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        WhatsAppReportManager.sendNightlyClosingSummary(
                            context = context,
                            dateStr = CalculationEngine.formatDateDisplay(uiState.selectedDate),
                            totalMilkLiters = uiState.totalMilk,
                            totalCashCollected = uiState.totalCashCollected,
                            completedDeliveries = uiState.completedCount,
                            totalCustomers = uiState.customerCards.size,
                            nagaCount = uiState.nagaCount,
                            alternateOffCount = uiState.customerCards.count { it.isAlternateOff }
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "मालिक रिपोर्ट", tint = Color.White, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("🌙 बंद", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // TIER 1: TOP HORIZONTAL SPEED STRIP / STORY CAROUSEL (PANEL 3)
        if (cards.isNotEmpty()) {
            val horizontalStripListState = rememberLazyListState()
            LaunchedEffect(currentIndex) {
                if (currentIndex in cards.indices) {
                    val targetScroll = (currentIndex - 2).coerceAtLeast(0)
                    horizontalStripListState.animateScrollToItem(targetScroll)
                }
            }

            LazyRow(
                state = horizontalStripListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(cards) { idx, cardItem ->
                    val isSelected = cardItem.customer.id == currentItem?.customer?.id
                    val isDone = cardItem.isDone
                    val isNaga = cardItem.isNaga

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isSelected -> colorScheme.primary
                            isDone -> Color(0xFFDCFCE7)
                            isNaga -> Color(0xFFFEE2E2)
                            else -> colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            when {
                                isSelected -> colorScheme.primary
                                isDone -> Color(0xFF22C55E)
                                isNaga -> Color(0xFFEF4444)
                                else -> colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeCustomerId = cardItem.customer.id
                            viewModel.setActiveRunnerCustomerId(cardItem.customer.id)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${cardItem.routeIndex + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else colorScheme.onSurface
                            )
                            if (isDone) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else Color(0xFF15803D))
                            } else if (isNaga) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("✕", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }
        }

        // 4. MAIN RUNNER DELIVERY CARD
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else if (allCards.isEmpty()) {
            EmptyState(
                title = "कोई ग्राहक नहीं मिला",
                message = "आज के लिए कोई ग्राहक नहीं है।"
            )
        } else if (currentItem != null) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                DailyTaskRunnerCard(
                    item = currentItem,
                    currentIndex = currentIndex,
                    totalCount = cards.size,
                    completedCount = uiState.completedCount,
                    pendingCount = uiState.pendingCount,
                    nagaCount = uiState.nagaCount,
                    onPrev = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentIndex > 0) {
                            val prevId = cards[currentIndex - 1].customer.id
                            activeCustomerId = prevId
                            viewModel.setActiveRunnerCustomerId(prevId)
                        }
                    },
                    onNext = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextId = viewModel.findSmartNextCustomerId(currentItem.customer.id, cards, skipCompleted = false)
                        if (nextId != null) {
                            activeCustomerId = nextId
                            viewModel.setActiveRunnerCustomerId(nextId)
                        }
                    },
                    onQuantityChanged = { q -> viewModel.onQuantityChanged(currentItem.customer.id, q) },
                    onCashChanged = { c -> viewModel.onCashChanged(currentItem.customer.id, c) },
                    onUseSuggestedMilk = { suggestedLiters ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.useSuggestedMilk(currentItem.customer.id, suggestedLiters)
                    },
                    onSaveClicked = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        deliveredAnimCustomer = currentItem.customer.name
                        val milkLiters = currentItem.currentMilkInput.toDoubleOrNull() ?: currentItem.customer.defaultLiters
                        val billAmt = CalculationEngine.round2(milkLiters * currentItem.applicableRate)
                        voiceManager.speakDelivery(currentItem.customer.name, milkLiters, billAmt)
                        val nextTargetId = viewModel.findSmartNextCustomerId(currentItem.customer.id, cards, skipCompleted = true)
                        viewModel.confirmAndSaveTransaction(currentItem) {
                            if (nextTargetId != null) {
                                activeCustomerId = nextTargetId
                                viewModel.setActiveRunnerCustomerId(nextTargetId)
                            }
                        }
                    },
                    onRateSelected = { r ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.updateCustomerRate(currentItem.customer.id, r)
                    },
                    onCustomRateClicked = {
                        customRateDialogCustomer = currentItem
                    },
                    onAiMunshiClicked = {
                        showAiMunshiDialog = true
                    },
                    onSendWhatsAppReminder = {
                        val reminderMsg = com.shahbuddindairy.app.ai.DairyAiPredictor.generateSoftReminderText(currentItem.customer.name, currentItem.prevDue)
                        val cleanPhone = currentItem.customer.phone.replace("+", "").replace(" ", "").replace("-", "")
                        val url = if (cleanPhone.isNotBlank()) {
                            val fullPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                            "https://api.whatsapp.com/send?phone=$fullPhone&text=" + URLEncoder.encode(reminderMsg, "UTF-8")
                        } else {
                            "https://api.whatsapp.com/send?text=" + URLEncoder.encode(reminderMsg, "UTF-8")
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    onNagaClicked = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentItem.isNaga) {
                            viewModel.undoLastDeliveryAction(currentItem.customer.id)
                        } else {
                            voiceManager.speakNaga(currentItem.customer.name)
                            val nextTargetId = viewModel.findSmartNextCustomerId(currentItem.customer.id, cards, skipCompleted = true)
                            viewModel.markNaga(currentItem) {
                                if (nextTargetId != null) {
                                    activeCustomerId = nextTargetId
                                    viewModel.setActiveRunnerCustomerId(nextTargetId)
                                }
                            }
                        }
                    },
                    onSkipClicked = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        voiceManager.speakSkip(currentItem.customer.name)
                        skippedCustomerIds = skippedCustomerIds + currentItem.customer.id
                        val nextTargetId = viewModel.findSmartNextCustomerId(currentItem.customer.id, cards, skipCompleted = false)
                        if (nextTargetId != null) {
                            activeCustomerId = nextTargetId
                            viewModel.setActiveRunnerCustomerId(nextTargetId)
                        }
                    },
                    onCustomerNumberClicked = {
                        showQuickJumpDialog = true
                    },
                    onWhatsAppReceiptClicked = {
                        val lit = currentItem.currentMilkInput.toDoubleOrNull() ?: currentItem.customer.defaultLiters
                        val rate = currentItem.applicableRate
                        val bill = CalculationEngine.round2(lit * rate)
                        val cash = currentItem.currentCashInput.toDoubleOrNull() ?: 0.0
                        val pbs = if (currentItem.prevDue > 0) currentItem.prevDue else 0.0
                        val adv = if (currentItem.prevDue < 0) -currentItem.prevDue else 0.0
                        val netDue = CalculationEngine.round2((bill + pbs) - (cash + adv))
                        WhatsAppReportManager.sendDeliveryReceipt(
                            context = context,
                            customerPhone = currentItem.customer.phone,
                            customerName = currentItem.customer.name,
                            dateStr = CalculationEngine.formatDateDisplay(uiState.selectedDate),
                            liters = lit,
                            milkType = currentItem.customer.milkType,
                            rate = rate,
                            todayBill = bill,
                            cashPaid = cash,
                            totalRemainingDue = netDue
                        )
                    },
                    onAlternateClicked = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextTargetId = viewModel.findSmartNextCustomerId(currentItem.customer.id, cards, skipCompleted = true)
                        viewModel.markAlternate(currentItem) {
                            if (nextTargetId != null) {
                                activeCustomerId = nextTargetId
                                viewModel.setActiveRunnerCustomerId(nextTargetId)
                            }
                        }
                    },
                    onUndoClicked = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.undoLastDeliveryAction(currentItem.customer.id)
                    },
                    onKhataClicked = { onNavigateToCustomerKhata(currentItem.customer.id) },
                    onPbsEditClicked = { pbsManagerCustomer = currentItem },
                    onCallClicked = {
                        if (currentItem.customer.phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentItem.customer.phone}"))
                            context.startActivity(intent)
                        }
                    },
                    onNotesClicked = {
                        customerForNotes = Pair(currentItem.customer.id, currentItem.customer.name)
                    },
                    onOneTapPayment = { amt ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.recordOneTapPayment(currentItem.customer.id, amt)
                    },
                    onEditCustomerClicked = { editingMasterCustomer = it },
                    onMilkEditClicked = { editingMilkCustomer = currentItem },
                    onBillEditClicked = { overridingBillCustomer = currentItem },
                    onDueEditClicked = { editingDueCustomer = currentItem },
                    onAdvanceEditClicked = { editingAdvanceCustomer = currentItem },
                    onMoreClicked = { runnerMoreCustomerItem = currentItem },
                    onAiNoteClicked = { aiDeliveryNoteCustomer = currentItem },
                    onPoliteReminderClicked = { faceToFaceReminderCustomer = currentItem }
                )

                    // Instant Delivery Checkmark Animation Overlay
                    if (deliveredAnimCustomer != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF15803D),
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("DELIVERED ✓", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.White)
                                    Text(deliveredAnimCustomer ?: "", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.95f))
                                }
                            }
                        }
                    }
                }

                // PERMANENT RIGHT FAST THUMB SCRUBBER (28dp width, zero card overlap)
                RunnerPermanentRightScrubber(
                    cards = cards,
                    currentIndex = currentIndex,
                    currentItemId = currentItem.customer.id,
                    onSelectCustomer = { cId ->
                        activeCustomerId = cId
                        viewModel.setActiveRunnerCustomerId(cId)
                    },
                    onScrubberBubbleChanged = { scrubberBubbleText = it }
                )
            }
        }
    }

    // CUSTOMER LIST SHEET (All 46 customers with search & filter)
    if (showCustomerListSheet) {
        CustomerListSheet(
            allCards = uiState.customerCards,
            onSelectCustomer = { cId ->
                activeCustomerId = cId
                viewModel.setActiveRunnerCustomerId(cId)
                showCustomerListSheet = false
            },
            onDismiss = { showCustomerListSheet = false }
        )
    }

    // QUICK CASH CUSTOMER DIALOG (+ नकद ग्राहक)
    if (showCashCustomerDialog) {
        CashCustomerDialog(
            onDismiss = { showCashCustomerDialog = false },
            onSaveAndDeliver = { name, milkType, liters, rate, cash ->
                viewModel.addQuickCustomerAndDeliver(
                    name = name,
                    milkType = milkType,
                    liters = liters,
                    rate = rate,
                    cash = cash,
                    saveAsRegular = false
                ) {
                    showCashCustomerDialog = false
                }
            }
        )
    }
    // CUSTOM RATE DIALOG (1-Tap Rate Customizer)
    customRateDialogCustomer?.let { custItem ->
        var tempRateText by remember { mutableStateOf(custItem.applicableRate.toString()) }
        AlertDialog(
            onDismissRequest = { customRateDialogCustomer = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Customer Rate (दर)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "${custItem.customer.name} ke liye naya rate (₹/L) set karein:",
                        fontSize = 13.sp,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempRateText,
                        onValueChange = { tempRateText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = colorScheme.primary) },
                        suffix = { Text("/L", fontWeight = FontWeight.Bold, color = colorScheme.primary) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = tempRateText.toDoubleOrNull()
                        if (r != null && r > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateCustomerRate(custItem.customer.id, r)
                        }
                        customRateDialogCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE RATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customRateDialogCustomer = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // 1. EDIT CUSTOMER MASTER DIALOG (V7.6)
    editingMasterCustomer?.let { cust ->
        EditCustomerMasterDialog(
            customer = cust,
            onDismiss = { editingMasterCustomer = null },
            onSave = { updated ->
                viewModel.updateCustomerMaster(updated) {
                    editingMasterCustomer = null
                }
            }
        )
    }

    // 2. RECONCILE DUE DIALOG (V7.6)
    editingDueCustomer?.let { item ->
        ReconcileDueDialog(
            item = item,
            onDismiss = { editingDueCustomer = null },
            onConfirm = { targetDue, note ->
                val baseBal = if (item.prevDue > 0) item.prevDue else 0.0
                viewModel.recordDueAdjustment(
                    customerId = item.customer.id,
                    currentBalance = baseBal,
                    targetDue = targetDue,
                    note = note
                ) {
                    editingDueCustomer = null
                }
            }
        )
    }

    // 3. ADJUST ADVANCE DIALOG (V7.6)
    editingAdvanceCustomer?.let { item ->
        AdjustAdvanceDialog(
            item = item,
            onDismiss = { editingAdvanceCustomer = null },
            onConfirm = { targetAdvance, note ->
                val curAdv = if (item.prevDue < 0) -item.prevDue else 0.0
                viewModel.recordAdvanceAdjustment(
                    customerId = item.customer.id,
                    currentAdvance = curAdv,
                    targetAdvance = targetAdvance,
                    note = note
                ) {
                    editingAdvanceCustomer = null
                }
            }
        )
    }

    // 4. OVERRIDE BILL DIALOG (V7.6)
    overridingBillCustomer?.let { item ->
        OverrideBillDialog(
            item = item,
            onDismiss = { overridingBillCustomer = null },
            onConfirm = { customBill, customCash ->
                viewModel.saveDeliveryWithCustomBill(
                    item = item,
                    customBill = customBill,
                    customCash = customCash
                ) {
                    overridingBillCustomer = null
                }
            }
        )
    }

    // 5. MILK NUMERIC EDITOR DIALOG (V7.6)
    editingMilkCustomer?.let { item ->
        MilkEditorDialog(
            item = item,
            onDismiss = { editingMilkCustomer = null },
            onSave = { newLiters ->
                viewModel.onQuantityChanged(item.customer.id, "%.2f".format(Locale.US, newLiters))
                editingMilkCustomer = null
            }
        )
    }

    // 6. PBS / MANUAL BOOK MANAGER DIALOG (V7.6)
    pbsManagerCustomer?.let { item ->
        PbsManagerDialog(
            item = item,
            viewModel = viewModel,
            onDismiss = { pbsManagerCustomer = null }
        )
    }

    // 7. RUNNER CASH RECEIPT CONFIRMATION DIALOG (V7.6)
    runnerCashConfirmItem?.let { item ->
        RunnerCashConfirmDialog(
            item = item,
            onConfirm = {
                val currentCards = uiState.customerCards
                val nextTargetId = viewModel.findSmartNextCustomerId(item.customer.id, currentCards, skipCompleted = true)

                viewModel.confirmAndSaveTransaction {
                    if (nextTargetId != null) {
                        activeCustomerId = nextTargetId
                        viewModel.setActiveRunnerCustomerId(nextTargetId)
                    }
                    runnerCashConfirmItem = null
                }
            },
            onDismiss = { runnerCashConfirmItem = null }
        )
    }

    // 8. RUNNER MORE MENU DIALOG (V8.0)
    runnerMoreCustomerItem?.let { item ->
        RunnerMoreMenuDialog(
            item = item,
            onDismiss = { runnerMoreCustomerItem = null },
            onOpenKhata = { onNavigateToCustomerKhata(item.customer.id) },
            onOpenQr = { runnerQrCustomerItem = item },
            onOpenReceipt = { runnerReceiptCustomerItem = item },
            onOpenNotes = { customerForNotes = Pair(item.customer.id, item.customer.name) },
            onOpenWhatsApp = {
                val due = if (item.remainingDue > 0) item.remainingDue else 0.0
                val text = "शाहबुद्दीन डेयरी (Shahbuddin Dairy)\nनमस्ते ${item.customer.name} जी,\nआज का दूध: ${item.currentMilkInput}L\nआज का बिल: ${CalculationEngine.formatMoney(item.todayBill)}\nकुल शेष बकाया: ${CalculationEngine.formatMoney(due)}\nधन्यवाद!"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    setPackage("com.whatsapp")
                }
                try {
                    context.startActivity(shareIntent)
                } catch (e: Exception) {
                    val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }, "Share via WhatsApp")
                    context.startActivity(chooser)
                }
            },
            onEditMaster = { editingMasterCustomer = item.customer },
            onAiCleanName = { aiCustomerCleanCustomer = item },
            onAiDeliveryNote = { aiDeliveryNoteCustomer = item }
        )
    }

    // 8.1 AI DELIVERY NOTE DIALOG
    aiDeliveryNoteCustomer?.let { item ->
        AiDeliveryNoteDialog(
            item = item,
            aiClient = aiClient,
            onDismiss = { aiDeliveryNoteCustomer = null },
            onSaveNote = { content ->
                viewModel.saveCustomerNote(
                    customerId = item.customer.id,
                    content = content,
                    isPinned = true
                ) {
                    aiDeliveryNoteCustomer = null
                }
            }
        )
    }

    // 8.2 AI CUSTOMER NAME & ADDRESS NORMALIZER DIALOG
    aiCustomerCleanCustomer?.let { item ->
        AiCustomerCleanDialog(
            item = item,
            aiClient = aiClient,
            onDismiss = { aiCustomerCleanCustomer = null },
            onSave = { cName, cMohalla, finalLandmark ->
                val updated = item.customer.copy(
                    name = cName,
                    mohalla = cMohalla,
                    landmark = finalLandmark
                )
                viewModel.updateCustomerMaster(updated) {
                    aiCustomerCleanCustomer = null
                }
            }
        )
    }

    // 8.3 FACE-TO-FACE POLITE DUE REMINDER DIALOG
    faceToFaceReminderCustomer?.let { item ->
        FaceToFaceReminderDialog(
            item = item,
            voiceManager = voiceManager,
            onDismiss = { faceToFaceReminderCustomer = null }
        )
    }

    // 9. RUNNER DUE QR DIALOG (V8.0)
    runnerQrCustomerItem?.let { item ->
        RunnerDueQrDialog(
            item = item,
            upiId = "9897259461@upi",
            payeeName = "Shahbuddin Dairy",
            onDismiss = { runnerQrCustomerItem = null }
        )
    }

    // 10. RUNNER RECEIPT SHARE DIALOG (V8.0)
    runnerReceiptCustomerItem?.let { item ->
        RunnerReceiptShareDialog(
            item = item,
            upiId = "9897259461@upi",
            dairyPhone = "9897259461",
            onDismiss = { runnerReceiptCustomerItem = null }
        )
    }

    // 11. AI MUNSHI OFFLINE VOICE & TEXT DIALOG
    if (showAiMunshiDialog) {
        AiMunshiDialog(
            customers = uiState.allActiveCustomers,
            onDismiss = { showAiMunshiDialog = false },
            onExecuteCommand = { cmd ->
                viewModel.executeAiMunshiCommand(
                    cmd = cmd,
                    onSuccess = {
                        showAiMunshiDialog = false
                    },
                    onError = { err ->
                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // DAIRY CALCULATOR DIALOG
    if (showCalculatorDialog) {
        DairyCalculatorDialog(
            onDismiss = { showCalculatorDialog = false }
        )
    }

    // QUICK GUEST CUSTOMER DIALOG
    if (showQuickCustomerDialog) {
        QuickCustomerDialog(
            onDismiss = { showQuickCustomerDialog = false },
            onSave = { name, milkType, liters, rate, cash, saveAsRegular ->
                viewModel.addQuickCustomerAndDeliver(
                    name = name,
                    milkType = milkType,
                    liters = liters,
                    rate = rate,
                    cash = cash,
                    saveAsRegular = saveAsRegular
                ) {
                    showQuickCustomerDialog = false
                }
            }
        )
    }

    // CUSTOMER NOTES DIALOG
    customerForNotes?.let { (cId, cName) ->
        CustomerNotesDialog(
            customerId = cId,
            customerName = cName,
            viewModel = viewModel,
            onDismiss = { customerForNotes = null }
        )
    }

    // ROUTE MANAGER DIALOG
    if (showRouteManagerDialog) {
        RouteManagerDialog(
            availableRoutes = uiState.availableRoutes,
            allCustomers = uiState.allActiveCustomers,
            viewModel = viewModel,
            onDismiss = { showRouteManagerDialog = false }
        )
    }

    // TIER 2: SLIDE-OUT LEFT CUSTOMER DRAWER (PANEL 2)
    if (showSideTray) {
        RunnerLeftCustomerDrawer(
            cards = cards,
            currentItemId = currentItem?.customer?.id,
            onSelectCustomer = { cId ->
                activeCustomerId = cId
                viewModel.setActiveRunnerCustomerId(cId)
            },
            onClose = {
                showSideTray = false
            }
        )
    }

    // TIER 2.5: IN-APP RUNNER 3.0 GUIDELINES MODAL
    if (showGuidelineDialog) {
        RunnerGuidelineDialog(
            onDismiss = { showGuidelineDialog = false }
        )
    }

    // TIER 3: QUICK JUMP KEYPAD DIALOG (PANEL 4)
    if (showQuickJumpDialog) {
        QuickJumpKeypadDialog(
            cards = cards,
            onSelectCustomer = { cId ->
                activeCustomerId = cId
                viewModel.setActiveRunnerCustomerId(cId)
            },
            onDismiss = { showQuickJumpDialog = false }
        )
    }

    // FLOATING SCRUBBER MAGNIFICATION BUBBLE (PANEL 1)
    if (scrubberBubbleText != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                shadowElevation = 16.dp,
                border = BorderStroke(2.dp, Color(0xFF60A5FA)),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = scrubberBubbleText ?: "",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}




@Composable
fun CustomerListSheet(
    allCards: List<DailyCustomerCardItem>,
    onSelectCustomer: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(allCards, searchQuery, selectedFilter) {
        allCards.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.customer.name.contains(searchQuery, ignoreCase = true) ||
                item.customer.mohalla.contains(searchQuery, ignoreCase = true) ||
                item.customer.id.toString().contains(searchQuery)

            val matchesFilter = when (selectedFilter) {
                "PENDING" -> !item.isDone && !item.isNaga && !item.isAlternateOff
                "DONE" -> item.isDone
                "NAGA" -> item.isNaga
                "ALTERNATE" -> item.isAlternateOff
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val pendingCount = remember(allCards) { allCards.count { !it.isDone && !it.isNaga && !it.isAlternateOff } }
    val doneCount = remember(allCards) { allCards.count { it.isDone } }
    val nagaCount = remember(allCards) { allCards.count { it.isNaga } }
    val altCount = remember(allCards) { allCards.count { it.isAlternateOff } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ग्राहक सूची (${allCards.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("नाम से खोजें...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter pills
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "सभी (${allCards.size})",
                        "PENDING" to "बाकी ($pendingCount)",
                        "DONE" to "पूर्ण ($doneCount)",
                        "NAGA" to "नागा ($nagaCount)",
                        "ALTERNATE" to "आज नहीं ($altCount)"
                    ).forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("कोई ग्राहक नहीं मिला", fontSize = 13.sp, color = Color(0xFF64748B))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredList, key = { it.customer.id }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (item.isDone) Color(0xFFF0FDF4) else if (item.isNaga) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (item.isDone) Color(0xFFBBF7D0) else if (item.isNaga) Color(0xFFFECACA) else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCustomer(item.customer.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${item.routeIndex + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.customer.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                if (item.customer.phone.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("📞", fontSize = 11.sp)
                                                }
                                            }
                                            Text(
                                                text = "तय: %.1fL • ₹%.0f/L".format(item.customer.defaultLiters, item.applicableRate),
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            if (item.isDone) {
                                                Text("पूर्ण ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                Text("दूध %.1fL".format(item.delivery?.liters ?: 0.0), fontSize = 10.sp, color = Color(0xFF15803D))
                                            } else if (item.isNaga) {
                                                Text("नागा ✗", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                            } else if (item.isAlternateOff) {
                                                Text("आज नहीं (Alt)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                                            } else {
                                                Text("बाकी", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            }

                                            if (item.advance > 0) {
                                                Text("एडवांस ₹%.0f".format(item.advance), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                            } else if (item.remainingDue > 0) {
                                                Text("बकाया ₹%.0f".format(item.remainingDue), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें (Close)")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * TIER 3: QUICK JUMP KEYPAD DIALOG (PANEL 4)
 * Allows the runner to jump directly to any customer by number (1..46) or by Mohalla.
 */
@Composable
fun QuickJumpKeypadDialog(
    cards: List<DailyCustomerCardItem>,
    onSelectCustomer: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Number, 1: Mohalla
    var numberInput by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme

    val enteredNumber = numberInput.toIntOrNull()
    val previewItem = if (enteredNumber != null && enteredNumber in 1..cards.size) {
        cards[enteredNumber - 1]
    } else null

    val uniqueMohallas = remember(cards) {
        cards.map { it.customer.mohalla.ifBlank { "सामान्य" } }.distinct().sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Customer पर Jump करें", fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tab Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("नंबर से (Number)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("मोहल्ला से (Area)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedTab == 0) {
                    // Number display field
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.surfaceVariant,
                        border = BorderStroke(1.5.dp, colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (numberInput.isEmpty()) "नंबर दर्ज करें (1 - ${cards.size})" else "#$numberInput",
                                fontSize = if (numberInput.isEmpty()) 13.sp else 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (numberInput.isEmpty()) colorScheme.onSurfaceVariant else colorScheme.primary
                            )
                            if (numberInput.isNotEmpty()) {
                                IconButton(onClick = { numberInput = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Numeric Keypad Grid (1..9, 0, Backspace, Go)
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "GO")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val key = keys[row * 3 + col]
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (key) {
                                            "GO" -> Color(0xFF15803D)
                                            "⌫" -> colorScheme.surfaceContainerHighest
                                            else -> colorScheme.surfaceContainerHigh
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clickable {
                                                when (key) {
                                                    "⌫" -> if (numberInput.isNotEmpty()) numberInput = numberInput.dropLast(1)
                                                    "GO" -> {
                                                        if (previewItem != null) {
                                                            onSelectCustomer(previewItem.customer.id)
                                                            onDismiss()
                                                        }
                                                    }
                                                    else -> {
                                                        if (numberInput.length < 3) numberInput += key
                                                    }
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (key == "GO") Color.White else colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Customer Preview Banner
                    if (previewItem != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                onSelectCustomer(previewItem.customer.id)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape).background(
                                        when {
                                            previewItem.isDone -> Color(0xFF15803D)
                                            previewItem.isNaga -> Color(0xFFDC2626)
                                            else -> Color(0xFF2563EB)
                                        }
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${previewItem.routeIndex + 1} ${previewItem.customer.name}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                    Text(
                                        text = "${previewItem.customer.mohalla} • ${previewItem.customer.defaultLiters}L",
                                        fontSize = 11.5.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        onSelectCustomer(previewItem.customer.id)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text("JUMP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Mohalla Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uniqueMohallas) { mohalla ->
                            val mohallaCustomers = cards.filter { it.customer.mohalla.ifBlank { "सामान्य" } == mohalla }
                            val firstInMohalla = mohallaCustomers.firstOrNull()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (firstInMohalla != null) {
                                        onSelectCustomer(firstInMohalla.customer.id)
                                        onDismiss()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(mohalla, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Text("${mohallaCustomers.size} ग्राहक", fontSize = 12.sp, color = colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * PERMANENT RIGHT FAST THUMB SCRUBBER (Panel 1)
 * Permanent 28dp right-edge dial for 1-handed thumb scrubbing. Zero overlap with delivery cards.
 */
@Composable
fun RunnerPermanentRightScrubber(
    cards: List<DailyCustomerCardItem>,
    currentIndex: Int,
    currentItemId: Long?,
    onSelectCustomer: (Long) -> Unit,
    onScrubberBubbleChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var scrubberHeightPx by remember { mutableFloatStateOf(1f) }

    Surface(
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .padding(vertical = 4.dp)
            .onGloballyPositioned { layoutCoordinates ->
                scrubberHeightPx = layoutCoordinates.size.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(cards) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val fraction = (offset.y / scrubberHeightPx).coerceIn(0f, 0.999f)
                        val targetIdx = (fraction * cards.size).toInt().coerceIn(0, cards.size - 1)
                        val targetCard = cards[targetIdx]
                        onSelectCustomer(targetCard.customer.id)
                        onScrubberBubbleChanged("#${targetCard.routeIndex + 1} ${targetCard.customer.name}\n${targetCard.customer.defaultLiters}L • ${targetCard.customer.mohalla}")
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.y / scrubberHeightPx).coerceIn(0f, 0.999f)
                        val targetIdx = (fraction * cards.size).toInt().coerceIn(0, cards.size - 1)
                        val targetCard = cards[targetIdx]
                        if (targetCard.customer.id != currentItemId) {
                            onSelectCustomer(targetCard.customer.id)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onScrubberBubbleChanged("#${targetCard.routeIndex + 1} ${targetCard.customer.name}\n${targetCard.customer.defaultLiters}L • ${targetCard.customer.mohalla}")
                    },
                    onDragEnd = {
                        onScrubberBubbleChanged(null)
                    },
                    onDragCancel = {
                        onScrubberBubbleChanged(null)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val step = if (cards.size > 14) (cards.size / 10).coerceAtLeast(1) else 1
            for (i in 0 until cards.size step step) {
                val card = cards[i]
                val isCurrent = card.customer.id == currentItemId
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> colorScheme.primary
                                card.isDone -> Color(0xFF15803D).copy(alpha = 0.25f)
                                card.isNaga -> Color(0xFFDC2626).copy(alpha = 0.25f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectCustomer(card.customer.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${i + 1}",
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                        color = when {
                            isCurrent -> Color.White
                            card.isDone -> Color(0xFF15803D)
                            card.isNaga -> Color(0xFFDC2626)
                            else -> colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * TIER 2: SLIDE-OUT LEFT CUSTOMER DRAWER (Panel 2)
 * Slide-out customer directory from left edge with search, filter tabs and status dots.
 */
@Composable
fun RunnerLeftCustomerDrawer(
    cards: List<DailyCustomerCardItem>,
    currentItemId: Long?,
    onSelectCustomer: (Long) -> Unit,
    onClose: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredCards = remember(cards, searchQuery, selectedFilter) {
        cards.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "DONE" -> item.isDone
                "NAGA" -> item.isNaga
                "PENDING" -> !item.isDone && !item.isNaga
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.customer.name.contains(searchQuery, ignoreCase = true) ||
                item.customer.mohalla.contains(searchQuery, ignoreCase = true) ||
                (item.routeIndex + 1).toString().contains(searchQuery)
            }
            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.78f)
                .align(Alignment.CenterStart)
                .clickable(enabled = false) {}, // prevent click-through
            color = colorScheme.surface,
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, colorScheme.outlineVariant),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ग्राहकों की सूची", fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("कुल ${cards.size} ग्राहक • चुनें", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("नाम, गली या # खोजें...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(fontSize = 12.5.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Filter tabs: [सभी] [बाकी] [पूर्ण] [नागा]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allCount = cards.size
                    val pendingCount = cards.count { !it.isDone && !it.isNaga }
                    val doneCount = cards.count { it.isDone }
                    val nagaCount = cards.count { it.isNaga }

                    listOf(
                        "ALL" to "सभी ($allCount)",
                        "PENDING" to "बाकी ($pendingCount)",
                        "DONE" to "पूर्ण ($doneCount)",
                        "NAGA" to "नागा ($nagaCount)"
                    ).forEach { (fKey, fLabel) ->
                        val isSel = selectedFilter == fKey
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) colorScheme.primary else colorScheme.surfaceContainerHigh,
                            border = BorderStroke(0.5.dp, if (isSel) colorScheme.primary else colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFilter = fKey
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Customer List
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(filteredCards) { _, item ->
                        val isCurrent = item.customer.id == currentItemId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isCurrent -> colorScheme.primaryContainer
                                item.isDone -> Color(0xFFF0FDF4)
                                item.isNaga -> Color(0xFFFEF2F2)
                                else -> colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 0.5.dp,
                                if (isCurrent) colorScheme.primary else colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectCustomer(item.customer.id)
                                    onClose()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status Dot
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                item.isDone -> Color(0xFF15803D)
                                                item.isNaga -> Color(0xFFDC2626)
                                                else -> Color(0xFF9CA3AF)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${item.routeIndex + 1} ${item.customer.name}",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                        color = if (isCurrent) colorScheme.primary else colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.customer.mohalla} • ${item.customer.defaultLiters}L",
                                        fontSize = 10.5.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                if (item.isDone) {
                                    Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                } else if (item.isNaga) {
                                    Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * IN-APP RUNNER 3.0 GUIDELINES & TOUCH RULES MODAL
 */
@Composable
fun RunnerGuidelineDialog(
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("समझ गया (ठीक है) ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("रनर 3.0 निर्देश व नियम", fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Gestures
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("👆 टच जेस्चर (Touch Gestures)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• 👈 स्वाइप बायाँ: अगला ग्राहक (Next)", fontSize = 11.5.sp)
                        Text("• 👉 स्वाइप दायाँ: पिछला ग्राहक (Prev)", fontSize = 11.5.sp)
                        Text("• ⬇️ स्वाइप नीचे: तुरंत डिलीवर करें (Deliver ✓)", fontSize = 11.5.sp)
                        Text("• ⬆️ स्वाइप ऊपर: नागा दर्ज करें (Naga ✕)", fontSize = 11.5.sp)
                        Text("• 👆👆 डबल-टैप: पूरा बिल नकद बॉक्स में स्वतः भरें", fontSize = 11.5.sp)
                    }
                }

                // Section 2: 4 Financial Boxes
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("📊 4 हिसाब टाइल्स (1-टैप एडिट)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• आज बिल ✎: आज के बिल पर टैप करके सीधे एडिट करें", fontSize = 11.5.sp, color = Color(0xFF1E3A8A))
                        Text("• पुराना PBS ✎: पुराना बकाया सीधे बदलें", fontSize = 11.5.sp, color = Color(0xFF1E3A8A))
                        Text("• कुल देय ✎: कुल देय राशि तुरंत सेट करें", fontSize = 11.5.sp, color = Color(0xFF1E3A8A))
                        Text("• एडवांस ✎: ग्राहक का जमा एडवांस एडजस्ट करें", fontSize = 11.5.sp, color = Color(0xFF1E3A8A))
                    }
                }

                // Section 3: Navigation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("🎚️ फास्ट नेविगेशन", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• दायाँ स्क्रबर: अँगूठे से 1..42 डायल स्लाइड करके किसी भी ग्राहक पर सीधे कूदें", fontSize = 11.5.sp)
                        Text("• [📋 सूची]: बाएँ तरफ से पूरी ग्राहक लिस्ट खोलें, खोजें या स्टेटस फ़िल्टर करें", fontSize = 11.5.sp)
                        Text("• [⚡ जंप]: कीपैड से ग्राहक नंबर दर्ज करके पहुँचें", fontSize = 11.5.sp)
                    }
                }

                // Section 4: AI & WhatsApp
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("🤖 AI मुंशी और आवाज (TTS)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• आवाज: हर डिलीवरी पर बोलकर पुष्टि करेगा", fontSize = 11.5.sp, color = Color(0xFF14532D))
                        Text("• AI नोट (🎤): ग्राहक की हिदायत बोलकर सेव करें", fontSize = 11.5.sp, color = Color(0xFF14532D))
                        Text("• AI सुझाव: दूध मात्रा का स्वतः सटीक अनुमान", fontSize = 11.5.sp, color = Color(0xFF14532D))
                        Text("• WhatsApp (↗): 1-क्लिक में ग्राहक को पक्की पर्ची भेजें", fontSize = 11.5.sp, color = Color(0xFF14532D))
                    }
                }
            }
        }
    )
}

@Composable
fun CashCustomerDialog(
    onDismiss: () -> Unit,
    onSaveAndDeliver: (name: String, milkType: String, liters: Double, rate: Double, cash: Double) -> Unit
) {
    var name by remember { mutableStateOf("नकद ग्राहक") }
    var milkType by remember { mutableStateOf("भैंस") }
    var litersText by remember { mutableStateOf("1.0") }
    var rate by remember { mutableFloatStateOf(70f) }
    var customRateText by remember { mutableStateOf("") }
    var isCustomRate by remember { mutableStateOf(false) }

    val currentLiters = litersText.toDoubleOrNull() ?: 1.0
    val activeRate = if (isCustomRate) (customRateText.toDoubleOrNull() ?: rate.toDouble()) else rate.toDouble()
    val totalBill = CalculationEngine.round2(currentLiters * activeRate)
    var cashText by remember { mutableStateOf("") }

    LaunchedEffect(totalBill) {
        if (cashText.isBlank() || cashText == "0") {
            cashText = "%.0f".format(Locale.US, totalBill)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("नकद / गेस्ट ग्राहक डिलीवरी", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ग्राहक का नाम", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Milk type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("भैंस", "गाय").forEach { type ->
                        val isSel = milkType == type
                        FilterChip(
                            selected = isSel,
                            onClick = { milkType = type },
                            label = { Text(type, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Liters
                Text("दूध की मात्रा (लीटर)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val curr = litersText.toDoubleOrNull() ?: 1.0
                            val nextVal = (curr - 0.5).coerceAtLeast(0.5)
                            litersText = "%.2f".format(Locale.US, nextVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("-0.5L", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = litersText,
                        onValueChange = { litersText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("L", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val curr = litersText.toDoubleOrNull() ?: 1.0
                            litersText = "%.2f".format(Locale.US, curr + 0.5)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("+0.5L", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                // Rate presets
                Text("रेट तय करें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(70f, 65f, 60f).forEach { rPreset ->
                        val isSel = !isCustomRate && rate == rPreset
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                isCustomRate = false
                                rate = rPreset
                            },
                            label = { Text("₹%.0f".format(rPreset), fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF15803D),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    FilterChip(
                        selected = isCustomRate,
                        onClick = { isCustomRate = true },
                        label = { Text("Custom", fontWeight = if (isCustomRate) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (isCustomRate) {
                    OutlinedTextField(
                        value = customRateText,
                        onValueChange = { customRateText = it },
                        label = { Text("कस्टम रेट (₹/लीटर)", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Bill Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("कुल बिल:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        Text("₹%.0f".format(totalBill), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E40AF))
                    }
                }

                // Cash Received
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = { Text("नकद प्राप्त (₹)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cashPaid = cashText.toDoubleOrNull() ?: totalBill
                    onSaveAndDeliver(name.ifBlank { "नकद ग्राहक" }, milkType, currentLiters, activeRate, cashPaid)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ तुरंत दें और जोड़ें", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun DailyTaskRunnerCard(
    item: DailyCustomerCardItem,
    currentIndex: Int,
    totalCount: Int,
    completedCount: Int,
    pendingCount: Int,
    nagaCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onCashChanged: (String) -> Unit,
    onUseSuggestedMilk: (Double) -> Unit,
    onSaveClicked: () -> Unit,
    onRateSelected: (Double) -> Unit = {},
    onCustomRateClicked: () -> Unit = {},
    onAiMunshiClicked: () -> Unit = {},
    onSendWhatsAppReminder: () -> Unit = {},
    onNagaClicked: () -> Unit,
    onAlternateClicked: () -> Unit = {},
    onUndoClicked: () -> Unit = {},
    onKhataClicked: () -> Unit,
    onPbsEditClicked: () -> Unit,
    onCallClicked: () -> Unit,
    onNotesClicked: () -> Unit = {},
    onOneTapPayment: (Double) -> Unit = {},
    onEditCustomerClicked: (CustomerEntity) -> Unit = {},
    onMilkEditClicked: () -> Unit = {},
    onBillEditClicked: () -> Unit = {},
    onDueEditClicked: () -> Unit = {},
    onAdvanceEditClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onCustomerNumberClicked: () -> Unit = {},
    onSkipClicked: () -> Unit = {},
    onWhatsAppReceiptClicked: () -> Unit = {},
    onAiNoteClicked: () -> Unit = {},
    onPoliteReminderClicked: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // Gesture tracking state
    var totalDragX by remember(item.customer.id) { mutableFloatStateOf(0f) }
    var totalDragY by remember(item.customer.id) { mutableFloatStateOf(0f) }

    // Accurate Invariant Accounting
    val milkVal = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
    val rateVal = item.applicableRate
    val todayBillVal = CalculationEngine.round2(milkVal * rateVal)
    val existingPbs = if (item.prevDue > 0) item.prevDue else 0.0
    val existingAdvance = if (item.prevDue < 0) -item.prevDue else 0.0
    val totalRequiredVal = CalculationEngine.round2(todayBillVal + existingPbs)
    val cashReceivedVal = item.currentCashInput.toDoubleOrNull() ?: 0.0
    val netBalance = CalculationEngine.round2(totalRequiredVal - (cashReceivedVal + existingAdvance))
    val remainingDueVal = if (netBalance > 0) netBalance else 0.0
    val advanceRemainingVal = if (netBalance < 0) CalculationEngine.round2(-netBalance) else 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.customer.id) {
                detectTapGestures(
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val exactAmt = (totalRequiredVal - existingAdvance).coerceAtLeast(0.0)
                        onCashChanged("%.0f".format(Locale.US, exactAmt))
                    }
                )
            }
            .pointerInput(item.customer.id) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    },
                    onDragEnd = {
                        val absX = kotlin.math.abs(totalDragX)
                        val absY = kotlin.math.abs(totalDragY)
                        val threshold = 90f
                        if (absX > absY && absX > threshold) {
                            if (totalDragX < 0) onNext() else onPrev()
                        } else if (absY >= absX && absY > threshold) {
                            if (totalDragY > 0) onSaveClicked() else onNagaClicked()
                        }
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            border = BorderStroke(1.5.dp, colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // 1. CUSTOMER HEADER ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCustomerNumberClicked()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${item.routeIndex + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.customer.name,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(status = item.statusText)
                        }
                        Text(
                            text = "ID: #${item.customer.id} • ${item.customer.mohalla}${if (item.customer.landmark.isNotBlank()) " • ${item.customer.landmark}" else ""}",
                            fontSize = 11.5.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    // Action icon buttons
                    if (item.customer.phone.isNotBlank()) {
                        IconButton(onClick = onCallClicked, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = colorScheme.primary, modifier = Modifier.size(19.dp))
                        }
                    }
                    IconButton(onClick = onKhataClicked, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.History, contentDescription = "Khata", tint = colorScheme.primary, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onAiNoteClicked, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Mic, contentDescription = "AI Note", tint = Color(0xFFD97706), modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onMoreClicked, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }

                // PINNED NOTE (If present)
                if (item.pinnedNote != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(0.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth().clickable { onAiNoteClicked() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📌 ${item.pinnedNote}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E), modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(13.dp))
                        }
                    }
                }

                // YESTERDAY SUMMARY (Compact 1-liner)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.yesterdayWasNaga) {
                        Text("कल: नागा (0L)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    } else if (item.yesterdayDeliveredLiters != null && item.yesterdayDeliveredLiters > 0.0) {
                        Text(
                            "कल: ${item.yesterdayDeliveredLiters}L • बिल ₹%.0f • नकद ₹%.0f".format(
                                item.yesterdayBill ?: 0.0,
                                item.yesterdayPaidCash ?: 0.0
                            ),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("कल: 0L", fontSize = 11.5.sp, color = colorScheme.onSurfaceVariant)
                    }

                    if (item.pinnedNote == null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.clickable { onAiNoteClicked() }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("AI नोट", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // 2. RATE PRESETS SELECTOR (1-Tap: ₹70, ₹65, ₹60, Custom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("रेट:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                    listOf(70.0, 65.0, 60.0).forEach { rPreset ->
                        val isSel = item.applicableRate == rPreset
                        FilterChip(
                            selected = isSel,
                            onClick = { onRateSelected(rPreset) },
                            label = { Text("₹%.0f".format(rPreset), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF15803D),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                    val isCustom = item.applicableRate != 70.0 && item.applicableRate != 65.0 && item.applicableRate != 60.0
                    FilterChip(
                        selected = isCustom,
                        onClick = onCustomRateClicked,
                        label = {
                            Text(
                                if (isCustom) "₹%.0f (कस्टम)".format(item.applicableRate) else "कस्टम",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }

                // AI Smart Liters / Routine Predictor
                val suggestedLiters: Double? = when {
                    item.suggestedMilkFromCash != null && item.suggestedMilkFromCash > 0 -> item.suggestedMilkFromCash
                    item.yesterdayDeliveredLiters != null && item.yesterdayDeliveredLiters > 0 && item.yesterdayDeliveredLiters != (item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters) -> item.yesterdayDeliveredLiters
                    else -> null
                }

                if (suggestedLiters != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF3E8FF),
                        border = BorderStroke(0.5.dp, Color(0xFFC084FC)),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQuantityChanged("%.2f".format(Locale.US, suggestedLiters))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "✨ AI सुझाव: %.2fL लागू करें".format(suggestedLiters),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7E22CE)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. MILK QUANTITY STEPPERS & TYPING FIELD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val curr = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
                            val nextVal = (curr - 0.5).coerceAtLeast(0.0)
                            onQuantityChanged("%.2f".format(Locale.US, nextVal))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("-0.5L", color = colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = item.currentMilkInput,
                        onValueChange = onQuantityChanged,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("L", fontWeight = FontWeight.Bold, color = colorScheme.primary, fontSize = 16.sp) },
                        textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    )

                    Button(
                        onClick = {
                            val curr = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
                            onQuantityChanged("%.2f".format(Locale.US, curr + 0.5))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("+0.5L", color = colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Quantity Chips: 0.5L, 1L, 1.5L, 2L, 2.5L (Replaced 800g with 0.5L)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf(0.5 to "0.5L", 1.0 to "1L", 1.5 to "1.5L", 2.0 to "2L", 2.5 to "2.5L").forEach { (qPreset, qLabel) ->
                        val isSelected = (item.currentMilkInput.toDoubleOrNull() ?: 0.0) == qPreset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onQuantityChanged("%.2f".format(Locale.US, qPreset)) },
                            label = { Text(qLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. SPACIOUS ACCOUNTING GRID (4 Distinct Boxes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Box 1: आज बिल (Editable)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.weight(1f).height(62.dp).clickable { onBillEditClicked() }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("आज बिल", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
                                Text("✎", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("₹%.0f".format(todayBillVal), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E40AF))
                        }
                    }

                    // Box 2: पुराना PBS / बकाया (Editable)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (existingPbs > 0) Color(0xFFFFF7ED) else colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (existingPbs > 0) Color(0xFFFED7AA) else colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f).height(62.dp).clickable { onDueEditClicked() }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("पुराना PBS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (existingPbs > 0) Color(0xFFC2410C) else colorScheme.onSurfaceVariant)
                                Text("✎", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (existingPbs > 0) Color(0xFFC2410C) else colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("₹%.0f".format(existingPbs), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (existingPbs > 0) Color(0xFFC2410C) else colorScheme.onSurface)
                        }
                    }

                    // Box 3: कुल देय (Editable)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f).height(62.dp).clickable { onDueEditClicked() }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("कुल देय", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                Text("✎", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("₹%.0f".format(totalRequiredVal), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        }
                    }

                    // Box 4: जमा एडवांस (Editable)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (existingAdvance > 0) Color(0xFFF0FDF4) else colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (existingAdvance > 0) Color(0xFFBBF7D0) else colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f).height(62.dp).clickable { onAdvanceEditClicked() }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("एडवांस", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (existingAdvance > 0) Color(0xFF15803D) else colorScheme.onSurfaceVariant)
                                Text("✎", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (existingAdvance > 0) Color(0xFF15803D) else colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("₹%.0f".format(existingAdvance), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (existingAdvance > 0) Color(0xFF15803D) else colorScheme.onSurface)
                        }
                    }
                }

                if (item.prevDue > 500) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth().clickable { onPoliteReminderClicked() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "💡 विनम्र तकादा (पिछला बकाया ₹%.0f)".format(item.prevDue),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Text("तकादा स्क्रिप्ट ➔", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. CASH RECEIVED ROW & QUICK PILLS (58dp height, zero vertical clipping)
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = item.currentCashInput,
                        onValueChange = onCashChanged,
                        placeholder = { Text("0", fontSize = 16.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        label = { Text("नकद (₹)", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.15f).fillMaxHeight()
                    )

                    // Quick pills: [₹50] [₹100] [₹200] [Exact]
                    Row(
                        modifier = Modifier.weight(1.9f).fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("50", "100", "200").forEach { pVal ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCashChanged(pVal)
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("₹$pVal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                }
                            }
                        }

                        // [Exact / पूरा बिल]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.5.dp, Color(0xFF93C5FD)),
                            modifier = Modifier.weight(1.2f).fillMaxHeight().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val exactAmt = (totalRequiredVal - existingAdvance).coerceAtLeast(0.0)
                                onCashChanged("%.0f".format(Locale.US, exactAmt))
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Exact", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1D4ED8))
                            }
                        }
                    }
                }

                // 6. NET STATUS SUMMARY
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        advanceRemainingVal > 0 -> Color(0xFFF0FDF4)
                        remainingDueVal > 0 -> Color(0xFFFEF2F2)
                        else -> Color(0xFFF8FAFC)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (advanceRemainingVal > 0) {
                            Text("बचा एडवांस: ₹%.0f (अगले बिल में जमा)".format(advanceRemainingVal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        } else if (remainingDueVal > 0) {
                            Text("बाकी बकाया: ₹%.0f".format(remainingDueVal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        } else {
                            Text("हिसाब बराबर ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }

                        if (item.suggestedMilkFromCash != null) {
                            Text(
                                "💡 नकद = %.2fL".format(item.suggestedMilkFromCash),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary,
                                modifier = Modifier.clickable { onUseSuggestedMilk(item.suggestedMilkFromCash) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7. BIG ACTION BUTTONS ROW (Naga + Skip + Deliver + WhatsApp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Naga Button [❌ नागा]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (item.isNaga) Color(0xFFDC2626) else Color(0xFFFEE2E2),
                        border = BorderStroke(1.5.dp, if (item.isNaga) Color(0xFF991B1B) else Color(0xFFEF4444)),
                        modifier = Modifier
                            .weight(0.28f)
                            .height(54.dp)
                            .clickable { onNagaClicked() }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "नागा",
                                tint = if (item.isNaga) Color.White else Color(0xFFDC2626),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (item.isNaga) "नागा ✓" else "नागा",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (item.isNaga) Color.White else Color(0xFFB91C1C),
                                maxLines = 1
                            )
                        }
                    }

                    // 2. Skip to Later Button [⏰ बाद में]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(0.28f)
                            .height(54.dp)
                            .clickable { onSkipClicked() }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "बाद में",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "बाद में",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Deliver Button
                    Button(
                        onClick = onSaveClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.44f).height(54.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Text(
                                text = if (item.isDone) "SAVED ✓" else "डिलीवर ✓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // 1-Tap WhatsApp Receipt Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF25D366),
                        modifier = Modifier
                            .size(54.dp)
                            .clickable { onWhatsAppReceiptClicked() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp Receipt", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // 8. SUBTLE GESTURE HINT
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "👈 पिछला | अगला 👉 | ⬇️ डिलीवरी | ⬆️ नागा | 👆👆 Exact",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}


@Composable
fun DairyCalculatorDialog(
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    var litersText by remember { mutableStateOf("1.5") }
    var rateText by remember { mutableStateOf("60") }
    var cashText by remember { mutableStateOf("") }
    var prevDueText by remember { mutableStateOf("0") }

    val liters = litersText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val cash = cashText.toDoubleOrNull() ?: 0.0
    val prevDue = prevDueText.toDoubleOrNull() ?: 0.0

    val bill = CalculationEngine.round2(liters * rate)
    val totalReq = CalculationEngine.round2(bill + prevDue)
    val suggestedMilkFromCash = if (rate > 0 && cash > 0) CalculationEngine.round2(cash / rate) else null
    val net = CalculationEngine.round2(totalReq - cash)
    val remDue = if (net > 0) net else 0.0
    val advance = if (net < 0) CalculationEngine.round2(-net) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dairy Calculator", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = litersText,
                        onValueChange = { litersText = it },
                        label = { Text("Milk (L)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate (₹/L)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prevDueText,
                        onValueChange = { prevDueText = it },
                        label = { Text("Prev Due (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cashText,
                        onValueChange = { cashText = it },
                        label = { Text("Cash Given (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Results Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Milk Bill:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            Text(CalculationEngine.formatMoney(bill), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Required (Bill + Prev):", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            Text(CalculationEngine.formatMoney(totalReq), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (suggestedMilkFromCash != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { litersText = "%.2f".format(Locale.US, suggestedMilkFromCash) },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cash ₹%.0f → Suggested Milk:".format(cash), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.primary)
                                Text("%.2f L (Tap to use)".format(suggestedMilkFromCash), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            }
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (advance > 0) "Advance (अग्रिम):" else "Remaining Due (बकाया):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (advance > 0) colorScheme.primary else StatusRed)
                            Text(if (advance > 0) CalculationEngine.formatMoney(advance) else CalculationEngine.formatMoney(remDue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (advance > 0) colorScheme.primary else StatusRed)
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
        shape = RoundedCornerShape(16.dp),
        containerColor = colorScheme.surface
    )
}

// ---------------------------------------------------------------------------
// 3. QUICK / GUEST CUSTOMER DIALOG
// ---------------------------------------------------------------------------
@Composable
fun QuickCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, milkType: String, liters: Double, rate: Double, cash: Double, saveAsRegular: Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    var name by remember { mutableStateOf("") }
    var milkType by remember { mutableStateOf("भैंस") }
    var litersText by remember { mutableStateOf("1.0") }
    var rateText by remember { mutableStateOf("60") }
    var cashText by remember { mutableStateOf("60") }
    var saveAsRegular by remember { mutableStateOf(false) }

    val liters = litersText.toDoubleOrNull() ?: 1.0
    val rate = rateText.toDoubleOrNull() ?: 60.0
    val bill = CalculationEngine.round2(liters * rate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick / Guest Customer", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
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
                    label = { Text("Name (Optional)") },
                    placeholder = { Text("Guest Customer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Milk type chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("भैंस", "गाय").forEach { type ->
                        FilterChip(
                            selected = milkType == type,
                            onClick = { milkType = type },
                            label = { Text(type, fontWeight = if (milkType == type) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary,
                                selectedLabelColor = colorScheme.onPrimary
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = litersText,
                        onValueChange = {
                            litersText = it
                            val l = it.toDoubleOrNull() ?: 0.0
                            val r = rateText.toDoubleOrNull() ?: 60.0
                            cashText = "%.0f".format(Locale.US, l * r)
                        },
                        label = { Text("Liters (दूध)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = {
                            rateText = it
                            val r = it.toDoubleOrNull() ?: 0.0
                            val l = litersText.toDoubleOrNull() ?: 1.0
                            cashText = "%.0f".format(Locale.US, l * r)
                        },
                        label = { Text("Rate (₹/L)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = { Text("Cash Received (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveAsRegular,
                        onCheckedChange = { saveAsRegular = it },
                        colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save as Regular Route Customer", fontSize = 12.5.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cash = cashText.toDoubleOrNull() ?: bill
                    onSave(name, milkType, liters, rate, cash, saveAsRegular)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE & DELIVER", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = colorScheme.surface
    )
}

@Composable
fun CustomerNotesDialog(
    customerId: Long,
    customerName: String,
    viewModel: DailyTaskViewModel,
    onDismiss: () -> Unit
) {
    val notesFlow = remember(customerId) { viewModel.getNotesForCustomer(customerId) }
    val notes by notesFlow.collectAsState(initial = emptyList())
    var noteContent by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    var isPinned by remember { mutableStateOf(false) }
    val categories = listOf("General", "Delivery", "Payment", "Timing")
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Customer Notes (टिप्पणियाँ)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "#$customerId • $customerName",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add note input
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    placeholder = { Text("Add note (e.g. सुबह 6 बजे, gate से देना है...)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surfaceContainer,
                        unfocusedContainerColor = colorScheme.surfaceContainer
                    )
                )

                // Category selection chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                // Pin toggle & Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isPinned = !isPinned }
                    ) {
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                        Text("Pin note (पिन करें)", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            if (noteContent.isNotBlank()) {
                                viewModel.saveCustomerNote(
                                    customerId = customerId,
                                    content = noteContent,
                                    category = selectedCategory,
                                    isPinned = isPinned
                                )
                                noteContent = ""
                                isPinned = false
                            }
                        },
                        enabled = noteContent.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                // List of existing notes
                if (notes.isEmpty()) {
                    Text(
                        text = "No notes recorded for this customer.",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    notes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (note.isPinned) Color(0xFFFEF3C7) else colorScheme.surfaceContainer,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (note.isPinned) Color(0xFFF59E0B) else colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
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
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (note.isPinned) Color(0xFFB45309) else colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = note.content,
                                        fontSize = 12.5.sp,
                                        color = if (note.isPinned) Color(0xFF78350F) else colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.togglePinCustomerNote(note) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pin",
                                            tint = if (note.isPinned) Color(0xFFD97706) else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteCustomerNote(note.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done (बंद करें)")
            }
        }
    )
}

@Composable
fun RouteManagerDialog(
    availableRoutes: List<RouteEntity>,
    allCustomers: List<CustomerEntity>,
    viewModel: DailyTaskViewModel,
    onDismiss: () -> Unit
) {
    var showCreateRoute by remember { mutableStateOf(false) }
    var newRouteName by remember { mutableStateOf("") }
    var newRouteDesc by remember { mutableStateOf("") }
    var selectedCustomerForAssign by remember { mutableStateOf<CustomerEntity?>(null) }
    var targetRouteId by remember { mutableStateOf("default") }
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Route Manager (मार्ग व्यवस्थापक)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Manage delivery routes, track progress, and assign customers to routes.",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )

                // List of routes
                availableRoutes.forEach { route ->
                    val custCount = allCustomers.count { it.routeId == route.id }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = route.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "$custCount Customers" + if (route.description.isNotBlank()) " • ${route.description}" else "",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            if (route.id != "default") {
                                IconButton(
                                    onClick = { viewModel.deleteRoute(route.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Route",
                                        tint = colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Create new route section
                if (showCreateRoute) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("New Route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newRouteName,
                                onValueChange = { newRouteName = it },
                                placeholder = { Text("Route name (e.g. Gulab Nagar)", fontSize = 11.5.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newRouteDesc,
                                onValueChange = { newRouteDesc = it },
                                placeholder = { Text("Description / notes (optional)", fontSize = 11.5.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showCreateRoute = false }) { Text("Cancel") }
                                Button(
                                    onClick = {
                                        if (newRouteName.isNotBlank()) {
                                            val rId = "route_${System.currentTimeMillis()}"
                                            viewModel.saveRoute(
                                                RouteEntity(
                                                    id = rId,
                                                    name = newRouteName.trim(),
                                                    description = newRouteDesc.trim(),
                                                    sortOrder = availableRoutes.size
                                                )
                                            )
                                            newRouteName = ""
                                            newRouteDesc = ""
                                            showCreateRoute = false
                                        }
                                    },
                                    enabled = newRouteName.isNotBlank()
                                ) {
                                    Text("Create Route")
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showCreateRoute = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add New Route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Assign customer to route
                Text("Assign Customer to Route (ग्राहक जोड़ें)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                var custSearch by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = custSearch,
                    onValueChange = { custSearch = it },
                    placeholder = { Text("Search customer to assign...", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val matchedCustomers = if (custSearch.isBlank()) emptyList() else
                    allCustomers.filter { it.name.contains(custSearch, ignoreCase = true) || it.id.toString() == custSearch }.take(5)

                matchedCustomers.forEach { cust ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomerForAssign = cust }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${cust.id} ${cust.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        val currRouteName = availableRoutes.find { it.id == cust.routeId }?.name ?: "Main Route"
                        Text(currRouteName, fontSize = 11.sp, color = colorScheme.primary)
                    }
                }

                selectedCustomerForAssign?.let { cust ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Move #${cust.id} ${cust.name} to:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableRoutes.forEach { r ->
                                    FilterChip(
                                        selected = targetRouteId == r.id,
                                        onClick = { targetRouteId = r.id },
                                        label = { Text(r.name, fontSize = 10.5.sp) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.assignCustomerToRoute(cust.id, targetRouteId)
                                    selectedCustomerForAssign = null
                                    custSearch = ""
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Assign to Route", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ===========================================================================
// V7.6 DIALOG COMPOSABLES (Auditability, In-Place Edits, Runner Cash)
// ===========================================================================

// 1. EDIT CUSTOMER MASTER DIALOG
@Composable
fun EditCustomerMasterDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var mohalla by remember { mutableStateOf(customer.mohalla) }
    var landmark by remember { mutableStateOf(customer.landmark) }
    var milkType by remember { mutableStateOf(customer.milkType) }
    var defaultLitersText by remember { mutableStateOf(customer.defaultLiters.toString()) }
    var rateText by remember { mutableStateOf(customer.ratePerLiter.toString()) }
    var billingType by remember { mutableStateOf(customer.billingType) }
    var deliveryFrequency by remember { mutableStateOf(customer.deliveryFrequency) }
    var isActive by remember { mutableStateOf(customer.isActive == 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Edit Customer Master",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Customer ID: #${customer.id} (READ-ONLY)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary
                )
            }
        },
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
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Text("Milk Type:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Buffalo", "Cow", "Mix").forEach { m ->
                        FilterChip(
                            selected = milkType.equals(m, ignoreCase = true),
                            onClick = { milkType = m },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultLitersText,
                        onValueChange = { defaultLitersText = it },
                        label = { Text("Default Liters") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate / Liter (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Billing Type:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("daily_cash" to "Daily Cash", "monthly_khata" to "Monthly Khata").forEach { (type, label) ->
                        FilterChip(
                            selected = billingType == type,
                            onClick = { billingType = type },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Frequency:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("daily" to "Daily", "alternate" to "Alternate").forEach { (freq, label) ->
                        FilterChip(
                            selected = deliveryFrequency == freq,
                            onClick = { deliveryFrequency = freq },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customer Active Status", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = customer.copy(
                        name = name.trim().ifBlank { customer.name },
                        phone = phone.trim(),
                        mohalla = mohalla.trim().ifBlank { customer.mohalla },
                        landmark = landmark.trim(),
                        milkType = milkType,
                        defaultLiters = defaultLitersText.toDoubleOrNull() ?: customer.defaultLiters,
                        ratePerLiter = rateText.toDoubleOrNull() ?: customer.ratePerLiter,
                        billingType = billingType,
                        deliveryFrequency = deliveryFrequency,
                        isActive = if (isActive) 1 else 0
                    )
                    onSave(updated)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 2. RECONCILE DUE DIALOG
@Composable
fun ReconcileDueDialog(
    item: DailyCustomerCardItem,
    onDismiss: () -> Unit,
    onConfirm: (targetDue: Double, note: String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var targetDueText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val currentDue = if (item.prevDue > 0) item.prevDue else item.remainingDue
    val targetDue = targetDueText.toDoubleOrNull() ?: 0.0
    val deltaAdjustment = CalculationEngine.round2(currentDue - targetDue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Reconcile / Correct Due (बकाया सुधार)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Customer #${item.customer.id}: ${item.customer.name}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Current Due (वर्तमान बकाया): ${CalculationEngine.formatMoney(currentDue)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentDue > 0) colorScheme.error else colorScheme.onSurface
                        )
                        if (targetDueText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target Due: ${CalculationEngine.formatMoney(targetDue)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.primary
                            )
                            Text(
                                text = "Adjustment Record: ${if (deltaAdjustment >= 0) "-${CalculationEngine.formatMoney(deltaAdjustment)}" else "+${CalculationEngine.formatMoney(-deltaAdjustment)}"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (deltaAdjustment >= 0) colorScheme.primary else Color(0xFFD97706)
                            )
                        }
                    }
                }

                Text("Quick Options:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.clickable { targetDueText = "0" }
                    ) {
                        Text(
                            text = "Set to ₹0 (Cleared)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    if (currentDue > 100.0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable {
                                targetDueText = "%.0f".format(Locale.US, currentDue - 100.0)
                            }
                        ) {
                            Text(
                                text = "Reduce ₹100",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetDueText,
                    onValueChange = { targetDueText = it },
                    label = { Text("Target Due Amount (₹)") },
                    placeholder = { Text("e.g. 0 or 250") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reason / Audit Note") },
                    placeholder = { Text("e.g. Settled offline / correction") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "• Note: An auditable correction record will be written to customer Khata. History is preserved.",
                    fontSize = 10.5.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(targetDue, noteText.trim())
                },
                enabled = targetDueText.isNotBlank()
            ) {
                Text("Confirm Reconcile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 3. ADJUST ADVANCE DIALOG
@Composable
fun AdjustAdvanceDialog(
    item: DailyCustomerCardItem,
    onDismiss: () -> Unit,
    onConfirm: (targetAdvance: Double, note: String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var targetAdvanceText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val currentAdvance = item.advance
    val targetAdvance = targetAdvanceText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Adjust Advance Balance", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Customer #${item.customer.id}: ${item.customer.name}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Current Advance: ${CalculationEngine.formatMoney(currentAdvance)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { targetAdvanceText = "0" }
                    ) {
                        Text(
                            text = "Set to ₹0",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable {
                            targetAdvanceText = "%.0f".format(Locale.US, currentAdvance + 100.0)
                        }
                    ) {
                        Text(
                            text = "+₹100",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable {
                            targetAdvanceText = "%.0f".format(Locale.US, currentAdvance + 500.0)
                        }
                    ) {
                        Text(
                            text = "+₹500",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = targetAdvanceText,
                    onValueChange = { targetAdvanceText = it },
                    label = { Text("New Advance Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reason / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(targetAdvance, noteText.trim()) },
                enabled = targetAdvanceText.isNotBlank()
            ) {
                Text("Save Advance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// 4. OVERRIDE BILL DIALOG
@Composable
fun OverrideBillDialog(
    item: DailyCustomerCardItem,
    onDismiss: () -> Unit,
    onConfirm: (customBill: Double?, customCash: Double?) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val liters = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
    val rate = item.applicableRate
    val calcBill = CalculationEngine.round2(liters * rate)

    var manualBillText by remember { mutableStateOf(calcBill.toString()) }
    var cashText by remember { mutableStateOf(item.currentCashInput) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Override Today's Bill", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Customer #${item.customer.id}: ${item.customer.name}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Calculated Bill: %.2f L × ₹%.0f/L = %s".format(
                                liters,
                                rate,
                                CalculationEngine.formatMoney(calcBill)
                            ),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = manualBillText,
                    onValueChange = { manualBillText = it },
                    label = { Text("Manual Override Bill (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = { Text("Cash Received (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                val enteredBill = manualBillText.toDoubleOrNull()
                if (enteredBill != null && enteredBill != calcBill) {
                    Text(
                        text = "⚠️ Manual bill differs from standard calculation. Difference will be logged in audit notes.",
                        fontSize = 11.sp,
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBill = manualBillText.toDoubleOrNull()
                    val finalCash = cashText.toDoubleOrNull()
                    onConfirm(finalBill, finalCash)
                }
            ) {
                Text("Apply Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// 5. MILK NUMERIC EDITOR DIALOG
@Composable
fun MilkEditorDialog(
    item: DailyCustomerCardItem,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var litersText by remember { mutableStateOf(item.currentMilkInput) }
    val currentLiters = litersText.toDoubleOrNull() ?: item.customer.defaultLiters

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Milk Quantity", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Customer #${item.customer.id}: ${item.customer.name} (@ ₹%.0f/L)".format(item.applicableRate),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Steppers Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val next = (currentLiters - 0.5).coerceAtLeast(0.0)
                            litersText = "%.2f".format(Locale.US, next)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-0.5L", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val next = currentLiters + 0.5
                            litersText = "%.2f".format(Locale.US, next)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+0.5L", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val next = currentLiters + 1.0
                            litersText = "%.2f".format(Locale.US, next)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1.0L", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Preset Chips Grid
                Text("Presets:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                val presets = listOf(1.0, 1.5, 2.0, 2.25, 2.50, 2.75, 3.0, 4.0, 5.0)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { p ->
                        FilterChip(
                            selected = "%.2f".format(Locale.US, currentLiters) == "%.2f".format(Locale.US, p),
                            onClick = { litersText = "%.2f".format(Locale.US, p) },
                            label = { Text("%.2f L".format(p), fontSize = 11.sp) }
                        )
                    }
                }

                // Direct Decimal Input
                OutlinedTextField(
                    value = litersText,
                    onValueChange = { litersText = it },
                    label = { Text("Exact Quantity (Liters)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("L", fontWeight = FontWeight.Bold, color = colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth()
                )

                val billEst = CalculationEngine.round2(currentLiters * item.applicableRate)
                Text(
                    text = "Estimated Bill: ${CalculationEngine.formatMoney(billEst)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(currentLiters) }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// 6. PBS / MANUAL BOOK MANAGER DIALOG (With Full Audit History)
@Composable
fun PbsManagerDialog(
    item: DailyCustomerCardItem,
    viewModel: DailyTaskViewModel,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Adjust, 1 = History
    var amountText by remember { mutableStateOf(if (item.pbsPending > 0) "%.0f".format(Locale.US, item.pbsPending) else "") }
    var reasonText by remember { mutableStateOf("") }
    var historyItems by remember { mutableStateOf<List<PbsHistoryItem>>(emptyList()) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            historyItems = viewModel.getPbsHistory(item.customer.id)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("PBS / External Book Manager", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Customer #${item.customer.id}: ${item.customer.name} (Current: ${CalculationEngine.formatMoney(item.pbsPending)})",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedTab == 0) colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                    ) {
                        Text(
                            text = "Edit / Adjust",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) colorScheme.onPrimary else colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedTab == 1) colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                    ) {
                        Text(
                            text = "Audit History",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) colorScheme.onPrimary else colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Quick Set Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { amountText = "0" }
                        ) {
                            Text("Clear to ₹0", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable {
                                val curr = amountText.toDoubleOrNull() ?: item.pbsPending
                                amountText = "%.0f".format(Locale.US, (curr - 100.0).coerceAtLeast(0.0))
                            }
                        ) {
                            Text("-₹100", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable {
                                val curr = amountText.toDoubleOrNull() ?: item.pbsPending
                                amountText = "%.0f".format(Locale.US, curr + 100.0)
                            }
                        ) {
                            Text("+₹100", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("PBS Pending Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason / Note for Change") },
                        placeholder = { Text("e.g. Copied from paper diary / Paid in cash") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val newAmt = amountText.toDoubleOrNull() ?: 0.0
                    val delta = newAmt - item.pbsPending
                    if (delta != 0.0) {
                        Text(
                            text = "Adjustment Delta: ${if (delta > 0) "+₹%.0f".format(delta) else "-₹%.0f".format(-delta)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (delta > 0) Color(0xFFD97706) else colorScheme.primary
                        )
                    }
                } else {
                    // History List
                    if (historyItems.isEmpty()) {
                        Text(
                            text = "No recorded adjustments for this customer yet.",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        historyItems.forEach { h ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(h.timestamp, fontSize = 10.5.sp, color = colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (h.delta >= 0) "+₹%.0f".format(h.delta) else "-₹%.0f".format(-h.delta),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (h.delta >= 0) Color(0xFFD97706) else colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = "₹%.0f ➔ ₹%.0f".format(h.oldAmount, h.newAmount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (h.reason.isNotBlank()) {
                                        Text(text = "Note: ${h.reason}", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        val newAmount = amountText.toDoubleOrNull() ?: 0.0
                        viewModel.updatePbsWithHistory(
                            customerId = item.customer.id,
                            currentAmt = item.pbsPending,
                            newAmt = newAmount,
                            reason = reasonText.trim()
                        ) {
                            onDismiss()
                        }
                    }
                ) {
                    Text("Save Adjustment")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (selectedTab == 0) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// 7. RUNNER CASH RECEIPT CONFIRMATION DIALOG
@Composable
fun RunnerCashConfirmDialog(
    item: DailyCustomerCardItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val liters = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
    val rate = item.applicableRate
    val bill = CalculationEngine.round2(liters * rate)
    val cash = item.currentCashInput.toDoubleOrNull() ?: 0.0
    val totalReq = CalculationEngine.round2(bill + item.prevDue)
    val net = CalculationEngine.round2(totalReq - cash)
    val newDue = if (net > 0) net else 0.0
    val advance = if (net < 0) CalculationEngine.round2(-net) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Delivery & Cash",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#${item.routeIndex + 1} ${item.customer.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Text(
                    text = "${item.customer.mohalla}${if (item.customer.landmark.isNotBlank()) " • ${item.customer.landmark}" else ""}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Milk:", fontSize = 12.5.sp, color = colorScheme.onSurfaceVariant)
                            Text("%.2f L (@ ₹%.0f/L)".format(liters, rate), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Today's Bill:", fontSize = 12.5.sp, color = colorScheme.onSurfaceVariant)
                            Text(CalculationEngine.formatMoney(bill), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Previous Due:", fontSize = 12.5.sp, color = colorScheme.onSurfaceVariant)
                            Text(CalculationEngine.formatMoney(item.prevDue), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Required:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(CalculationEngine.formatMoney(totalReq), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CASH RECEIVED:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            Text(
                                text = CalculationEngine.formatMoney(cash),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (advance > 0) "Advance Balance:" else "Remaining Due:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (advance > 0) colorScheme.primary else colorScheme.error
                            )
                            Text(
                                text = CalculationEngine.formatMoney(if (advance > 0) advance else newDue),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (advance > 0) colorScheme.primary else colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("CONFIRM & SAVE RECEIPT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Back")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// 8. RUNNER MORE MENU DIALOG (V8.0)
// ---------------------------------------------------------------------------
@Composable
fun RunnerMoreMenuDialog(
    item: DailyCustomerCardItem,
    onDismiss: () -> Unit,
    onOpenKhata: () -> Unit,
    onOpenQr: () -> Unit,
    onOpenReceipt: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onEditMaster: () -> Unit,
    onAiCleanName: () -> Unit = {},
    onAiDeliveryNote: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = item.customer.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Text(
                    text = "Customer #${item.customer.id} • ${item.customer.mohalla}${if (item.customer.landmark.isNotBlank()) " • ${item.customer.landmark}" else ""}",
                    fontSize = 11.5.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Balance mini card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(0.5.dp, colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Today's Bill: ${CalculationEngine.formatMoney(item.todayBill)}", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                            Text("Cash: ${CalculationEngine.formatMoney(item.currentCashInput.toDoubleOrNull() ?: 0.0)}", fontSize = 11.sp, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (item.advance > 0) {
                                Text("Advance: ${CalculationEngine.formatMoney(item.advance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            } else {
                                Text("Due: ${CalculationEngine.formatMoney(item.remainingDue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (item.remainingDue > 0) colorScheme.error else colorScheme.onSurface)
                            }
                            if (item.pbsPending > 0) {
                                Text("PBS: ${CalculationEngine.formatMoney(item.pbsPending)}", fontSize = 10.5.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // AI Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAiCleanName()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7E22CE)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("✨ AI नाम सुधारें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAiDeliveryNote()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🎙️ AI नोट", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action grid / list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenKhata()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Khata", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenQr()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UPI QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenReceipt()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenNotes()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenWhatsApp()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onEditMaster()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Master", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}

/**
 * AI-assisted voice/typing delivery note dialog.
 * Saves concise instructions (e.g. "Gate band rehta hai", "2nd floor bell") pinned to customer in Room DB.
 */
@Composable
fun AiDeliveryNoteDialog(
    item: DailyCustomerCardItem,
    aiClient: PrivateAiApiClient,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var noteText by remember { mutableStateOf(item.pinnedNote ?: "") }
    var isAiFormatting by remember { mutableStateOf(false) }

    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                noteText = spoken
                if (aiClient.isAiEnabled) {
                    isAiFormatting = true
                    coroutineScope.launch {
                        val formatted = aiClient.formatDeliveryNote(spoken)
                        if (formatted.isNotBlank()) {
                            noteText = formatted
                        }
                        isAiFormatting = false
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("AI डिलीवरी नोट", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("#${item.routeIndex + 1} ${item.customer.name} (${item.customer.mohalla})", fontSize = 11.5.sp, color = colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 बोलकर या लिखकर निर्देश दें (उदा: 'गेट बंद रहता है 2nd फ्लोर पर घंटी बजाना' या 'कल 1L ज्यादा देना')",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("डिलीवरी नोट (निर्देश)") },
                    placeholder = { Text("यहाँ लिखें या माइक दबाकर बोलें...") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "डिलीवरी नोट बोलें...")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "माइक उपलब्ध नहीं है, कृपया टाइप करें", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Speak", tint = colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                isAiFormatting = true
                                coroutineScope.launch {
                                    val formatted = aiClient.formatDeliveryNote(noteText)
                                    if (formatted.isNotBlank()) {
                                        noteText = formatted
                                    }
                                    isAiFormatting = false
                                }
                            }
                        },
                        enabled = !isAiFormatting && noteText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isAiFormatting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("✨ AI संक्षेप करें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    if (item.pinnedNote != null) {
                        TextButton(
                            onClick = { onSaveNote("") },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Text("नोट हटाएं", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveNote(noteText.trim()) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("📌 पिन व सहेजें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}

/**
 * AI-assisted customer name and mohalla cleaner dialog.
 * Strips messy emojis, isolates profession/nickname, and cleans mohalla name.
 */
@Composable
fun AiCustomerCleanDialog(
    item: DailyCustomerCardItem,
    aiClient: PrivateAiApiClient,
    onDismiss: () -> Unit,
    onSave: (cleanName: String, cleanMohalla: String, landmarkWithNick: String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    var cleanName by remember { mutableStateOf(item.customer.name) }
    var nickname by remember { mutableStateOf("") }
    var cleanMohalla by remember { mutableStateOf(item.customer.mohalla) }
    var isCleaning by remember { mutableStateOf(false) }

    LaunchedEffect(item.customer.id) {
        if (aiClient.isAiEnabled) {
            isCleaning = true
            coroutineScope.launch {
                val (cName, nick, cMohalla) = aiClient.cleanCustomerIdentity(item.customer.name, item.customer.mohalla)
                cleanName = cName
                nickname = nick
                cleanMohalla = cMohalla
                isCleaning = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("AI नाम व पता सुधारक", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("अनावश्यक इमोजी व कचरा हटाकर साफ करें", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("मूल नाम: ${item.customer.name}", fontSize = 11.5.sp, color = colorScheme.onSurfaceVariant)
                        Text("मूल मोहल्ला: ${item.customer.mohalla}", fontSize = 11.5.sp, color = colorScheme.onSurfaceVariant)
                    }
                }

                if (isCleaning) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Debian AI नाम साफ कर रहा है...", fontSize = 11.5.sp, color = colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = cleanName,
                    onValueChange = { cleanName = it },
                    label = { Text("साफ नाम (Proper Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("उपनाम / पेशा / उर्फ (वैकल्पिक)") },
                    placeholder = { Text("उदा: दर्जी, मास्टर जी, नैनुआ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = cleanMohalla,
                    onValueChange = { cleanMohalla = it },
                    label = { Text("साफ मोहल्ला / गली") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLandmark = if (nickname.isNotBlank()) {
                        if (item.customer.landmark.isNotBlank() && !item.customer.landmark.contains(nickname)) {
                            "${item.customer.landmark} ($nickname)"
                        } else if (item.customer.landmark.isBlank()) {
                            nickname
                        } else item.customer.landmark
                    } else item.customer.landmark
                    onSave(cleanName.trim(), cleanMohalla.trim(), finalLandmark)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("स्वीकार करें व सहेजें ✓")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}

/**
 * Courteous face-to-face Hindi collection reminder dialogue.
 * Offers 1-tap TTS vocalization and WhatsApp reminder for customers with balance > ₹500.
 */
@Composable
fun FaceToFaceReminderDialog(
    item: DailyCustomerCardItem,
    voiceManager: VoiceFeedbackManager,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val dueAmt = if (item.prevDue > 0) item.prevDue else item.remainingDue
    val totalAmt = item.totalRequired

    val politeHindiScript = "आदाब / नमस्ते ${item.customer.name} जी, पिछला हिसाब ₹%.0f बाकी चल रहा है। आज का बिल मिलाकर कुल ₹%.0f हुआ है। कृपया देख लीजिएगा।".format(Locale.US, dueAmt, totalAmt)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFFC2410C), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("विनम्र तकादा (Face-to-Face)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("#${item.routeIndex + 1} ${item.customer.name} (बकाया: ₹%.0f)".format(Locale.US, dueAmt), fontSize = 11.5.sp, color = colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🗣️ ग्राहक से बोलने के लिए शब्द:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$politeHindiScript\"",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF78350F),
                            lineHeight = 19.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            voiceManager.speak(politeHindiScript)
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("बोलें (TTS)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val cleanPhone = item.customer.phone.replace("+", "").replace(" ", "").replace("-", "")
                            val url = if (cleanPhone.isNotBlank()) {
                                val fullPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                "https://api.whatsapp.com/send?phone=$fullPhone&text=" + URLEncoder.encode(politeHindiScript, "UTF-8")
                            } else {
                                "https://api.whatsapp.com/send?text=" + URLEncoder.encode(politeHindiScript, "UTF-8")
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1.2f).height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}

// ---------------------------------------------------------------------------
// 9. RUNNER DUE UPI QR DIALOG (V8.0)
// ---------------------------------------------------------------------------
@Composable
fun RunnerDueQrDialog(
    item: DailyCustomerCardItem,
    upiId: String,
    payeeName: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val effectiveAmount = if (item.remainingDue > 0) item.remainingDue else if (item.todayBill > 0) item.todayBill else 0.0

    val upiUri = remember(item, upiId, effectiveAmount) {
        val cleanUpi = upiId.ifBlank { "9897259461@upi" }
        UpiQrGenerator.buildUpiUri(
            upiId = cleanUpi,
            payeeName = payeeName.ifBlank { "Shahbuddin Dairy" },
            amount = if (effectiveAmount > 0) effectiveAmount else null,
            note = "Due Cust #${item.customer.id} ${item.customer.name}"
        )
    }

    val qrBitmap = remember(upiUri) {
        UpiQrGenerator.generateUpiQrBitmap(upiUri, size = 480)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customer Due UPI QR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.customer.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Customer #${item.customer.id} • Due: ${CalculationEngine.formatMoney(effectiveAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (effectiveAmount > 0) colorScheme.error else colorScheme.primary
                )

                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI Due QR",
                            modifier = Modifier.size(200.dp).padding(6.dp)
                        )
                    }
                }

                Text(
                    text = "Scan with any UPI App (GPay, PhonePe, Paytm, BHIM)\nAmount ₹${"%.0f".format(effectiveAmount)} pre-filled",
                    fontSize = 10.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = "शाहबुद्दीन डेयरी (Shahbuddin Dairy)\nग्राहक: ${item.customer.name} (#${item.customer.id})\nबकाया राशि: ₹${"%.0f".format(effectiveAmount)}\nUPI ID: ${upiId.ifBlank { "9897259461@upi" }}\nभुगतान लिंक: $upiUri"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Due UPI Link"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SHARE UPI LINK")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}

// ---------------------------------------------------------------------------
// 10. RUNNER RECEIPT SHARE DIALOG (V8.0 Image / PDF / WhatsApp)
// ---------------------------------------------------------------------------
@Composable
fun RunnerReceiptShareDialog(
    item: DailyCustomerCardItem,
    upiId: String,
    dairyPhone: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val receiptData = remember(item, upiId, dairyPhone) {
        val liters = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
        val rate = item.applicableRate
        val bill = item.todayBill
        val cash = item.currentCashInput.toDoubleOrNull() ?: 0.0
        ReceiptData(
            businessName = "शाहबुद्दीन डेयरी (Shahbuddin Dairy)",
            businessPhone = dairyPhone.ifBlank { "9897259461" },
            upiId = upiId,
            customerName = item.customer.name,
            customerId = item.customer.id,
            date = CalculationEngine.todayStr(),
            milkLiters = liters,
            milkType = item.customer.milkType,
            rate = rate,
            billAmount = bill,
            previousDue = item.prevDue,
            cashPaid = cash,
            totalPaid = cash,
            advance = item.advance,
            remainingDue = item.remainingDue,
            pbsPending = item.pbsPending,
            receiptNumber = "RCP-${item.customer.id}-${System.currentTimeMillis() % 100000}"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Share Delivery Receipt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${item.customer.name} • #${item.customer.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = colorScheme.primary
                )
                Text(
                    text = "Milk: ${receiptData.milkLiters}L • Bill: ₹%.0f • Paid: ₹%.0f • Net Due: ₹%.0f".format(
                        receiptData.billAmount,
                        receiptData.totalPaid,
                        receiptData.remainingDue
                    ),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 1. Image Receipt
                Button(
                    onClick = {
                        val upiUri = if (upiId.isNotBlank()) UpiQrGenerator.buildUpiUri(upiId, "Shahbuddin Dairy", receiptData.remainingDue) else null
                        val qrBitmap = upiUri?.let { UpiQrGenerator.generateUpiQrBitmap(it, 250) }
                        val bmp = ReceiptImageGenerator.createReceiptBitmap(receiptData, qrBitmap)
                        val file = ReceiptImageGenerator.saveReceiptImage(context, bmp, receiptData.customerId)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
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
                    Text("SHARE DIGITAL IMAGE RECEIPT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // 2. PDF Receipt
                OutlinedButton(
                    onClick = {
                        val pdfRes = com.shahbuddindairy.app.pdf.PdfReportGenerator.generateSingleReceiptPdf(context, receiptData)
                        if (pdfRes.success && pdfRes.uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, pdfRes.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt PDF"))
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SHARE PDF RECEIPT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // 3. WhatsApp Message
                OutlinedButton(
                    onClick = {
                        val text = "शाहबुद्दीन डेयरी (Shahbuddin Dairy)\nरसीद: ${receiptData.receiptNumber}\nग्राहक: ${receiptData.customerName} (#${receiptData.customerId})\nदिनांक: ${receiptData.date}\nदूध: ${receiptData.milkLiters}L (${receiptData.milkType}) @ ₹${receiptData.rate}/L\nआज का बिल: ₹${receiptData.billAmount}\nपिछला बकाया: ₹${receiptData.previousDue}\nजमा राशि: ₹${receiptData.totalPaid}\nकुल शेष बकाया: ₹${receiptData.remainingDue}\n${if (upiId.isNotBlank()) "UPI ID: $upiId\n" else ""}धन्यवाद!"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }, "Share Receipt via WhatsApp")
                            context.startActivity(chooser)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SEND VIA WHATSAPP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}



