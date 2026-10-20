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

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class PaymentItemRow(
    val customerName: String,
    val amount: Double,
    val time: String = "",
    val mode: String = "Cash"
)

data class DailyCustomerReportRow(
    val customer: CustomerEntity,
    val delivery: DeliveryEntity?,
    val isServed: Boolean,
    val isNaga: Boolean,
    val isAltDay: Boolean = false,
    val liters: Double,
    val rate: Double,
    val bill: Double,
    val cashPaid: Double,
    val netBalance: Double = 0.0,
    val deliveryTime: String = "",
    val note: String = ""
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.DAILY,
    val selectedDate: String = CalculationEngine.todayStr(),
    val totalSoldLiters: Double = 0.0,
    val buffaloLiters: Double = 0.0,
    val cowLiters: Double = 0.0,
    val totalBill: Double = 0.0,
    val totalCashCollected: Double = 0.0,
    val totalOnlineCollected: Double = 0.0,
    val pendingDue: Double = 0.0,
    val advanceCollected: Double = 0.0,
    val servedCount: Int = 0,
    val nagaCount: Int = 0,
    val pendingCount: Int = 0,
    val altDayCount: Int = 0,
    val totalScheduledCount: Int = 0,
    val totalMarketDue: Double = 0.0,
    val totalCustomerAdvance: Double = 0.0,
    val cashReceiptsList: List<PaymentItemRow> = emptyList(),
    val onlineReceiptsList: List<PaymentItemRow> = emptyList(),
    val topDueCustomers: List<Pair<String, Double>> = emptyList(),
    val topAdvanceCustomers: List<Pair<String, Double>> = emptyList(),
    val last7DaysVolume: List<Pair<String, Double>> = emptyList(),
    val avgMilkPerCustomer: Double = 0.0,
    val avgBillPerCustomer: Double = 0.0,
    val highestMilkCustomer: Pair<String, Double>? = null,
    val highestBillCustomer: Pair<String, Double>? = null,
    val customerRows: List<DailyCustomerReportRow> = emptyList(),
    val periodLabel: String = "Today",
    val trendVsYesterdayPercent: Double? = null,
    val factualObservations: List<String> = emptyList(),
    val factualAlerts: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val pdfResult: PdfReportGenerator.PdfResult? = null
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository

    private val _selectedDate = MutableStateFlow(CalculationEngine.todayStr())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.DAILY)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _pdfResult = MutableStateFlow<PdfReportGenerator.PdfResult?>(null)
    val pdfResult = _pdfResult.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf = _isGeneratingPdf.asStateFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedDate,
        _selectedPeriod,
        repository.getAllCustomers(),
        _pdfResult
    ) { date, period, customers, pdfRes ->
        buildReport(date, period, customers, pdfRes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportsUiState(isLoading = true)
    )

    private suspend fun buildReport(
        date: String,
        period: ReportPeriod,
        customers: List<CustomerEntity>,
        pdfRes: PdfReportGenerator.PdfResult?
    ): ReportsUiState = withContext(Dispatchers.IO) {
        val allDeliveries = repository.getAllDeliveriesDirect()
        val allPayments = repository.getAllPaymentsDirect()

        val periodDeliveries = when (period) {
            ReportPeriod.DAILY -> allDeliveries.filter { it.date == date }
            ReportPeriod.WEEKLY -> {
                val days = (0..6).map { CalculationEngine.prevDayStr(date, it) }.toSet()
                allDeliveries.filter { it.date in days }
            }
            ReportPeriod.MONTHLY -> {
                val days = (0..29).map { CalculationEngine.prevDayStr(date, it) }.toSet()
                allDeliveries.filter { it.date in days }
            }
        }

        val periodPayments = when (period) {
            ReportPeriod.DAILY -> allPayments.filter { it.date == date }
            ReportPeriod.WEEKLY -> {
                val days = (0..6).map { CalculationEngine.prevDayStr(date, it) }.toSet()
                allPayments.filter { it.date in days }
            }
            ReportPeriod.MONTHLY -> {
                val days = (0..29).map { CalculationEngine.prevDayStr(date, it) }.toSet()
                allPayments.filter { it.date in days }
            }
        }

        val pbsMap = repository.getAllPbsMap()

        val delivMap = periodDeliveries.groupBy { it.customerId }

        var sumLiters = 0.0
        var buffalo = 0.0
        var cow = 0.0
        var sumBill = 0.0
        var served = 0
        var naga = 0
        var pending = 0
        var altDay = 0

        val cashReceipts = mutableListOf<PaymentItemRow>()
        val onlineReceipts = mutableListOf<PaymentItemRow>()

        for (d in periodDeliveries) {
            if (!d.isVoid) {
                val cName = customers.find { it.id == d.customerId }?.name ?: "Customer #${d.customerId}"
                val timeStr = d.deliveryTime.ifBlank { "12:00 pm" }
                if (d.paidCash > 0) {
                    cashReceipts.add(PaymentItemRow(customerName = cName, amount = d.paidCash, time = timeStr, mode = "Cash"))
                }
                if (d.paidOnline > 0) {
                    onlineReceipts.add(PaymentItemRow(customerName = cName, amount = d.paidOnline, time = timeStr, mode = "Online"))
                }
            }
        }

        for (p in periodPayments) {
            if (!p.isVoid) {
                val cName = customers.find { it.id == p.customerId }?.name ?: "Customer #${p.customerId}"
                val timeStr = p.createdAt.takeLast(8).ifBlank { "12:00 pm" }
                if (p.paymentMode == "Cash") {
                    cashReceipts.add(PaymentItemRow(customerName = cName, amount = p.amount, time = timeStr, mode = "Cash"))
                } else {
                    onlineReceipts.add(PaymentItemRow(customerName = cName, amount = p.amount, time = timeStr, mode = p.paymentMode))
                }
            }
        }

        val sumCash = cashReceipts.sumOf { it.amount }
        val sumOnline = onlineReceipts.sumOf { it.amount }

        val rows = mutableListOf<DailyCustomerReportRow>()

        for (c in customers) {
            if (c.isArchived == 1 || c.isActive == 0) continue

            val custDelivs = delivMap[c.id] ?: emptyList()
            val latestD = custDelivs.lastOrNull()
            val isServed = custDelivs.any { !it.isVoid && it.status == "delivered" }
            val isNaga = custDelivs.any { !it.isVoid && (it.status == "skipped" || it.status == "naga") }
            val isAltDay = if (period == ReportPeriod.DAILY) CalculationEngine.isOffDay(c, date) else false

            val custLiters = custDelivs.filter { !it.isVoid && it.status == "delivered" }.sumOf { it.liters }
            val custBill = custDelivs.filter { !it.isVoid && it.status == "delivered" }.sumOf { it.totalAmount }
            val custCash = custDelivs.filter { !it.isVoid }.sumOf { it.paidCash }
            val custOnline = custDelivs.filter { !it.isVoid }.sumOf { it.paidOnline }

            if (isServed) {
                served++
                sumLiters += custLiters
                custDelivs.filter { !it.isVoid && it.status == "delivered" }.forEach { d ->
                    if (d.milkType == "गाय") cow += d.liters else buffalo += d.liters
                }
                sumBill += custBill
            } else if (isNaga) {
                naga++
            } else if (isAltDay) {
                altDay++
            } else {
                pending++
            }

            val prevBal = CalculationEngine.calculateCustomerBalance(c.id, allDeliveries, allPayments)
            val pbs = pbsMap[c.id] ?: 0.0

            rows.add(
                DailyCustomerReportRow(
                    customer = c,
                    delivery = latestD,
                    isServed = isServed,
                    isNaga = isNaga,
                    isAltDay = isAltDay,
                    liters = custLiters,
                    rate = latestD?.rate ?: c.ratePerLiter,
                    bill = custBill,
                    cashPaid = custCash + custOnline,
                    netBalance = CalculationEngine.round2(prevBal + pbs),
                    deliveryTime = latestD?.deliveryTime ?: "",
                    note = c.note
                )
            )
        }

        val pendingDue = if (sumBill > (sumCash + sumOnline)) sumBill - (sumCash + sumOnline) else 0.0
        val advance = if ((sumCash + sumOnline) > sumBill) (sumCash + sumOnline) - sumBill else 0.0

        // Calculate comparison trend (vs yesterday)
        val trend = if (period == ReportPeriod.DAILY) {
            val yesterdayDate = CalculationEngine.prevDayStr(date)
            val yLiters = allDeliveries.filter { it.date == yesterdayDate && !it.isVoid && it.status == "delivered" }.sumOf { it.liters }
            if (yLiters > 0.0) {
                CalculationEngine.round2(((sumLiters - yLiters) / yLiters) * 100.0)
            } else null
        } else null

        // Top Dues & Advances
        var totalMarketDue = 0.0
        var totalCustomerAdvance = 0.0
        val dueList = mutableListOf<Pair<String, Double>>()
        val advanceList = mutableListOf<Pair<String, Double>>()

        val activeCustomers = customers.filter { it.isActive == 1 && it.isArchived == 0 }
        for (c in activeCustomers) {
            val net = CalculationEngine.calculateCustomerBalance(c.id, allDeliveries, allPayments)
            val pbs = pbsMap[c.id] ?: 0.0
            val totalBal = CalculationEngine.round2(net + pbs)
            if (totalBal > 0) {
                totalMarketDue += totalBal
                dueList.add(c.name to totalBal)
            } else if (totalBal < 0) {
                totalCustomerAdvance += (-totalBal)
                advanceList.add(c.name to (-totalBal))
            }
        }
        dueList.sortByDescending { it.second }
        advanceList.sortByDescending { it.second }

        // Last 7 Days Volume Trend
        val last7Days = (0..6).reversed().map { daysAgo ->
            val dStr = CalculationEngine.prevDayStr(date, daysAgo)
            val v = allDeliveries.filter { it.date == dStr && !it.isVoid && it.status == "delivered" }.sumOf { it.liters }
            val dispDate = try {
                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfOut = SimpleDateFormat("dd MMM", Locale.US)
                sdfOut.format(sdfIn.parse(dStr) ?: Date())
            } catch (e: Exception) {
                dStr.takeLast(5)
            }
            dispDate to CalculationEngine.round2(v)
        }

        val avgMilk = if (served > 0) CalculationEngine.round2(sumLiters / served) else 0.0
        val avgBill = if (served > 0) CalculationEngine.round2(sumBill / served) else 0.0
        val highestMilk = rows.filter { it.isServed }.maxByOrNull { it.liters }?.let { it.customer.name to it.liters }
        val highestBill = rows.filter { it.isServed }.maxByOrNull { it.bill }?.let { it.customer.name to it.bill }

        val pLabel = when (period) {
            ReportPeriod.DAILY -> "Daily (${CalculationEngine.formatDateDisplay(date)})"
            ReportPeriod.WEEKLY -> "Last 7 Days (साप्ताहिक)"
            ReportPeriod.MONTHLY -> "Last 30 Days (मासिक)"
        }

        // Factual Local Rule-based Smart Daily Assistant observations
        val pinnedNotes = repository.getAllPinnedNotesDirect()
        val lastBackup = repository.getSetting("last_backup_time") ?: repository.getSetting("last_local_backup_time")

        val obs = mutableListOf<String>()
        val alerts = mutableListOf<String>()

        val totalActive = customers.count { it.isArchived == 0 && it.isActive == 1 }
        if (totalActive > 0 && period == ReportPeriod.DAILY) {
            val pct = (served * 100) / totalActive
            obs.add("Route $pct% completed ($served/$totalActive delivered)")
        }
        if (pending > 0 && period == ReportPeriod.DAILY) {
            obs.add("$pending customer deliveries remain pending today")
        }
        if (sumBill > 0.0) {
            obs.add("Total collection volume: ${CalculationEngine.formatMoney(sumBill)} (Collected: ${CalculationEngine.formatMoney(sumCash + sumOnline)})")
        }
        if (pinnedNotes.isNotEmpty()) {
            obs.add("${pinnedNotes.size} pinned customer notes active")
        }

        // Factual Business Alerts
        if (naga > 0 && period == ReportPeriod.DAILY) {
            alerts.add("$naga customers marked Naga (नागा) today")
        }
        if (pendingDue > 0.0) {
            alerts.add("Pending route balance today: ${CalculationEngine.formatMoney(pendingDue)}")
        }
        if (lastBackup.isNullOrBlank()) {
            alerts.add("Local database backup has not been created yet")
        }

        ReportsUiState(
            selectedPeriod = period,
            selectedDate = date,
            totalSoldLiters = CalculationEngine.round2(sumLiters),
            buffaloLiters = CalculationEngine.round2(buffalo),
            cowLiters = CalculationEngine.round2(cow),
            totalBill = CalculationEngine.round2(sumBill),
            totalCashCollected = CalculationEngine.round2(sumCash),
            totalOnlineCollected = CalculationEngine.round2(sumOnline),
            pendingDue = CalculationEngine.round2(pendingDue),
            advanceCollected = CalculationEngine.round2(advance),
            servedCount = served,
            nagaCount = naga,
            pendingCount = pending,
            altDayCount = altDay,
            totalScheduledCount = served + naga + pending,
            totalMarketDue = CalculationEngine.round2(totalMarketDue),
            totalCustomerAdvance = CalculationEngine.round2(totalCustomerAdvance),
            cashReceiptsList = cashReceipts,
            onlineReceiptsList = onlineReceipts,
            topDueCustomers = dueList.take(7),
            topAdvanceCustomers = advanceList.take(7),
            last7DaysVolume = last7Days,
            avgMilkPerCustomer = avgMilk,
            avgBillPerCustomer = avgBill,
            highestMilkCustomer = highestMilk,
            highestBillCustomer = highestBill,
            customerRows = rows,
            periodLabel = pLabel,
            trendVsYesterdayPercent = trend,
            factualObservations = obs,
            factualAlerts = alerts,
            isLoading = false,
            pdfResult = pdfRes
        )
    }

    fun onPeriodSelected(period: ReportPeriod) {
        _selectedPeriod.value = period
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
            _isGeneratingPdf.value = true
            try {
                val date = _selectedDate.value
                val customers = repository.getAllCustomersDirect()
                val deliveries = repository.getAllDeliveriesDirect()
                val payments = repository.getAllPaymentsDirect()
                val pbsMap = repository.getAllPbsMap()
                val res = PdfReportGenerator.generateDailyReportPdf(context, date, customers, deliveries, payments, pbsMap)
                _pdfResult.value = res
            } catch (e: Exception) {
                _pdfResult.value = PdfReportGenerator.PdfResult(false, "त्रुटि: ${e.message}", null, null)
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun clearPdfResult() {
        _pdfResult.value = null
    }

    fun generateWhatsAppReportText(state: ReportsUiState): String {
        return generateReportText(state)
    }

    companion object {
        fun getHindiDayOfWeek(dateStr: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val d = sdf.parse(dateStr) ?: return ""
                val cal = Calendar.getInstance().apply { time = d }
                when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> "रविवार"
                    Calendar.MONDAY -> "सोमवार"
                    Calendar.TUESDAY -> "मंगलवार"
                    Calendar.WEDNESDAY -> "बुधवार"
                    Calendar.THURSDAY -> "गुरुवार"
                    Calendar.FRIDAY -> "शुक्रवार"
                    Calendar.SATURDAY -> "शनिवार"
                    else -> ""
                }
            } catch (e: Exception) {
                ""
            }
        }

        fun generateReportText(state: ReportsUiState): String {
            val dateDisplay = CalculationEngine.formatDateDisplay(state.selectedDate)
            val hindiDay = getHindiDayOfWeek(state.selectedDate)
            val totalCustomers = state.customerRows.size
            val totalMilk = state.totalSoldLiters
            val totalBill = state.totalBill
            val totalCash = state.totalCashCollected
            val totalOnline = state.totalOnlineCollected
            val totalDue = state.pendingDue
            val totalAdv = state.totalCustomerAdvance

            val totalCollected = totalCash + totalOnline

            return buildString {
                appendLine("*SHAHBUDDIN DAIRY (शाहबुद्दीन डेयरी)*")
                appendLine("📍 96, Gulab Nagar, Bareilly, Uttar Pradesh, India")
                appendLine("मो०: 9258263340")
                appendLine("*Daily Business Statement • दैनिक रिपोर्ट*")
                appendLine("$dateDisplay • $hindiDay")
                appendLine("📍 Main Route (मुख्य रूट)")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("📊 *आज का वित्तीय हिसाब (Financial Summary)*")
                val trendStr = if (state.trendVsYesterdayPercent != null) {
                    if (state.trendVsYesterdayPercent >= 0) " (↑ +${state.trendVsYesterdayPercent}%)" else " (↓ ${state.trendVsYesterdayPercent}%)"
                } else ""
                appendLine("🥛 कुल दूध वितरण: *%.2f L*${trendStr}".format(totalMilk))
                appendLine("💵 आज का कुल बिल: *₹%,.2f*".format(totalBill))
                appendLine("💰 आज प्राप्त नकद: *₹%,.2f* (${state.cashReceiptsList.size} ग्राहक)".format(totalCash))
                if (totalOnline > 0) {
                    appendLine("📱 आज प्राप्त ऑनलाइन: *₹%,.2f* (${state.onlineReceiptsList.size} ग्राहक)".format(totalOnline))
                }
                appendLine("⚡ आज की कुल वसूली: *₹%,.2f*".format(totalCollected))
                appendLine("⏳ आज का बाकी बिल: *₹%,.2f*".format(state.pendingDue))
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("⚠️ *कुल बाजार उधारी (Market Due): ₹%,.2f*".format(state.totalMarketDue))
                appendLine("🟢 *कुल अग्रिम जमा (Advance): ₹%,.2f*".format(totalAdv))
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("🚚 *डिलीवरी स्थिति ($totalCustomers सक्रिय ग्राहक)*")
                appendLine("🟢 Delivered: *${state.servedCount}*")
                appendLine("🟠 Pending: *${state.pendingCount}*")
                appendLine("🔴 Naaga: *${state.nagaCount}*")
                appendLine("🔵 Alt-Day (ऑफ): *${state.altDayCount}* (आज बंद)")
                appendLine("📊 शेड्यूल पूर्ण: *${state.servedCount} / ${state.servedCount + state.pendingCount + state.nagaCount}*")
                appendLine("━━━━━━━━━━━━━━━━━━━━")

                if (state.cashReceiptsList.isNotEmpty()) {
                    appendLine("💵 *आज प्राप्त नकद (₹%,.0f)*".format(totalCash))
                    state.cashReceiptsList.take(15).forEach { r ->
                        appendLine("✓ ${r.customerName}: ₹%,.0f".format(r.amount))
                    }
                    if (state.cashReceiptsList.size > 15) {
                        appendLine("...तथा अन्य ${state.cashReceiptsList.size - 15} ग्राहक")
                    }
                    appendLine("*कुल नकद प्राप्त: ₹%,.0f*".format(totalCash))
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                }

                if (state.onlineReceiptsList.isNotEmpty()) {
                    appendLine("📱 *आज प्राप्त ऑनलाइन (₹%,.0f)*".format(totalOnline))
                    state.onlineReceiptsList.forEach { r ->
                        appendLine("✓ ${r.customerName}: ₹%,.0f (${r.mode})".format(r.amount))
                    }
                    appendLine("*कुल ऑनलाइन प्राप्त: ₹%,.0f*".format(totalOnline))
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                }

                appendLine("📑 *स्टेटस अनुसार ग्राहक सूची*")
                appendLine("🟢 *Delivered (${state.servedCount})*")
                appendLine("कुल दूध: %.2f L | कुल बिल: ₹%,.0f | नकद: ₹%,.0f".format(totalMilk, totalBill, totalCash))
                appendLine("🟠 *Pending (${state.pendingCount})*")
                appendLine("कुल दूध: 0.00 L | कुल बिल: ₹0.00 | नकद: ₹0")
                appendLine("🔴 *Naga (${state.nagaCount})*")
                appendLine("कुल दूध: 0.00 L | कुल बिल: ₹0.00 | नकद: ₹0")
                if (state.altDayCount > 0) {
                    appendLine("🔵 *Alt-Day (${state.altDayCount})*")
                    appendLine("आज दूध नहीं आता (1 दिन छोड़कर आने वाले)")
                }
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("⚠️ *महत्वपूर्ण नोट*")
                appendLine("• कुछ ग्राहकों से प्राप्त नकद उनके आज के बिल से अधिक है।")
                appendLine("• यह राशि पुराने बकाये या एडवांस में जा सकती है।")
                appendLine("• कृपया खाता में मिलान करके पुष्टि कर लें।")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("Generated by Shahbuddin Dairy")
                appendLine("Built by Uwais Ghosi • V8.9")
                val printDate = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date())
                appendLine(printDate)
            }
        }

        fun generateWhatsAppReportText(
            selectedDate: String,
            customers: List<com.shahbuddindairy.app.data.model.CustomerEntity>,
            deliveries: List<com.shahbuddindairy.app.data.model.DeliveryEntity>,
            payments: List<com.shahbuddindairy.app.data.model.PaymentEntity> = emptyList()
        ): String {
            val dateDisplay = CalculationEngine.formatDateDisplay(selectedDate)
            val hindiDay = getHindiDayOfWeek(selectedDate)
            val delivered = deliveries.filter { it.status == "delivered" && !it.isVoid }
            val naga = deliveries.filter { (it.status == "skipped" || it.status == "naga") && !it.isVoid }
            val totalMilk = delivered.sumOf { it.liters }
            val totalBill = delivered.sumOf { it.totalAmount }
            val totalCash = delivered.sumOf { it.paidCash } + payments.filter { it.date == selectedDate && it.paymentMode == "Cash" && !it.isVoid }.sumOf { it.amount }
            val totalOnline = delivered.sumOf { it.paidOnline } + payments.filter { it.date == selectedDate && it.paymentMode != "Cash" && !it.isVoid }.sumOf { it.amount }
            val pendingDue = (totalBill - (totalCash + totalOnline)).coerceAtLeast(0.0)

            return buildString {
                appendLine("*SHAHBUDDIN DAIRY*")
                appendLine("*Daily Milk Sales Report*")
                appendLine("$dateDisplay • $hindiDay")
                appendLine("📍 Main Route (मुख्य रूट)")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("📊 *आज का सारांश (Summary)*")
                appendLine("🥛 कुल दूध: *%.2f L*".format(totalMilk))
                appendLine("💵 कुल बिक्री: *₹%,.2f*".format(totalBill))
                appendLine("💰 कैश प्राप्त: *₹%,.2f*".format(totalCash))
                appendLine("📱 ऑनलाइन: *₹%,.2f*".format(totalOnline))
                appendLine("⚠️ कुल बकाया: *₹%,.2f*".format(pendingDue))
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("🚚 *डिलीवरी स्थिति (${customers.size} ग्राहक)*")
                appendLine("🟢 Delivered: *${delivered.size}*")
                appendLine("🔴 Naaga: *${naga.size}*")
                val pendingCount = (customers.size - delivered.size - naga.size).coerceAtLeast(0)
                appendLine("🟠 Pending: *${pendingCount}*")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("Generated by Shahbuddin Dairy V8.9")
            }
        }
    }
}

