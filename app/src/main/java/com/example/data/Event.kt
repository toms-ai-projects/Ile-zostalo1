package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrenceType {
    NONE, WEEKLY, MONTHLY, YEARLY
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val targetTimestamp: Long,
    val colorArgb: Int,
    val note: String = "",
    val imageUri: String = "",
    val recurrence: String = RecurrenceType.NONE.name,
    val reminderDays: Int? = null,
    val reminderHours: Int? = null,
    val reminderMinutes: Int? = null,
    val theme: String = "Classic"
) {
    fun getNextOccurrence(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val recType = try {
            RecurrenceType.valueOf(recurrence)
        } catch (e: IllegalArgumentException) {
            RecurrenceType.NONE
        }

        if (recType == RecurrenceType.NONE) return targetTimestamp
        if (targetTimestamp > currentTimeMillis) return targetTimestamp

        val originalCal = java.util.Calendar.getInstance().apply { timeInMillis = targetTimestamp }
        val currentCal = java.util.Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

        when (recType) {
            RecurrenceType.WEEKLY -> {
                val diff = currentTimeMillis - targetTimestamp
                val weeks = diff / (7 * 24 * 60 * 60 * 1000L)
                var currentWeeks = weeks.toInt()
                var nextTime = (originalCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.WEEK_OF_YEAR, currentWeeks)
                }
                while (nextTime.timeInMillis <= currentTimeMillis) {
                    currentWeeks++
                    nextTime = (originalCal.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.WEEK_OF_YEAR, currentWeeks)
                    }
                }
                return nextTime.timeInMillis
            }
            RecurrenceType.MONTHLY -> {
                val yearDiff = currentCal.get(java.util.Calendar.YEAR) - originalCal.get(java.util.Calendar.YEAR)
                val monthDiff = currentCal.get(java.util.Calendar.MONTH) - originalCal.get(java.util.Calendar.MONTH)
                var totalMonths = yearDiff * 12 + monthDiff
                if (totalMonths < 0) totalMonths = 0
                
                var nextTime = (originalCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.MONTH, totalMonths)
                }
                while (nextTime.timeInMillis <= currentTimeMillis) {
                    totalMonths++
                    nextTime = (originalCal.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.MONTH, totalMonths)
                    }
                }
                return nextTime.timeInMillis
            }
            RecurrenceType.YEARLY -> {
                val yearDiff = currentCal.get(java.util.Calendar.YEAR) - originalCal.get(java.util.Calendar.YEAR)
                var totalYears = if (yearDiff > 0) yearDiff else 0
                
                var nextTime = (originalCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.YEAR, totalYears)
                }
                while (nextTime.timeInMillis <= currentTimeMillis) {
                    totalYears++
                    nextTime = (originalCal.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.YEAR, totalYears)
                    }
                }
                return nextTime.timeInMillis
            }
            RecurrenceType.NONE -> return targetTimestamp
        }
    }
}
