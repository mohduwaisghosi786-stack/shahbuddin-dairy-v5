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
    val pbsPending: Double = 0.0,
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

    val uiState: StateFlow<CustomerDetailUiState> = combine(
        repository.getCustomerById(customerId),
        repository.getDeliveriesForCustomer(customerId),
        repository.getPaymentsForCustomer(customerId),
        repository.getSettingFlow("pbs_pending_$customerId"),
        _pdfResult
    ) { customer, deliveries, payments, pbsStr, pdfRes ->
        val pbs = pbsStr?.toDoubleOrNull() ?: 0.0
        buildCustomerState(customer, deliveries, payments, pbs, pdfRes)
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

    private fun buildCustomerState(
        customer: CustomerEntity?,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>,
        pbsPending: Double,
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
            pbsPending = pbsPending,
            isLoading = false,
            pdfExportResult = pdfRes
        )
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

    fun exportKhataPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val cust = repository.getCustomerByIdDirect(customerId) ?: return@launch
            val deliveries = repository.getDeliveriesForCustomerDirect(customerId)
            val payments = repository.getPaymentsForCustomerDirect(customerId)
            val res = PdfReportGenerator.generateCustomerKhataPdf(context, cust, deliveries, payments)
            _pdfResult.value = res
        }
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
