package com.shahbuddindairy.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.RouteEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RouteProgressStat(
    val routeId: String,
    val routeName: String,
    val totalCount: Int,
    val deliveredCount: Int,
    val pendingCount: Int,
    val nagaCount: Int,
    val altDayCount: Int = 0,
    val percent: Int
)

data class TopAccountItem(
    val customer: CustomerEntity,
    val amount: Double
)

data class DashboardUiState(
    val selectedDate: String = CalculationEngine.todayStr(),
    val totalMilkDispatched: Double = 0.0,
    val totalBillToday: Double = 0.0,
    val totalCashCollectedToday: Double = 0.0,
    val totalOnlineCollectedToday: Double = 0.0,
    val totalMarketDue: Double = 0.0,
    val totalAdvanceBalance: Double = 0.0,
    val totalActiveCustomers: Int = 0,
    val deliveredCount: Int = 0,
    val pendingCount: Int = 0,
    val nagaCount: Int = 0,
    val altDayCount: Int = 0,
    val routeProgressStats: List<RouteProgressStat> = emptyList(),
    val tickerHeadlines: List<String> = emptyList(),
    val topDebtors: List<TopAccountItem> = emptyList(),
    val topAdvances: List<TopAccountItem> = emptyList(),
    val highestPaymentCustomer: String = "",
    val highestPaymentAmount: Double = 0.0,
    val canCapacityLoaded: Double = 50.0,
    val estimatedProfitToday: Double = 0.0,
    val aiAdvice: String = "",
    val isLoading: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository

    private val _selectedDate = MutableStateFlow(CalculationEngine.todayStr())
    val selectedDate: StateFlow<String> = _selectedDate

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedDate,
        repository.getAllActiveCustomers(),
        repository.getAllRoutes()
    ) { date, customers, routes ->
        val deliveries = repository.getDeliveriesForDateDirect(date)
        val allDeliveries = repository.getAllDeliveriesDirect()
        val allPayments = repository.getAllPaymentsDirect()
        val paymentsToday = allPayments.filter { it.date == date && !it.isVoid }

        var totalMilk = 0.0
        var totalBill = 0.0
        var totalCash = 0.0
        var totalOnline = 0.0
        var servedCount = 0
        var nagaCount = 0
        var pendingCount = 0
        var altDayCount = 0

        val delivMap = deliveries.filter { it.date == date && !it.isVoid }.associateBy { it.customerId }

        for (c in customers) {
            val d = delivMap[c.id]
            if (d != null && !d.isVoid) {
                if (d.status == "delivered") {
                    servedCount++
                    totalMilk += d.liters
                    totalBill += d.totalAmount
                    totalCash += d.paidCash
                    totalOnline += d.paidOnline
                } else if (d.status == "skipped" || d.status == "naga") {
                    nagaCount++
                }
            } else {
                if (CalculationEngine.isOffDay(c, date)) {
                    altDayCount++
                } else {
                    pendingCount++
                }
            }
        }

        // Add standalone direct payments received today
        for (p in paymentsToday) {
            if (p.paymentMode == "Cash") {
                totalCash += p.amount
            } else {
                totalOnline += p.amount
            }
        }

        // Financial balances across entire market (strictly unified with PBS)
        val debtorList = mutableListOf<TopAccountItem>()
        val advanceList = mutableListOf<TopAccountItem>()
        var totalMarketDue = 0.0
        var totalAdvance = 0.0
        for (c in customers) {
            val net = CalculationEngine.calculateCustomerBalance(c.id, allDeliveries, allPayments)
            val pbs = repository.getPbsPending(c.id)
            val totalBal = CalculationEngine.round2(net + pbs)
            if (totalBal > 0) {
                totalMarketDue += totalBal
                debtorList.add(TopAccountItem(c, totalBal))
            } else if (totalBal < 0) {
                totalAdvance += (-totalBal)
                advanceList.add(TopAccountItem(c, -totalBal))
            }
        }
        debtorList.sortByDescending { it.amount }
        advanceList.sortByDescending { it.amount }
        val top3Debtors = debtorList.take(3)
        val top3Advances = advanceList.take(3)

        // Find highest payment today
        var highestPayCust = ""
        var highestPayAmt = 0.0
        for (d in deliveries.filter { it.date == date && !it.isVoid }) {
            val p = d.paidCash + d.paidOnline
            if (p > highestPayAmt) {
                highestPayAmt = p
                highestPayCust = customers.find { it.id == d.customerId }?.name ?: "Customer #${d.customerId}"
            }
        }
        for (p in paymentsToday) {
            if (p.amount > highestPayAmt) {
                highestPayAmt = p.amount
                highestPayCust = customers.find { it.id == p.customerId }?.name ?: "Customer #${p.customerId}"
            }
        }

        // Ticker headlines
        val headlines = mutableListOf<String>()
        if (top3Debtors.isNotEmpty()) {
            val debtorStr = top3Debtors.joinToString(" • ") { "${it.customer.name}: ₹%,.0f Due".format(it.amount) }
            headlines.add("🚨 PAYMENT ALERT: $debtorStr")
        }
        headlines.add("🥛 SUPPLY UPDATE: %.2f L delivered today (%d served, %d naaga)".format(totalMilk, servedCount, nagaCount))
        val totalColl = totalCash + totalOnline
        headlines.add("💰 COLLECTION UPDATE: ₹%,.0f collected today (Cash: ₹%,.0f • Online: ₹%,.0f)".format(totalColl, totalCash, totalOnline))
        if (altDayCount > 0) {
            headlines.add("🔵 ALTERNATE REMINDER: %d customers on alternate off schedule today".format(altDayCount))
        }
        if (highestPayAmt > 0) {
            headlines.add("⭐ HIGHEST PAYMENT TODAY: ₹%,.0f received from $highestPayCust".format(highestPayAmt))
        }

        val profitEst = CalculationEngine.round2(totalMilk * 15.0) // Average margin ₹15/L
        val advice = when {
            servedCount == 0 -> "Ready for morning route. ${customers.size} active customers scheduled."
            pendingCount > 0 -> "Route in progress: $pendingCount remaining customers, $servedCount completed."
            else -> "Route complete! All deliveries verified. Total collection ₹%,.0f recorded.".format(totalColl)
        }

        // Route wise breakdown
        val routeStats = mutableListOf<RouteProgressStat>()
        val routeMap = routes.associateBy { it.id }
        val customersByRoute = customers.groupBy { it.routeId }

        for ((routeId, routeCusts) in customersByRoute) {
            val rName = routeMap[routeId]?.name ?: "Main Route"
            var rDelivered = 0
            var rNaga = 0
            var rPending = 0
            var rAltDay = 0
            for (rc in routeCusts) {
                val d = delivMap[rc.id]
                if (d != null && !d.isVoid) {
                    if (d.status == "delivered") rDelivered++
                    else if (d.status == "skipped" || d.status == "naga") rNaga++
                } else {
                    if (CalculationEngine.isOffDay(rc, date)) {
                        rAltDay++
                    } else {
                        rPending++
                    }
                }
            }
            val scheduledCount = rDelivered + rNaga + rPending
            val pct = if (scheduledCount > 0) (rDelivered * 100) / scheduledCount else if (routeCusts.isNotEmpty()) (rDelivered * 100) / routeCusts.size else 0
            routeStats.add(
                RouteProgressStat(
                    routeId = routeId,
                    routeName = rName,
                    totalCount = routeCusts.size,
                    deliveredCount = rDelivered,
                    pendingCount = rPending,
                    nagaCount = rNaga,
                    altDayCount = rAltDay,
                    percent = pct
                )
            )
        }

        DashboardUiState(
            selectedDate = date,
            totalMilkDispatched = CalculationEngine.round2(totalMilk),
            totalBillToday = CalculationEngine.round2(totalBill),
            totalCashCollectedToday = CalculationEngine.round2(totalCash),
            totalOnlineCollectedToday = CalculationEngine.round2(totalOnline),
            totalMarketDue = CalculationEngine.round2(totalMarketDue),
            totalAdvanceBalance = CalculationEngine.round2(totalAdvance),
            totalActiveCustomers = customers.size,
            deliveredCount = servedCount,
            pendingCount = pendingCount,
            nagaCount = nagaCount,
            altDayCount = altDayCount,
            routeProgressStats = routeStats,
            tickerHeadlines = headlines,
            topDebtors = top3Debtors,
            topAdvances = top3Advances,
            highestPaymentCustomer = highestPayCust,
            highestPaymentAmount = highestPayAmt,
            canCapacityLoaded = 50.0,
            estimatedProfitToday = profitEst,
            aiAdvice = advice,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState(isLoading = true)
    )

    fun onDateSelected(date: String) {
        _selectedDate.value = date
    }

    fun onPrevDay() {
        _selectedDate.value = CalculationEngine.prevDayStr(_selectedDate.value)
    }

    fun onNextDay() {
        _selectedDate.value = CalculationEngine.nextDayStr(_selectedDate.value)
    }
}
