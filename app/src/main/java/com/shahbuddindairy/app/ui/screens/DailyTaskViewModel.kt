package com.shahbuddindairy.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DailyFilter {
    ALL,
    PENDING,
    DONE,
    NAGA,
    ALTERNATE,
    PAYMENT_PENDING,
    DUE,
    ADVANCE
}

data class DailyCustomerCardItem(
    val customer: CustomerEntity,
    val delivery: DeliveryEntity?,
    val isDone: Boolean,
    val isNaga: Boolean,
    val isAlternateOff: Boolean,
    val statusText: String,
    val applicableRate: Double,
    val currentMilkInput: String,
    val currentCashInput: String,
    val todayBill: Double,
    val prevDue: Double,
    val totalRequired: Double,
    val remainingDue: Double,
    val advance: Double,
    val lastPaymentDate: String?,
    val lastPaymentAmount: Double?,
    val lastPaymentMode: String?,
    val pbsPending: Double = 0.0,
    val suggestedMilkFromCash: Double? = null,
    val hasUnsavedChanges: Boolean = false,
    val routeIndex: Int = 0
)

data class DailyTaskUiState(
    val selectedDate: String = CalculationEngine.todayStr(),
    val searchQuery: String = "",
    val activeFilter: DailyFilter = DailyFilter.ALL,
    val customerCards: List<DailyCustomerCardItem> = emptyList(),
    val totalMilk: Double = 0.0,
    val totalBill: Double = 0.0,
    val totalCashCollected: Double = 0.0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val nagaCount: Int = 0,
    val totalMarketDue: Double = 0.0,
    val totalPbsPending: Double = 0.0,
    val isLoading: Boolean = false,
    val allActiveCustomers: List<CustomerEntity> = emptyList()
)

class DailyTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository

    private val _selectedDate = MutableStateFlow(CalculationEngine.todayStr())
    val selectedDate = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(DailyFilter.ALL)
    val activeFilter = _activeFilter.asStateFlow()

    // Bumps when DB mutations occur (save delivery, payment, route update)
    private val _refreshTrigger = MutableStateFlow(0)

    // Transient inputs keyed strictly by immutable customerId
    private val _quantities = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val _cashInputs = MutableStateFlow<Map<Long, String>>(emptyMap())

    // Safe pending confirmation dialog item
    private val _pendingConfirmItem = MutableStateFlow<DailyCustomerCardItem?>(null)
    val pendingConfirmItem: StateFlow<DailyCustomerCardItem?> = _pendingConfirmItem.asStateFlow()

    private data class CustomerProfile(
        val prevDue: Double,
        val currentBal: Double,
        val lastPayment: CalculationEngine.LastPaymentInfo?,
        val pbsPending: Double
    )

    private data class BaseDailyData(
        val date: String,
        val customers: List<CustomerEntity>,
        val todayDeliveries: Map<Long, DeliveryEntity>,
        val profiles: Map<Long, CustomerProfile>,
        val totalMarketDue: Double,
        val totalPbsPending: Double
    )

    // Cached base data flow: Queries DB ONLY when date, customers, pbs, or mutation trigger changes
    private val baseDataFlow = combine(
        _selectedDate,
        repository.getAllActiveCustomers(),
        repository.getAllPbsSettingsFlow(),
        _refreshTrigger
    ) { date, customers, pbsSettings, _ ->
        withContext(Dispatchers.IO) {
            val todayDeliveries = repository.getDeliveriesForDateDirect(date)
            val todayDelivMap = todayDeliveries.associateBy { it.customerId }

            val allDeliveries = repository.getAllDeliveriesDirect()
            val allPayments = repository.getAllPaymentsDirect()
            val deliveriesByCust = allDeliveries.groupBy { it.customerId }
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

            var marketDue = 0.0
            var sumPbs = 0.0
            val profileMap = mutableMapOf<Long, CustomerProfile>()

            for (c in customers) {
                val custDelivs = deliveriesByCust[c.id] ?: emptyList()
                val custPayments = paymentsByCust[c.id] ?: emptyList()

                val prevDue = CalculationEngine.calculatePrevDueBefore(c.id, date, custDelivs, custPayments)
                val currentBal = CalculationEngine.calculateCustomerBalance(c.id, custDelivs, custPayments)
                if (currentBal > 0) marketDue += currentBal

                val lastPay = CalculationEngine.getLastPayment(c.id, custDelivs, custPayments)
                val custPbs = pbsMap[c.id] ?: 0.0
                sumPbs += custPbs

                profileMap[c.id] = CustomerProfile(
                    prevDue = prevDue,
                    currentBal = currentBal,
                    lastPayment = lastPay,
                    pbsPending = custPbs
                )
            }

            BaseDailyData(
                date = date,
                customers = customers,
                todayDeliveries = todayDelivMap,
                profiles = profileMap,
                totalMarketDue = marketDue,
                totalPbsPending = sumPbs
            )
        }
    }

    // High performance UI state: pure in-memory calculation (0 DB queries on keystroke)
    val uiState: StateFlow<DailyTaskUiState> = combine(
        baseDataFlow,
        _searchQuery,
        _activeFilter,
        _quantities,
        _cashInputs
    ) { base, query, filter, quantities, cashInputs ->
        var sumMilk = 0.0
        var sumBill = 0.0
        var sumCash = 0.0
        var completed = 0
        var pending = 0
        var naga = 0

        val cards = mutableListOf<DailyCustomerCardItem>()

        val activeCustomers = base.customers.filter { it.isArchived == 0 }

        for ((idx, c) in activeCustomers.withIndex()) {
            val d = base.todayDeliveries[c.id]
            val isDone = d != null && !d.isVoid && d.status == "delivered"
            val isNaga = d != null && !d.isVoid && d.status == "skipped"
            val isAltOff = CalculationEngine.isOffDay(c, base.date)

            val statusText = when {
                isDone -> "DONE"
                isNaga -> "NAGA"
                isAltOff -> "ALTERNATE"
                c.isActive == 0 -> "PAUSED"
                else -> "PENDING"
            }

            if (isDone && d != null) {
                completed++
                sumMilk += d.liters
                sumBill += d.totalAmount
                sumCash += d.paidCash
            } else if (isNaga) {
                naga++
            } else if (!isAltOff && c.isActive == 1) {
                pending++
            }

            val applicableRate = when {
                c.billingType == "daily_cash" -> if (c.ratePerLiter > 0) c.ratePerLiter else 60.0
                c.ratePerLiter > 0 -> c.ratePerLiter
                else -> 0.0
            }

            val existingQuantityStr = quantities[c.id]
            val milkQuantity = when {
                existingQuantityStr != null -> existingQuantityStr.toDoubleOrNull() ?: 0.0
                d != null && d.status == "delivered" -> d.liters
                else -> c.defaultLiters
            }
            val milkInputStr = existingQuantityStr ?: if (d != null && d.status == "delivered") "%.2f".format(Locale.US, d.liters) else "%.2f".format(Locale.US, c.defaultLiters)

            val existingCashStr = cashInputs[c.id]
            val cashPaid = when {
                existingCashStr != null -> existingCashStr.toDoubleOrNull() ?: 0.0
                d != null -> d.paidCash
                else -> 0.0
            }
            val cashInputStr = existingCashStr ?: if (d != null && d.paidCash > 0) "%.0f".format(Locale.US, d.paidCash) else ""

            val profile = base.profiles[c.id] ?: CustomerProfile(0.0, 0.0, null, 0.0)
            val prevDue = profile.prevDue
            val todayBill = CalculationEngine.round2(milkQuantity * applicableRate)
            val totalReq = CalculationEngine.round2(todayBill + prevDue)
            val net = CalculationEngine.round2(totalReq - cashPaid)
            val remDue = if (net > 0) net else 0.0
            val advance = if (net < 0) CalculationEngine.round2(-net) else 0.0

            // Cash -> Milk suggestion whenever cash > 0 and rate > 0
            val suggestedMilk = if (cashPaid > 0 && applicableRate > 0) {
                CalculationEngine.round2(cashPaid / applicableRate)
            } else null

            val hasUnsaved = existingQuantityStr != null || existingCashStr != null

            val item = DailyCustomerCardItem(
                customer = c,
                delivery = d,
                isDone = isDone,
                isNaga = isNaga,
                isAlternateOff = isAltOff,
                statusText = statusText,
                applicableRate = applicableRate,
                currentMilkInput = milkInputStr,
                currentCashInput = cashInputStr,
                todayBill = todayBill,
                prevDue = prevDue,
                totalRequired = totalReq,
                remainingDue = remDue,
                advance = advance,
                lastPaymentDate = profile.lastPayment?.date,
                lastPaymentAmount = profile.lastPayment?.amount,
                lastPaymentMode = profile.lastPayment?.mode,
                pbsPending = profile.pbsPending,
                suggestedMilkFromCash = suggestedMilk,
                hasUnsavedChanges = hasUnsaved,
                routeIndex = idx
            )

            val q = query.trim()
            val matchesQuery = q.isBlank() ||
                    c.name.contains(q, ignoreCase = true) ||
                    c.id.toString() == q ||
                    c.phone.contains(q) ||
                    c.mohalla.contains(q, ignoreCase = true) ||
                    c.landmark.contains(q, ignoreCase = true)

            if (!matchesQuery) continue

            val matchesFilter = when (filter) {
                DailyFilter.ALL -> true
                DailyFilter.PENDING -> !isDone && !isNaga && !isAltOff && c.isActive == 1
                DailyFilter.DONE -> isDone
                DailyFilter.NAGA -> isNaga
                DailyFilter.ALTERNATE -> isAltOff
                DailyFilter.PAYMENT_PENDING -> todayBill > 0 && cashPaid == 0.0
                DailyFilter.DUE -> remDue > 0 || profile.currentBal > 0
                DailyFilter.ADVANCE -> advance > 0 || profile.currentBal < 0
            }

            if (matchesFilter) {
                cards.add(item)
            }
        }

        DailyTaskUiState(
            selectedDate = base.date,
            searchQuery = query,
            activeFilter = filter,
            customerCards = cards,
            totalMilk = CalculationEngine.round2(sumMilk),
            totalBill = CalculationEngine.round2(sumBill),
            totalCashCollected = CalculationEngine.round2(sumCash),
            completedCount = completed,
            pendingCount = pending,
            nagaCount = naga,
            totalMarketDue = CalculationEngine.round2(base.totalMarketDue),
            totalPbsPending = CalculationEngine.round2(base.totalPbsPending),
            isLoading = false,
            allActiveCustomers = activeCustomers
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyTaskUiState(isLoading = true)
    )

    fun onDateSelected(newDate: String) {
        _selectedDate.value = newDate
        _quantities.value = emptyMap()
        _cashInputs.value = emptyMap()
    }

    fun onPreviousDay() {
        onDateSelected(CalculationEngine.prevDayStr(_selectedDate.value))
    }

    fun onNextDay() {
        onDateSelected(CalculationEngine.nextDayStr(_selectedDate.value))
    }

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onFilterSelected(f: DailyFilter) {
        _activeFilter.value = f
    }

    fun onQuantityChanged(customerId: Long, text: String) {
        val current = _quantities.value.toMutableMap()
        current[customerId] = text
        _quantities.value = current
    }

    fun onCashChanged(customerId: Long, text: String) {
        val current = _cashInputs.value.toMutableMap()
        current[customerId] = text
        _cashInputs.value = current
    }

    fun useSuggestedMilk(customerId: Long, suggestedLiters: Double) {
        onQuantityChanged(customerId, "%.2f".format(Locale.US, suggestedLiters))
    }

    fun updatePbsPending(customerId: Long, amount: Double) {
        viewModelScope.launch {
            repository.setPbsPending(customerId, amount)
            _refreshTrigger.value++
        }
    }

    fun requestConfirmSave(item: DailyCustomerCardItem) {
        _pendingConfirmItem.value = item
    }

    fun dismissConfirmDialog() {
        _pendingConfirmItem.value = null
    }

    fun confirmAndSaveTransaction(onSuccess: (() -> Unit)? = null) {
        val item = _pendingConfirmItem.value ?: return
        viewModelScope.launch {
            val date = _selectedDate.value
            val liters = item.currentMilkInput.toDoubleOrNull() ?: item.customer.defaultLiters
            val rate = item.applicableRate
            val bill = CalculationEngine.round2(liters * rate)
            val cash = item.currentCashInput.toDoubleOrNull() ?: 0.0

            val now = Date()
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)

            val existing = repository.getDeliveryForCustomerAndDate(item.customer.id, date)
            val deliveryId = existing?.id ?: item.delivery?.id ?: "d_${date}_${item.customer.id}"

            val delivery = DeliveryEntity(
                id = deliveryId,
                customerId = item.customer.id,
                date = date,
                deliveryTime = timeStr,
                milkType = item.customer.milkType,
                liters = liters,
                rate = rate,
                totalAmount = bill,
                paidCash = cash,
                paidOnline = 0.0,
                totalPaid = cash,
                status = "delivered",
                note = "Daily Delivery",
                isVoid = false
            )

            repository.saveDelivery(delivery)

            val q = _quantities.value.toMutableMap()
            q.remove(item.customer.id)
            _quantities.value = q

            val c = _cashInputs.value.toMutableMap()
            c.remove(item.customer.id)
            _cashInputs.value = c

            _pendingConfirmItem.value = null
            _refreshTrigger.value++
            onSuccess?.invoke()
        }
    }

    fun markNaga(item: DailyCustomerCardItem, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val existing = repository.getDeliveryForCustomerAndDate(item.customer.id, date)
            val deliveryId = existing?.id ?: item.delivery?.id ?: "d_${date}_${item.customer.id}"
            val delivery = DeliveryEntity(
                id = deliveryId,
                customerId = item.customer.id,
                date = date,
                deliveryTime = "",
                milkType = item.customer.milkType,
                liters = 0.0,
                rate = item.applicableRate,
                totalAmount = 0.0,
                paidCash = 0.0,
                paidOnline = 0.0,
                totalPaid = 0.0,
                status = "skipped",
                note = "Naga (Skipped)",
                isVoid = false
            )
            repository.saveDelivery(delivery)

            val q = _quantities.value.toMutableMap()
            q.remove(item.customer.id)
            _quantities.value = q

            val c = _cashInputs.value.toMutableMap()
            c.remove(item.customer.id)
            _cashInputs.value = c

            _refreshTrigger.value++
            onSuccess?.invoke()
        }
    }

    // Route order saving: updates routeOrder column for all customers without touching customerId
    fun saveRouteOrder(reorderedCustomers: List<CustomerEntity>, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val updates = reorderedCustomers.mapIndexed { index, cust ->
                Pair(cust.id, index)
            }
            repository.updateCustomerRouteOrders(updates)
            _refreshTrigger.value++
            onSuccess?.invoke()
        }
    }

    // Quick / Guest Customer addition with optional regular route retention
    fun addQuickCustomerAndDeliver(
        name: String,
        milkType: String,
        liters: Double,
        rate: Double,
        cash: Double,
        saveAsRegular: Boolean,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val nextId = repository.getNextCustomerId()
            val displayName = name.ifBlank { "Quick Customer #$nextId" }
            val today = _selectedDate.value
            val currentCustomers = repository.getAllActiveCustomersDirect()
            val maxRoute = currentCustomers.maxOfOrNull { it.routeOrder } ?: 0

            val newCustomer = CustomerEntity(
                id = nextId,
                name = displayName,
                phone = "",
                mohalla = "कटरा",
                landmark = if (!saveAsRegular) "Guest" else "",
                milkType = milkType,
                defaultLiters = liters,
                ratePerLiter = rate,
                billingType = "daily_cash",
                deliveryFrequency = "daily",
                isActive = 1,
                isArchived = 0,
                routeOrder = maxRoute + 1,
                createdAt = today
            )
            repository.saveCustomer(newCustomer)

            val now = Date()
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
            val bill = CalculationEngine.round2(liters * rate)

            val delivery = DeliveryEntity(
                id = "d_${today}_${nextId}",
                customerId = nextId,
                date = today,
                deliveryTime = timeStr,
                milkType = milkType,
                liters = liters,
                rate = rate,
                totalAmount = bill,
                paidCash = cash,
                paidOnline = 0.0,
                totalPaid = cash,
                status = "delivered",
                note = if (saveAsRegular) "First Regular Delivery" else "Quick Cash Sale",
                isVoid = false
            )
            repository.saveDelivery(delivery)

            _refreshTrigger.value++
            onSuccess?.invoke()
        }
    }

    // Auditable Due Settlement / Adjustment
    fun recordDueSettlement(
        customerId: Long,
        amountToSettle: Double,
        mode: String,
        note: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val id = "pay_settle_${System.currentTimeMillis()}_$customerId"
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val payment = PaymentEntity(
                id = id,
                customerId = customerId,
                date = _selectedDate.value,
                amount = amountToSettle,
                paymentMode = mode,
                paymentSubtype = "Due Settlement",
                note = note.ifBlank { "Due Settlement / Adjustment" },
                createdAt = nowStr,
                isVoid = false
            )
            repository.savePayment(payment)
            _refreshTrigger.value++
            onSuccess?.invoke()
        }
    }
}
