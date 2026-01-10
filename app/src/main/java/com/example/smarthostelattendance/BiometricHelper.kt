package com.example.smarthostelattendance

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class StrictBiometricHelper(private val context: Context) {

    enum class BiometricStatus {
        AVAILABLE,
        UNAVAILABLE,
        NO_HARDWARE,
        NOT_ENROLLED,
        ERROR
    }

    /**
     * STRICT CHECK: Only biometric strong authentication allowed
     */
    fun isBiometricStrictlyAvailable(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.ERROR
        }
    }

    /**
     * STRICT: No other method allowed - only biometric
     */
    fun showStrictBiometricScanner(
        activity: FragmentActivity,
        title: String = "Hostel Attendance - STRICT MODE",
        subtitle: String = "Fingerprint/Thumb Required",
        description: String = "Only registered biometric can mark attendance",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // First check if STRONG biometric is available
        val status = isBiometricStrictlyAvailable()
        if (status != BiometricStatus.AVAILABLE) {
            onError("❌ STRICT MODE: Only biometric attendance allowed\n${getStrictErrorMessage(status)}")
            return
        }

        val executor: Executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User cancelled - show strict message
                            Toast.makeText(
                                context,
                                "Attendance cancelled. Biometric required.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        BiometricPrompt.ERROR_LOCKOUT -> {
                            onError("Too many failed attempts. Try again in 30 seconds.")
                        }
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            onError("Biometric permanently locked. Contact admin.")
                        }
                        else -> {
                            onError("Authentication failed: $errString")
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // Verify it's STRONG biometric
                    if (result.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC) {
                        onSuccess()
                    } else {
                        onError("Invalid authentication method. Only biometric allowed.")
                    }
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(context, "Biometric not recognized. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        // STRICT: Only allow BIOMETRIC_STRONG
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)  // STRICT

        try {
            val promptInfo = promptInfoBuilder.build()
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError("System error: ${e.message}")
        }
    }

    /**
     * Strict error messages - no alternative options
     */
    private fun getStrictErrorMessage(status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.NO_HARDWARE ->
                "This device doesn't support biometric authentication.\nContact hostel admin for alternative arrangement."
            BiometricStatus.NOT_ENROLLED ->
                "No fingerprint/face enrolled.\nEnroll biometric in device settings to mark attendance."
            BiometricStatus.UNAVAILABLE ->
                "Biometric sensor unavailable.\nRestart device or contact technical support."
            else -> "Biometric authentication required. No alternative method available."
        }
    }

    /**
     * Check if device meets strict requirements
     */
    fun checkDeviceCompliance(): Boolean {
        val status = isBiometricStrictlyAvailable()
        return status == BiometricStatus.AVAILABLE
    }
}