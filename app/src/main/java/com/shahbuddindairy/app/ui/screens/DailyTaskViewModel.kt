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
    val hasUnsavedChanges: Boolean = false
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
    val isLoading: Boolean = false
)

class DailyTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository

    private val _selectedDate = MutableStateFlow(CalculationEngine.todayStr())
    val selectedDate = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(DailyFilter.ALL)
    val activeFilter = _activeFilter.asStateFlow()

    // Transient input state for cards: customerId -> quantity string
    private val _quantities = MutableStateFlow<Map<Long, String>>(emptyMap())
    // Transient input state for cards: customerId -> cash string
    private val _cashInputs = MutableStateFlow<Map<Long, String>>(emptyMap())

    // Pending confirmation dialog item
    private val _pendingConfirmItem = MutableStateFlow<DailyCustomerCardItem?>(null)
    val pendingConfirmItem: StateFlow<DailyCustomerCardItem?> = _pendingConfirmItem.asStateFlow()

    private data class FilterConfig(
        val date: String,
        val query: String,
        val filter: DailyFilter
    )

    private val filterConfigFlow = combine(_selectedDate, _searchQuery, _activeFilter) { date, query, filter ->
        FilterConfig(date, query, filter)
    }

    val uiState: StateFlow<DailyTaskUiState> = combine(
        filterConfigFlow,
        repository.getAllActiveCustomers(),
        repository.getAllPbsSettingsFlow(),
        _quantities,
        _cashInputs
    ) { config, customers, pbsSettings, quantities, cashInputs ->
        buildUiState(config.date, config.query, config.filter, customers, pbsSettings, quantities, cashInputs)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyTaskUiState(isLoading = true)
    )

    private suspend fun buildUiState(
        date: String,
        query: String,
        filter: DailyFilter,
        customers: List<CustomerEntity>,
        pbsSettings: List<com.shahbuddindairy.app.data.model.AppSettingEntity>,
        quantities: Map<Long, String>,
        cashInputs: Map<Long, String>
    ): DailyTaskUiState = withContext(Dispatchers.IO) {
        val todayDeliveries = repository.getDeliveriesForDateDirect(date)
        val todayDelivMap = todayDeliveries.associateBy { it.customerId }

        // Fetch once and pre-group by customerId to eliminate O(N^2) linear scans
        val allDeliveries = repository.getAllDeliveriesDirect()
        val allPayments = repository.getAllPaymentsDirect()
        val deliveriesByCust = allDeliveries.groupBy { it.customerId }
        val paymentsByCust = allPayments.groupBy { it.customerId }

        // Build PBS lookup map
        val pbsMap = mutableMapOf<Long, Double>()
        for (s in pbsSettings) {
            if (s.key.startsWith("pbs_pending_")) {
                val cId = s.key.removePrefix("pbs_pending_").toLongOrNull()
                if (cId != null) {
                    pbsMap[cId] = s.value.toDoubleOrNull() ?: 0.0
                }
            }
        }

        var sumMilk = 0.0
        var sumBill = 0.0
        var sumCash = 0.0
        var completed = 0
        var pending = 0
        var naga = 0
        var marketDue = 0.0
        var sumPbs = 0.0

        val cards = mutableListOf<DailyCustomerCardItem>()

        for (c in customers) {
            val isArchived = c.isArchived == 1
            if (isArchived) continue

            val d = todayDelivMap[c.id]
            val isDone = d != null && !d.isVoid && d.status == "delivered"
            val isNaga = d != null && !d.isVoid && d.status == "skipped"
            val isAltOff = CalculationEngine.isOffDay(c, date)

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

            // Customer Rate Logic
            val applicableRate = when {
                c.billingType == "daily_cash" -> if (c.ratePerLiter > 0) c.ratePerLiter else 60.0
                c.ratePerLiter > 0 -> c.ratePerLiter
                else -> 0.0
            }

            // Determine milk quantity
            val existingQuantityStr = quantities[c.id]
            val milkQuantity = when {
                existingQuantityStr != null -> existingQuantityStr.toDoubleOrNull() ?: 0.0
                d != null && d.status == "delivered" -> d.liters
                else -> c.defaultLiters
            }
            val milkInputStr = existingQuantityStr ?: if (d != null && d.status == "delivered") "%.2f".format(Locale.US, d.liters) else "%.2f".format(Locale.US, c.defaultLiters)

            // Determine cash input
            val existingCashStr = cashInputs[c.id]
            val cashPaid = when {
                existingCashStr != null -> existingCashStr.toDoubleOrNull() ?: 0.0
                d != null -> d.paidCash
                else -> 0.0
            }
            val cashInputStr = existingCashStr ?: if (d != null && d.paidCash > 0) "%.0f".format(Locale.US, d.paidCash) else ""

            // High Performance calculation using customer's own scoped records:
            val custDeliveries = deliveriesByCust[c.id] ?: emptyList()
            val custPayments = paymentsByCust[c.id] ?: emptyList()

            val prevDue = CalculationEngine.calculatePrevDueBefore(c.id, date, custDeliveries, custPayments)
            val todayBill = CalculationEngine.round2(milkQuantity * applicableRate)
            val totalReq = CalculationEngine.round2(todayBill + prevDue)
            val net = CalculationEngine.round2(totalReq - cashPaid)
            val remDue = if (net > 0) net else 0.0
            val advance = if (net < 0) CalculationEngine.round2(-net) else 0.0

            val currentBal = CalculationEngine.calculateCustomerBalance(c.id, custDeliveries, custPayments)
            if (currentBal > 0) marketDue += currentBal

            // Last Payment Info
            val lastPay = CalculationEngine.getLastPayment(c.id, custDeliveries, custPayments)

            // PBS External Manual Pending Amount
            val custPbs = pbsMap[c.id] ?: 0.0
            sumPbs += custPbs

            // Suggested Milk from Cash (for Daily Customers only)
            val suggestedMilk = if (c.billingType == "daily_cash" && cashPaid > 0 && applicableRate > 0) {
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
                lastPaymentDate = lastPay?.date,
                lastPaymentAmount = lastPay?.amount,
                lastPaymentMode = lastPay?.mode,
                pbsPending = custPbs,
                suggestedMilkFromCash = suggestedMilk,
                hasUnsavedChanges = hasUnsaved
            )

            // Filter logic
            val matchesQuery = query.isBlank() ||
                    c.name.contains(query, ignoreCase = true) ||
                    c.id.toString() == query.trim() ||
                    c.phone.contains(query.trim()) ||
                    c.mohalla.contains(query, ignoreCase = true)

            if (!matchesQuery) continue

            val matchesFilter = when (filter) {
                DailyFilter.ALL -> true
                DailyFilter.PENDING -> !isDone && !isNaga && !isAltOff && c.isActive == 1
                DailyFilter.DONE -> isDone
                DailyFilter.NAGA -> isNaga
                DailyFilter.ALTERNATE -> isAltOff
                DailyFilter.PAYMENT_PENDING -> todayBill > 0 && cashPaid == 0.0
                DailyFilter.DUE -> remDue > 0 || currentBal > 0
                DailyFilter.ADVANCE -> advance > 0 || currentBal < 0
            }

            if (matchesFilter) {
                cards.add(item)
            }
        }

        DailyTaskUiState(
            selectedDate = date,
            searchQuery = query,
            activeFilter = filter,
            customerCards = cards,
            totalMilk = CalculationEngine.round2(sumMilk),
            totalBill = CalculationEngine.round2(sumBill),
            totalCashCollected = CalculationEngine.round2(sumCash),
            completedCount = completed,
            pendingCount = pending,
            nagaCount = naga,
            totalMarketDue = CalculationEngine.round2(marketDue),
            totalPbsPending = CalculationEngine.round2(sumPbs),
            isLoading = false
        )
    }

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

            // Idempotent delivery ID checking to guarantee zero duplicate deliveries
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

            // Clear transient inputs for this customer
            val q = _quantities.value.toMutableMap()
            q.remove(item.customer.id)
            _quantities.value = q

            val c = _cashInputs.value.toMutableMap()
            c.remove(item.customer.id)
            _cashInputs.value = c

            _pendingConfirmItem.value = null
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

            onSuccess?.invoke()
        }
    }
}
