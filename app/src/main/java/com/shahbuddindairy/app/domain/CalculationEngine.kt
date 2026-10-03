package com.shahbuddindairy.app.domain

import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object CalculationEngine {

    fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0

    fun suggestMilkFromCash(cash: Double, rate: Double): Double {
        if (cash <= 0.0 || rate <= 0.0) return 0.0
        return round2(cash / rate)
    }

    fun todayStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun prevDayStr(currentDate: String, daysAgo: Int = 1): String {
        return shiftDay(currentDate, -daysAgo)
    }

    fun nextDayStr(currentDate: String): String {
        return shiftDay(currentDate, 1)
    }

    private fun shiftDay(dateStr: String, deltaDays: Int): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val parsed = sdf.parse(dateStr)
            if (parsed != null) {
                cal.time = parsed
                cal.add(Calendar.DAY_OF_YEAR, deltaDays)
                sdf.format(cal.time)
            } else {
                todayStr()
            }
        } catch (e: Exception) {
            todayStr()
        }
    }

    fun formatDateDisplay(dateStr: String): String {
        return try {
            val parts = dateStr.take(10).split("-")
            if (parts.size == 3) {
                val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val mIdx = parts[1].toIntOrNull()?.minus(1)?.coerceIn(0, 11) ?: 0
                "${parts[2]} ${months[mIdx]} ${parts[0]}"
            } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatMoney(v: Double): String {
        val r = round2(v)
        return if (r % 1.0 == 0.0) {
            "₹%,d".format(Locale.ENGLISH, r.toLong())
        } else {
            "₹%,.2f".format(Locale.ENGLISH, r)
        }
    }

    fun formatLiters(v: Double): String {
        return "%.2f L".format(Locale.ENGLISH, round2(v))
    }

    fun isOffDay(customer: CustomerEntity, dateStr: String): Boolean {
        if (customer.deliveryFrequency != "alternate") return false
        val anchor = customer.frequencyAnchorDate.ifBlank { customer.createdAt.take(10) }.ifBlank { todayStr() }
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d1 = sdf.parse(anchor)?.time ?: return false
            val d2 = sdf.parse(dateStr)?.time ?: return false
            val diffDays = ((d2 - d1) / (1000 * 60 * 60 * 24)).toInt()
            (diffDays % 2 != 0)
        } catch (e: Exception) {
            false
        }
    }

    fun calculateCustomerBalance(
        customerId: Long,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>
    ): Double {
        var totalBill = 0.0
        var totalPaid = 0.0

        for (d in deliveries) {
            if (d.customerId != customerId || d.isVoid) continue
            if (d.status == "delivered") {
                totalBill += d.totalAmount
            }
            totalPaid += d.totalPaid
        }

        for (p in payments) {
            if (p.customerId != customerId || p.isVoid) continue
            totalPaid += p.amount
        }

        return round2(totalBill - totalPaid)
    }

    fun calculatePrevDueBefore(
        customerId: Long,
        date: String,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>
    ): Double {
        var totalBill = 0.0
        var totalPaid = 0.0

        for (d in deliveries) {
            if (d.customerId != customerId || d.isVoid || d.date >= date) continue
            if (d.status == "delivered") {
                totalBill += d.totalAmount
            }
            totalPaid += d.totalPaid
        }

        for (p in payments) {
            if (p.customerId != customerId || p.isVoid || p.date >= date) continue
            totalPaid += p.amount
        }

        return round2(totalBill - totalPaid)
    }

    data class FinancialBreakdown(
        val bill: Double,
        val prevDue: Double,
        val totalRequired: Double,
        val paidCash: Double,
        val remainingDue: Double,
        val advance: Double
    )

    fun calculateBreakdown(
        customerId: Long,
        date: String,
        liters: Double,
        rate: Double,
        paidCash: Double,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>
    ): FinancialBreakdown {
        val bill = round2(liters * rate)
        val prevDue = calculatePrevDueBefore(customerId, date, deliveries, payments)
        val totalReq = round2(bill + prevDue)
        val net = round2(totalReq - paidCash)
        val remDue = if (net > 0) net else 0.0
        val adv = if (net < 0) round2(-net) else 0.0

        return FinancialBreakdown(
            bill = bill,
            prevDue = prevDue,
            totalRequired = totalReq,
            paidCash = paidCash,
            remainingDue = remDue,
            advance = adv
        )
    }

    data class LastPaymentInfo(
        val date: String,
        val amount: Double,
        val mode: String
    )

    fun getLastPayment(
        customerId: Long,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>
    ): LastPaymentInfo? {
        val candidates = mutableListOf<LastPaymentInfo>()

        for (p in payments) {
            if (p.customerId == customerId && !p.isVoid && p.amount > 0) {
                candidates.add(LastPaymentInfo(p.date, p.amount, p.paymentMode))
            }
        }

        for (d in deliveries) {
            if (d.customerId == customerId && !d.isVoid && d.totalPaid > 0) {
                candidates.add(LastPaymentInfo(d.date, d.totalPaid, if (d.paidOnline > 0) "Online" else "Cash"))
            }
        }

        // Sort descending by date
        return candidates.maxByOrNull { it.date }
    }
}
