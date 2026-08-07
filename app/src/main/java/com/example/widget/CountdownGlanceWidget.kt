package com.example.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius

import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import android.graphics.BitmapFactory
import java.io.File
import com.example.MainActivity
import com.example.data.AppDatabase
import java.util.concurrent.TimeUnit

class CountdownGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)

        provideContent {
            val eventList by database.eventDao().getAllEvents().collectAsState(initial = null)
            
            val contentModifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFE8DEF8)) // M3 light primary container approx
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp)

            if (eventList == null) {
                // Loading state
                Column(
                    modifier = contentModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ładowanie...",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = androidx.glance.unit.ColorProvider(Color(0xFF1D192B)),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                val currentTime = System.currentTimeMillis()
                val activeEvent = eventList!!.filter { it.getNextOccurrence(currentTime) >= currentTime }.minByOrNull { it.getNextOccurrence(currentTime) } ?: eventList!!.firstOrNull()

                if (activeEvent == null) {
                    Column(
                        modifier = contentModifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Dodaj pierwsze wydarzenie",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = androidx.glance.unit.ColorProvider(Color(0xFF1D192B)),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                } else {
                    val nextTimestamp = activeEvent.getNextOccurrence(currentTime)
                    val diffMillis = nextTimestamp - currentTime
                    val isPast = diffMillis < 0
                    val absDiff = Math.abs(diffMillis)
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(absDiff)
                    val hoursLeft = TimeUnit.MILLISECONDS.toHours(absDiff) % 24
                    val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(absDiff) % 60
                    val label = if (isPast) "DNI TEMU" else "POZOSTAŁO DNI"

                    val hasImage = activeEvent.imageUri.isNotBlank()
                    val themeConfig = com.example.ui.theme.EventThemes.getTheme(activeEvent.theme)
                    val textColor = if (hasImage) Color.White else themeConfig.textColor
                    val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
                    // Zawsze kolor motywu — musi być spójne z HomeScreen/DetailScreen,
                    // które od commitu "UI refinements" też już nie używają colorArgb.
                    val backgroundColor = themeConfig.backgroundColor

                    var bitmap: android.graphics.Bitmap? = null
                    if (hasImage) {
                        try {
                            bitmap = BitmapFactory.decodeFile(activeEvent.imageUri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    Box(modifier = contentModifier.background(if (bitmap != null) Color.Transparent else backgroundColor)) {
                        if (bitmap != null) {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // We can't do gradient easily in Glance Box, so we use a solid semi-transparent background
                            Box(modifier = GlanceModifier.fillMaxSize().background(Color(0x80000000))) {}
                        }
                        
                        Column(
                            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = activeEvent.name,
                                style = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.unit.ColorProvider(textColor),
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$daysLeft",
                                    style = TextStyle(
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.glance.unit.ColorProvider(textColor)
                                    )
                                )
                            }
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${hoursLeft}g ${minutesLeft}m",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class CountdownGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownGlanceWidget()
}
