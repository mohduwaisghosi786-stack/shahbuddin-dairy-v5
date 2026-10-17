package com.shahbuddindairy.app.ai

import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DemandForecast(
    val totalLiters: Double,
    val buffaloLiters: Double,
    val cowLiters: Double,
    val totalScheduledCustomers: Int,
    val alternateScheduledCount: Int,
    val alternateSkippedCount: Int,
    val explanation: String
)

data class CustomerPaymentRisk(
    val customerId: Long,
    val customerName: String,
    val totalDue: Double,
    val consecutiveUnpaidDays: Int,
    val isHighRisk: Boolean,
    val alertMessage: String
)

data class DayHistoryPoint(
    val date: String,
    val dayLabel: String,
    val liters: Double,
    val isNaga: Boolean,
    val hasRecord: Boolean
)

/**
 * 100% Offline, Deterministic Local AI Analytics & Prediction Suite.
 * Runs instantly on device with zero cloud API or external latency.
 */
object DairyAiPredictor {

    /**
     * Forecasts tomorrow's milk requirements based on customer schedules, alternate-day cycles, and recent patterns.
     */
    fun forecastTomorrowDemand(
        customers: List<CustomerEntity>,
        recentDeliveries: List<DeliveryEntity>,
        targetDate: String // YYYY-MM-DD
    ): DemandForecast {
        var totalBuff = 0.0
        var totalCow = 0.0
        var scheduledCusts = 0
        var altScheduled = 0
        var altSkipped = 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        try {
            val d = dateFormat.parse(targetDate)
            if (d != null) cal.time = d
        } catch (_: Exception) {}

        for (cust in customers.filter { it.isActive == 1 && it.isArchived == 0 }) {
            val isScheduled = isCustomerScheduledOnDate(cust, targetDate)
            if (isScheduled) {
                scheduledCusts++
                val liters = if (cust.defaultLiters > 0) cust.defaultLiters else 1.0
                if (cust.milkType.contains("गाय", ignoreCase = true) || cust.milkType.contains("cow", ignoreCase = true)) {
                    totalCow += liters
                } else {
                    totalBuff += liters
                }
                if (cust.deliveryFrequency == "alternate") altScheduled++
            } else {
                if (cust.deliveryFrequency == "alternate") altSkipped++
            }
        }

        val total = totalBuff + totalCow
        val explanation = "Kal ka estimated demand: %.1fL (Buffalo: %.1fL • Cow: %.1fL). %d customers scheduled (%d alternate on off-cycle)."
            .format(Locale.ENGLISH, total, totalBuff, totalCow, scheduledCusts, altSkipped)

        return DemandForecast(
            totalLiters = total,
            buffaloLiters = totalBuff,
            cowLiters = totalCow,
            totalScheduledCustomers = scheduledCusts,
            alternateScheduledCount = altScheduled,
            alternateSkippedCount = altSkipped,
            explanation = explanation
        )
    }

    /**
     * Checks if a customer is scheduled for delivery on the given date based on their frequency and anchor.
     */
    fun isCustomerScheduledOnDate(customer: CustomerEntity, dateStr: String): Boolean {
        if (customer.deliveryFrequency != "alternate") return true
        if (customer.frequencyAnchorDate.isBlank()) return true

        return try {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val anchor = df.parse(customer.frequencyAnchorDate) ?: return true
            val target = df.parse(dateStr) ?: return true
            val diffMs = target.time - anchor.time
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            // Even day difference means scheduled on same alternate cycle
            (diffDays % 2) == 0
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Evaluates payment risk and creates soft collection reminders.
     */
    fun evaluatePaymentRisk(
        customer: CustomerEntity,
        currentDue: Double,
        recentDeliveries: List<DeliveryEntity>
    ): CustomerPaymentRisk? {
        if (currentDue <= 0.0) return null

        val custDeliveries = recentDeliveries
            .filter { it.customerId == customer.id && !it.isVoid && it.status == "delivered" }
            .sortedByDescending { it.date }

        var consecutiveUnpaidDays = 0
        for (deliv in custDeliveries) {
            if (deliv.totalPaid == 0.0) {
                consecutiveUnpaidDays++
            } else {
                break
            }
        }

        val isHighRisk = currentDue >= 1000.0 || consecutiveUnpaidDays >= 4
        if (currentDue < 300.0 && consecutiveUnpaidDays < 3) return null

        val alertMsg = when {
            isHighRisk -> "⚠️ Udhari Alert: ₹%.0f (%d din se unpaid)".format(Locale.ENGLISH, currentDue, consecutiveUnpaidDays)
            else -> "Baaki: ₹%.0f".format(Locale.ENGLISH, currentDue)
        }

        return CustomerPaymentRisk(
            customerId = customer.id,
            customerName = customer.name,
            totalDue = currentDue,
            consecutiveUnpaidDays = consecutiveUnpaidDays,
            isHighRisk = isHighRisk,
            alertMessage = alertMsg
        )
    }

    /**
     * Generates a 7-day delivery consumption history for the customer card mini-graph.
     */
    fun get7DayHistory(
        customerId: Long,
        endDateStr: String,
        allDeliveries: List<DeliveryEntity>
    ): List<DayHistoryPoint> {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayLabelDf = SimpleDateFormat("EEE", Locale.US)
        val list = mutableListOf<DayHistoryPoint>()

        val cal = Calendar.getInstance()
        try {
            val d = df.parse(endDateStr)
            if (d != null) cal.time = d
        } catch (_: Exception) {}

        val delivMap = allDeliveries
            .filter { it.customerId == customerId && !it.isVoid }
            .associateBy { it.date }

        // Last 7 days ending at endDateStr
        for (i in 6 downTo 0) {
            val targetCal = cal.clone() as Calendar
            targetCal.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = df.format(targetCal.time)
            val label = dayLabelDf.format(targetCal.time).take(2).uppercase(Locale.US)

            val deliv = delivMap[dStr]
            if (deliv != null) {
                list.add(
                    DayHistoryPoint(
                        date = dStr,
                        dayLabel = label,
                        liters = deliv.liters,
                        isNaga = deliv.status == "skipped",
                        hasRecord = true
                    )
                )
            } else {
                list.add(
                    DayHistoryPoint(
                        date = dStr,
                        dayLabel = label,
                        liters = 0.0,
                        isNaga = false,
                        hasRecord = false
                    )
                )
            }
        }
        return list
    }

    /**
     * Generates a polite, respectful WhatsApp reminder message string.
     */
    fun generateSoftReminderText(customerName: String, dueAmount: Double): String {
        return "Assalamualaikum ${customerName} ji, Shahbuddin Dairy se aapke doodh ka kul ₹%.0f ka hisaab baaki hai. Jab aapko suvidha ho, kripya online/nakad jama karwa dein. Shukriya! — Shahbuddin Dairy"
            .format(Locale.ENGLISH, dueAmount)
    }
}
