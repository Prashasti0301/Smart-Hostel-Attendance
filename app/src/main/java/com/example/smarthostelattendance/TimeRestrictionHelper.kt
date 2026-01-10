package com.example.smarthostelattendance

import java.text.SimpleDateFormat
import java.util.*

class TimeRestrictionHelper {

    companion object {
        // Attendance allowed only between these times
        private const val START_HOUR = 21  // 9 PM (24-hour format)
        private const val START_MINUTE = 30  // 9:30 PM
        private const val END_HOUR = 22     // 10 PM
        private const val END_MINUTE = 30    // 10:30 PM

        /**
         * Check if current time is within allowed attendance window
         */
        fun isWithinAttendanceTime(): Boolean {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            // Convert to minutes for easier comparison
            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val startTimeInMinutes = START_HOUR * 60 + START_MINUTE
            val endTimeInMinutes = END_HOUR * 60 + END_MINUTE

            return currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
        }

        /**
         * Get remaining time for attendance (if within window)
         */
        fun getRemainingTimeFormatted(): String {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val endTimeInMinutes = END_HOUR * 60 + END_MINUTE

            if (currentTimeInMinutes > endTimeInMinutes) {
                return "Attendance time ended"
            }

            val remainingMinutes = endTimeInMinutes - currentTimeInMinutes
            val hours = remainingMinutes / 60
            val minutes = remainingMinutes % 60

            return "$hours hr $minutes min remaining"
        }

        /**
         * Get next available time (if outside window)
         */
        fun getNextAvailableTimeFormatted(): String {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val startTimeInMinutes = START_HOUR * 60 + START_MINUTE

            if (currentTimeInMinutes < startTimeInMinutes) {
                // Before start time
                val minutesUntilStart = startTimeInMinutes - currentTimeInMinutes
                val hours = minutesUntilStart / 60
                val minutes = minutesUntilStart % 60

                return if (hours > 0) {
                    "Available in $hours hr $minutes min"
                } else {
                    "Available in $minutes min"
                }
            } else {
                // After end time - show next day's time
                val minutesUntilNextDay = (24 * 60 - currentTimeInMinutes) + startTimeInMinutes
                val hours = minutesUntilNextDay / 60
                val minutes = minutesUntilNextDay % 60

                return if (hours >= 24) {
                    "Available tomorrow at 9:30 PM"
                } else if (hours > 0) {
                    "Available in $hours hr $minutes min"
                } else {
                    "Available in $minutes min"
                }
            }
        }

        /**
         * Get attendance window as formatted string
         */
        fun getAttendanceWindowFormatted(): String {
            return "9:30 PM - 10:30 PM"
        }

        /**
         * Check if attendance time has ended for today
         */
        fun hasAttendanceTimeEnded(): Boolean {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val endTimeInMinutes = END_HOUR * 60 + END_MINUTE

            return currentTimeInMinutes > endTimeInMinutes
        }

        /**
         * Get detailed time status
         */
        fun getTimeStatus(): TimeStatus {
            return if (isWithinAttendanceTime()) {
                TimeStatus.WITHIN_WINDOW
            } else if (hasAttendanceTimeEnded()) {
                TimeStatus.ENDED_FOR_TODAY
            } else {
                TimeStatus.NOT_STARTED_YET
            }
        }

        enum class TimeStatus {
            WITHIN_WINDOW,
            NOT_STARTED_YET,
            ENDED_FOR_TODAY
        }
    }
}