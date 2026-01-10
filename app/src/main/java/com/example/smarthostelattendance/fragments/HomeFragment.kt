package com.example.smarthostelattendance.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smarthostelattendance.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ✅ Hostel Coordinates (Set your hostel's GPS coordinates)
    private val HOSTEL_LATITUDE = 28.6139  // Delhi example
    private val HOSTEL_LONGITUDE = 77.2090
    private val MAX_DISTANCE_METERS = 100.0  // 100 meters radius

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupBiometric()
        setupUI()
        setupButtons()
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // ✅ Biometric passed, now check location
                    checkLocationAndMarkAttendance()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "❌ Biometric failed: $errString", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
            })
    }

    private fun setupUI() {
        val currentUser = auth.currentUser
        binding.textWelcome.text = if (currentUser != null) {
            "Student: ${currentUser.email?.split("@")?.firstOrNull() ?: "User"}"
        } else {
            "Please login"
        }

        binding.textTimeStatus.text = "🔒 Secure Biometric Attendance"
        binding.textTimeDetails.text = "Fingerprint + Location verified"
        binding.textRemainingTime.text = "Hostel premises required"
    }

    private fun setupButtons() {
        binding.buttonMarkAttendance.setOnClickListener {
            // ✅ Step 1: Check time restriction first
            if (isWithinAttendanceTime()) {
                // ✅ Step 2: Start biometric verification
                startBiometricVerification()
            } else {
                Toast.makeText(requireContext(), "⏰ Attendance time: 9:30-10:30 PM only", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
    }

    private fun isWithinAttendanceTime(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // 9:30 PM to 10:30 PM (21:30 to 22:30 in 24-hour)
        return (currentHour == 21 && currentMinute >= 30) ||
                (currentHour == 22 && currentMinute <= 30)
    }

    private fun startBiometricVerification() {
        binding.buttonMarkAttendance.isEnabled = false
        binding.buttonMarkAttendance.text = "Scanning..."

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Student Identity Verification")
            .setSubtitle("Use your registered fingerprint")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun checkLocationAndMarkAttendance() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // ✅ Calculate distance from hostel
                    val hostelLocation = Location("hostel").apply {
                        latitude = HOSTEL_LATITUDE
                        longitude = HOSTEL_LONGITUDE
                    }

                    val distance = location.distanceTo(hostelLocation)

                    if (distance <= MAX_DISTANCE_METERS) {
                        // ✅ Within hostel premises - mark attendance
                        saveAttendanceToFirebase(location.latitude, location.longitude, distance)
                    } else {
                        // ❌ Outside hostel
                        binding.buttonMarkAttendance.text = "❌ Outside hostel"
                        Toast.makeText(
                            requireContext(),
                            "You are ${String.format("%.0f", distance)}m away from hostel",
                            Toast.LENGTH_LONG
                        ).show()
                        resetButton()
                    }
                } else {
                    Toast.makeText(requireContext(), "Cannot get location", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
            }
    }

    private fun saveAttendanceToFirebase(lat: Double, lon: Double, distance: Float) {
        val user = auth.currentUser
        if (user != null) {
            val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val attendanceData = hashMapOf(
                "studentId" to user.uid,
                "studentEmail" to user.email,
                "date" to date,
                "time" to time,
                "method" to "Biometric+Location",
                "status" to "Present",
                "latitude" to lat,
                "longitude" to lon,
                "distanceFromHostel" to distance,
                "timestamp" to System.currentTimeMillis(),
                "deviceId" to android.os.Build.SERIAL,
                "verified" to true
            )

            val attendanceRef = database.reference.child("secure_attendance")
            attendanceRef.push().setValue(attendanceData)
                .addOnSuccessListener {
                    binding.buttonMarkAttendance.text = "✅ Verified"
                    Toast.makeText(
                        requireContext(),
                        "✅ Secure attendance marked!\nDistance: ${String.format("%.0f", distance)}m",
                        Toast.LENGTH_LONG
                    ).show()

                    binding.buttonMarkAttendance.postDelayed({
                        resetButton()
                    }, 3000)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "❌ Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
        }
    }

    private fun resetButton() {
        binding.buttonMarkAttendance.text = "Mark Attendance"
        binding.buttonMarkAttendance.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}