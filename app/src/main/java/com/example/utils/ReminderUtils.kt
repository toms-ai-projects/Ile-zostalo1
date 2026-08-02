package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.Event
import com.example.worker.ReminderReceiver

object ReminderUtils {

    fun scheduleReminder(context: Context, event: Event) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("eventId", event.id)
            putExtra("eventName", event.name)
            
            val daysStr = if (event.reminderDays != null && event.reminderDays > 0) "${event.reminderDays} dni" else ""
            val hoursStr = if (event.reminderHours != null && event.reminderHours > 0) "${event.reminderHours} godzin" else ""
            val minutesStr = if (event.reminderMinutes != null && event.reminderMinutes > 0) "${event.reminderMinutes} minut" else ""
            val timeStr = listOf(daysStr, hoursStr, minutesStr).filter { it.isNotEmpty() }.joinToString(" i ")
            
            putExtra("message", "Zostało $timeStr!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel previous alarm
        alarmManager.cancel(pendingIntent)

        if (event.reminderDays == null && event.reminderHours == null && event.reminderMinutes == null) {
            return
        }
        
        val days = event.reminderDays ?: 0
        val hours = event.reminderHours ?: 0
        val minutes = event.reminderMinutes ?: 0
        
        if (days == 0 && hours == 0 && minutes == 0) {
            return
        }

        val offsetMillis = (days * 24L * 60 * 60 * 1000) + (hours * 60L * 60 * 1000) + (minutes * 60L * 1000)
        
        // Find next occurrence
        val nextOccurrence = event.getNextOccurrence(System.currentTimeMillis())
        val alarmTime = nextOccurrence - offsetMillis

        if (alarmTime > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime,
                            pendingIntent
                        )
                    } else {
                        // Fallback
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
    
    fun cancelReminder(context: Context, eventId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
