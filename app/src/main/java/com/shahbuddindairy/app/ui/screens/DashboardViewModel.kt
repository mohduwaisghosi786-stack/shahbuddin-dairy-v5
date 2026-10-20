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
        var totalMarketDue = 0.0
        var totalAdvance = 0.0
        for (c in customers) {
            val net = CalculationEngine.calculateCustomerBalance(c.id, allDeliveries, allPayments)
            val pbs = repository.getPbsPending(c.id)
            val totalBal = CalculationEngine.round2(net + pbs)
            if (totalBal > 0) totalMarketDue += totalBal
            else if (totalBal < 0) totalAdvance += (-totalBal)
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
