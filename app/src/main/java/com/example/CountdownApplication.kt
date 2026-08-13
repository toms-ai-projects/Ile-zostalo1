package com.example

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.example.data.AppDatabase
import com.example.data.EventRepository
import com.example.widget.UserPresentReceiver

class CountdownApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)

        // ACTION_USER_PRESENT/ACTION_SCREEN_ON docierają WYŁĄCZNIE do odbiorników
        // zarejestrowanych dynamicznie (Context.registerReceiver) — deklaracja w
        // AndroidManifest.xml jest przez system po cichu ignorowana dla tych dwóch
        // akcji. Patrz obszerny komentarz w UserPresentReceiver.kt. Rejestrowany tu,
        // nie w manifeście.
        registerReceiver(
            UserPresentReceiver(),
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            }
        )
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
