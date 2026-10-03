package com.shahbuddindairy.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.MainActivity
import com.shahbuddindairy.app.R
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

object DailyTaskNotificationManager {
    const val CHANNEL_ID = "daily_task_delivery_route"
    const val NOTIFICATION_ID = 6001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Task Delivery Route"
            val descriptionText = "Shows active customer and quick delivery actions during route"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(context: Context, targetCustomerId: Long? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DairyApplication
                val repo = app.repository

                val isEnabledStr = repo.getSetting("notif_enabled") ?: "true"
                if (isEnabledStr == "false") {
                    cancelNotification(context)
                    return@launch
                }

                if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    return@launch
                }

                createNotificationChannel(context)

                val today = CalculationEngine.todayStr()
                val customers = repo.getAllActiveCustomersDirect()
                if (customers.isEmpty()) {
                    cancelNotification(context)
                    return@launch
                }

                val todayDeliveries = repo.getDeliveriesForDateDirect(today).associateBy { it.customerId }
                val allDeliveries = repo.getAllDeliveriesDirect().groupBy { it.customerId }
                val allPayments = repo.getAllPaymentsDirect().groupBy { it.customerId }

                // Find active target customer
                val customerIndex = if (targetCustomerId != null) {
                    customers.indexOfFirst { it.id == targetCustomerId }.takeIf { it >= 0 } ?: 0
                } else {
                    // Find first pending customer in route
                    val firstPendingIdx = customers.indexOfFirst { c ->
                        val d = todayDeliveries[c.id]
                        val isDone = d != null && !d.isVoid && d.status == "delivered"
                        val isNaga = d != null && !d.isVoid && d.status == "skipped"
                        val isOff = CalculationEngine.isOffDay(c, today)
                        !isDone && !isNaga && !isOff && c.isActive == 1
                    }
                    if (firstPendingIdx >= 0) firstPendingIdx else 0
                }

                val currentCustomer = customers.getOrNull(customerIndex)
                if (currentCustomer == null) {
                    cancelNotification(context)
                    return@launch
                }

                // Check completed count vs total
                var completedCount = 0
                var pendingCount = 0
                for (c in customers) {
                    val d = todayDeliveries[c.id]
                    if (d != null && !d.isVoid && d.status == "delivered") completedCount++
                    else if (!CalculationEngine.isOffDay(c, today) && c.isActive == 1) pendingCount++
                }

                val privacyMode = repo.getSetting("notif_privacy") ?: "full" // "full" or "private"

                val rate = when {
                    currentCustomer.billingType == "daily_cash" -> if (currentCustomer.ratePerLiter > 0) currentCustomer.ratePerLiter else 60.0
                    currentCustomer.ratePerLiter > 0 -> currentCustomer.ratePerLiter
                    else -> 0.0
                }
                val bill = CalculationEngine.round2(currentCustomer.defaultLiters * rate)

                val custDeliveries = allDeliveries[currentCustomer.id] ?: emptyList()
                val custPayments = allPayments[currentCustomer.id] ?: emptyList()
                val prevDue = CalculationEngine.calculatePrevDueBefore(currentCustomer.id, today, custDeliveries, custPayments)
                val currentBal = CalculationEngine.calculateCustomerBalance(currentCustomer.id, custDeliveries, custPayments)

                // Intent to open app
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Navigation Prev & Next customer IDs
                val prevCustomerId = if (customerIndex > 0) customers[customerIndex - 1].id else currentCustomer.id
                val nextCustomerId = if (customerIndex < customers.size - 1) customers[customerIndex + 1].id else currentCustomer.id

                // Unique request code base using customer ID
                val baseCode = (currentCustomer.id % 10000).toInt() * 10

                // Action Deliver
                val deliverIntent = Intent(context, DailyTaskNotificationReceiver::class.java).apply {
                    action = DailyTaskNotificationReceiver.ACTION_DELIVER
                    data = Uri.parse("dairy://customer/${currentCustomer.id}/deliver")
                    putExtra(DailyTaskNotificationReceiver.EXTRA_CUSTOMER_ID, currentCustomer.id)
                }
                val deliverPendingIntent = PendingIntent.getBroadcast(
                    context, baseCode + 1, deliverIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Action Naga
                val nagaIntent = Intent(context, DailyTaskNotificationReceiver::class.java).apply {
                    action = DailyTaskNotificationReceiver.ACTION_NAGA
                    data = Uri.parse("dairy://customer/${currentCustomer.id}/naga")
                    putExtra(DailyTaskNotificationReceiver.EXTRA_CUSTOMER_ID, currentCustomer.id)
                }
                val nagaPendingIntent = PendingIntent.getBroadcast(
                    context, baseCode + 2, nagaIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Action Prev
                val prevIntent = Intent(context, DailyTaskNotificationReceiver::class.java).apply {
                    action = DailyTaskNotificationReceiver.ACTION_PREV
                    data = Uri.parse("dairy://customer/$prevCustomerId/prev")
                    putExtra(DailyTaskNotificationReceiver.EXTRA_CUSTOMER_ID, prevCustomerId)
                }
                val prevPendingIntent = PendingIntent.getBroadcast(
                    context, baseCode + 3, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Action Next
                val nextIntent = Intent(context, DailyTaskNotificationReceiver::class.java).apply {
                    action = DailyTaskNotificationReceiver.ACTION_NEXT
                    data = Uri.parse("dairy://customer/$nextCustomerId/next")
                    putExtra(DailyTaskNotificationReceiver.EXTRA_CUSTOMER_ID, nextCustomerId)
                }
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context, baseCode + 4, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val title = "Route #${customerIndex + 1}: ${currentCustomer.name} (ID: ${currentCustomer.id})"
                val subText = "${currentCustomer.mohalla} • Route ${customerIndex + 1}/${customers.size} • Done: $completedCount"


                val contentText = if (privacyMode == "private") {
                    "%.2f L ${currentCustomer.milkType} • Tap to deliver".format(Locale.US, currentCustomer.defaultLiters)
                } else {
                    "Milk: %.2fL @ ₹%.0f | Bill: ₹%,d | Due: ₹%,d".format(
                        Locale.US,
                        currentCustomer.defaultLiters,
                        rate,
                        bill.toLong(),
                        (if (currentBal > 0) currentBal else prevDue).toLong()
                    )
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(contentText)
                    .setSubText(subText)
                    .setContentIntent(openPendingIntent)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(
                        if (privacyMode == "private") NotificationCompat.VISIBILITY_PRIVATE
                        else NotificationCompat.VISIBILITY_PUBLIC
                    )
                    .addAction(R.mipmap.ic_launcher, "◀ Prev", prevPendingIntent)
                    .addAction(R.mipmap.ic_launcher, "✓ Deliver", deliverPendingIntent)
                    .addAction(R.mipmap.ic_launcher, "Naga", nagaPendingIntent)
                    .addAction(R.mipmap.ic_launcher, "Next ▶", nextPendingIntent)

                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
