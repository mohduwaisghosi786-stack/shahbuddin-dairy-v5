package com.shahbuddindairy.app.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.ui.components.AppTopHeader
import com.shahbuddindairy.app.ui.components.BillingTypeBadge
import com.shahbuddindairy.app.ui.components.EmptyState
import com.shahbuddindairy.app.ui.components.MilkTypeBadge
import com.shahbuddindairy.app.ui.components.RateBadge
import com.shahbuddindairy.app.ui.components.StatCard
import com.shahbuddindairy.app.ui.theme.StatusRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CustomerCardItem(
    val customer: CustomerEntity,
    val currentBalance: Double,
    val lastPaymentDate: String?,
    val lastPaymentAmount: Double?,
    val pbsPending: Double = 0.0
)

data class CustomersUiState(
    val customers: List<CustomerCardItem> = emptyList(),
    val totalCustomersCount: Int = 0,
    val totalMarketDue: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val searchQuery: String = "",
    val selectedMohalla: String = "ALL",
    val mohallas: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class CustomersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedMohalla = MutableStateFlow("ALL")
    val selectedMohalla = _selectedMohalla.asStateFlow()

    private data class BaseCustomerData(
        val allCards: List<CustomerCardItem>,
        val totalCustomersCount: Int,
        val totalMarketDue: Double,
        val totalAdvance: Double,
        val mohallas: List<String>
    )

    private val baseCustomersFlow = combine(
        repository.getAllCustomers(),
        repository.getAllPbsSettingsFlow()
    ) { customers, pbsSettings ->
        withContext(Dispatchers.IO) {
            val allDelivs = repository.getAllDeliveriesDirect()
            val allPayments = repository.getAllPaymentsDirect()
            val delivsByCust = allDelivs.groupBy { it.customerId }
            val paymentsByCust = allPayments.groupBy { it.customerId }

            val pbsMap = mutableMapOf<Long, Double>()
            for (s in pbsSettings) {
                if (s.key.startsWith("pbs_pending_")) {
                    val cId = s.key.removePrefix("pbs_pending_").toLongOrNull()
                    if (cId != null) {
                        pbsMap[cId] = s.value.toDoubleOrNull() ?: 0.0
                    }
                }
            }

            var totalDue = 0.0
            var totalAdv = 0.0
            val mohallaSet = sortedSetOf<String>()
            val cards = mutableListOf<CustomerCardItem>()

            for (c in customers) {
                if (c.isArchived == 1) continue
                if (c.mohalla.isNotBlank()) mohallaSet.add(c.mohalla)

                val custDelivs = delivsByCust[c.id] ?: emptyList()
                val custPayments = paymentsByCust[c.id] ?: emptyList()

                val bal = CalculationEngine.calculateCustomerBalance(c.id, custDelivs, custPayments)
                if (bal > 0) totalDue += bal else if (bal < 0) totalAdv += -bal

                val lastPay = CalculationEngine.getLastPayment(c.id, custDelivs, custPayments)
                val pbs = pbsMap[c.id] ?: 0.0

                cards.add(
                    CustomerCardItem(
                        customer = c,
                        currentBalance = bal,
                        lastPaymentDate = lastPay?.date,
                        lastPaymentAmount = lastPay?.amount,
                        pbsPending = pbs
                    )
                )
            }

            BaseCustomerData(
                allCards = cards,
                totalCustomersCount = customers.count { it.isArchived == 0 },
                totalMarketDue = CalculationEngine.round2(totalDue),
                totalAdvance = CalculationEngine.round2(totalAdv),
                mohallas = listOf("ALL") + mohallaSet.toList()
            )
        }
    }

    val uiState = combine(
        baseCustomersFlow,
        _searchQuery,
        _selectedMohalla
    ) { base, query, mohalla ->
        val q = query.trim()
        val filtered = base.allCards.filter { item ->
            val c = item.customer
            val matchesQuery = q.isBlank() ||
                    c.name.contains(q, ignoreCase = true) ||
                    c.id.toString() == q ||
                    c.phone.contains(q) ||
                    c.mohalla.contains(q, ignoreCase = true) ||
                    c.landmark.contains(q, ignoreCase = true)
            val matchesMohalla = mohalla == "ALL" || c.mohalla == mohalla
            matchesQuery && matchesMohalla
        }

        CustomersUiState(
            customers = filtered,
            totalCustomersCount = base.totalCustomersCount,
            totalMarketDue = base.totalMarketDue,
            totalAdvance = base.totalAdvance,
            searchQuery = query,
            selectedMohalla = mohalla,
            mohallas = base.mohallas,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CustomersUiState(isLoading = true)
    )

    fun saveRouteOrder(reordered: List<CustomerEntity>, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val updates = reordered.mapIndexed { index, cust ->
                Pair(cust.id, index)
            }
            repository.updateCustomerRouteOrders(updates)
            onSuccess?.invoke()
        }
    }


    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onMohallaFilterSelected(m: String) {
        _selectedMohalla.value = m
    }

    fun addNewCustomer(
        name: String,
        phone: String,
        mohalla: String,
        defaultLiters: Double,
        ratePerLiter: Double,
        billingType: String,
        landmark: String = "",
        milkType: String = "भैंस",
        deliveryFrequency: String = "daily"
    ) {
        viewModelScope.launch {
            val nextId = repository.getNextCustomerId()
            val newCustomer = CustomerEntity(
                id = nextId,
                name = name,
                phone = phone,
                mohalla = mohalla.ifBlank { "कटरा" },
                landmark = landmark,
                milkType = milkType,
                defaultLiters = defaultLiters,
                ratePerLiter = ratePerLiter,
                billingType = billingType,
                deliveryFrequency = deliveryFrequency,
                routeOrder = nextId.toInt(),
                createdAt = CalculationEngine.todayStr()
            )
            repository.saveCustomer(newCustomer)
        }
    }
}

