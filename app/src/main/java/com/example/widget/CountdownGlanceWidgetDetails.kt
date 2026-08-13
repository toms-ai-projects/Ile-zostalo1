package com.example.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.layout.size
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
 * Szósta odmiana widgetu ("Szczegóły", 4x2) — pierścień postępu z lewej (jak
 * CountdownGlanceWidgetRing, ta sama współdzielona buildRingBitmap) plus nazwa/data/
 * przypomnienie z prawej, echo "PRZYPOMNIENIE" z DetailScreen (buildReminderText w
 * WidgetShared.kt) — 1:1 wg makiety dostarczonej przez użytkownika.
 */
class CountdownGlanceWidgetDetails : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)

        provideContent {
            val eventList by database.eventDao().getAllEvents().collectAsState(initial = null)

            val contentModifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFE8DEF8)) // M3 light primary container approx
                .cornerRadius(32.dp)
                .padding(16.dp)

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
            // Ta sama funkcja co Home/Detail/pozostałe widgety — jedno źródło prawdy.
            val progress = activeEvent.progressFraction(currentTime)
            val dateFormatter = remember { SimpleDateFormat("d MMMM, HH:mm", Locale.getDefault()) }
            val dateString = dateFormatter.format(Date(nextTimestamp))
            val reminderText = buildReminderText(activeEvent)

            val hasImage = activeEvent.imageUri.isNotBlank()
            val themeConfig = com.example.ui.theme.EventThemes.getTheme(activeEvent.theme)
            val textColor = if (hasImage) Color.White else themeConfig.textColor
            val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
            val backgroundColor = themeConfig.backgroundColor
            // Te same dwa kolory co pierścień na DetailScreen/CountdownGlanceWidgetRing.
            val trackColor = if (hasImage) Color.White.copy(alpha = 0.3f) else themeConfig.dividerColor
            val ringColor = if (hasImage) Color.White else themeConfig.iconTint
            val isDarkCard = hasImage || backgroundColor.luminance() < 0.5f
            val pillBg = if (isDarkCard) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.6f)

            var bitmap: android.graphics.Bitmap? = null
            if (hasImage) {
                try {
                    bitmap = BitmapFactory.decodeFile(activeEvent.imageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val ringSizeDp = 140
            val ringBitmap = buildRingBitmap(
                sizeDp = ringSizeDp,
                strokeWidthDp = 10,
                progress = progress,
                trackColorArgb = trackColor.toArgb(),
                ringColorArgb = ringColor.toArgb()
            )
            val bellIconBitmap = buildBellIconBitmap(sizeDp = 15, colorArgb = textColor.toArgb())

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

                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            provider = ImageProvider(ringBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(ringSizeDp.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$daysLeft",
                                style = TextStyle(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(textColor)
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = if (isPast) "DNI TEMU" else "DNI",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.width(20.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "NAJBLIŻSZE",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = activeEvent.name,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.glance.unit.ColorProvider(textColor)
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = dateString,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        Row(
                            modifier = GlanceModifier
                                .background(pillBg)
                                .cornerRadius(20.dp)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bitmapa, nie emoji — emoji "🔔" ignoruje kolor tekstu i miał
                            // wbudowany żółty kolor, źle dopasowany do niektórych motywów.
                            Image(
                                provider = ImageProvider(bellIconBitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.size(15.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = reminderText,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = androidx.glance.unit.ColorProvider(textColor)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

class CountdownGlanceWidgetDetailsReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownGlanceWidgetDetails()
}
