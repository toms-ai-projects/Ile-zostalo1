package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.EventRepository

class CountdownApplication : Application() {
    lateinit var container: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}

interface AppContainer {
    val eventRepository: EventRepository
}

class AppDataContainer(private val context: android.content.Context) : AppContainer {
    override val eventRepository: EventRepository by lazy {
        EventRepository(AppDatabase.getDatabase(context).eventDao(), context)
    }
}
