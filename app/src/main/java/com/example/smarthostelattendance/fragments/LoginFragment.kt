package com.example.smarthostelattendance.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smarthostelattendance.R
import com.example.smarthostelattendance.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    companion object {
        private const val TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        setupPasswordToggle()
        setupClickListeners()
    }

    private fun setupPasswordToggle() {
        binding.ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            binding.loginPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            binding.ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off)
        } else {
            // Show password
            binding.loginPassword.transformationMethod = null
            binding.ivPasswordToggle.setImageResource(R.drawable.ic_visibility)
        }
        isPasswordVisible = !isPasswordVisible
        binding.loginPassword.setSelection(binding.loginPassword.text.length)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }

        binding.goToRegister.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                Toast.makeText(requireContext(), "Going to Register", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Forgot Password clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAndLogin() {
        val email = binding.loginEmail.text.toString().trim()
        val password = binding.loginPassword.text.toString().trim()

        // Clear errors
        binding.loginEmail.error = null
        binding.loginPassword.error = null

        var isValid = true

        if (email.isEmpty()) {
            binding.loginEmail.error = "Email is required"
            binding.loginEmail.requestFocus()
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginEmail.error = "Please enter a valid email"
            binding.loginEmail.requestFocus()
            isValid = false
        }

        if (password.isEmpty()) {
            binding.loginPassword.error = "Password is required"
            if (isValid) binding.loginPassword.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            binding.loginPassword.error = "Password must be at least 6 characters"
            if (isValid) binding.loginPassword.requestFocus()
            isValid = false
        }

        if (!isValid) return

        // Show loading
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Logging in..."

        // Firebase login
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "LOGIN"

                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    val errorMsg = task.exception?.message ?: "Login failed"
                    onLoginFailure(errorMsg)
                }
            }
    }

    private fun onLoginSuccess() {
        Log.d(TAG, " Login successful!")
        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()


        try {



            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Navigation error: ${e.message}")
            Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onLoginFailure(errorMessage: String) {
        val friendlyMsg = when {
            errorMessage.contains("no user record") -> "No account found with this email"
            errorMessage.contains("password is invalid") -> "Incorrect password"
            errorMessage.contains("network") -> "Network error. Check your connection"
            else -> "Login failed. Please try again"
        }

        Toast.makeText(requireContext(), friendlyMsg, Toast.LENGTH_LONG).show()
        binding.loginPassword.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}