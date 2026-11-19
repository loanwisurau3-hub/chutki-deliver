package com.example

import com.example.data.DailyEarningPoint
import com.example.ui.components.EarningsChartScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EarningsChartScaleTest {

    @Test
    fun `computeNiceAxisMax produces clean D3-style rounded ceilings`() {
        assertEquals(100.0, EarningsChartScale.computeNiceAxisMax(0.0), 0.01)
        assertEquals(60.0, EarningsChartScale.computeNiceAxisMax(45.0), 0.01)
        assertEquals(150.0, EarningsChartScale.computeNiceAxisMax(120.0), 0.01)
        assertEquals(500.0, EarningsChartScale.computeNiceAxisMax(430.0), 0.01)
        assertEquals(1500.0, EarningsChartScale.computeNiceAxisMax(1150.0), 0.01)
    }

    @Test
    fun `computeYAxisTicks returns top mid and zero ticks`() {
        val ticks = EarningsChartScale.computeYAxisTicks(500.0)
        assertEquals(listOf(500.0, 250.0, 0.0), ticks)
    }

    @Test
    fun `computeMonotoneTangents flattens zero plateaus and local extrema without overshoot`() {
        // Simulated Y pixel coordinates: flat zero plateau, spike, then zero plateau
        val ys = listOf(100f, 100f, 20f, 100f, 100f)
        val tangents = EarningsChartScale.computeMonotoneTangents(ys)
        assertEquals(5, tangents.size)
        // Flat segments must have 0 slope (prevents spline from dipping below baseline)
        assertEquals(0f, tangents[0], 0.001f)
        assertEquals(0f, tangents[1], 0.001f)
        assertEquals(0f, tangents[2], 0.001f)
        assertEquals(0f, tangents[3], 0.001f)
        assertEquals(0f, tangents[4], 0.001f)
    }

    @Test
    fun `computeTrendSummary aggregates 7-day totals average peak day and delta accurately`() {
        val series = listOf(
            DailyEarningPoint(1L, "Mon", 120.0, 3, 12.0),
            DailyEarningPoint(2L, "Tue", 0.0, 0, 0.0),
            DailyEarningPoint(3L, "Wed", 280.0, 6, 25.5),
            DailyEarningPoint(4L, "Thu", 190.0, 4, 17.0),
            DailyEarningPoint(5L, "Fri", 350.0, 8, 31.2),
            DailyEarningPoint(6L, "Sat", 200.0, 5, 19.0),
            DailyEarningPoint(7L, "Today", 260.0, 6, 22.3)
        )
        val summary = EarningsChartScale.computeTrendSummary(series)
        assertTrue(summary.hasAnyEarnings)
        assertEquals(1400.0, summary.totalEarnings, 0.01)
        assertEquals(32, summary.totalDeliveries)
        assertEquals(200.0, summary.dailyAverage, 0.01)
        assertNotNull(summary.peakDay)
        assertEquals("Fri", summary.peakDay?.dayLabel)
        assertEquals(60.0, summary.todayVsYesterdayDelta, 0.01)
    }

    @Test
    fun `computeTrendSummary handles all-zero empty week cleanly`() {
        val series = (1..7).map { DailyEarningPoint(it.toLong(), "D$it", 0.0, 0, 0.0) }
        val summary = EarningsChartScale.computeTrendSummary(series)
        assertFalse(summary.hasAnyEarnings)
        assertEquals(0.0, summary.totalEarnings, 0.01)
        assertNull(summary.peakDay)
    }
}
