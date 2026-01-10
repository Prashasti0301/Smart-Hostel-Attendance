package com.example.smarthostelattendance

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthostelattendance.databinding.ActivityMainBinding
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Firebase Services
    private lateinit var analytics: FirebaseAnalytics
    private val auth = Firebase.auth
    private val database = Firebase.database
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("MainActivity", "🏠 Smart Hostel Attendance Started")

        // Initialize all Firebase services
        initializeAllFirebaseServices()
    }

    private fun initializeAllFirebaseServices() {
        try {
            // 1. Analytics
            analytics = Firebase.analytics

            // All other services already initialized above
            Log.d("Firebase", "✅ ALL FIREBASE SERVICES INITIALIZED:")
            Log.d("Firebase", "   • Analytics: Ready")
            Log.d("Firebase", "   • Auth: ${auth.app.name}")
            Log.d("Firebase", "   • Realtime DB: ${database.reference}")
            Log.d("Firebase", "   • Firestore: ${firestore}")
            Log.d("Firebase", "   • Storage: ${storage}")

            // Test connection to all services
            testAllFirebaseConnections()

            // Log app launch to Analytics
            logAppLaunchEvent()

        } catch (e: Exception) {
            Log.e("Firebase", "❌ Firebase setup failed", e)
        }
    }

    private fun testAllFirebaseConnections() {
        // Test 1: Realtime Database
        val testRef = database.getReference("app_status/main_activity")
        testRef.setValue("App launched at ${System.currentTimeMillis()}")
            .addOnSuccessListener {
                Log.d("Firebase", "✅ Realtime DB: Working")
            }

        // Test 2: Firestore
        val testDoc = firestore.collection("app_logs").document("launch")
        testDoc.set(hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "event" to "app_launch"
        ))

        // Test 3: Check auth state
        val user = auth.currentUser
        if (user != null) {
            Log.d("Firebase", "✅ Auth: User logged in (${user.email})")
        } else {
            Log.d("Firebase", "✅ Auth: No user (ready for login)")
        }
    }

    private fun logAppLaunchEvent() {
        val bundle = Bundle().apply {
            putString("screen", "MainActivity")
            putString("app_name", "Smart Hostel Attendance")
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("app_launch", bundle)
        Log.d("Analytics", "✅ App launch event logged")
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "App visible to user")
    }
}