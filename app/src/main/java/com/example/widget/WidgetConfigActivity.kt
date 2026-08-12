package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ekran wyboru wydarzenia dla konkretnej instancji widgetu — wspólny dla OBU odmian
 * widgetu (CountdownGlanceWidget "krótki" i CountdownGlanceWidgetPanorama "duży"), oba
 * mają android:configure wskazujące tu w swoich glance_widget_info*.xml. Android sam
 * odpala tę Activity przy dodawaniu widgetu do ekranu głównego, i ponownie przy "Edytuj
 * widżet" z długiego przytrzymania. Wybór jest zapisywany per-appWidgetId (nie globalnie),
 * więc różne instancje widgetu mogą pokazywać różne wydarzenia — patrz
 * WidgetShared.kt (SELECTED_EVENT_ID_KEY).
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Standardowa konwencja Androida dla activity konfiguracyjnych widgetów: jeśli
        // użytkownik cofnie się bez wyboru, system ma domyślnie potraktować to jako
        // anulowanie i NIE dodawać widgetu.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                WidgetConfigScreen(
                    onEventSelected = { event -> saveSelectionAndFinish(event.id) }
                )
            }
        }
    }

    private fun saveSelectionAndFinish(eventId: Int) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                prefs[SELECTED_EVENT_ID_KEY] = eventId
            }
            // Ten sam ekran obsługuje obie odmiany widgetu — trzeba sprawdzić, do KTÓREGO
            // providera należy ten appWidgetId, żeby odświeżyć właściwy (inaczej wybór
            // zapisany dla Panoramy nigdy by się nie pokazał, bo update() poszedłby zawsze
            // do krótkiego widgetu).
            val providerClassName = AppWidgetManager.getInstance(this@WidgetConfigActivity)
                .getAppWidgetInfo(appWidgetId)?.provider?.className
            val widget: androidx.glance.appwidget.GlanceAppWidget =
                if (providerClassName == CountdownGlanceWidgetPanoramaReceiver::class.java.name) {
                    CountdownGlanceWidgetPanorama()
                } else {
                    CountdownGlanceWidget()
                }
            widget.update(this@WidgetConfigActivity, glanceId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun WidgetConfigScreen(onEventSelected: (Event) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var events by remember { mutableStateOf<List<Event>?>(null) }

    LaunchedEffect(Unit) {
        events = AppDatabase.getDatabase(context).eventDao().getAllEventsSnapshot()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wybierz wydarzenie dla widgetu") })
        }
    ) { innerPadding ->
        val currentEvents = events
        when {
            currentEvents == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            currentEvents.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "Brak wydarzeń — dodaj jedno w aplikacji, żeby móc je przypisać do widgetu.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            else -> {
                val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(currentEvents) { event ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                .clickable { onEventSelected(event) }
                                .padding(16.dp)
                        ) {
                            Text(event.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                dateFormatter.format(Date(event.targetTimestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
