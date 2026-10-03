package com.shahbuddindairy.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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

data class DailyCustomerReportRow(
    val customer: CustomerEntity,
    val delivery: DeliveryEntity?,
    val isServed: Boolean,
    val isNaga: Boolean,
    val liters: Double,
    val rate: Double,
    val bill: Double,
    val cashPaid: Double
)

data class ReportsUiState(
    val selectedDate: String = CalculationEngine.todayStr(),
    val totalSoldLiters: Double = 0.0,
    val buffaloLiters: Double = 0.0,
    val cowLiters: Double = 0.0,
    val totalBill: Double = 0.0,
    val totalCashCollected: Double = 0.0,
    val pendingDue: Double = 0.0,
    val advanceCollected: Double = 0.0,
    val servedCount: Int = 0,
    val nagaCount: Int = 0,
    val pendingCount: Int = 0,
    val customerRows: List<DailyCustomerReportRow> = emptyList(),
    val isLoading: Boolean = false,
    val pdfResult: PdfReportGenerator.PdfResult? = null
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository

    private val _selectedDate = MutableStateFlow(CalculationEngine.todayStr())
    val selectedDate = _selectedDate.asStateFlow()

    private val _pdfResult = MutableStateFlow<PdfReportGenerator.PdfResult?>(null)
    val pdfResult = _pdfResult.asStateFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedDate,
        repository.getAllCustomers(),
        _pdfResult
    ) { date, customers, pdfRes ->
        buildDailyReport(date, customers, pdfRes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportsUiState(isLoading = true)
    )

    private suspend fun buildDailyReport(
        date: String,
        customers: List<CustomerEntity>,
        pdfRes: PdfReportGenerator.PdfResult?
    ): ReportsUiState = withContext(Dispatchers.IO) {
        val deliveries = repository.getDeliveriesForDateDirect(date)
        val delivMap = deliveries.associateBy { it.customerId }

        var sumLiters = 0.0
        var buffalo = 0.0
        var cow = 0.0
        var sumBill = 0.0
        var sumCash = 0.0
        var served = 0
        var naga = 0
        var pending = 0

        val rows = mutableListOf<DailyCustomerReportRow>()

        for (c in customers) {
            if (c.isArchived == 1 || c.isActive == 0) continue

            val d = delivMap[c.id]
            val isServed = d != null && !d.isVoid && d.status == "delivered"
            val isNaga = d != null && !d.isVoid && d.status == "skipped"

            if (isServed && d != null) {
                served++
                sumLiters += d.liters
                if (d.milkType == "गाय") cow += d.liters else buffalo += d.liters
                sumBill += d.totalAmount
                sumCash += d.paidCash
            } else if (isNaga) {
                naga++
            } else {
                pending++
            }

            rows.add(
                DailyCustomerReportRow(
                    customer = c,
                    delivery = d,
                    isServed = isServed,
                    isNaga = isNaga,
                    liters = if (isServed && d != null) d.liters else 0.0,
                    rate = if (d != null) d.rate else c.ratePerLiter,
                    bill = if (isServed && d != null) d.totalAmount else 0.0,
                    cashPaid = if (d != null) d.paidCash else 0.0
                )
            )
        }

        val pendingDue = if (sumBill > sumCash) sumBill - sumCash else 0.0
        val advance = if (sumCash > sumBill) sumCash - sumBill else 0.0

        ReportsUiState(
            selectedDate = date,
            totalSoldLiters = CalculationEngine.round2(sumLiters),
            buffaloLiters = CalculationEngine.round2(buffalo),
            cowLiters = CalculationEngine.round2(cow),
            totalBill = CalculationEngine.round2(sumBill),
            totalCashCollected = CalculationEngine.round2(sumCash),
            pendingDue = CalculationEngine.round2(pendingDue),
            advanceCollected = CalculationEngine.round2(advance),
            servedCount = served,
            nagaCount = naga,
            pendingCount = pending,
            customerRows = rows,
            isLoading = false,
            pdfResult = pdfRes
        )
    }

    fun onDateSelected(date: String) {
        _selectedDate.value = date
    }

    fun onPrevDay() {
        _selectedDate.value = CalculationEngine.prevDayStr(_selectedDate.value)
    }

    fun onNextDay() {
        _selectedDate.value = CalculationEngine.nextDayStr(_selectedDate.value)
    }

    fun exportDailyPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = _selectedDate.value
            val customers = repository.getAllCustomersDirect()
            val deliveries = repository.getAllDeliveriesDirect()
            val payments = repository.getAllPaymentsDirect()
            val res = PdfReportGenerator.generateDailyReportPdf(context, date, customers, deliveries, payments)
            _pdfResult.value = res
        }
    }

    fun clearPdfResult() {
        _pdfResult.value = null
    }

    fun generateWhatsAppReportText(state: ReportsUiState): String {
        return generateReportText(state)
    }

    companion object {
        fun generateReportText(state: ReportsUiState): String {
            val dateDisplay = CalculationEngine.formatDateDisplay(state.selectedDate)
            val sb = StringBuilder()
            sb.append("*SHAHBUDDIN DAIRY*\n")
            sb.append("*Daily Milk Sales Report*\n\n")
            sb.append("Date: $dateDisplay\n\n")
            sb.append("━━━━━━━━━━━━━━━━━━\n\n")
            sb.append("*SUMMARY*\n\n")
            sb.append("Customers: ${state.customerRows.size}\n")
            sb.append("Delivered: ${state.servedCount}\n")
            sb.append("Naaga: ${state.nagaCount}\n")
            sb.append("Pending: ${state.pendingCount}\n\n")
            sb.append("Total Milk: ${CalculationEngine.formatLiters(state.totalSoldLiters)}\n")
            sb.append("Total Bill: ${CalculationEngine.formatMoney(state.totalBill)}\n")
            sb.append("Cash: ${CalculationEngine.formatMoney(state.totalCashCollected)}\n")
            sb.append("Online: ₹0\n")
            sb.append("Due: ${CalculationEngine.formatMoney(state.pendingDue)}\n\n")
            sb.append("━━━━━━━━━━━━━━━━━━\n\n")
            sb.append("*DELIVERY DETAILS*\n\n")

            val servedRows = state.customerRows.filter { it.isServed }
            if (servedRows.isEmpty()) {
                sb.append("No deliveries recorded for this date.\n\n")
            } else {
                servedRows.forEachIndexed { index, row ->
                    sb.append("${index + 1}. ${row.customer.name}\n")
                    if (row.delivery?.deliveryTime?.isNotBlank() == true) {
                        sb.append("   Time: ${row.delivery.deliveryTime}\n")
                    }
                    sb.append("   Milk: ${CalculationEngine.formatLiters(row.liters)}\n")
                    sb.append("   Bill: ${CalculationEngine.formatMoney(row.bill)}\n")
                    sb.append("   Cash: ${CalculationEngine.formatMoney(row.cashPaid)}\n")
                    sb.append("   ✓ Delivered\n\n")
                }
            }

            sb.append("━━━━━━━━━━━━━━━━━━\n\n")
            sb.append("*TOTAL*\n\n")
            sb.append("Milk: ${CalculationEngine.formatLiters(state.totalSoldLiters)}\n")
            sb.append("Bill: ${CalculationEngine.formatMoney(state.totalBill)}\n")
            sb.append("Cash: ${CalculationEngine.formatMoney(state.totalCashCollected)}\n\n")
            sb.append("Generated by Shahbuddin Dairy")

            return sb.toString()
        }

        fun generateWhatsAppReportText(
            selectedDate: String,
            customers: List<com.shahbuddindairy.app.data.model.CustomerEntity>,
            deliveries: List<com.shahbuddindairy.app.data.model.DeliveryEntity>,
            payments: List<com.shahbuddindairy.app.data.model.PaymentEntity> = emptyList()
        ): String {
            val dateDisplay = CalculationEngine.formatDateDisplay(selectedDate)
            val delivered = deliveries.filter { it.status == "delivered" && !it.isVoid }
            val naga = deliveries.filter { it.status == "naga" && !it.isVoid }
            val totalMilk = delivered.sumOf { it.liters }
            val totalBill = delivered.sumOf { it.totalAmount }
            val totalCash = delivered.sumOf { it.paidCash }
            val pendingDue = totalBill - totalCash

            val sb = StringBuilder()
            sb.append("*SHAHBUDDIN DAIRY*\n")
            sb.append("*Daily Milk Sales Report*\n\n")
            sb.append("Date: $dateDisplay\n\n")
            sb.append("━━━━━━━━━━━━━━━━━━\n\n")
            sb.append("*SUMMARY*\n\n")
            sb.append("Customers: ${customers.size}\n")
            sb.append("Delivered: ${delivered.size}\n")
            sb.append("Naaga: ${naga.size}\n")
            val pendingCount = (customers.size - delivered.size - naga.size).coerceAtLeast(0)
            sb.append("Pending: $pendingCount\n\n")
            sb.append("Total Milk: ${CalculationEngine.formatLiters(totalMilk)}\n")
            sb.append("Total Bill: ${CalculationEngine.formatMoney(totalBill)}\n")
            sb.append("Cash: ${CalculationEngine.formatMoney(totalCash)}\n")
            sb.append("Online: ₹0\n")
            sb.append("Due: ${CalculationEngine.formatMoney(pendingDue)}\n\n")
            sb.append("━━━━━━━━━━━━━━━━━━\n\n")
            sb.append("*DELIVERY DETAILS*\n\n")

            if (deliveries.isEmpty()) {
                sb.append("No deliveries recorded for this date.\n\n")
            } else {
                deliveries.filter { !it.isVoid }.forEachIndexed { index, d ->
                    val custName = customers.find { it.id == d.customerId }?.name ?: "Customer #${d.customerId}"
                    sb.append("${index + 1}. $custName\n")
                    if (!d.deliveryTime.isNullOrBlank()) {
                        sb.append("   Time: ${d.deliveryTime}\n")
                    }
                    if (d.status == "delivered") {
                        sb.append("   Milk: ${CalculationEngine.formatLiters(d.liters)}\n")
                        sb.append("   Bill: ${CalculationEngine.formatMoney(d.totalAmount)}\n")
                        sb.append("   Cash: ${CalculationEngine.formatMoney(d.paidCash)}\n")
                        sb.append("   ✓ Delivered\n\n")
                    } else if (d.status == "naga") {
                        sb.append("   ✗ Naaga (नागा)\n\n")
                    }
                }
            }

            sb.append("━━━━━━━━━━━━━━━━━━\n\n")
            sb.append("*TOTAL*\n\n")
            sb.append("Milk: ${CalculationEngine.formatLiters(totalMilk)}\n")
            sb.append("Bill: ${CalculationEngine.formatMoney(totalBill)}\n")
            sb.append("Cash: ${CalculationEngine.formatMoney(totalCash)}\n\n")
            sb.append("Generated by Shahbuddin Dairy")

            return sb.toString()
        }
    }
}

