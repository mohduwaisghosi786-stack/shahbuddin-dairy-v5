package com.shahbuddindairy.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.pdf.PdfReportGenerator
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

import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import com.shahbuddindairy.app.data.model.RouteEntity

data class KhataTimelineItem(
    val id: String,
    val date: String,
    val type: String, // "DELIVERY" or "PAYMENT"
    val description: String,
    val liters: Double,
    val rate: Double,
    val billAmount: Double,
    val paidAmount: Double,
    val paymentMode: String,
    val runningBalance: Double,
    val isVoid: Boolean,
    val voidedAt: String?,
    val note: String
)

data class CustomerDetailUiState(
    val customer: CustomerEntity? = null,
    val currentBalance: Double = 0.0,
    val totalLitersDelivered: Double = 0.0,
    val totalBilled: Double = 0.0,
    val totalPaid: Double = 0.0,
    val lastPayment: CalculationEngine.LastPaymentInfo? = null,
    val timeline: List<KhataTimelineItem> = emptyList(),
    val milkDeliveries: List<DeliveryEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val customerNotes: List<CustomerNoteEntity> = emptyList(),
    val pinnedNote: CustomerNoteEntity? = null,
    val routeName: String = "Main Route",
    val pbsPending: Double = 0.0,
    val tags: Set<String> = emptySet(),
    val upiId: String = "",
    val payeeName: String = "Shahbuddin Dairy",
    val dairyPhone: String = "9897259461",
    val todayLiters: Double = 0.0,
    val todayBill: Double = 0.0,
    val todayCashPaid: Double = 0.0,
    val todayOnlinePaid: Double = 0.0,
    val isLoading: Boolean = false,
    val pdfExportResult: PdfReportGenerator.PdfResult? = null
)

