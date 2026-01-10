package com.example.smarthostelattendance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthostelattendance.R

class AttendanceAdapter : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    private var attendanceList = listOf<Map<String, Any>>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textDate: TextView = itemView.findViewById(R.id.textDate)
        val textTime: TextView = itemView.findViewById(R.id.textTime)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendance = attendanceList[position]

        holder.textDate.text = "Date: ${attendance["date"] ?: "N/A"}"
        holder.textTime.text = "Time: ${attendance["time"] ?: "N/A"}"

        val status = attendance["status"]?.toString() ?: "N/A"
        holder.textStatus.text = "Status: $status"

        // Set color based on status
        val statusColor = when (status) {
            "Present" -> android.R.color.holo_green_dark
            "Absent" -> android.R.color.holo_red_dark
            "Late" -> android.R.color.holo_orange_dark
            else -> android.R.color.darker_gray
        }
        holder.textStatus.setTextColor(holder.itemView.context.getColor(statusColor))
    }

    override fun getItemCount(): Int = attendanceList.size

    fun submitList(list: List<Map<String, Any>>) {
        attendanceList = list
        notifyDataSetChanged()
    }
}