package com.example

import com.example.domain.prayer.QiblaCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {

    @Test
    fun knownCities_haveExpectedBearing() {
        assertEquals(118.99, QiblaCalculator.calculateQiblaDirection(51.5074, -0.1278), 0.5)   // London
        assertEquals(58.48, QiblaCalculator.calculateQiblaDirection(40.7128, -74.0060), 0.5)   // New York
        assertEquals(267.74, QiblaCalculator.calculateQiblaDirection(24.8607, 67.0011), 0.5)   // Karachi
        assertEquals(295.15, QiblaCalculator.calculateQiblaDirection(-6.2088, 106.8456), 0.5)  // Jakarta
        assertEquals(277.5, QiblaCalculator.calculateQiblaDirection(-33.8688, 151.2093), 0.5)  // Sydney
    }

    @Test
    fun bearing_isAlwaysWithinZeroTo360() {
        var lat = -80
        while (lat <= 80) {
            var lng = -180
            while (lng <= 180) {
                val b = QiblaCalculator.calculateQiblaDirection(lat.toDouble(), lng.toDouble())
                assertTrue("bearing $b at $lat,$lng", b >= 0.0 && b < 360.0)
                lng += 30
            }
            lat += 20
        }
    }

    @Test
    fun distanceToKaaba_londonIsAboutFourThousandSevenHundredKm() {
        val km = QiblaCalculator.calculateDistanceToKaabaKm(51.5074, -0.1278)
        assertTrue("distance $km", km in 4700.0..4900.0)
    }
}
