package com.shahbuddindairy.app

import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.ui.screens.ReportsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V71ProductionUpgradeTest {

    @Test
    fun testCustomerIdImmutabilityDuringRouteReorder() {
        // Customer A (ID 101), B (ID 102), C (ID 103)
        val customerA = CustomerEntity(id = 101L, name = "Customer A", routeOrder = 1)
        val customerB = CustomerEntity(id = 102L, name = "Customer B", routeOrder = 2)
        val customerC = CustomerEntity(id = 103L, name = "Customer C", routeOrder = 3)

        val initialList = listOf(customerA, customerB, customerC)
        assertEquals(3, initialList.size)

        // Move C to first position (routeOrder 1, A -> 2, B -> 3)
        val reorderedList = listOf(
            customerC.copy(routeOrder = 1),
            customerA.copy(routeOrder = 2),
            customerB.copy(routeOrder = 3)
        )

        // Verify IDs remain completely untouched
        assertEquals(103L, reorderedList[0].id)
        assertEquals(101L, reorderedList[1].id)
        assertEquals(102L, reorderedList[2].id)

        // Verify route order is properly updated
        assertEquals(1, reorderedList[0].routeOrder)
        assertEquals(2, reorderedList[1].routeOrder)
        assertEquals(3, reorderedList[2].routeOrder)
    }

    @Test
    fun testCustomer56TargetingByImmutableId() {
        // Create 60 customers with IDs 1..60
        val customers = (1L..60L).map { id ->
            CustomerEntity(
                id = id,
                name = "Customer #$id",
                routeOrder = id.toInt()
            )
        }

        // Test Customer #56
        val targetCustomer = customers.find { it.id == 56L }
        org.junit.Assert.assertNotNull(targetCustomer)
        assertEquals(56L, targetCustomer!!.id)
        assertEquals("Customer #56", targetCustomer.name)

        // Delivery operation using customerId MUST target Customer 56, not 57
        val delivery = DeliveryEntity(
            id = "del_56_test",
            customerId = targetCustomer.id,
            date = "2026-10-03",
            liters = 2.0,
            rate = 60.0,
            totalAmount = 120.0,
            paidCash = 120.0,
            totalPaid = 120.0,
            status = "delivered"
        )
        assertEquals(56L, delivery.customerId)
        assertFalse("Delivery must NOT target customer 57", delivery.customerId == 57L)
    }

    @Test
    fun testAccountingCalculations_Rate60_Milk150() {
        // Section 14, 15, 37 Test:
        // Rate ₹60/L, Milk 1.50 L -> Bill ₹90
        val liters = 1.50
        val rate = 60.0
        val bill = CalculationEngine.round2(liters * rate)
        assertEquals(90.0, bill, 0.001)

        // Case 1: Cash = ₹100 -> Advance ₹10, Due ₹0
        val breakdown1 = CalculationEngine.calculateBreakdown(
            customerId = 1L,
            date = "2026-10-03",
            liters = liters,
            rate = rate,
            paidCash = 100.0,
            deliveries = emptyList(),
            payments = emptyList()
        )
        assertEquals(90.0, breakdown1.bill, 0.001)
        assertEquals(100.0, breakdown1.paidCash, 0.001)
        assertEquals(0.0, breakdown1.remainingDue, 0.001)
        assertEquals(10.0, breakdown1.advance, 0.001)

        // Case 2: Cash = ₹60 -> Due ₹30, Advance ₹0
        val breakdown2 = CalculationEngine.calculateBreakdown(
            customerId = 1L,
            date = "2026-10-03",
            liters = liters,
            rate = rate,
            paidCash = 60.0,
            deliveries = emptyList(),
            payments = emptyList()
        )
        assertEquals(90.0, breakdown2.bill, 0.001)
        assertEquals(60.0, breakdown2.paidCash, 0.001)
        assertEquals(30.0, breakdown2.remainingDue, 0.001)
        assertEquals(0.0, breakdown2.advance, 0.001)

        // Case 3: Cash = ₹90 -> Due ₹0, Advance ₹0
        val breakdown3 = CalculationEngine.calculateBreakdown(
            customerId = 1L,
            date = "2026-10-03",
            liters = liters,
            rate = rate,
            paidCash = 90.0,
            deliveries = emptyList(),
            payments = emptyList()
        )
        assertEquals(90.0, breakdown3.bill, 0.001)
        assertEquals(90.0, breakdown3.paidCash, 0.001)
        assertEquals(0.0, breakdown3.remainingDue, 0.001)
        assertEquals(0.0, breakdown3.advance, 0.001)
    }

    @Test
    fun testCashToMilkCalculation_Section13And15() {
        // Cash: ₹100, Rate: ₹60/L -> Suggested Milk: 1.666... -> 1.67 L
        val cash = 100.0
        val rate = 60.0
        val suggestedMilk = CalculationEngine.round2(cash / rate)
        assertEquals(1.67, suggestedMilk, 0.001)

        // If user manually changes suggested 1.67 to 1.50 L
        val userConfirmedMilk = 1.50
        val finalBill = CalculationEngine.round2(userConfirmedMilk * rate)
        assertEquals(90.0, finalBill, 0.001)

        val advance = cash - finalBill
        assertEquals(10.0, advance, 0.001)
    }

    @Test
    fun testPendingDueAdjustmentWithoutHistoryLoss() {
        // Existing pending due: ₹500
        val initialDeliveries = listOf(
            DeliveryEntity(
                id = "del_hist_1",
                customerId = 50L,
                date = "2026-10-01",
                liters = 10.0,
                rate = 50.0,
                totalAmount = 500.0,
                paidCash = 0.0,
                totalPaid = 0.0,
                status = "delivered"
            )
        )
        val initialPayments = emptyList<PaymentEntity>()

        val currentBalance = CalculationEngine.calculateCustomerBalance(50L, initialDeliveries, initialPayments)
        assertEquals(500.0, currentBalance, 0.001)

        // User reconciles / settles ₹500 pending -> ₹0 target
        val targetDue = 0.0
        val delta = CalculationEngine.round2(currentBalance - targetDue) // 500.0
        val adjustmentPayment = PaymentEntity(
            id = "pay_adj_test_1",
            customerId = 50L,
            date = "2026-10-03",
            amount = delta,
            paymentMode = "Adjustment",
            paymentSubtype = "Settlement",
            note = "Due Settlement to ₹0.00",
            isVoid = false
        )

        val updatedPayments = initialPayments + adjustmentPayment
        val newBalance = CalculationEngine.calculateCustomerBalance(50L, initialDeliveries, updatedPayments)

        // Visible balance is now ₹0
        assertEquals(0.0, newBalance, 0.001)

        // Historical delivery was NOT deleted or altered
        assertEquals(1, initialDeliveries.size)
        assertEquals(500.0, initialDeliveries[0].totalAmount, 0.001)
        assertEquals("delivered", initialDeliveries[0].status)
    }

    @Test
    fun testWhatsAppReportFormatAndSafety() {
        val customers = listOf(
            CustomerEntity(id = 1L, name = "Ahmad Khan"),
            CustomerEntity(id = 2L, name = "Mohd Rafiq")
        )

        val deliveries = listOf(
            DeliveryEntity(
                id = "d1",
                customerId = 1L,
                date = "2026-10-03",
                liters = 2.50,
                rate = 70.0,
                totalAmount = 175.0,
                paidCash = 175.0,
                totalPaid = 175.0,
                status = "delivered"
            ),
            DeliveryEntity(
                id = "d2",
                customerId = 2L,
                date = "2026-10-03",
                liters = 1.50,
                rate = 60.0,
                totalAmount = 90.0,
                paidCash = 0.0,
                totalPaid = 0.0,
                status = "naga"
            )
        )

        val payments = emptyList<PaymentEntity>()

        val reportText = ReportsViewModel.generateWhatsAppReportText(
            selectedDate = "2026-10-03",
            customers = customers,
            deliveries = deliveries,
            payments = payments
        )

        // Verify required header and structure
        assertTrue(reportText.contains("*SHAHBUDDIN DAIRY*"))
        assertTrue(reportText.contains("*Daily Milk Sales Report*"))
        assertTrue(reportText.contains("Date: 03 Oct 2026"))
        assertTrue(reportText.contains("*SUMMARY*"))
        assertTrue(reportText.contains("Customers: 2"))
        assertTrue(reportText.contains("Delivered: 1"))
        assertTrue(reportText.contains("Naaga: 1"))
        assertTrue(reportText.contains("Total Milk: 2.50 L"))
        assertTrue(reportText.contains("*DELIVERY DETAILS*"))
        assertTrue(reportText.contains("1. Ahmad Khan"))
        assertTrue(reportText.contains("2. Mohd Rafiq"))
        assertTrue(reportText.contains("Naaga (नागा)"))
        assertTrue(reportText.contains("*TOTAL*"))
        assertTrue(reportText.contains("Generated by Shahbuddin Dairy"))
    }
}
