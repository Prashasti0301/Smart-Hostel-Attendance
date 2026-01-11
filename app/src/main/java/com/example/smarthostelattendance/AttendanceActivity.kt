package com.example.smarthostelattendance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smarthostelattendance.databinding.ActivityAttendanceBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceBinding
    private lateinit var locationManager: LocationManager
    private val firestore = Firebase.firestore

    private val LOCATION_PERMISSION_CODE = 101
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        setupUI()
        checkLocationPermission()
    }

    private fun setupUI() {
        // Display hostel coordinates
        val hostelCoords = HostelBoundary.getHostelCoordinates()
        binding.tvHostelLocation.text = "Hostel: ${hostelCoords.first}, ${hostelCoords.second}"

        // Button click listeners
        binding.btnMarkAttendance.setOnClickListener {
            getCurrentLocation()
        }

        binding.btnCheckOut.setOnClickListener {
            markCheckOut()
        }

        binding.btnRefreshLocation.setOnClickListener {
            getCurrentLocation()
        }

        binding.btnViewMap.setOnClickListener {
            openGoogleMaps()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        } else {
            binding.btnMarkAttendance.isEnabled = true
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.btnMarkAttendance.isEnabled = true
                Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied!", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!isLocationEnabled()) {
            showLocationEnableDialog()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvStatus.text = "Fetching location..."

        try {
            // Request single location update
            locationManager.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                locationListener,
                null
            )

            // Also get last known location for faster response
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null) {
                updateLocationUI(lastLocation)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            updateLocationUI(location)
            binding.progressBar.visibility = android.view.View.GONE
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun updateLocationUI(location: Location) {
        runOnUiThread {
            // Update coordinates display
            binding.tvLatitude.text = "Your Latitude: ${location.latitude}"
            binding.tvLongitude.text = "Your Longitude: ${location.longitude}"

            // Check if within hostel boundary
            val isWithinBoundary = HostelBoundary.isWithinCircularBoundary(location)
            val distance = HostelBoundary.getDistanceFromHostel(location)
            val message = HostelBoundary.getLocationMessage(location)

            binding.tvDistance.text = "Distance: ${"%.1f".format(distance)}m"
            binding.tvStatus.text = message

            if (isWithinBoundary) {
                binding.btnMarkAttendance.isEnabled = true
                binding.btnMarkAttendance.text = "MARK CHECK-IN"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            } else {
                binding.btnMarkAttendance.isEnabled = false
                binding.btnMarkAttendance.text = " OUTSIDE HOSTEL"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        }
    }

    private fun markAttendance() {
        currentLocation?.let { location ->
            val userId = getUserId()
            val attendanceData = hashMapOf(
                "userId" to userId,
                "studentName" to getStudentName(),
                "type" to "check-in",
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to Timestamp.now(),
                "date" to SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()),
                "time" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                "isWithinHostel" to HostelBoundary.isWithinCircularBoundary(location),
                "distance" to HostelBoundary.getDistanceFromHostel(location)
            )

            // Save to Firestore
            firestore.collection("attendance_records")
                .add(attendanceData)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Check-in recorded successfully!", Toast.LENGTH_LONG).show()
                    logAttendanceSuccess()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, " Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } ?: run {
            Toast.makeText(this, "Location not available!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markCheckOut() {
        currentLocation?.let { location ->
            if (!HostelBoundary.isWithinCircularBoundary(location)) {
                AlertDialog.Builder(this)
                    .setTitle("Not in Hostel")
                    .setMessage("You must be within hostel premises to check-out. Are you sure?")
                    .setPositiveButton("Yes, Check-out") { _, _ ->
                        saveCheckOut(location)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                saveCheckOut(location)
            }
        }
    }

    private fun saveCheckOut(location: Location) {
        val userId = getUserId()
        val checkOutData = hashMapOf(
            "userId" to userId,
            "type" to "check-out",
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to Timestamp.now()
        )

        firestore.collection("attendance_records")
            .add(checkOutData)
            .addOnSuccessListener {
                Toast.makeText(this, " Check-out recorded!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getUserId(): String {
        // Get from SharedPreferences or Firebase Auth
        val prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "guest_${System.currentTimeMillis()}") ?: "unknown"
    }

    private fun getStudentName(): String {
        val prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return prefs.getString("student_name", "Student") ?: "Student"
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationEnableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location")
            .setMessage("Turn on location services to mark attendance")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openGoogleMaps() {
        currentLocation?.let { location ->
            val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
            startActivity(intent)
        } ?: run {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logAttendanceSuccess() {
        // Log to analytics or local database
        val prefs = getSharedPreferences("attendance_log", Context.MODE_PRIVATE)
        val count = prefs.getInt("total_checkins", 0) + 1
        prefs.edit().apply {
            putInt("total_checkins", count)
            putLong("last_checkin", System.currentTimeMillis())
            apply()
        }
    }
}