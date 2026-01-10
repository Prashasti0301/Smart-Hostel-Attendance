package com.example.smarthostelattendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat

class LocationHelper(private val context: Context) {

    // Check if location permission is granted
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    // Get last known location (simplified version)
    fun getLastKnownLocation(): Location? {
        // For now return a dummy location
        return try {
            val location = Location("dummy")
            location.latitude = 28.6139  // Delhi coordinates
            location.longitude = 77.2090
            location.accuracy = 50.0f
            location
        } catch (e: Exception) {
            null
        }
    }

    // Get location status message
    fun getLocationStatusMessage(location: Location?): String {
        return if (location != null) {
            "📍 Location available"
        } else {
            "📍 Location not available"
        }
    }

    // Check if within hostel premises (simplified)
    fun isWithinHostelPremises(location: Location?): Boolean {
        // For testing, always return true
        return true
    }

    // Get distance from hostel (simplified)
    fun getDistanceFromHostel(location: Location?): Float {
        return 50f  // 50 meters for testing
    }

    // Get location string
    fun getLocationString(location: Location?): String {
        return if (location != null) {
            "Lat: ${String.format("%.6f", location.latitude)}, " +
                    "Long: ${String.format("%.6f", location.longitude)}"
        } else {
            "Location not available"
        }
    }
}