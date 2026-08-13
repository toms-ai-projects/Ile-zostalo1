package com.example.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.AppDatabase

/**
 * Czwarta odmiana widgetu ("Pierścień", 2x2) — mini wersja pierścienia postępu z ekranu
 * szczegółów wydarzenia (DetailScreen), świadomie tymi samymi kolorami motywu
 * (dividerColor/iconTint) i tym samym kątem startowym (-90°, godzina 12) co tam, żeby
 * "postęp" znaczył wizualnie to samo w całej appce. Glance/RemoteViews nie ma własnego
 * rysowania łuków (jak Compose Canvas w DetailScreen) — pierścień jest więc renderowany
 * jako zwykła Bitmapa przez android.graphics.Canvas i wyświetlany jako Image.
 */
class CountdownGlanceWidgetRing : GlanceAppWidget() {

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
            val daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(absDiff)
            // Ta sama funkcja co Home/Detail/pozostałe widgety — jedno źródło prawdy.
            val progress = activeEvent.progressFraction(currentTime)

            val hasImage = activeEvent.imageUri.isNotBlank()
            val themeConfig = com.example.ui.theme.EventThemes.getTheme(activeEvent.theme)
            val textColor = if (hasImage) Color.White else themeConfig.textColor
            val backgroundColor = themeConfig.backgroundColor
            // Te same dwa kolory co pierścień na DetailScreen (dividerColor = tor,
            // iconTint = wypełnienie) — stąd wizualna spójność, o którą chodziło w makiecie.
            val trackColor = if (hasImage) Color.White.copy(alpha = 0.3f) else themeConfig.dividerColor
            val ringColor = if (hasImage) Color.White else themeConfig.iconTint

            var bitmap: android.graphics.Bitmap? = null
            if (hasImage) {
                try {
                    bitmap = BitmapFactory.decodeFile(activeEvent.imageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val ringSizeDp = 96
            val ringBitmap = remember_ring_bitmap(
                sizeDp = ringSizeDp,
                progress = progress,
                trackColorArgb = trackColor.toArgb(),
                ringColorArgb = ringColor.toArgb()
            )

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

                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
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
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(textColor)
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = if (isPast) "DNI TEMU" else "DNI",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(textColor)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = activeEvent.name,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.glance.unit.ColorProvider(textColor),
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// Nie ma androidx.compose.runtime.remember w tym pliku (jest w innych widgetach dla
// SimpleDateFormat) — bitmapa pierścienia jest tania w budowie i budowana od nowa przy
// każdym provideGlance, nie ma potrzeby prawdziwego remember{}.
private fun remember_ring_bitmap(
    sizeDp: Int,
    progress: Float,
    trackColorArgb: Int,
    ringColorArgb: Int
): android.graphics.Bitmap {
    // 3x gęstość ekranu (sprawdzone: `wm density` na tym telefonie = 480dpi), wystarczy
    // dla ostrego obrazu bez próby dynamicznego odczytu DisplayMetrics w tym kontekście.
    val scale = 3f
    val sizePx = (sizeDp * scale).toInt()
    val strokeWidthPx = 7.dp.value * scale
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val inset = strokeWidthPx / 2f
    val rectF = android.graphics.RectF(inset, inset, sizePx - inset, sizePx - inset)
    val trackPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = android.graphics.Paint.Cap.ROUND
        color = trackColorArgb
    }
    val progressPaint = android.graphics.Paint(trackPaint).apply { color = ringColorArgb }
    canvas.drawArc(rectF, -90f, 360f, false, trackPaint)
    canvas.drawArc(rectF, -90f, 360f * progress.coerceIn(0f, 1f), false, progressPaint)
    return bitmap
}

class CountdownGlanceWidgetRingReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownGlanceWidgetRing()
}
