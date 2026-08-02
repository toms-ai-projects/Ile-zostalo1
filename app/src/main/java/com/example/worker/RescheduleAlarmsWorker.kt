package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.utils.ReminderUtils

class RescheduleAlarmsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val events = database.eventDao().getAllEventsSnapshot()
        
        events.forEach { event ->
            ReminderUtils.scheduleReminder(applicationContext, event)
        }
        
        return Result.success()
    }
}
