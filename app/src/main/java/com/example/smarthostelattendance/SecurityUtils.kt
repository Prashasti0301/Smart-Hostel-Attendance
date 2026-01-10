// File: app/java/com/example/smarthostelattendance/SecurityUtils.kt
package com.example.smarthostelattendance  // ✅ IMPORTANT: Package name yahi hai

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class SecurityUtils(private val context: Context) {

    companion object {
        // ⚠️ APNE HOSTEL KI LOCATION DAALO
        const val HOSTEL_LAT = 28.6139  // CHANGE THIS
        const val HOSTEL_LNG = 77.2090  // CHANGE THIS
    }

    fun isInsideHostel(yourLat: Double, yourLng: Double): Boolean {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            yourLat, yourLng,
            HOSTEL_LAT, HOSTEL_LNG,
            results
        )
        return results[0] <= 150 // 150 meters
    }
}