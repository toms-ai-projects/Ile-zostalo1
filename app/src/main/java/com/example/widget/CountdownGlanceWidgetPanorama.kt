package com.example.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.fillMaxWidth
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
 * Druga, większa odmiana widgetu ("Panorama", 4x2) — obok CountdownGlanceWidget (krótsza
 * karta jak na Home, 4x1). Ta pokazuje więcej naraz: nazwę, datę, godziny/minuty jako
 * osobne kafelki, duży licznik dni z etykietą "DNI" i pasek postępu — 1:1 wg makiety
 * dostarczonej przez użytkownika.
 */
class CountdownGlanceWidgetPanorama : GlanceAppWidget() {

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
                            fontSize = 22.sp,
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
                            fontSize = 22.sp,
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
            // Ta sama funkcja co Home/Detail/drugi widget — jedno źródło prawdy.
            val progress = activeEvent.progressFraction(currentTime)
            val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
            val dateString = dateFormatter.format(Date(nextTimestamp))

            val hasImage = activeEvent.imageUri.isNotBlank()
            val themeConfig = com.example.ui.theme.EventThemes.getTheme(activeEvent.theme)
            val textColor = if (hasImage) Color.White else themeConfig.textColor
            val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
            val backgroundColor = themeConfig.backgroundColor
            val progressTrackColor = if (hasImage) Color.White.copy(alpha = 0.25f) else themeConfig.accentColor.copy(alpha = 0.15f)
            val progressFillColor = if (hasImage) Color.White else themeConfig.accentColor
            // Ten sam luminance()-based dobór co pigułka na Home i "krótki" widget.
            val isDarkCard = hasImage || backgroundColor.luminance() < 0.5f
            val chipBg = if (isDarkCard) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.5f)

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

                // CenterVertically zamiast rozpychania Spacerem(defaultWeight) — na tym
                // launcherze wiersz siatki jest wyższy niż ta zwarta treść potrzebuje
                // (patrz krótszy widget), więc treść ma się skupić w środku, a nie
                // rozjeżdżać do góry/dołu pudełka.
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nazwa + data, samodzielny górny wiersz (pełna szerokość, bez pigułek).
                    Text(
                        text = activeEvent.name,
                        style = TextStyle(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.glance.unit.ColorProvider(textColor)
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = dateString,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Liczba dni i pigułki godz./min. na tej samej wysokości — liczba dni
                    // z lewej, pigułki dosunięte do prawej krawędzi (Spacer z
                    // defaultWeight() zjada resztę miejsca pomiędzy nimi).
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$daysLeft",
                                style = TextStyle(
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(textColor)
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = if (isPast) "DNI TEMU" else "DNI",
                                style = TextStyle(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                                ),
                                modifier = GlanceModifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        TimeChip(
                            value = "$hoursLeft",
                            label = "GODZ.",
                            bg = chipBg,
                            valueColor = textColor,
                            labelColor = secondaryTextColor
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        TimeChip(
                            value = "$minutesLeft",
                            label = "MIN.",
                            bg = chipBg,
                            valueColor = textColor,
                            labelColor = secondaryTextColor
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Row w Glance obsługuje max 10 dzieci w widgetach — dokładnie 10
                    // segmentów bez odstępów (patrz ten sam komentarz w
                    // CountdownGlanceWidget.kt, gdzie dodatkowe Spacery ucinały segmenty).
                    Row(modifier = GlanceModifier.fillMaxWidth().height(8.dp)) {
                        val filledSegments = (progress * 10).toInt().coerceIn(0, 10)
                        for (i in 0 until 10) {
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .height(8.dp)
                                    .background(if (i < filledSegments) progressFillColor else progressTrackColor)
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeChip(value: String, label: String, bg: Color, valueColor: Color, labelColor: Color) {
    Column(
        modifier = GlanceModifier
            .background(bg)
            .cornerRadius(14.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.glance.unit.ColorProvider(valueColor)
            ),
            maxLines = 1
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.glance.unit.ColorProvider(labelColor)
            ),
            maxLines = 1
        )
    }
}

class CountdownGlanceWidgetPanoramaReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownGlanceWidgetPanorama()
}
