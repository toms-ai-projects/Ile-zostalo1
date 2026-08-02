package com.example.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text
import com.example.data.AppDatabase

class TestWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        provideContent {
            val eventList by database.eventDao().getAllEvents().collectAsState(initial = emptyList())
            Text(text = "Count: ${eventList.size}")
        }
    }
}