class CustomerDetailViewModel(
    application: Application,
    private val customerId: Long
) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository

    private val _pdfResult = MutableStateFlow<PdfReportGenerator.PdfResult?>(null)
    val pdfResult = _pdfResult.asStateFlow()

    private var lastPaymentActionTime = 0L

    private data class CustomerBaseData(
        val customer: CustomerEntity?,
        val deliveries: List<DeliveryEntity>,
        val payments: List<PaymentEntity>,
        val pbs: Double
    )

    private data class CustomerMetaData(
        val tags: Set<String>,
        val upiId: String,
        val payeeName: String,
        val dairyPhone: String
    )

    private val baseDataFlow = combine(
        repository.getCustomerById(customerId),
        repository.getDeliveriesForCustomer(customerId),
        repository.getPaymentsForCustomer(customerId),
        repository.getSettingFlow("pbs_pending_$customerId")
    ) { customer, deliveries, payments, pbsStr ->
        CustomerBaseData(
            customer = customer,
            deliveries = deliveries,
            payments = payments,
            pbs = pbsStr?.toDoubleOrNull() ?: 0.0
        )
    }

    private val metaDataFlow = combine(
        repository.getSettingFlow("customer_tags_$customerId"),
        repository.getSettingFlow("upi_id"),
        repository.getSettingFlow("payee_name"),
        repository.getSettingFlow("dairy_phone")
    ) { tagsCsv, upi, payee, phone ->
        val tagSet = tagsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        CustomerMetaData(
            tags = tagSet,
            upiId = upi ?: "",
            payeeName = payee?.ifBlank { "Shahbuddin Dairy" } ?: "Shahbuddin Dairy",
            dairyPhone = phone?.ifBlank { "9897259461" } ?: "9897259461"
        )
    }

    val uiState: StateFlow<CustomerDetailUiState> = combine(
        baseDataFlow,
        metaDataFlow,
        repository.getNotesForCustomer(customerId),
        repository.getAllRoutes(),
        _pdfResult
    ) { base, meta, notes, routes, pdfRes ->
        val rName = routes.find { it.id == base.customer?.routeId }?.name ?: "Main Route"
        buildCustomerState(base.customer, base.deliveries, base.payments, base.pbs, notes, rName, meta, pdfRes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CustomerDetailUiState(isLoading = true)
    )

    fun updatePbsPending(amount: Double) {
        viewModelScope.launch {
            repository.setPbsPending(customerId, amount)
        }
    }

    fun toggleCustomerTag(tag: String) {
        viewModelScope.launch {
            val currentCsv = repository.getSetting("customer_tags_$customerId") ?: ""
            val set = currentCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (set.contains(tag)) {
                set.remove(tag)
            } else {
                set.add(tag)
            }
            repository.setSetting("customer_tags_$customerId", set.joinToString(","))
        }
    }

    private fun buildCustomerState(
        customer: CustomerEntity?,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>,
        pbsPending: Double,
        notes: List<CustomerNoteEntity>,
        routeName: String,
        meta: CustomerMetaData,
        pdfRes: PdfReportGenerator.PdfResult?
    ): CustomerDetailUiState {
        if (customer == null) {
            return CustomerDetailUiState(isLoading = false)
        }

        var totalLiters = 0.0
        var totalBilled = 0.0
        var totalPaid = 0.0

        for (d in deliveries) {
            if (!d.isVoid) {
                if (d.status == "delivered") {
                    totalLiters += d.liters
                    totalBilled += d.totalAmount
                }
                totalPaid += d.totalPaid
            }
        }

        for (p in payments) {
            if (!p.isVoid) {
                totalPaid += p.amount
            }
        }

        val netBalance = CalculationEngine.round2(totalBilled - totalPaid)
        val lastPayment = CalculationEngine.getLastPayment(customerId, deliveries, payments)

        // Build Chronological Timeline
        val rawItems = mutableListOf<KhataTimelineItem>()

        for (d in deliveries) {
            val bill = if (d.status == "delivered" && !d.isVoid) d.totalAmount else 0.0
            val paid = if (!d.isVoid) d.totalPaid else 0.0
            rawItems.add(
                KhataTimelineItem(
                    id = d.id,
                    date = d.date,
                    type = "DELIVERY",
                    description = if (d.status == "skipped") "Naga (Skipped)" else "${d.milkType} Milk (%.2f L)".format(d.liters),
                    liters = if (d.status == "delivered") d.liters else 0.0,
                    rate = d.rate,
                    billAmount = bill,
                    paidAmount = paid,
                    paymentMode = if (d.paidOnline > 0) "Online" else if (d.paidCash > 0) "Cash" else "",
                    runningBalance = 0.0, // Calculated below in chronological order
                    isVoid = d.isVoid,
                    voidedAt = d.voidedAt,
                    note = d.note
                )
            )
        }

        for (p in payments) {
            val paid = if (!p.isVoid) p.amount else 0.0
            rawItems.add(
                KhataTimelineItem(
                    id = p.id,
                    date = p.date,
                    type = "PAYMENT",
                    description = "Payment (${p.paymentMode})",
                    liters = 0.0,
                    rate = 0.0,
                    billAmount = 0.0,
                    paidAmount = paid,
                    paymentMode = p.paymentMode,
                    runningBalance = 0.0,
                    isVoid = p.isVoid,
                    voidedAt = p.voidedAt,
                    note = p.note
                )
            )
        }

        // Sort ascending by date for correct running balance calculation
        rawItems.sortBy { it.date }

        var running = 0.0
        val timelineWithBalance = rawItems.map { item ->
            if (!item.isVoid) {
                running += (item.billAmount - item.paidAmount)
            }
            item.copy(runningBalance = CalculationEngine.round2(running))
        }.reversed() // Show newest first on screen

        val todayStr = CalculationEngine.todayStr()
        val todayDeliv = deliveries.find { it.date == todayStr && !it.isVoid && it.status == "delivered" }
        val todayPayments = payments.filter { it.date == todayStr && !it.isVoid }
        val todayCash = (todayDeliv?.paidCash ?: 0.0) + todayPayments.filter { it.paymentMode == "Cash" }.sumOf { it.amount }
        val todayOnline = (todayDeliv?.paidOnline ?: 0.0) + todayPayments.filter { it.paymentMode != "Cash" }.sumOf { it.amount }

        return CustomerDetailUiState(
            customer = customer,
            currentBalance = netBalance,
            totalLitersDelivered = CalculationEngine.round2(totalLiters),
            totalBilled = CalculationEngine.round2(totalBilled),
            totalPaid = CalculationEngine.round2(totalPaid),
            lastPayment = lastPayment,
            timeline = timelineWithBalance,
            milkDeliveries = deliveries,
            payments = payments,
            customerNotes = notes,
            pinnedNote = notes.firstOrNull { it.isPinned },
            routeName = routeName,
            pbsPending = pbsPending,
            tags = meta.tags,
            upiId = meta.upiId,
            payeeName = meta.payeeName,
            dairyPhone = meta.dairyPhone,
            todayLiters = todayDeliv?.liters ?: 0.0,
            todayBill = todayDeliv?.totalAmount ?: 0.0,
            todayCashPaid = CalculationEngine.round2(todayCash),
            todayOnlinePaid = CalculationEngine.round2(todayOnline),
            isLoading = false,
            pdfExportResult = pdfRes
        )
    }

    fun saveNote(content: String, category: String = "General", isPinned: Boolean = false) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val note = CustomerNoteEntity(
                id = "note_${System.currentTimeMillis()}_$customerId",
                customerId = customerId,
                content = content.trim(),
                category = category,
                isPinned = isPinned,
                createdAt = nowStr,
                updatedAt = nowStr
            )
            repository.saveNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun togglePinNote(note: CustomerNoteEntity) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val updated = note.copy(isPinned = !note.isPinned, updatedAt = nowStr)
            repository.saveNote(updated)
        }
    }

    fun recordOneTapPayment(amount: Double) {
        val now = System.currentTimeMillis()
        if (now - lastPaymentActionTime < 1500L) return
        lastPaymentActionTime = now

        viewModelScope.launch {
            val id = "pay_ot_${now}_$customerId"
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val payment = PaymentEntity(
                id = id,
                customerId = customerId,
                date = CalculationEngine.todayStr(),
                amount = amount,
                paymentMode = "Cash",
                paymentSubtype = "Quick Payment",
                note = "One-Tap Payment",
                createdAt = nowStr,
                isVoid = false
            )
            repository.savePayment(payment)
        }
    }

    fun recordDirectPayment(
        amount: Double,
        mode: String,
        note: String,
        date: String = CalculationEngine.todayStr()
    ) {
        viewModelScope.launch {
            val id = "pay_${System.currentTimeMillis().toString(36)}_${customerId}"
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val payment = PaymentEntity(
                id = id,
                customerId = customerId,
                date = date,
                amount = amount,
                paymentMode = mode,
                paymentSubtype = mode,
                note = note.ifBlank { "Payment" },
                createdAt = isoDate,
                isVoid = false
            )
            repository.savePayment(payment)
        }
    }

    fun recordDueAdjustment(
        currentBalance: Double,
        targetDue: Double,
        mode: String = "Adjustment",
        note: String = "Due Settlement / Adjustment",
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val delta = CalculationEngine.round2(currentBalance - targetDue)
            if (delta != 0.0) {
                val id = "pay_adj_${System.currentTimeMillis().toString(36)}_${customerId}"
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                val today = CalculationEngine.todayStr()
                val payment = PaymentEntity(
                    id = id,
                    customerId = customerId,
                    date = today,
                    amount = delta,
                    paymentMode = mode,
                    paymentSubtype = "Settlement",
                    note = note.ifBlank { "Due Settlement to ${CalculationEngine.formatMoney(targetDue)}" },
                    createdAt = isoDate,
                    isVoid = false
                )
                repository.savePayment(payment)
            }
            onSuccess?.invoke()
        }
    }


    fun voidTransaction(item: KhataTimelineItem) {
        viewModelScope.launch {
            if (item.type == "DELIVERY") {
                repository.voidDelivery(item.id)
            } else {
                repository.voidPayment(item.id)
            }
        }
    }

    fun updateCustomerDetails(
        name: String,
        phone: String,
        mohalla: String,
        landmark: String,
        defaultLiters: Double,
        ratePerLiter: Double,
        billingType: String,
        deliveryFrequency: String
    ) {
        viewModelScope.launch {
            val curr = repository.getCustomerByIdDirect(customerId) ?: return@launch
            val updated = curr.copy(
                name = name,
                phone = phone,
                mohalla = mohalla,
                landmark = landmark,
                defaultLiters = defaultLiters,
                ratePerLiter = ratePerLiter,
                billingType = billingType,
                deliveryFrequency = deliveryFrequency
            )
            repository.saveCustomer(updated)
        }
    }

    fun updateCustomerRate(newRate: Double, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val curr = repository.getCustomerByIdDirect(customerId) ?: return@launch
            val updated = curr.copy(ratePerLiter = newRate)
            repository.saveCustomer(updated)
            onSuccess?.invoke()
        }
    }

    fun clearCustomerDues(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val currentBal = uiState.value.currentBalance
            if (currentBal > 0.0) {
                recordDueAdjustment(
                    currentBalance = currentBal,
                    targetDue = 0.0,
                    mode = "Adjustment",
                    note = "Dues Settled / बकाया 0 किया गया"
                )
            }
            if (uiState.value.pbsPending > 0.0) {
                updatePbsPending(0.0)
            }
            onSuccess?.invoke()
        }
    }

    fun recordAdvanceAmount(amount: Double, mode: String = "Advance", note: String = "Advance Received", onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (amount > 0.0) {
                recordDirectPayment(
                    amount = amount,
                    mode = mode,
                    note = note.ifBlank { "Advance Payment" }
                )
            }
            onSuccess?.invoke()
        }
    }

    fun archiveCustomer(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.archiveCustomer(customerId)
            onSuccess?.invoke()
        }
    }

    fun exportKhataPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val cust = repository.getCustomerByIdDirect(customerId) ?: return@launch
            val deliveries = repository.getDeliveriesForCustomerDirect(customerId)
            val payments = repository.getPaymentsForCustomerDirect(customerId)
            val res = PdfReportGenerator.generateCustomerKhataPdf(context, cust, deliveries, payments)
            _pdfResult.value = res
        }
    }

    fun exportMonthlyStatementPdf(context: Context, monthStr: String = CalculationEngine.todayStr().take(7)) {
        viewModelScope.launch(Dispatchers.IO) {
            val cust = repository.getCustomerByIdDirect(customerId) ?: return@launch
            val deliveries = repository.getDeliveriesForCustomerDirect(customerId)
            val payments = repository.getPaymentsForCustomerDirect(customerId)
            val res = PdfReportGenerator.generateMonthlyStatementPdf(context, cust, monthStr, deliveries, payments)
            _pdfResult.value = res
        }
    }

    fun exportSingleReceiptPdf(context: Context, receiptData: com.shahbuddindairy.app.receipt.ReceiptData) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = PdfReportGenerator.generateSingleReceiptPdf(context, receiptData)
            _pdfResult.value = res
        }
    }

    fun getReceiptData(date: String = CalculationEngine.todayStr()): com.shahbuddindairy.app.receipt.ReceiptData {
        val state = uiState.value
        val cust = state.customer
        val deliv = state.milkDeliveries.find { it.date == date && !it.isVoid }
        val datePayments = state.payments.filter { it.date == date && !it.isVoid }
        val cash = (deliv?.paidCash ?: 0.0) + datePayments.filter { it.paymentMode == "Cash" }.sumOf { it.amount }
        val online = (deliv?.paidOnline ?: 0.0) + datePayments.filter { it.paymentMode != "Cash" }.sumOf { it.amount }
        val totPaid = cash + online
        val bill = deliv?.totalAmount ?: 0.0

        return com.shahbuddindairy.app.receipt.ReceiptData(
            businessName = state.payeeName,
            businessPhone = state.dairyPhone,
            upiId = state.upiId,
            customerName = cust?.name ?: "Customer",
            customerId = customerId,
            date = date,
            milkLiters = deliv?.liters ?: (cust?.defaultLiters ?: 0.0),
            milkType = deliv?.milkType ?: (cust?.milkType ?: "भैंस"),
            rate = deliv?.rate ?: (cust?.ratePerLiter ?: 0.0),
            billAmount = bill,
            previousDue = state.currentBalance,
            cashPaid = cash,
            onlinePaid = online,
            totalPaid = totPaid,
            advance = if (state.currentBalance < 0) -state.currentBalance else 0.0,
            remainingDue = if (state.currentBalance > 0) state.currentBalance else 0.0,
            pbsPending = state.pbsPending,
            receiptNumber = "RCP-$customerId-${date.replace("-", "")}",
            note = deliv?.note ?: ""
        )
    }

    fun clearPdfResult() {
        _pdfResult.value = null
    }
}

class CustomerDetailViewModelFactory(
    private val application: Application,
    private val customerId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CustomerDetailViewModel(application, customerId) as T
    }
}
