package com.shahbuddindairy.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
    onNavigateToCustomerKhata: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingConfirmItem by viewModel.pendingConfirmItem.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Mode: 0 = Route List, 1 = Runner Mode (1-by-1 step)
    var viewMode by remember { mutableIntStateOf(0) }
    var activeCustomerId by remember { mutableStateOf<Long?>(null) }
    var showRouteReorderDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showQuickCustomerDialog by remember { mutableStateOf(false) }
    var customerForNotes by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showRouteManagerDialog by remember { mutableStateOf(false) }

    // External PBS Pending Amount Edit Dialog State
    var editingPbsCustomer by remember { mutableStateOf<Pair<Long, Double>?>(null) }
    var editingPbsText by remember { mutableStateOf("") }

    // Synchronize ongoing delivery route notification
    LaunchedEffect(uiState.selectedDate, uiState.completedCount) {
        DailyTaskNotificationManager.updateNotification(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // TOP APP HEADER
        AppTopHeader(
            title = "Daily Task",
            subtitle = "वितरण सूची • ${CalculationEngine.formatDateDisplay(uiState.selectedDate)}",
            marketDue = uiState.totalMarketDue
        )

        // DATE SELECTOR BAR
        DateSelectorHeader(
            selectedDate = uiState.selectedDate,
            onPrevDay = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onPreviousDay()
            },
            onNextDay = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onNextDay()
            },
            onToday = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onDateSelected(CalculationEngine.todayStr())
            }
        )

        // MOTIVATION / DAILY FACTUAL PROGRESS BANNER
        if (uiState.showMotivation && uiState.motivationText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorScheme.secondaryContainer.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.motivationText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.toggleMotivation(false) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // METRICS DASHBOARD STRIP (With Total Route Progress)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Today's Milk",
                value = CalculationEngine.formatLiters(uiState.totalMilk),
                subValue = "Bill: ${CalculationEngine.formatMoney(uiState.totalBill)}",
                valueColor = colorScheme.primary,
                icon = Icons.Default.WaterDrop,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Cash Collected",
                value = CalculationEngine.formatMoney(uiState.totalCashCollected),
                subValue = "Due: ${CalculationEngine.formatMoney(uiState.totalMarketDue)}",
                valueColor = colorScheme.primary,
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
            val totalRouteCount = uiState.completedCount + uiState.pendingCount + uiState.nagaCount
            StatCard(
                label = "Route Progress",
                value = "${uiState.completedCount} / $totalRouteCount",
                subValue = "Pending: ${uiState.pendingCount} • Naga: ${uiState.nagaCount}",
                valueColor = if (uiState.pendingCount == 0 && uiState.completedCount > 0) colorScheme.primary else Color(0xFFE65100),
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f)
            )
        }

        // ROUTE SELECTOR CHIPS & ROUTE MANAGER ROW
        if (uiState.availableRoutes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route Manager Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showRouteManagerDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = "Manage Routes",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Routes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }

                // All Routes Chip
                val isAllSel = uiState.selectedRouteId == "ALL"
                FilterChip(
                    selected = isAllSel,
                    onClick = { viewModel.selectRouteFilter("ALL") },
                    label = { Text("All Routes (${uiState.customerCards.size})", fontSize = 11.sp, fontWeight = if (isAllSel) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorScheme.primary,
                        selectedLabelColor = colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                uiState.availableRoutes.forEach { route ->
                    val isSel = uiState.selectedRouteId == route.id
                    FilterChip(
                        selected = isSel,
                        onClick = { viewModel.selectRouteFilter(route.id) },
                        label = { Text(route.name, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // LIVE ROUTE PROGRESS BAR
        uiState.routeProgress?.let { prog ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${prog.routeName}: ${prog.deliveredCount}/${prog.totalCustomers} Delivered",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${prog.percentComplete}% (${prog.pendingCount} Pending)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (prog.percentComplete / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceContainerHigh
                    )
                }
            }
        }

        // VIEW MODE TOGGLE & SEARCH ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search name, ID, phone, mohalla...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = colorScheme.onSurfaceVariant)
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
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )

            // Mode Toggle Button (List vs Runner)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewMode = if (viewMode == 0) 1 else 0
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (viewMode == 0) Icons.AutoMirrored.Filled.DirectionsRun else Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (viewMode == 0) "Runner" else "List",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
            }

            // Route Reorder Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showRouteReorderDialog = true
                }
            ) {
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Route Order",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Dairy Calculator Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showCalculatorDialog = true
                }
            ) {
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculator",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Quick Guest Sale Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showQuickCustomerDialog = true
                }
            ) {
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Quick Customer",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }


        // FILTER CHIPS ROW
        FilterChipsRow(
            activeFilter = uiState.activeFilter,
            onFilterSelected = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onFilterSelected(it)
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // MAIN CONTENT AREA
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else if (uiState.customerCards.isEmpty()) {
            EmptyState(
                title = if (uiState.searchQuery.isNotBlank()) "No Matching Customers" else "No Deliveries Pending",
                message = if (uiState.searchQuery.isNotBlank())
                    "No customers found matching \"${uiState.searchQuery}\""
                else "No customers found for the selected filter on this date."
            )
        } else if (viewMode == 1) {
            // RUNNER MODE (Single customer 1-hand delivery runner)
            val cards = uiState.customerCards
            val currentItem = cards.find { it.customer.id == activeCustomerId } ?: cards.firstOrNull()
            val currentIndex = if (currentItem != null) cards.indexOf(currentItem).coerceAtLeast(0) else 0
            if (activeCustomerId != currentItem?.customer?.id) {
                activeCustomerId = currentItem?.customer?.id
            }

            if (currentItem != null) {
                DailyTaskRunnerCard(
                    item = currentItem,
                    currentIndex = currentIndex,
                    totalCount = cards.size,
                    completedCount = uiState.completedCount,
                    pendingCount = uiState.pendingCount,
                    nagaCount = uiState.nagaCount,
                    onPrev = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentIndex > 0) activeCustomerId = cards[currentIndex - 1].customer.id
                    },
                    onNext = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentIndex < cards.size - 1) activeCustomerId = cards[currentIndex + 1].customer.id
                    },
                    onQuantityChanged = { q -> viewModel.onQuantityChanged(currentItem.customer.id, q) },
                    onCashChanged = { c -> viewModel.onCashChanged(currentItem.customer.id, c) },
                    onUseSuggestedMilk = { suggestedLiters ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.useSuggestedMilk(currentItem.customer.id, suggestedLiters)
                    },
                    onSaveClicked = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.requestConfirmSave(currentItem)
                    },
                    onNagaClicked = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextTargetId = cards.getOrNull(currentIndex + 1)?.customer?.id ?: cards.getOrNull(currentIndex - 1)?.customer?.id
                        viewModel.markNaga(currentItem) {
                            if (nextTargetId != null) activeCustomerId = nextTargetId
                        }
                    },

                    onKhataClicked = { onNavigateToCustomerKhata(currentItem.customer.id) },
                    onPbsEditClicked = {
                        editingPbsCustomer = Pair(currentItem.customer.id, currentItem.pbsPending)
                        editingPbsText = if (currentItem.pbsPending > 0) "%.0f".format(Locale.US, currentItem.pbsPending) else ""
                    },
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
                    }
                )
            }
        } else {
            // ROUTE LIST MODE (Compact scrollable list)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.customerCards, key = { it.customer.id }) { cardItem ->
                    val cardIndex = uiState.customerCards.indexOf(cardItem)
                    DailyCustomerCard(
                        item = cardItem,
                        onQuantityChanged = { q -> viewModel.onQuantityChanged(cardItem.customer.id, q) },
                        onCashChanged = { c -> viewModel.onCashChanged(cardItem.customer.id, c) },
                        onUseSuggestedMilk = { suggestedLiters ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.useSuggestedMilk(cardItem.customer.id, suggestedLiters)
                        },
                        onSaveClicked = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.requestConfirmSave(cardItem)
                        },
                        onNagaClicked = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.markNaga(cardItem)
                        },
                        onKhataClicked = { onNavigateToCustomerKhata(cardItem.customer.id) },
                        onPbsEditClicked = {
                            editingPbsCustomer = Pair(cardItem.customer.id, cardItem.pbsPending)
                            editingPbsText = if (cardItem.pbsPending > 0) "%.0f".format(Locale.US, cardItem.pbsPending) else ""
                        },
                        onNextClicked = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (cardIndex < uiState.customerCards.size - 1) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(cardIndex + 1)
                                }
                            }
                        },
                        onCallClicked = {
                            if (cardItem.customer.phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cardItem.customer.phone}"))
                                context.startActivity(intent)
                            }
                        },
                        onNotesClicked = {
                            customerForNotes = Pair(cardItem.customer.id, cardItem.customer.name)
                        },
                        onOneTapPayment = { amt ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.recordOneTapPayment(cardItem.customer.id, amt)
                        }
                    )
                }
            }
        }
    }

    // SAFE TRANSACTION CONFIRMATION DIALOG
    pendingConfirmItem?.let { item ->
        val liters = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
        val rate = item.applicableRate
        val bill = CalculationEngine.round2(liters * rate)
        val cash = item.currentCashInput.toDoubleOrNull() ?: 0.0
        val totalReq = CalculationEngine.round2(bill + item.prevDue)
        val net = CalculationEngine.round2(totalReq - cash)
        val newDue = if (net > 0) net else 0.0
        val advance = if (net < 0) CalculationEngine.round2(-net) else 0.0

        TransactionConfirmDialog(
            customerName = item.customer.name,
            customerId = item.customer.id,
            liters = liters,
            rate = rate,
            todayBill = bill,
            prevDue = item.prevDue,
            todayCash = cash,
            newDue = newDue,
            advance = advance,
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val currentCards = uiState.customerCards
                val cIdx = currentCards.indexOfFirst { it.customer.id == item.customer.id }
                val nextTargetId = if (cIdx >= 0) {
                    currentCards.getOrNull(cIdx + 1)?.customer?.id ?: currentCards.getOrNull(cIdx - 1)?.customer?.id
                } else null

                viewModel.confirmAndSaveTransaction {
                    if (viewMode == 1 && nextTargetId != null) {
                        activeCustomerId = nextTargetId
                    }
                }
            },
            onDismiss = { viewModel.dismissConfirmDialog() }

        )
    }

    // EDIT EXTERNAL BOOK / PBS PENDING DIALOG
    editingPbsCustomer?.let { (cId, currentAmt) ->
        val customerName = uiState.customerCards.find { it.customer.id == cId }?.customer?.name ?: "Customer"
        AlertDialog(
            onDismissRequest = { editingPbsCustomer = null },
            title = {
                Text(
                    text = "Book / PBS Pending Amount",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Customer #$cId: $customerName\n(बाहरी कॉपी का बकाया — डिजिटल खाते से अलग रहेगा)",
                        fontSize = 12.5.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editingPbsText,
                        onValueChange = { editingPbsText = it },
                        label = { Text("Manual Book Amount (₹)") },
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
                        viewModel.updatePbsPending(cId, amount)
                        editingPbsCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("Save (सुरक्षित करें)")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPbsCustomer = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ROUTE REORDER DIALOG
    if (showRouteReorderDialog) {
        RouteReorderDialog(
            initialCustomers = uiState.allActiveCustomers,
            onDismiss = { showRouteReorderDialog = false },
            onSaveOrder = { newOrder: List<CustomerEntity> ->
                viewModel.saveRouteOrder(newOrder) {
                    showRouteReorderDialog = false
                }
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
}



@Composable
private fun DateSelectorHeader(
    selectedDate: String,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val isToday = selectedDate == CalculationEngine.todayStr()
    val colorScheme = MaterialTheme.colorScheme

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
            IconButton(
                onClick = onPrevDay,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = colorScheme.primary)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToday() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = CalculationEngine.formatDateDisplay(selectedDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    if (isToday) {
                        Text(
                            text = "TODAY (आज)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = onNextDay,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = colorScheme.primary)
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    activeFilter: DailyFilter,
    onFilterSelected: (DailyFilter) -> Unit
) {
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DailyFilter.values().forEach { filter ->
            val isSelected = activeFilter == filter
            val label = when (filter) {
                DailyFilter.ALL -> "All"
                DailyFilter.PENDING -> "Pending"
                DailyFilter.DONE -> "Done ✓"
                DailyFilter.NAGA -> "Naga (नागा)"
                DailyFilter.ALTERNATE -> "Alternate"
                DailyFilter.PAYMENT_PENDING -> "Payment Pending"
                DailyFilter.DUE -> "Due"
                DailyFilter.ADVANCE -> "Advance"
            }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary,
                    selectedLabelColor = colorScheme.onPrimary,
                    containerColor = colorScheme.surface,
                    labelColor = colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun DailyCustomerCard(
    item: DailyCustomerCardItem,
    onQuantityChanged: (String) -> Unit,
    onCashChanged: (String) -> Unit,
    onUseSuggestedMilk: (Double) -> Unit,
    onSaveClicked: () -> Unit,
    onNagaClicked: () -> Unit,
    onKhataClicked: () -> Unit,
    onPbsEditClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onCallClicked: () -> Unit,
    onNotesClicked: () -> Unit = {},
    onOneTapPayment: (Double) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (item.isDone) 1.5.dp else 1.dp,
                color = if (item.isDone) colorScheme.primary.copy(alpha = 0.8f) else colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .animateContentSize()
        ) {
            // 1. HEADER ROW: Route #, Name, Status, Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route # Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.routeIndex + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.customer.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        if (item.hasUnsavedChanges) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE65100))
                            )
                        }
                        if (item.customer.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onCallClicked() }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ID: #${item.customer.id} • ${item.customer.mohalla}${if (item.customer.landmark.isNotBlank()) " • ${item.customer.landmark}" else ""}${if (item.routeName.isNotBlank()) " • 📍 ${item.routeName}" else ""}",
                            fontSize = 11.5.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }


                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(status = item.statusText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        MilkTypeBadge(milkType = item.customer.milkType)
                        RateBadge(rate = item.applicableRate)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))

            // PINNED CUSTOMER NOTE BADGE
            if (item.pinnedNote != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotesClicked() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📌", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.pinnedNote,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E),
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 2. MILK QUANTITY & BILL ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Editable Milk Quantity
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = "MILK QUANTITY (दूध)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = item.currentMilkInput,
                        onValueChange = onQuantityChanged,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("L", fontWeight = FontWeight.Bold, color = colorScheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surfaceContainer,
                            unfocusedContainerColor = colorScheme.surfaceContainer,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Increment Steppers
                Column(modifier = Modifier.weight(0.7f)) {
                    Text(
                        text = "QUICK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .clickable {
                                    val curr = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
                                    onQuantityChanged("%.2f".format(Locale.US, (curr + 0.5)))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+0.5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .clickable {
                                    val curr = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
                                    onQuantityChanged("%.2f".format(Locale.US, (curr + 1.0)))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+1.0", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                    }
                }

                // Today's Bill Box
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TODAY'S BILL",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = CalculationEngine.formatMoney(item.todayBill),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "@ ₹%.0f/L".format(item.applicableRate),
                        fontSize = 9.5.sp,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }

            // 3. FLEXIBLE CASH -> MILK CONVERSION CHIP (For Daily Cash Customers)
            if (item.suggestedMilkFromCash != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseSuggestedMilk(item.suggestedMilkFromCash) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💡 Cash ₹${item.currentCashInput} = %.2f L Milk".format(item.suggestedMilkFromCash),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = "USE %.2f L ▶".format(item.suggestedMilkFromCash),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. FINANCIAL BREAKDOWN ROW: Prev Due, Total Required, Cash Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Due
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                        .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "PREV DUE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CalculationEngine.formatMoney(item.prevDue),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.prevDue > 0) StatusRed else colorScheme.onSurface
                    )
                }

                // Total Required
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                        .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "TOTAL REQ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CalculationEngine.formatMoney(item.totalRequired),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }

                // Cash Paid Input
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "CASH (नकद)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    OutlinedTextField(
                        value = item.currentCashInput,
                        onValueChange = onCashChanged,
                        placeholder = { Text("₹ 0", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surfaceContainer,
                            unfocusedContainerColor = colorScheme.surfaceContainer,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ONE-TAP QUICK PAYMENT RECONCILIATION ROW
            if (item.prevDue > 0) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Quick:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onOneTapPayment(item.prevDue) }
                    ) {
                        Text(
                            text = "Pay Full ₹%.0f".format(item.prevDue),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    if (item.prevDue >= 100.0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { onOneTapPayment(100.0) }
                        ) {
                            Text(
                                text = "Pay ₹100",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (item.prevDue >= 500.0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { onOneTapPayment(500.0) }
                        ) {
                            Text(
                                text = "Pay ₹500",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 5. NET DUE / ADVANCE BANNER
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (item.advance > 0) colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else if (item.remainingDue > 0) colorScheme.errorContainer.copy(alpha = 0.4f)
                        else colorScheme.surfaceContainer
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.advance > 0) {
                    Text(text = "Advance Balance:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.primary)
                    Text(text = CalculationEngine.formatMoney(item.advance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                } else if (item.remainingDue > 0) {
                    Text(text = "Remaining Due (बकाया):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.error)
                    Text(text = CalculationEngine.formatMoney(item.remainingDue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.error)
                } else {
                    Text(text = "Balance:", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                    Text(text = "₹0 (Cleared ✓)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                }

                // Expand details chevron
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Details",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // EXPANDABLE DETAILS (With PBS Manual Record Display)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                ) {
                    if (item.lastPaymentDate != null && item.lastPaymentAmount != null) {
                        Text(
                            text = "• Last Payment: ₹%.0f on %s (%s)".format(
                                item.lastPaymentAmount,
                                CalculationEngine.formatDateDisplay(item.lastPaymentDate),
                                item.lastPaymentMode ?: "Cash"
                            ),
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "• No previous payments recorded",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "• Billing: ${if (item.customer.billingType == "daily_cash") "Daily Cash" else "Monthly Khata"} | Route Order #${item.customer.routeOrder}",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    // PBS External Record Row
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• External Book / PBS Pending: ${CalculationEngine.formatMoney(item.pbsPending)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.pbsPending > 0) Color(0xFFC2410C) else colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onPbsEditClicked() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit PBS",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Edit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. ACTION BUTTONS ROW (With Next Customer Jump)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Khata Button
                OutlinedButton(
                    onClick = onKhataClicked,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(width = 40.dp, height = 40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Khata",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Customer Notes Button
                OutlinedButton(
                    onClick = onNotesClicked,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Notes",
                        tint = if (item.notesCount > 0) colorScheme.primary else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                    if (item.notesCount > 0) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${item.notesCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }

                // Naga / Skip Button
                OutlinedButton(
                    onClick = onNagaClicked,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (item.isNaga) Color.White else colorScheme.error,
                        containerColor = if (item.isNaga) colorScheme.error else Color.Transparent
                    ),
                    modifier = Modifier.height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (item.isNaga) "NAGA ✓" else "Naga",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Primary Deliver & Save Button
                Button(
                    onClick = onSaveClicked,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isDone) colorScheme.secondary else colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.isDone) "SAVED ✓" else "DELIVER & SAVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Fast Next Customer Jump
                OutlinedButton(
                    onClick = onNextClicked,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Customer",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
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
    onNagaClicked: () -> Unit,
    onKhataClicked: () -> Unit,
    onPbsEditClicked: () -> Unit,
    onCallClicked: () -> Unit,
    onNotesClicked: () -> Unit = {},
    onOneTapPayment: (Double) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // STEP PROGRESS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customer ${currentIndex + 1} of $totalCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "Done: $completedCount • Pending: $pendingCount • Naga: $nagaCount",
                        fontSize = 10.5.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = item.statusText)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CUSTOMER HERO NAME
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.routeIndex + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customer.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "ID: #${item.customer.id} • ${item.customer.mohalla} ${if (item.customer.landmark.isNotBlank()) "• ${item.customer.landmark}" else ""}${if (item.routeName.isNotBlank()) " • 📍 ${item.routeName}" else ""}",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onKhataClicked) {
                    Icon(Icons.Default.History, contentDescription = "Khata", tint = colorScheme.primary)
                }
                if (item.customer.phone.isNotBlank()) {
                    IconButton(onClick = onCallClicked) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = colorScheme.primary)
                    }
                }
            }

            // PINNED CUSTOMER NOTE BADGE (Runner Mode)
            if (item.pinnedNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotesClicked() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📌", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.pinnedNote,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // BIG MILK QUANTITY CONTROL
            Text(
                text = "DELIVERY MILK (लीटर)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.currentMilkInput,
                    onValueChange = onQuantityChanged,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("L", fontWeight = FontWeight.Bold, color = colorScheme.primary) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                // Quick buttons
                Button(
                    onClick = {
                        val curr = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
                        onQuantityChanged("%.2f".format(Locale.US, (curr + 0.5)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+0.5L", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val curr = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
                        onQuantityChanged("%.2f".format(Locale.US, (curr + 1.0)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+1.0L", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            // FLEXIBLE CASH -> MILK CHIP (In Runner Mode)
            if (item.suggestedMilkFromCash != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseSuggestedMilk(item.suggestedMilkFromCash) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💡 Cash ₹${item.currentCashInput} = %.2f L Milk (@ ₹%.0f/L)".format(
                                item.suggestedMilkFromCash,
                                item.applicableRate
                            ),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = "USE %.2f L ▶".format(item.suggestedMilkFromCash),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BILL & CASH INPUT ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text("BILL TODAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                    Text(CalculationEngine.formatMoney(item.todayBill), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                    Text("@ ₹%.0f/L".format(item.applicableRate), fontSize = 10.sp, color = colorScheme.onPrimaryContainer)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text("PREV DUE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                    Text(
                        CalculationEngine.formatMoney(item.prevDue),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.prevDue > 0) StatusRed else colorScheme.onSurface
                    )
                    Text("Total: ${CalculationEngine.formatMoney(item.totalRequired)}", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                }

                Column(modifier = Modifier.weight(1.2f)) {
                    Text("CASH RECEIVED (₹)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                    OutlinedTextField(
                        value = item.currentCashInput,
                        onValueChange = onCashChanged,
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // NET DUE / ADVANCE & PBS ROW
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (item.advance > 0) colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else if (item.remainingDue > 0) colorScheme.errorContainer.copy(alpha = 0.4f)
                        else colorScheme.surfaceContainer
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.advance > 0) {
                    Text(text = "Advance Balance:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.primary)
                    Text(text = CalculationEngine.formatMoney(item.advance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                } else if (item.remainingDue > 0) {
                    Text(text = "Remaining Due (बकाया):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.error)
                    Text(text = CalculationEngine.formatMoney(item.remainingDue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.error)
                } else {
                    Text(text = "Balance:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    Text(text = "₹0 (Cleared ✓)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onPbsEditClicked() }
                ) {
                    Text(
                        text = "PBS: ${CalculationEngine.formatMoney(item.pbsPending)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC2410C)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit PBS",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // ONE-TAP QUICK PAYMENT RECONCILIATION ROW (Runner Mode)
            if (item.prevDue > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Quick Pay:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onOneTapPayment(item.prevDue) }
                    ) {
                        Text(
                            text = "Pay Full ₹%.0f".format(item.prevDue),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (item.prevDue >= 100.0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { onOneTapPayment(100.0) }
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
                    if (item.prevDue >= 500.0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { onOneTapPayment(500.0) }
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

            Spacer(modifier = Modifier.height(8.dp))

            // KHATA & NOTES QUICK ACCESS BUTTONS (Runner Mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onKhataClicked,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Khata", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onNotesClicked,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (item.notesCount > 0) "Notes (${item.notesCount})" else "Notes", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PRIMARY DELIVER & SAVE BUTTON
            Button(
                onClick = onSaveClicked,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (item.isDone) "SAVED ✓ (UPDATE)" else "DELIVER & SAVE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RUNNER NAVIGATION ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    Text("Previous")
                }

                OutlinedButton(
                    onClick = onNagaClicked,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Naga (Skip)")
                }

                OutlinedButton(
                    onClick = onNext,
                    enabled = currentIndex < totalCount - 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. ROUTE REORDER & ORDERING DIALOG
// ---------------------------------------------------------------------------
@Composable
fun RouteReorderDialog(
    initialCustomers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSaveOrder: (List<CustomerEntity>) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var currentList by remember(initialCustomers) { mutableStateOf(initialCustomers) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Route Order Manager", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Text(
                    text = "${currentList.size} Customers",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Use ▲ / ▼ to reorder delivery route. Route # updates without changing Customer IDs.",
                    fontSize = 11.5.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Sort Presets Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { currentList = initialCustomers }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { currentList = currentList.sortedBy { it.name } }
                    ) {
                        Text("Sort: Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { currentList = currentList.sortedWith(compareBy({ it.mohalla }, { it.name })) }
                    ) {
                        Text("Sort: Mohalla", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { currentList = currentList.sortedBy { it.id } }
                    ) {
                        Text("Sort: ID", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reorderable list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(currentList, key = { it.id }) { customer ->
                            val index = currentList.indexOf(customer)
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "ID: #${customer.id} • ${customer.mohalla}",
                                            fontSize = 10.5.sp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Up arrow
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = currentList.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                currentList = mutable
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                    }

                                    // Down arrow
                                    IconButton(
                                        onClick = {
                                            if (index < currentList.size - 1) {
                                                val mutable = currentList.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                currentList = mutable
                                            }
                                        },
                                        enabled = index < currentList.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveOrder(currentList) },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE ROUTE ORDER", fontWeight = FontWeight.Bold)
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

// ---------------------------------------------------------------------------
// 2. DAIRY CALCULATOR DIALOG
// ---------------------------------------------------------------------------
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


