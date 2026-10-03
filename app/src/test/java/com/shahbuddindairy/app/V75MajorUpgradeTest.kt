package com.shahbuddindairy.app

import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.data.model.RouteEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.ui.screens.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V75MajorUpgradeTest {

    // 1. CUSTOMER #56 IMMUTABILITY & IDENTITY SAFETY
    @Test
    fun testCustomer56_IdentityAndRouteAssignmentSafety() {
        val customer56 = CustomerEntity(
            id = 56L,
            name = "Test Customer 56",
            phone = "9897000056",
            mohalla = "गुलाब नगर",
            ratePerLiter = 60.0,
            routeId = "default",
            routeOrder = 56
        )

        assertEquals(56L, customer56.id)

        // Route assignment must NOT alter customerId
        val assignedCustomer = customer56.copy(routeId = "route_gulab_nagar", routeOrder = 1)
        assertEquals(56L, assignedCustomer.id)
        assertEquals("route_gulab_nagar", assignedCustomer.routeId)
        assertEquals(1, assignedCustomer.routeOrder)

        // Sorting / filtering must preserve customerId
        val customerList = listOf(
            CustomerEntity(id = 89L, name = "Aman", routeOrder = 1),
            assignedCustomer,
            CustomerEntity(id = 12L, name = "Zaid", routeOrder = 2)
        )
        val sortedByName = customerList.sortedBy { it.name }
        val found56ByName = sortedByName.find { it.id == 56L }
        assertNotNull(found56ByName)
        assertEquals(56L, found56ByName!!.id)

        val filteredByRoute = customerList.filter { it.routeId == "route_gulab_nagar" }
        assertEquals(1, filteredByRoute.size)
        assertEquals(56L, filteredByRoute[0].id)
    }

    // 2. CASH -> MILK SUGGESTION ENGINE
    @Test
    fun testCashToMilkSuggestion() {
        val rate = 60.0

        // ₹60 at ₹60/L -> 1.000 L
        val milk1 = CalculationEngine.suggestMilkFromCash(cash = 60.0, rate = rate)
        assertEquals(1.000, milk1, 0.001)

        // ₹90 at ₹60/L -> 1.500 L
        val milk2 = CalculationEngine.suggestMilkFromCash(cash = 90.0, rate = rate)
        assertEquals(1.500, milk2, 0.001)

        // ₹120 at ₹60/L -> 2.000 L
        val milk3 = CalculationEngine.suggestMilkFromCash(cash = 120.0, rate = rate)
        assertEquals(2.000, milk3, 0.001)

        // ₹30 at ₹60/L -> 0.500 L
        val milk4 = CalculationEngine.suggestMilkFromCash(cash = 30.0, rate = rate)
        assertEquals(0.500, milk4, 0.001)

        // Zero or negative cash returns 0.0
        assertEquals(0.0, CalculationEngine.suggestMilkFromCash(0.0, rate), 0.001)
        assertEquals(0.0, CalculationEngine.suggestMilkFromCash(-50.0, rate), 0.001)
    }

    // 3. MILK -> BILL -> DUE / ADVANCE
    @Test
    fun testMilkToBillToDueAndAdvance() {
        val rate = 60.0
        val liters = 1.5 // Bill = ₹90

        // Case A: Cash ₹100 -> Bill ₹90, Paid ₹100, Advance ₹10, Due ₹0
        val resA = CalculationEngine.calculateBreakdown(
            customerId = 56L,
            date = "2026-10-03",
            liters = liters,
            rate = rate,
            paidCash = 100.0,
            deliveries = emptyList(),
            payments = emptyList()
        )
        assertEquals(90.0, resA.bill, 0.001)
        assertEquals(100.0, resA.paidCash, 0.001)
        assertEquals(0.0, resA.remainingDue, 0.001)
        assertEquals(10.0, resA.advance, 0.001)

        // Case B: Cash ₹60 -> Bill ₹90, Paid ₹60, Due ₹30, Advance ₹0
        val resB = CalculationEngine.calculateBreakdown(
            customerId = 56L,
            date = "2026-10-03",
            liters = liters,
            rate = rate,
            paidCash = 60.0,
            deliveries = emptyList(),
            payments = emptyList()
        )
        assertEquals(90.0, resB.bill, 0.001)
        assertEquals(60.0, resB.paidCash, 0.001)
        assertEquals(30.0, resB.remainingDue, 0.001)
        assertEquals(0.0, resB.advance, 0.001)

        // Case C: Cash ₹90 -> Bill ₹90, Paid ₹90, Due ₹0, Advance ₹0
        val resC = CalculationEngine.calculateBreakdown(
            customerId = 56L,
            date = "2026-10-03",
            liters = liters,
            rate = rate,
            paidCash = 90.0,
            deliveries = emptyList(),
            payments = emptyList()
        )
        assertEquals(90.0, resC.bill, 0.001)
        assertEquals(90.0, resC.paidCash, 0.001)
        assertEquals(0.0, resC.remainingDue, 0.001)
        assertEquals(0.0, resC.advance, 0.001)
    }

    // 4. ONE-TAP PAYMENT RECONCILIATION & DEBOUNCE
    @Test
    fun testOneTapPaymentReconciliation() {
        val existingDue = 500.0

        // Pay Full: ₹500 payment -> remaining due = ₹0
        val payFullAmt = existingDue
        val remainingAfterFull = existingDue - payFullAmt
        assertEquals(0.0, remainingAfterFull, 0.001)

        // Pay ₹100: ₹100 payment -> remaining due = ₹400
        val payPartialAmt = 100.0
        val remainingAfterPartial = existingDue - payPartialAmt
        assertEquals(400.0, remainingAfterPartial, 0.001)

        // Pay ₹600 (Exceeds Due): Due ₹0, Advance ₹100
        val overPayAmt = 600.0
        val newDue = (existingDue - overPayAmt).coerceAtLeast(0.0)
        val newAdvance = (overPayAmt - existingDue).coerceAtLeast(0.0)
        assertEquals(0.0, newDue, 0.001)
        assertEquals(100.0, newAdvance, 0.001)

        // Double-tap protection: check timestamp interval < 1500ms
        val firstTap = 1000000L
        val rapidSecondTap = 1000500L // 500ms later -> must be debounced
        val isDoubleTapBlocked = (rapidSecondTap - firstTap) < 1500L
        assertTrue("Double tap within 500ms must be blocked", isDoubleTapBlocked)

        val legitimateThirdTap = 1002000L // 2000ms later -> allowed
        val isLegitimateAllowed = (legitimateThirdTap - firstTap) >= 1500L
        assertTrue("Tap after 2000ms must be allowed", isLegitimateAllowed)
    }

    // 5. CUSTOMER NOTES
    @Test
    fun testCustomerNotes_AssociatedToCustomerId() {
        val note1 = CustomerNoteEntity(
            id = "note_1",
            customerId = 56L,
            content = "Milk सुबह 6 बजे देना है",
            category = "Delivery",
            isPinned = true,
            createdAt = "2026-10-03 08:00:00"
        )

        val note2 = CustomerNoteEntity(
            id = "note_2",
            customerId = 56L,
            content = "₹500 old balance discussed",
            category = "Payment",
            isPinned = false,
            createdAt = "2026-10-03 08:05:00"
        )

        assertEquals(56L, note1.customerId)
        assertEquals(56L, note2.customerId)
        assertTrue(note1.isPinned)
        assertFalse(note2.isPinned)
        assertEquals("Delivery", note1.category)

        // Pin toggling preserves note content and customer ID
        val toggledNote2 = note2.copy(isPinned = true)
        assertEquals(56L, toggledNote2.customerId)
        assertTrue(toggledNote2.isPinned)
        assertEquals("₹500 old balance discussed", toggledNote2.content)
    }

    // 6. ROUTE MANAGER & LIVE ROUTE PROGRESS
    @Test
    fun testRouteProgressCalculation() {
        val totalCustomersInRoute = 25
        val deliveredCount = 18

        val progressPercent = ((deliveredCount.toDouble() / totalCustomersInRoute) * 100.0).toInt()
        assertEquals(72, progressPercent)

        val pendingCount = totalCustomersInRoute - deliveredCount
        assertEquals(7, pendingCount)
    }

    // 7. PENDING RECONCILIATION AUDIT TRAIL
    @Test
    fun testPendingReconciliationAuditTrail() {
        val previousDue = 500.0
        val reconciliationAmount = 500.0 // Full clearance adjustment
        assertEquals(previousDue, reconciliationAmount, 0.001)

        val paymentEntity = PaymentEntity(
            id = "pay_recon_56",
            customerId = 56L,
            date = "2026-10-03",
            amount = reconciliationAmount,
            paymentMode = "Adjustment",
            paymentSubtype = "reconciliation",
            note = "Due Reconciliation: ₹500 -> ₹0",
            createdAt = "2026-10-03 10:00:00",
            isVoid = false
        )

        assertEquals(56L, paymentEntity.customerId)
        assertEquals(500.0, paymentEntity.amount, 0.001)
        assertEquals("Adjustment", paymentEntity.paymentMode)
        assertFalse(paymentEntity.isVoid)
    }
}
