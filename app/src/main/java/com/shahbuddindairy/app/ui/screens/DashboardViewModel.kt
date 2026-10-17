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
    val percent: Int
)

data class DashboardUiState(
    val selectedDate: String = CalculationEngine.todayStr(),
    val totalMilkDispatched: Double = 0.0,
    val totalBillToday: Double = 0.0,
    val totalCashCollectedToday: Double = 0.0,
    val totalMarketDue: Double = 0.0,
    val totalAdvanceBalance: Double = 0.0,
    val totalActiveCustomers: Int = 0,
    val deliveredCount: Int = 0,
    val pendingCount: Int = 0,
    val nagaCount: Int = 0,
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

        var totalMilk = 0.0
        var totalBill = 0.0
        var totalCash = 0.0
        var servedCount = 0
        var nagaCount = 0
        var pendingCount = 0

        val delivMap = deliveries.filter { it.date == date }.associateBy { it.customerId }

        for (c in customers) {
            val d = delivMap[c.id]
            if (d != null && !d.isVoid) {
                if (d.status == "delivered") {
                    servedCount++
                    totalMilk += d.liters
                    totalBill += d.totalAmount
                    totalCash += d.paidCash
                } else if (d.status == "skipped") {
                    nagaCount++
                }
            } else {
                pendingCount++
            }
        }

        // Financial balances across entire market
        var totalMarketDue = 0.0
        var totalAdvance = 0.0
        for (c in customers) {
            val net = CalculationEngine.calculateCustomerBalance(c.id, allDeliveries, allPayments)
            if (net > 0) totalMarketDue += net
            else if (net < 0) totalAdvance += (-net)
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
            for (rc in routeCusts) {
                val d = delivMap[rc.id]
                if (d != null && !d.isVoid) {
                    if (d.status == "delivered") rDelivered++
                    else if (d.status == "skipped") rNaga++
                } else {
                    rPending++
                }
            }
            val pct = if (routeCusts.isNotEmpty()) (rDelivered * 100) / routeCusts.size else 0
            routeStats.add(
                RouteProgressStat(
                    routeId = routeId,
                    routeName = rName,
                    totalCount = routeCusts.size,
                    deliveredCount = rDelivered,
                    pendingCount = rPending,
                    nagaCount = rNaga,
                    percent = pct
                )
            )
        }

        DashboardUiState(
            selectedDate = date,
            totalMilkDispatched = totalMilk,
            totalBillToday = totalBill,
            totalCashCollectedToday = totalCash,
            totalMarketDue = totalMarketDue,
            totalAdvanceBalance = totalAdvance,
            totalActiveCustomers = customers.size,
            deliveredCount = servedCount,
            pendingCount = pendingCount,
            nagaCount = nagaCount,
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
