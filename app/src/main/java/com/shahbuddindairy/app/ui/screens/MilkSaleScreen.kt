package com.shahbuddindairy.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
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
import com.shahbuddindairy.app.ui.components.BillingTypeBadge
import com.shahbuddindairy.app.ui.components.EmptyState
import com.shahbuddindairy.app.ui.components.MilkTypeBadge
import com.shahbuddindairy.app.ui.components.RateBadge
import com.shahbuddindairy.app.ui.components.StatCard
import com.shahbuddindairy.app.ui.components.StatusBadge
import com.shahbuddindairy.app.ui.components.TransactionConfirmDialog
import com.shahbuddindairy.app.ui.theme.StatusRed

@Composable
fun MilkSaleScreen(
    viewModel: DailyTaskViewModel,
    onNavigateToCustomerKhata: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingConfirmItem by viewModel.pendingConfirmItem.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var onlyPending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // TOP APP HEADER
        AppTopHeader(
            title = "Milk Sale",
            subtitle = "दैनिक दूध बिक्री • ${CalculationEngine.formatDateDisplay(uiState.selectedDate)}",
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
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.onPreviousDay() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = colorScheme.primary)
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

                IconButton(
                    onClick = { viewModel.onNextDay() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = colorScheme.primary)
                }
            }
        }

        // SALES KPI STRIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Sold Liters",
                value = CalculationEngine.formatLiters(uiState.totalMilk),
                subValue = "Total Route",
                valueColor = colorScheme.primary,
                icon = Icons.Default.LocalDrink,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Sales",
                value = CalculationEngine.formatMoney(uiState.totalBill),
                subValue = "Cash: ${CalculationEngine.formatMoney(uiState.totalCashCollected)}",
                valueColor = colorScheme.primary,
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1.1f)
            )
            StatCard(
                label = "Progress",
                value = "${uiState.completedCount}/${uiState.customerCards.size}",
                subValue = "Naaga: ${uiState.nagaCount}",
                valueColor = colorScheme.primary,
                icon = Icons.Default.Check,
                modifier = Modifier.weight(0.9f)
            )
        }

        // SEARCH & PENDING TOGGLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search name, route, mohalla...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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

            // Only Pending Chip Filter
            FilterChip(
                selected = onlyPending,
                onClick = { onlyPending = !onlyPending },
                label = { Text(if (onlyPending) "Pending Only" else "All Route", fontSize = 11.5.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary,
                    selectedLabelColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // SALES ROUTE LIST
        val displayCards = if (onlyPending) {
            uiState.customerCards.filter { !it.isDone && !it.isNaga && !it.isAlternateOff }
        } else {
            uiState.customerCards
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else if (displayCards.isEmpty()) {
            EmptyState(
                title = if (onlyPending) "All Deliveries Complete! 🎉" else "No Records Found",
                message = if (onlyPending) "All customers on this route have been delivered or marked." else "No customers found for this date."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayCards, key = { it.customer.id }) { item ->
                    MilkSaleCard(
                        item = item,
                        onDeliver = { viewModel.requestConfirmSave(item) },
                        onNaga = { viewModel.markNaga(item) },
                        onOpenKhata = { onNavigateToCustomerKhata(item.customer.id) },
                        onCall = {
                            if (item.customer.phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.customer.phone}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    // CONFIRMATION DIALOG
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
            onConfirm = { viewModel.confirmAndSaveTransaction() },
            onDismiss = { viewModel.dismissConfirmDialog() }
        )
    }
}

@Composable
fun MilkSaleCard(
    item: DailyCustomerCardItem,
    onDeliver: () -> Unit,
    onNaga: () -> Unit,
    onOpenKhata: () -> Unit,
    onCall: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (item.isDone) colorScheme.primary.copy(alpha = 0.7f) else colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp)
        ) {
            // TOP ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.customer.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customer.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "${item.customer.mohalla}${if (item.customer.landmark.isNotBlank()) " • ${item.customer.landmark}" else ""}",
                        fontSize = 11.5.sp,
                        color = colorScheme.onSurfaceVariant
                    )
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
            Spacer(modifier = Modifier.height(6.dp))

            // SALE METRICS SUMMARY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Milk: ${item.currentMilkInput} L",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Bill: ${CalculationEngine.formatMoney(item.todayBill)}",
                        fontSize = 12.sp,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Prev Due",
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CalculationEngine.formatMoney(item.prevDue),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.prevDue > 0) StatusRed else colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (item.advance > 0) {
                        Text("Advance", fontSize = 10.sp, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(CalculationEngine.formatMoney(item.advance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                    } else if (item.remainingDue > 0) {
                        Text("Net Due", fontSize = 10.sp, color = StatusRed, fontWeight = FontWeight.Bold)
                        Text(CalculationEngine.formatMoney(item.remainingDue), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                    } else {
                        Text("Balance", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                        Text("₹0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // QUICK ACTIONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenKhata,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(width = 40.dp, height = 38.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "Khata", modifier = Modifier.size(17.dp))
                }

                if (item.customer.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 40.dp, height = 38.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = colorScheme.primary, modifier = Modifier.size(17.dp))
                    }
                }

                OutlinedButton(
                    onClick = onNaga,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (item.isNaga) Color.White else colorScheme.error,
                        containerColor = if (item.isNaga) colorScheme.error else Color.Transparent
                    ),
                    modifier = Modifier.height(38.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (item.isNaga) "NAGA ✓" else "Naga", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDeliver,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isDone) colorScheme.secondary else colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.isDone) "DELIVERED ✓" else "DELIVER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
