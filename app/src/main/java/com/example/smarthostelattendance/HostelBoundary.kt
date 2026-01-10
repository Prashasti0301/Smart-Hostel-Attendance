package com.example.smarthostelattendance

import android.location.Location

object HostelBoundary {

    // ✅ YOUR HOSTEL COORDINATES (Already correct)
    private const val HOSTEL_LATITUDE = 24.433093
    private const val HOSTEL_LONGITUDE = 77.159908

    // ✅ Radius adjusted (10000m = 10km - too big! Reduce it)
    private const val ALLOWED_RADIUS_METERS = 100.0 // Changed to 100m

    data class BoundaryPoint(val latitude: Double, val longitude: Double)

    // ❌ DELETE THIS OLD POLYGON (Delhi coordinates)
    // private val hostelPolygon = listOf(
    //     BoundaryPoint(28.6145, 77.2085),
    //     BoundaryPoint(28.6140, 77.2095),
    //     BoundaryPoint(28.6135, 77.2088),
    //     BoundaryPoint(28.6138, 77.2080)
    // )

    // ✅ CREATE NEW POLYGON WITH YOUR HOSTEL COORDINATES
    private val hostelPolygon = listOf(
        // Create a square around your hostel (approx 200m x 200m)
        // Point 1: Northwest corner
        BoundaryPoint(24.433493, 77.159508), // 50m North, 50m West
        // Point 2: Northeast corner
        BoundaryPoint(24.433493, 77.160308), // 50m North, 50m East
        // Point 3: Southeast corner
        BoundaryPoint(24.432693, 77.160308), // 50m South, 50m East
        // Point 4: Southwest corner
        BoundaryPoint(24.432693, 77.159508)  // 50m South, 50m West
    )

    /**
     * Check if location is within circular boundary (simple radius check)
     */
    fun isWithinCircularBoundary(location: Location?): Boolean {
        if (location == null) return false

        val results = FloatArray(1)
        Location.distanceBetween(
            HOSTEL_LATITUDE,
            HOSTEL_LONGITUDE,
            location.latitude,
            location.longitude,
            results
        )

        val distanceInMeters = results[0]

        // Debug log
        println("=== BOUNDARY DEBUG ===")
        println("Hostel: $HOSTEL_LATITUDE, $HOSTEL_LONGITUDE")
        println("Your Location: ${location.latitude}, ${location.longitude}")
        println("Distance: ${"%.1f".format(distanceInMeters)}m")
        println("Allowed Radius: ${ALLOWED_RADIUS_METERS}m")
        println("Within Radius: ${distanceInMeters <= ALLOWED_RADIUS_METERS}")
        println("=== END DEBUG ===")

        return distanceInMeters <= ALLOWED_RADIUS_METERS
    }

    /**
     * Check if location is within polygon boundary
     */
    fun isWithinPolygonBoundary(location: Location?): Boolean {
        if (location == null) return false

        val x = location.longitude
        val y = location.latitude

        var inside = false
        var j = hostelPolygon.size - 1

        for (i in hostelPolygon.indices) {
            val xi = hostelPolygon[i].longitude
            val yi = hostelPolygon[i].latitude
            val xj = hostelPolygon[j].longitude
            val yj = hostelPolygon[j].latitude

            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)

            if (intersect) inside = !inside
            j = i
        }

        return inside
    }

    /**
     * Get distance from hostel in meters
     */
    fun getDistanceFromHostel(location: Location?): Float {
        if (location == null) return Float.MAX_VALUE

        val results = FloatArray(1)
        Location.distanceBetween(
            HOSTEL_LATITUDE,
            HOSTEL_LONGITUDE,
            location.latitude,
            location.longitude,
            results
        )

        return results[0]
    }

    /**
     * Get hostel coordinates for display
     */
    fun getHostelCoordinates(): Pair<Double, Double> {
        return Pair(HOSTEL_LATITUDE, HOSTEL_LONGITUDE)
    }

    /**
     * Get formatted location message
     */
    fun getLocationMessage(location: Location?): String {
        if (location == null) return "Location unavailable"

        val distance = getDistanceFromHostel(location)
        val isWithin = isWithinCircularBoundary(location)

        return if (isWithin) {
            " Within hostel (${"%.0f".format(distance)}m)"
        } else {
            " ${"%.0f".format(distance)}m away"
        }
    }
    //change 1 here

    /**
     * NEW: Get simple status for button
     */
    fun getSimpleStatus(location: Location?): String {
        return if (isWithinCircularBoundary(location)) "INSIDE" else "OUTSIDE"
    }
}