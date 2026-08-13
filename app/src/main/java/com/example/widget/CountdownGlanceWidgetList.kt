package com.example.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Event
import java.util.concurrent.TimeUnit

/**
 * Piąta odmiana widgetu ("Lista", 4x2) — jedyna, która pokazuje TRZY najbliższe
 * wydarzenia naraz zamiast jednego, więc (w przeciwieństwie do pozostałych czterech) nie
 * ma pojęcia "wybranego wydarzenia" — nie korzysta z WidgetConfigActivity/
 * SELECTED_EVENT_ID_KEY, bo nie ma czego wybierać. Tło karty neutralne (BgLight z
 * głównego motywu appki, nie kolor konkretnego wydarzenia), a każdy wiersz dostaje własną
 * kropkę w kolorze SWOJEGO motywu — 1:1 wg makiety dostarczonej przez użytkownika.
 */
class CountdownGlanceWidgetList : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)

        provideContent {
            val eventList by database.eventDao().getAllEvents().collectAsState(initial = null)

            val contentModifier = GlanceModifier
                .fillMaxSize()
                .background(com.example.ui.theme.BgLight)
                .cornerRadius(32.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(18.dp)

            if (eventList == null) {
                Column(
                    modifier = contentModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ładowanie...",
                        style = TextStyle(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = androidx.glance.unit.ColorProvider(com.example.ui.theme.TextLight),
                            textAlign = TextAlign.Center
                        )
                    )
                }
                return@provideContent
            }

            val currentTime = System.currentTimeMillis()
            // Więcej niż 3 (mockup) — użytkownik poprosił o zapełnienie wolnej przestrzeni
            // pod spodem, skoro pudełko i tak jest wyższe niż trzy wiersze potrzebują.
            val upcoming: List<Event> = eventList!!
                .filter { it.getNextOccurrence(currentTime) >= currentTime }
                .sortedBy { it.getNextOccurrence(currentTime) }
                .take(5)

            if (upcoming.isEmpty()) {
                Column(
                    modifier = contentModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Brak nadchodzących wydarzeń",
                        style = TextStyle(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = androidx.glance.unit.ColorProvider(com.example.ui.theme.TextLight),
                            textAlign = TextAlign.Center
                        )
                    )
                }
                return@provideContent
            }

            Column(modifier = contentModifier) {
                Text(
                    text = "NAJBLIŻSZE",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.glance.unit.ColorProvider(com.example.ui.theme.OnSurfaceVariant)
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(10.dp))
                upcoming.forEachIndexed { index, event ->
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(
                        Math.abs(event.getNextOccurrence(currentTime) - currentTime)
                    )
                    val dotColor = com.example.ui.theme.EventThemes.getTheme(event.theme).accentColor

                    // Wiersz + separator zawinięte w OSOBNĄ, zagnieżdżoną Column — limit 10
                    // dzieci w Glance dotyczy KAŻDEGO kontenera z osobna, więc ta wewnętrzna
                    // para (wiersz, separator) liczy się jako JEDEN element zewnętrznej
                    // Column, a nie dwa. Bez tego, przy więcej niż ~4 wydarzeniach,
                    // zewnętrzna Column (nagłówek + N wierszy + (N-1) separatorów)
                    // przekroczyłaby limit i nadmiar zostałby po cichu ucięty.
                    Column {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(8.dp)
                                    .background(dotColor)
                                    .cornerRadius(4.dp)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(10.dp))
                            Text(
                                text = event.name,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = androidx.glance.unit.ColorProvider(com.example.ui.theme.TextLight)
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = "$daysLeft dni",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(com.example.ui.theme.TextLight)
                                ),
                                maxLines = 1
                            )
                        }
                        if (index < upcoming.size - 1) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(1.dp)
                                    .background(com.example.ui.theme.OnSurfaceVariant.copy(alpha = 0.25f))
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

class CountdownGlanceWidgetListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownGlanceWidgetList()
}
