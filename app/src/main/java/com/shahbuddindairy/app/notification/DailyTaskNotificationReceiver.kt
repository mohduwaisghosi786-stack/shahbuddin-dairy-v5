package com.shahbuddindairy.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyTaskNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DELIVER = "com.shahbuddindairy.app.ACTION_NOTIF_DELIVER"
        const val ACTION_NAGA = "com.shahbuddindairy.app.ACTION_NOTIF_NAGA"
        const val ACTION_PREV = "com.shahbuddindairy.app.ACTION_NOTIF_PREV"
        const val ACTION_NEXT = "com.shahbuddindairy.app.ACTION_NOTIF_NEXT"
        const val EXTRA_CUSTOMER_ID = "extra_customer_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val customerId = intent.getLongExtra(EXTRA_CUSTOMER_ID, -1L)
        if (customerId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DairyApplication
                val repo = app.repository
                val today = CalculationEngine.todayStr()

                when (action) {
                    ACTION_DELIVER -> {
                        val customer = repo.getCustomerByIdDirect(customerId)
                        if (customer != null) {
                            // Check existing to ensure 100% idempotency
                            val existing = repo.getDeliveryForCustomerAndDate(customerId, today)
                            val deliveryId = existing?.id ?: "d_${today}_${customerId}"

                            val rate = when {
                                customer.billingType == "daily_cash" -> if (customer.ratePerLiter > 0) customer.ratePerLiter else 60.0
                                customer.ratePerLiter > 0 -> customer.ratePerLiter
                                else -> 0.0
                            }
                            val liters = customer.defaultLiters
                            val bill = CalculationEngine.round2(liters * rate)

                            val now = Date()
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)

                            val delivery = DeliveryEntity(
                                id = deliveryId,
                                customerId = customer.id,
                                date = today,
                                deliveryTime = timeStr,
                                milkType = customer.milkType,
                                liters = liters,
                                rate = rate,
                                totalAmount = bill,
                                paidCash = existing?.paidCash ?: 0.0,
                                paidOnline = 0.0,
                                totalPaid = existing?.totalPaid ?: (existing?.paidCash ?: 0.0),
                                status = "delivered",
                                note = "Daily Delivery (via Quick Notification)",
                                isVoid = false
                            )
                            repo.saveDelivery(delivery)
                        }

                        // Advance to next pending customer in route
                        val nextPendingId = findNextPendingCustomerId(repo, today, customerId)
                        DailyTaskNotificationManager.updateNotification(context, nextPendingId)
                    }

                    ACTION_NAGA -> {
                        val customer = repo.getCustomerByIdDirect(customerId)
                        if (customer != null) {
                            val existing = repo.getDeliveryForCustomerAndDate(customerId, today)
                            val deliveryId = existing?.id ?: "d_${today}_${customerId}"

                            val delivery = DeliveryEntity(
                                id = deliveryId,
                                customerId = customer.id,
                                date = today,
                                deliveryTime = "",
                                milkType = customer.milkType,
                                liters = 0.0,
                                rate = customer.ratePerLiter,
                                totalAmount = 0.0,
                                paidCash = 0.0,
                                paidOnline = 0.0,
                                totalPaid = 0.0,
                                status = "skipped",
                                note = "Naga (via Quick Notification)",
                                isVoid = false
                            )
                            repo.saveDelivery(delivery)
                        }

                        // Advance to next pending customer in route
                        val nextPendingId = findNextPendingCustomerId(repo, today, customerId)
                        DailyTaskNotificationManager.updateNotification(context, nextPendingId)
                    }

                    ACTION_PREV, ACTION_NEXT -> {
                        DailyTaskNotificationManager.updateNotification(context, customerId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun findNextPendingCustomerId(
        repo: com.shahbuddindairy.app.data.repository.DairyRepository,
        today: String,
        currentId: Long
    ): Long? {
        val customers = repo.getAllActiveCustomersDirect()
        val deliveries = repo.getDeliveriesForDateDirect(today).associateBy { it.customerId }

        val currentIndex = customers.indexOfFirst { it.id == currentId }
        val searchOrder = if (currentIndex >= 0 && currentIndex < customers.size - 1) {
            customers.subList(currentIndex + 1, customers.size) + customers.subList(0, currentIndex)
        } else {
            customers
        }

        val nextPending = searchOrder.firstOrNull { c ->
            val d = deliveries[c.id]
            val isDone = d != null && !d.isVoid && d.status == "delivered"
            val isNaga = d != null && !d.isVoid && d.status == "skipped"
            val isOff = CalculationEngine.isOffDay(c, today)
            !isDone && !isNaga && !isOff && c.isActive == 1
        }

        return nextPending?.id ?: currentId
    }
}
