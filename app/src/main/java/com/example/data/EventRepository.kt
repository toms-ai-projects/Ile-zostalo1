package com.example.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.utils.ReminderUtils
import com.example.widget.CountdownGlanceWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EventRepository(private val eventDao: EventDao, private val context: Context) {
    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()

    fun getEvent(id: Int): Flow<Event?> = eventDao.getEventById(id)

    suspend fun insert(event: Event): Long {
        val id = eventDao.insertEvent(event)
        
        // Ensure the returned ID or existing ID is used
        val eventToSchedule = if (event.id == 0) event.copy(id = id.toInt()) else event
        ReminderUtils.scheduleReminder(context, eventToSchedule)
        
        updateWidget()
        return id
    }

    suspend fun delete(event: Event) {
        eventDao.deleteEvent(event)
        ReminderUtils.cancelReminder(context, event.id)
        updateWidget()
    }

    private fun updateWidget() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CountdownGlanceWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
