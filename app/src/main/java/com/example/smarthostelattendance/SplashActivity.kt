package com.example.smarthostelattendance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            val intent = Intent(this, MainActivity::class.java)

            if (currentUser != null) {
                // User already logged in, go to Home
                intent.putExtra("fragment", "home")
            } else {
                // User not logged in, go to Login
                intent.putExtra("fragment", "login")
            }

            startActivity(intent)
            finish()
        }, 2000) // 2 seconds delay
    }
}