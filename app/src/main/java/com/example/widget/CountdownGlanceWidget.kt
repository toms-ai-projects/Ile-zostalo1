package com.example.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.currentState

import androidx.glance.action.actionParametersOf
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
import androidx.glance.layout.width
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.MainActivity
import com.example.data.AppDatabase
import java.util.concurrent.TimeUnit

// Ten "krótki" widget (4x1, karta jak na Home). Druga odmiana ("Panorama", 4x2, więcej
// treści naraz) jest w CountdownGlanceWidgetPanorama.kt — obie współdzielą klucze stanu
// z WidgetShared.kt, żeby wybór wydarzenia i nawigacja po kliknięciu działały tak samo.
class CountdownGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)

        provideContent {
            val eventList by database.eventDao().getAllEvents().collectAsState(initial = null)

            // Kształt/promień 1:1 z EventCard na Home (HomeScreen.kt) — widget ma teraz
            // wyglądać jak ta sama karta, nie jak osobny "zegar" z designu. Stąd 32dp
            // (RoundedCornerShape(32.dp) tam) i 20dp padding (Column tam), zamiast
            // wcześniejszych wartości dobranych pod inny (odrzucony) układ kwadratowy.
            // Bez .clickable() tutaj — doklejany osobno per gałąź niżej, bo kliknięcie w
            // realne wydarzenie ma nieść ze sobą jego ID (żeby otworzyć od razu jego
            // szczegóły), a stany ładowania/pusty nie mają czego przekazać.
            val contentModifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFE8DEF8)) // M3 light primary container approx
                // cornerRadius działa tylko od Androida 12 (S) — poniżej po prostu ostre
                // rogi, nic się nie psuje.
                .cornerRadius(32.dp)
                .padding(20.dp)

            if (eventList == null) {
                // Loading state
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
            } else {
                val currentTime = System.currentTimeMillis()
                // Jeśli użytkownik wybrał konkretne wydarzenie dla TEJ instancji widgetu
                // (WidgetConfigActivity), pokazujemy je — dopóki nadal istnieje (mogło
                // zostać usunięte, wtedy po prostu wracamy do domyślnego "najbliższe").
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
                } else {
                    val nextTimestamp = activeEvent.getNextOccurrence(currentTime)
                    val diffMillis = nextTimestamp - currentTime
                    val isPast = diffMillis < 0
                    val absDiff = Math.abs(diffMillis)
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(absDiff)
                    // Ta sama funkcja co Home/Detail (Event.progressFraction) — jedno
                    // źródło prawdy dla krzywej postępu, żeby widget nigdy nie rozjechał
                    // się z resztą appki.
                    val progress = activeEvent.progressFraction(currentTime)
                    val dateFormatter = remember { SimpleDateFormat("dd MMMM, HH:mm", Locale.getDefault()) }
                    val dateString = dateFormatter.format(Date(nextTimestamp))

                    val hasImage = activeEvent.imageUri.isNotBlank()
                    val themeConfig = com.example.ui.theme.EventThemes.getTheme(activeEvent.theme)
                    val textColor = if (hasImage) Color.White else themeConfig.textColor
                    val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
                    val backgroundColor = themeConfig.backgroundColor
                    val pillTextColor = if (hasImage) Color.White.copy(alpha = 0.85f) else themeConfig.textColor.copy(alpha = 0.6f)
                    val progressTrackColor = if (hasImage) Color.White.copy(alpha = 0.25f) else themeConfig.accentColor.copy(alpha = 0.15f)
                    val progressFillColor = if (hasImage) Color.White else themeConfig.accentColor
                    // Ten sam luminance()-based dobór (nie sztywne sprawdzanie nazwy motywu)
                    // co poprawka pigułki na Home — patrz komentarz tam.
                    val isDarkCard = hasImage || backgroundColor.luminance() < 0.5f
                    val pillBg = if (isDarkCard) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.5f)

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
                            // clipToOutline na kontenerze (contentModifier) nie przycina Image
                            // rysowanego jako osobny widok wewnątrz RemoteViews (na czym
                            // widgety są oparte) — stąd kwadratowe rogi tylko dla zdjęć, gdy
                            // promień był ustawiony wyłącznie na kontenerze. Trzeba dać ten sam
                            // promień bezpośrednio na Image (i nakładkę), nie tylko na kontener.
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.fillMaxSize().cornerRadius(32.dp),
                                contentScale = ContentScale.Crop
                            )
                            // We can't do gradient easily in Glance Box, so we use a solid semi-transparent background
                            Box(modifier = GlanceModifier.fillMaxSize().background(Color(0x80000000)).cornerRadius(32.dp)) {}
                        }

                        // Układ 1:1 z EventCard na Home: nazwa + pigułka dni w jednym rzędzie,
                        // data pod spodem, pasek postępu na dole. Pasek postępu jako 10
                        // równych segmentów (Row.defaultWeight() w Glance rozdziela miejsce
                        // TYLKO po równo, bez wag proporcjonalnych jak w Compose) — najbliższe
                        // podejście do płynnego paska bez wychodzenia poza to, co RemoteViews
                        // faktycznie obsługuje.
                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeEvent.name,
                                    style = TextStyle(
                                        // 17sp/pigułka 16x8dp = dokładnie te same wartości co
                                        // EventCard na Home po jej ostatnim powiększeniu — box
                                        // widgetu jest wyższy niż potrzeba (patrz wcześniejsze
                                        // rozmowa o siatce launchera), więc powiększona treść
                                        // lepiej go wypełnia zamiast ginąć w pustej przestrzeni.
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.glance.unit.ColorProvider(textColor)
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .background(pillBg)
                                        .cornerRadius(24.dp)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isPast) "$daysLeft dni temu" else "$daysLeft dni",
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = androidx.glance.unit.ColorProvider(pillTextColor)
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Text(
                                text = dateString,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = androidx.glance.unit.ColorProvider(secondaryTextColor)
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(16.dp))
                            // Glance Row obsługuje maksymalnie 10 elementów potomnych w
                            // widgetach (nadmiarowe są po prostu ucinane) — 10 segmentów +
                            // odstępy Spacer między nimi to było 19 elementów, więc realnie
                            // renderowało się tylko 5 segmentów. Bez Spacerów, dokładnie 10
                            // segmentów = dokładnie limit, różnica kolorów sama tworzy podział.
                            Row(
                                modifier = GlanceModifier.fillMaxWidth().height(10.dp)
                            ) {
                                val filledSegments = (progress * 10).toInt().coerceIn(0, 10)
                                for (i in 0 until 10) {
                                    Box(
                                        modifier = GlanceModifier
                                            .defaultWeight()
                                            .height(10.dp)
                                            .background(if (i < filledSegments) progressFillColor else progressTrackColor)
                                    ) {}
                                }
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
