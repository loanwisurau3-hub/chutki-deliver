package com.example

import com.example.data.AdminSettings
import com.example.data.OrderRecord
import com.example.data.RiderEarningsCalculator
import com.example.data.StoreInfo
import com.example.data.TripGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiderEarningsCalculatorTest {

    private val gossaigaonStore = StoreInfo(
        id = "store_1",
        name = "Gossaigaon Bazar Mart",
        lat = 26.4524,
        lng = 89.9723
    )

    @Test
    fun `earning scales dynamically with store-to-customer distance and admin per-km rate`() {
        val order = OrderRecord(
            orderId = "ord_1",
            restaurantId = "store_1",
            customerLat = 26.4794, // ~3.0 km north of store
            customerLng = 89.9723,
            deliveryCharge = 25.0
        )

        val settingsAt10 = AdminSettings(riderPerKmRate = 10.0)
        val breakdown10 = RiderEarningsCalculator.calculateOrderEarning(order, gossaigaonStore, settingsAt10)
        assertTrue(breakdown10.isDistanceBased)
        assertFalse(breakdown10.isLocked)
        assertNotNull(breakdown10.distanceKm)
        assertEquals(3.0, breakdown10.distanceKm!!, 0.05)
        assertEquals(30.0, breakdown10.totalEarning, 0.5)

        // Admin increases per-km rate from ₹10 to ₹15 -> earning updates immediately
        val settingsAt15 = AdminSettings(riderPerKmRate = 15.0)
        val breakdown15 = RiderEarningsCalculator.calculateOrderEarning(order, gossaigaonStore, settingsAt15)
        assertTrue(breakdown15.isDistanceBased)
        assertEquals(45.0, breakdown15.totalEarning, 0.8)
    }

    @Test
    fun `explicit order distanceKm overrides GPS coordinates`() {
        val order = OrderRecord(
            orderId = "ord_2",
            restaurantId = "store_1",
            distanceKm = 4.5,
            customerLat = 26.4530,
            customerLng = 89.9723
        )
        val settings = AdminSettings(riderPerKmRate = 12.0)
        val breakdown = RiderEarningsCalculator.calculateOrderEarning(order, gossaigaonStore, settings)
        assertEquals(4.5, breakdown.distanceKm!!, 0.001)
        assertEquals(54.0, breakdown.totalEarning, 0.001)
    }

    @Test
    fun `optional admin base pay, included km, and minimum floor work accurately`() {
        val order = OrderRecord(orderId = "ord_3", distanceKm = 5.0)
        val settings = AdminSettings(
            riderPerKmRate = 10.0,
            riderDistanceBasePay = 20.0,
            riderBaseDistanceKm = 2.0,
            riderMinTripEarning = 25.0
        )
        // ₹20 base for first 2 km + (5 - 2) * ₹10 = ₹50
        val breakdown = RiderEarningsCalculator.calculateOrderEarning(order, null, settings)
        assertEquals(50.0, breakdown.totalEarning, 0.001)

        // Short 0.5 km trip enforces minimum floor ₹25
        val shortOrder = OrderRecord(orderId = "ord_4", distanceKm = 0.5)
        val shortBreakdown = RiderEarningsCalculator.calculateOrderEarning(shortOrder, null, settings)
        assertEquals(25.0, shortBreakdown.totalEarning, 0.001)
    }

    @Test
    fun `locked riderEarning on completed order is preserved when admin changes rate later`() {
        val deliveredOrder = OrderRecord(
            orderId = "ord_5",
            status = "DELIVERED",
            distanceKm = 3.5,
            riderPerKmRate = 10.0,
            riderEarning = 35.0
        )
        val newAdminSettings = AdminSettings(riderPerKmRate = 20.0)
        val breakdown = RiderEarningsCalculator.calculateOrderEarning(deliveredOrder, null, newAdminSettings)
        assertTrue(breakdown.isLocked)
        assertEquals(35.0, breakdown.totalEarning, 0.001)
        assertEquals(10.0, breakdown.perKmRate, 0.001)
    }

    @Test
    fun `missing GPS coordinates or zero per-km rate falls back to deliveryCharge or riderBaseFee`() {
        val noGpsOrder = OrderRecord(orderId = "ord_6", deliveryCharge = 30.0)
        val breakdown = RiderEarningsCalculator.calculateOrderEarning(noGpsOrder, null, AdminSettings(riderPerKmRate = 10.0))
        assertFalse(breakdown.isDistanceBased)
        assertNull(breakdown.distanceKm)
        assertEquals(30.0, breakdown.totalEarning, 0.001)

        val zeroRateOrder = OrderRecord(orderId = "ord_7", distanceKm = 4.0)
        val fallbackBreakdown = RiderEarningsCalculator.calculateOrderEarning(
            zeroRateOrder,
            null,
            AdminSettings(riderPerKmRate = 0.0, riderBaseFee = 25.0)
        )
        assertFalse(fallbackBreakdown.isDistanceBased)
        assertEquals(25.0, fallbackBreakdown.totalEarning, 0.001)
    }

    @Test
    fun `multi-store trip sums earnings and distance cleanly`() {
        val o1 = OrderRecord(orderId = "o1", distanceKm = 2.0, status = "DELIVERED")
        val o2 = OrderRecord(orderId = "o2", distanceKm = 3.5, status = "DELIVERED")
        val trip = TripGroup(
            tripId = "trip_1",
            customerUid = "c1",
            phone = "9876543210",
            address = "Station Road",
            customerLat = null,
            customerLng = null,
            locationLink = "",
            paymentMode = "COD",
            paymentStatus = "COD",
            emergencyAlert = false,
            emergencyDetails = "",
            assignedAt = 1000L,
            orders = listOf(o1, o2)
        )
        val summary = RiderEarningsCalculator.calculateTripEarning(
            trip = trip,
            stores = emptyMap(),
            settings = AdminSettings(riderPerKmRate = 10.0)
        )
        assertEquals(5.5, summary.totalDistanceKm!!, 0.001)
        assertEquals(55.0, summary.totalEarning, 0.001)
        assertEquals("5.5 km × ₹10/km", RiderEarningsCalculator.formatTripBreakdown(summary, AdminSettings(riderPerKmRate = 10.0)))
    }
}
