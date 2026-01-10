package com.example.smarthostelattendance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smarthostelattendance.R
import com.example.smarthostelattendance.databinding.FragmentRegisterBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // ✅ Option A: Use lazy initialization
    private val auth by lazy { Firebase.auth }

    // ✅ Option B: Or use Firebase.auth directly everywhere

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupBackButton()
    }

    private fun setupBackButton() {
        binding.ivBack.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun validateAndRegister() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Clear errors
        binding.tilFullName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        var isValid = true

        if (name.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            binding.etFullName.requestFocus()
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            if (isValid) binding.etEmail.requestFocus()
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter valid email"
            if (isValid) binding.etEmail.requestFocus()
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            if (isValid) binding.etPassword.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            if (isValid) binding.etPassword.requestFocus()
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm password"
            if (isValid) binding.etConfirmPassword.requestFocus()
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            if (isValid) binding.etConfirmPassword.requestFocus()
            isValid = false
        }

        if (!isValid) return

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Registering..."

        // ✅ Use auth (which is now initialized)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "CREATE ACCOUNT"

                if (task.isSuccessful) {
                    onRegistrationSuccess(name, email)
                } else {
                    val errorMsg = task.exception?.message ?: "Registration failed"
                    onRegistrationFailure(errorMsg)
                }
            }
    }

    private fun onRegistrationSuccess(name: String, email: String) {
        // ✅ auth is now initialized
        val currentUser = auth.currentUser

        currentUser?.let { user ->
            val userData = hashMapOf(
                "name" to name,
                "email" to email,
                "phone" to "",
                "room" to "",
                "createdAt" to System.currentTimeMillis()
            )

            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(user.uid)
                .setValue(userData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Account created!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to save data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(requireContext(), "Registration failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onRegistrationFailure(errorMessage: String) {
        val friendlyMsg = when {
            errorMessage.contains("email address is already") -> "Email already registered"
            errorMessage.contains("network") -> "Network error. Check your connection"
            else -> "Registration failed. Please try again"
        }

        Toast.makeText(requireContext(), friendlyMsg, Toast.LENGTH_LONG).show()
        binding.etPassword.text?.clear()
        binding.etConfirmPassword.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}