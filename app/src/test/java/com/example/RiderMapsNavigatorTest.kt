package com.example

import com.example.location.RiderMapsNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiderMapsNavigatorTest {

    @Test
    fun `buildMapsLaunchPlan automatically includes rider exact origin coordinates when within drivable range`() {
        val plan = RiderMapsNavigator.buildMapsLaunchPlan(
            originLat = 26.452412,
            originLng = 89.972345,
            destLat = 26.461234,
            destLng = 89.981111,
            destLabelFallback = "Gossaigaon Bazar"
        )
        assertTrue(plan.usedExactOrigin)
        assertFalse(plan.remoteOriginSkipped)
        assertNull(plan.nativeNavigationUri)
        assertTrue(plan.url.startsWith("https://www.google.com/maps/dir/?api=1"))
        assertTrue(plan.url.contains("&origin=26.452412,89.972345"))
        assertTrue(plan.url.contains("&destination=26.461234,89.981111"))
        assertTrue(plan.url.contains("&travelmode=driving&dir_action=navigate"))
    }

    @Test
    fun `buildMapsLaunchPlan uses local saved RTDB fallback origin when emulator GPS is cross-continent`() {
        val plan = RiderMapsNavigator.buildMapsLaunchPlan(
            originLat = 37.422093,
            originLng = -122.083922,
            destLat = 26.455639,
            destLng = 89.971000,
            destLabelFallback = "Nur Mini Mart, Gossaigaon",
            fallbackOriginLat = 26.452400,
            fallbackOriginLng = 89.972300
        )
        assertTrue(plan.usedExactOrigin)
        assertFalse(plan.remoteOriginSkipped)
        assertTrue(plan.url.contains("&origin=26.452400,89.972300"))
        assertTrue(plan.url.contains("&destination=26.455639,89.971000"))
        assertFalse(plan.url.contains("37.422093"))
    }

    @Test
    fun `buildMapsLaunchPlan avoids cross-continent routing failure when no local fallback exists`() {
        val plan = RiderMapsNavigator.buildMapsLaunchPlan(
            originLat = 37.422093,
            originLng = -122.083922,
            destLat = 26.452400,
            destLng = 89.972300,
            destLabelFallback = "Nur Mini Mart, Gossaigaon"
        )
        assertFalse(plan.usedExactOrigin)
        assertTrue(plan.remoteOriginSkipped)
        assertNull(plan.nativeNavigationUri)
        assertTrue(plan.url.startsWith("https://www.google.com/maps/search/?api=1&query=26.452400,89.972300"))
        assertFalse(plan.url.contains("37.422093"))
    }

    @Test
    fun `buildMapsLaunchPlan never leaves Choose start location blank when GPS is missing`() {
        val plan = RiderMapsNavigator.buildMapsLaunchPlan(
            originLat = null,
            originLng = null,
            destLat = 26.455639,
            destLng = 89.971000,
            destLabelFallback = "Customer Address"
        )
        assertFalse(plan.usedExactOrigin)
        assertFalse(plan.remoteOriginSkipped)
        assertNotNull(plan.nativeNavigationUri)
        assertEquals("google.navigation:q=26.455639,89.971000&mode=d", plan.nativeNavigationUri)
        assertTrue(plan.url.contains("&origin=My+Location"))
        assertTrue(plan.url.contains("&destination=26.455639,89.971000"))
    }

    @Test
    fun `buildMapsLaunchPlan handles invalid zero coordinates with native navigation auto-location`() {
        val plan = RiderMapsNavigator.buildMapsLaunchPlan(
            originLat = 0.0,
            originLng = 0.0,
            destLat = 26.4612,
            destLng = 89.9811,
            destLabelFallback = "Store"
        )
        assertEquals("google.navigation:q=26.461200,89.981100&mode=d", plan.nativeNavigationUri)
        assertTrue(plan.url.contains("&origin=My+Location"))
        assertTrue(plan.url.contains("&destination=26.461200,89.981100"))
    }

    @Test
    fun `buildDirectionsUrl extracts coordinates from customer locationLink and attaches rider origin`() {
        val url = RiderMapsNavigator.buildDirectionsUrl(
            originLat = 26.4501,
            originLng = 89.9702,
            destLat = null,
            destLng = null,
            destLabelFallback = "Station Road",
            locationLink = "https://maps.google.com/?q=26.458812,89.975421"
        )
        assertTrue(url.contains("&origin=26.450100,89.970200"))
        assertTrue(url.contains("&destination=26.458812,89.975421"))
    }

    @Test
    fun `buildDirectionsUrl qualifies local Gossaigaon address while preserving rider exact origin`() {
        val url = RiderMapsNavigator.buildDirectionsUrl(
            originLat = 26.4524,
            originLng = 89.9723,
            destLat = null,
            destLng = null,
            destLabelFallback = "Nur Mini Mart"
        )
        assertTrue(url.contains("&origin=26.452400,89.972300"))
        assertTrue(url.contains("Nur%20Mini%20Mart%2C%20Gossaigaon%2C%20Assam%2C%20India"))
    }

    @Test
    fun `extractLatLngFromLocationLink parses standard Google Maps and geo URIs`() {
        val qCoords = RiderMapsNavigator.extractLatLngFromLocationLink("https://maps.google.com/?q=26.4524,89.9723")
        assertEquals(26.4524 to 89.9723, qCoords)

        val atCoords = RiderMapsNavigator.extractLatLngFromLocationLink("https://www.google.com/maps/@26.4581,89.9799,17z")
        assertEquals(26.4581 to 89.9799, atCoords)

        val geoCoords = RiderMapsNavigator.extractLatLngFromLocationLink("geo:26.4600,89.9800?q=Customer")
        assertEquals(26.46 to 89.98, geoCoords)

        assertNull(RiderMapsNavigator.extractLatLngFromLocationLink("https://maps.app.goo.gl/abc123"))
    }
}
