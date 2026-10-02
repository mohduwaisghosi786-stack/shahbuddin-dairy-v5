package com.shahbuddindairy.app

import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalculationEngineTest {

    @Test
    fun testFinancialBreakdownWithRemainingDue() {
        // Today's Bill = ₹180, Previous Due = ₹500, Today's Cash = ₹300
        // Expected Total Required = ₹680, Remaining Due = ₹380, Advance = ₹0
        val deliveries = listOf(
            DeliveryEntity(
                id = "d_1",
                customerId = 1L,
                date = "2026-03-01",
                liters = 5.0,
                rate = 100.0,
                totalAmount = 500.0,
                paidCash = 0.0,
                totalPaid = 0.0,
                status = "delivered"
            )
        )
        val payments = emptyList<PaymentEntity>()

        val result = CalculationEngine.calculateBreakdown(
            customerId = 1L,
            date = "2026-03-02",
            liters = 3.0,
            rate = 60.0,
            paidCash = 300.0,
            deliveries = deliveries,
            payments = payments
        )

        assertEquals(180.0, result.bill, 0.001)
        assertEquals(500.0, result.prevDue, 0.001)
        assertEquals(680.0, result.totalRequired, 0.001)
        assertEquals(300.0, result.paidCash, 0.001)
        assertEquals(380.0, result.remainingDue, 0.001)
        assertEquals(0.0, result.advance, 0.001)
    }

    @Test
    fun testFinancialBreakdownWithAdvance() {
        // Today's Bill = ₹180, Previous Due = ₹0, Today's Cash = ₹200
        // Expected Total Required = ₹180, Remaining Due = ₹0, Advance = ₹20
        val deliveries = emptyList<DeliveryEntity>()
        val payments = emptyList<PaymentEntity>()

        val result = CalculationEngine.calculateBreakdown(
            customerId = 1L,
            date = "2026-03-02",
            liters = 3.0,
            rate = 60.0,
            paidCash = 200.0,
            deliveries = deliveries,
            payments = payments
        )

        assertEquals(180.0, result.bill, 0.001)
        assertEquals(0.0, result.prevDue, 0.001)
        assertEquals(180.0, result.totalRequired, 0.001)
        assertEquals(200.0, result.paidCash, 0.001)
        assertEquals(0.0, result.remainingDue, 0.001)
        assertEquals(20.0, result.advance, 0.001)
    }

    @Test
    fun testCustomerBalanceIgnoresVoidTransactions() {
        val deliveries = listOf(
            DeliveryEntity(
                id = "d_valid",
                customerId = 1L,
                date = "2026-03-01",
                liters = 2.0,
                rate = 70.0,
                totalAmount = 140.0,
                paidCash = 0.0,
                totalPaid = 0.0,
                status = "delivered",
                isVoid = false
            ),
            DeliveryEntity(
                id = "d_voided",
                customerId = 1L,
                date = "2026-03-02",
                liters = 5.0,
                rate = 70.0,
                totalAmount = 350.0,
                paidCash = 0.0,
                totalPaid = 0.0,
                status = "delivered",
                isVoid = true
            )
        )
        val payments = listOf(
            PaymentEntity(
                id = "p_valid",
                customerId = 1L,
                date = "2026-03-01",
                amount = 40.0,
                paymentMode = "Cash",
                isVoid = false
            ),
            PaymentEntity(
                id = "p_voided",
                customerId = 1L,
                date = "2026-03-02",
                amount = 100.0,
                paymentMode = "Cash",
                isVoid = true
            )
        )

        // Balance should be: 140.0 (valid bill) - 40.0 (valid payment) = 100.0 due
        val balance = CalculationEngine.calculateCustomerBalance(1L, deliveries, payments)
        assertEquals(100.0, balance, 0.001)
    }

    @Test
    fun testGetLastPayment() {
        val deliveries = listOf(
            DeliveryEntity(
                id = "d_1",
                customerId = 1L,
                date = "2026-02-15",
                liters = 1.0,
                rate = 60.0,
                totalAmount = 60.0,
                paidCash = 60.0,
                totalPaid = 60.0,
                status = "delivered"
            )
        )
        val payments = listOf(
            PaymentEntity(
                id = "p_1",
                customerId = 1L,
                date = "2026-02-28",
                amount = 500.0,
                paymentMode = "Cash"
            )
        )

        val lastPayment = CalculationEngine.getLastPayment(1L, deliveries, payments)
        assertNotNull(lastPayment)
        assertEquals("2026-02-28", lastPayment?.date)
        assertEquals(500.0, lastPayment?.amount ?: 0.0, 0.001)
        assertEquals("Cash", lastPayment?.mode)
    }

    @Test
    fun testAlternateDayLogic() {
        val customer = CustomerEntity(
            id = 1L,
            name = "Test Cust",
            deliveryFrequency = "alternate",
            frequencyAnchorDate = "2026-03-01"
        )

        // 2026-03-01 is anchor (diff 0 -> offDay = false)
        assertEquals(false, CalculationEngine.isOffDay(customer, "2026-03-01"))
        // 2026-03-02 is next day (diff 1 -> offDay = true)
        assertEquals(true, CalculationEngine.isOffDay(customer, "2026-03-02"))
        // 2026-03-03 is 2 days after (diff 2 -> offDay = false)
        assertEquals(false, CalculationEngine.isOffDay(customer, "2026-03-03"))
    }
}
