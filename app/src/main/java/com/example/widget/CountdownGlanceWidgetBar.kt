package com.example.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Trzecia odmiana widgetu ("Pasek", 4x1, niski profil) — obok CountdownGlanceWidget
 * (karta jak na Home) i CountdownGlanceWidgetPanorama (rozbudowana, z paskiem postępu).
 * Ta jest najbardziej skondensowana: duża liczba dni z lewej, z prawej nazwa wydarzenia
 * i jedna linia "data · Xg Ym" — 1:1 wg makiety dostarczonej przez użytkownika.
 */
class CountdownGlanceWidgetBar : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)

        provideContent {
            val eventList by database.eventDao().getAllEvents().collectAsState(initial = null)

            val contentModifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFE8DEF8)) // M3 light primary container approx
                .cornerRadius(32.dp)
                .padding(20.dp)

            if (eventList == null) {
                Column(
                    modifier = contentModifier.clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ładowanie...",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = androidx.glance.unit.ColorProvider(Color(0xFF1D192B)),
                            textAlign = TextAlign.Center
                        )
                    )
                }
                return@provideContent
            }

            val currentTime = System.currentTimeMillis()
            val selectedEventId = currentState(SELECTED_EVENT_ID_KEY)
            val nearestEvent = eventList!!.filter { it.getNextOccurrence(currentTime) >= currentTime }.minByOrNull { it.getNextOccurrence(currentTime) } ?: eventList!!.firstOrNull()
            val activeEvent = selectedEventId?.let { id -> eventList!!.find { it.id == id } } ?: nearestEvent

            if (activeEvent == null) {
                Column(
                    modifier = contentModifier.clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dodaj pierwsze wydarzenie",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = androidx.glance.unit.ColorProvider(Color(0xFF1D192B)),
                            textAlign = TextAlign.Center
                        )
                    )
                }
                return@provideContent
            }

            val nextTimestamp = activeEvent.getNextOccurrence(currentTime)
            val diffMillis = nextTimestamp - currentTime
            val isPast = diffMillis < 0
            val absDiff = Math.abs(diffMillis)
            val daysLeft = TimeUnit.MILLISECONDS.toDays(absDiff)
            val hoursLeft = TimeUnit.MILLISECONDS.toHours(absDiff) % 24
            val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(absDiff) % 60
            // Bez roku i bez godziny zegarowej — po powiększeniu fontów cała linia
            // "data · Xg Ym" musiała się zmieścić w węższej kolumnie (obok dużej liczby
            // dni), rok się nie mieścił i się ucinał. Rok i tak nieistotny dla najbliższego
            // wydarzenia w praktyce.
            val dateFormatter = remember { SimpleDateFormat("d MMMM", Locale.getDefault()) }
            val dateString = dateFormatter.format(Date(nextTimestamp))
            val subtitle = "$dateString · ${hoursLeft}g ${minutesLeft}m"

            val hasImage = activeEvent.imageUri.isNotBlank()
            val themeConfig = com.example.ui.theme.EventThemes.getTheme(activeEvent.theme)
            val textColor = if (hasImage) Color.White else themeConfig.textColor
            val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
            val backgroundColor = themeConfig.backgroundColor

            var bitmap: android.graphics.Bitmap? = null
            if (hasImage) {
                try {
                    bitmap = BitmapFactory.decodeFile(activeEvent.imageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Box(
                modifier = contentModifier
                    .background(if (bitmap != null) Color.Transparent else backgroundColor)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(EVENT_ID_PARAM to activeEvent.id)
                        )
                    )
            ) {
                if (bitmap != null) {
                    // Patrz komentarz w CountdownGlanceWidget.kt — clipToOutline na
                    // kontenerze nie przycina Image, trzeba dać promień też bezpośrednio na nim.
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(32.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = GlanceModifier.fillMaxSize().background(Color(0x80000000)).cornerRadius(32.dp)) {}
                }

                // Ta odmiana miała być "jedna linia, niski profil" wg makiety, ale na tym
                // telefonie/launcherze widget o szerokości 4 kolumn NIE MA opcji 1 wiersza —
                // system (dumpsys appwidget: semAppWidgetRowSpan=2) zawsze przydziela 2
                // wiersze niezależnie od targetCellHeight/minHeight w naszym kodzie (to samo
                // dotyczy nawet systemowych widgetów Samsunga przy tej szerokości — nie da się
                // tego wymusić z poziomu appki). Zamiast wyświetlać małą treść pływającą w
                // dużym pudełku, fonty wyraźnie powiększone, żeby "jedna linia" sensownie
                // wypełniała realną wysokość.
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$daysLeft",
                            style = TextStyle(
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.glance.unit.ColorProvider(textColor)
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = if (isPast) "DNI TEMU" else "DNI",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(18.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = activeEvent.name,
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.glance.unit.ColorProvider(textColor)
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Medium,
                                color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

class CountdownGlanceWidgetBarReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownGlanceWidgetBar()
}
