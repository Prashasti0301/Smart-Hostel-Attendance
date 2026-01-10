package com.example.smarthostelattendance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarthostelattendance.R
import com.example.smarthostelattendance.adapters.AttendanceAdapter
import com.example.smarthostelattendance.databinding.FragmentAttendanceHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class AttendanceHistoryFragment : Fragment() {

    private var _binding: FragmentAttendanceHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: AttendanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupRecyclerView()
        loadAttendanceHistory()

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = AttendanceAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun loadAttendanceHistory() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            binding.progressBar.visibility = View.VISIBLE

            val attendanceRef = database.reference.child("attendance")

            attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val attendanceList = mutableListOf<Map<String, Any>>()

                    for (dateSnapshot in snapshot.children) {
                        val date = dateSnapshot.key ?: continue
                        for (userSnapshot in dateSnapshot.children) {
                            if (userSnapshot.key == user.uid) {
                                val attendanceData = userSnapshot.getValue() as? Map<String, Any>
                                attendanceData?.let {
                                    attendanceList.add(it)
                                }
                            }
                        }
                    }

                    // Sort by timestamp (newest first)
                    attendanceList.sortByDescending {
                        it["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                    }

                    adapter.submitList(attendanceList)
                    binding.progressBar.visibility = View.GONE

                    if (attendanceList.isEmpty()) {
                        binding.textEmpty.visibility = View.VISIBLE
                    } else {
                        binding.textEmpty.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error loading attendance: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            })
        } ?: run {
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}