@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Customer", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Customer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colorScheme.background)
        ) {
            // TOP APP HEADER
            AppTopHeader(
                title = "Customer Khata",
                subtitle = "ग्राहक खाता पंजी • कुल ग्राहक: ${uiState.totalCustomersCount}",
                marketDue = uiState.totalMarketDue
            )

            // METRICS STATS BANNER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Total Customers",
                    value = "${uiState.totalCustomersCount}",
                    valueColor = colorScheme.primary,
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Market Due",
                    value = CalculationEngine.formatMoney(uiState.totalMarketDue),
                    valueColor = StatusRed,
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Advance",
                    value = CalculationEngine.formatMoney(uiState.totalAdvance),
                    valueColor = colorScheme.primary,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
            }

            // SEARCH BAR
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search name, phone, mohalla, landmark...", fontSize = 13.sp) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // MOHALLA CHIPS ROW
            if (uiState.mohallas.size > 1) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.mohallas.forEach { m ->
                        val isSel = uiState.selectedMohalla == m
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.onMohallaFilterSelected(m) },
                            label = {
                                Text(
                                    text = if (m == "ALL") "All Mohallas" else m,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
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

            Spacer(modifier = Modifier.height(2.dp))

            // CUSTOMER LIST
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else if (uiState.customers.isEmpty()) {
                EmptyState(
                    title = "No Customers Found",
                    message = if (uiState.searchQuery.isNotBlank())
                        "No customer matched \"${uiState.searchQuery}\""
                    else "No customers found in this Mohalla filter."
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
                    items(uiState.customers, key = { it.customer.id }) { item ->
                        CustomerListItemCard(
                            item = item,
                            onClick = { onNavigateToCustomerDetail(item.customer.id) },
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

        // ADD NEW CUSTOMER MULTI-SECTION DIALOG
        if (showAddDialog) {
            AddNewCustomerDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, phone, mohalla, landmark, milkType, liters, rate, bType, freq ->
                    viewModel.addNewCustomer(
                        name = name,
                        phone = phone,
                        mohalla = mohalla,
                        defaultLiters = liters,
                        ratePerLiter = rate,
                        billingType = bType,
                        landmark = landmark,
                        milkType = milkType,
                        deliveryFrequency = freq
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun CustomerListItemCard(
    item: CustomerCardItem,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Customer ID Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${item.customer.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.customer.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    if (item.customer.phone.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCall() }
                        )
                    }
                }

                Text(
                    text = "${item.customer.mohalla}${if (item.customer.landmark.isNotBlank()) " • ${item.customer.landmark}" else ""} | ${item.customer.defaultLiters}L",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BillingTypeBadge(billingType = item.customer.billingType)
                    MilkTypeBadge(milkType = item.customer.milkType)
                    RateBadge(rate = item.customer.ratePerLiter)
                }
            }

            // Balance Column
            Column(horizontalAlignment = Alignment.End) {
                if (item.currentBalance > 0) {
                    Text(
                        text = "DUE (बकाया)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusRed
                    )
                    Text(
                        text = CalculationEngine.formatMoney(item.currentBalance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusRed
                    )
                } else if (item.currentBalance < 0) {
                    Text(
                        text = "ADVANCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = CalculationEngine.formatMoney(-item.currentBalance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                } else {
                    Text(
                        text = "CLEARED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "₹0",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }

                if (item.pbsPending > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFF7ED),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEA580C)),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "PBS: ₹%,d".format(item.pbsPending.toLong()),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (item.lastPaymentAmount != null && item.lastPaymentDate != null) {
                    Text(
                        text = "Last: ₹%.0f (%s)".format(
                            item.lastPaymentAmount,
                            CalculationEngine.formatDateDisplay(item.lastPaymentDate).take(6)
                        ),
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddNewCustomerDialog(
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        phone: String,
        mohalla: String,
        landmark: String,
        milkType: String,
        defaultLiters: Double,
        ratePerLiter: Double,
        billingType: String,
        deliveryFrequency: String
    ) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // Form Fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var mohalla by remember { mutableStateOf("कटरा") }
    var landmark by remember { mutableStateOf("") }
    var milkType by remember { mutableStateOf("भैंस") }
    var litersStr by remember { mutableStateOf("1.0") }
    var rateStr by remember { mutableStateOf("60") }
    var billingType by remember { mutableStateOf("daily_cash") }
    var deliveryFrequency by remember { mutableStateOf("daily") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Customer (नया ग्राहक)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SECTION 1: BASIC INFORMATION
                SectionHeader("1. Basic Information (प्राथमिक विवरण)")

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name * (नाम)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (मोबाइल नंबर)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mohalla,
                        onValueChange = { mohalla = it },
                        label = { Text("Mohalla (मोहल्ला)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = landmark,
                        onValueChange = { landmark = it },
                        label = { Text("Landmark (स्थान)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Mohalla Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("कटरा", "गुलाब नगर", "किला", "चौपुला", "सुभाष नगर").forEach { area ->
                        FilterChip(
                            selected = mohalla == area,
                            onClick = { mohalla = area },
                            label = { Text(area, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SECTION 2: MILK PLAN
                SectionHeader("2. Milk Plan (दूध विवरण)")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = litersStr,
                        onValueChange = { litersStr = it },
                        label = { Text("Daily Liters (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Rate (₹/L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Milk Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = milkType == "भैंस",
                        onClick = { milkType = "भैंस" },
                        label = { Text("भैंस का दूध (Buffalo)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = milkType == "गाय",
                        onClick = { milkType = "गाय" },
                        label = { Text("गाय का दूध (Cow)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SECTION 3: BILLING & FREQUENCY
                SectionHeader("3. Billing & Schedule (खाता व समय)")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = billingType == "daily_cash",
                        onClick = { billingType = "daily_cash" },
                        label = { Text("Daily Cash (नकद)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = billingType == "monthly",
                        onClick = { billingType = "monthly" },
                        label = { Text("Monthly (मासिक खाता)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = deliveryFrequency == "daily",
                        onClick = { deliveryFrequency = "daily" },
                        label = { Text("Everyday (रोजाना)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = deliveryFrequency == "alternate",
                        onClick = { deliveryFrequency = "alternate" },
                        label = { Text("Alternate (एक दिन छोड़)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // LIVE CALCULATION PREVIEW BOX
                val previewLiters = litersStr.toDoubleOrNull() ?: 1.0
                val previewRate = rateStr.toDoubleOrNull() ?: 60.0
                val dailyBill = previewLiters * previewRate
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Estimate: %.2f L @ ₹%.0f/L".format(previewLiters, previewRate),
                            fontSize = 12.sp,
                            color = colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹%.0f / day".format(dailyBill),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val l = litersStr.toDoubleOrNull() ?: 1.0
                        val r = rateStr.toDoubleOrNull() ?: 60.0
                        onAdd(name.trim(), phone.trim(), mohalla.trim(), landmark.trim(), milkType, l, r, billingType, deliveryFrequency)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("SAVE CUSTOMER", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = colorScheme.surface
    )
}

@Composable
private fun SectionHeader(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            letterSpacing = 0.4.sp
        )
        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(top = 2.dp))
    }
}
