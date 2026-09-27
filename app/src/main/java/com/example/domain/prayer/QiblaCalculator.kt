package com.example.domain.prayer

import kotlin.math.*

object QiblaCalculator {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LNG = 39.826206

    /**
     * Calculates Qibla direction in degrees clockwise from True North (0° = North, 90° = East, etc.)
     */
    fun calculateQiblaDirection(latitude: Double, longitude: Double): Double {
        val userLatRad = Math.toRadians(latitude)
        val userLngRad = Math.toRadians(longitude)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val kaabaLngRad = Math.toRadians(KAABA_LNG)

        val deltaLng = kaabaLngRad - userLngRad

        val y = sin(deltaLng) * cos(kaabaLatRad)
        val x = cos(userLatRad) * sin(kaabaLatRad) - sin(userLatRad) * cos(kaabaLatRad) * cos(deltaLng)

        var qiblaDegrees = Math.toDegrees(atan2(y, x))
        qiblaDegrees = (qiblaDegrees + 360.0) % 360.0
        return qiblaDegrees
    }

    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double =
        calculateQiblaDirection(latitude, longitude)

    fun calculateDistanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LNG - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
