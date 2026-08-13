package com.example.widget

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.action.ActionParameters

/**
 * Stałe współdzielone przez wszystkie odmiany widgetu (CountdownGlanceWidget,
 * CountdownGlanceWidgetPanorama, ...) i WidgetConfigActivity, żeby klucz per-instancji
 * i klucz parametru intencji nie mogły się rozjechać między widgetami.
 */

// Klucz per-instancji widgetu (Glance domyślnie ma stateDefinition =
// PreferencesGlanceStateDefinition, więc to działa "za darmo" bez dodatkowej
// konfiguracji) — WidgetConfigActivity zapisuje tu ID wybranego wydarzenia,
// provideGlance każdego widgetu go odczytuje.
val SELECTED_EVENT_ID_KEY = intPreferencesKey("selected_event_id")

// Nazwa musi być identyczna z kluczem, którego MainActivity nasłuchuje
// (intent.getIntExtra("eventId", -1)) — to ten sam mechanizm co np. otwieranie
// konkretnego wydarzenia z powiadomienia, tylko wyzwolony kliknięciem widgetu.
val EVENT_ID_PARAM = ActionParameters.Key<Int>("eventId")

/**
 * Pierścień postępu jako Bitmapa (android.graphics.Canvas) — Glance/RemoteViews nie ma
 * własnego rysowania łuków jak Compose Canvas, którego używa DetailScreen. Współdzielone
 * przez CountdownGlanceWidgetRing i CountdownGlanceWidgetDetails, żeby oba wyglądały
 * identycznie (ten sam kąt startowy -90°, ta sama grubość).
 */
fun buildRingBitmap(
    sizeDp: Int,
    strokeWidthDp: Int,
    progress: Float,
    trackColorArgb: Int,
    ringColorArgb: Int
): android.graphics.Bitmap {
    // 3x gęstość ekranu (sprawdzone: `wm density` na tym telefonie = 480dpi), wystarczy
    // dla ostrego obrazu bez próby dynamicznego odczytu DisplayMetrics w tym kontekście.
    val scale = 3f
    val sizePx = (sizeDp * scale).toInt()
    val strokeWidthPx = strokeWidthDp * scale
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

/**
 * Ikona dzwonka jako Bitmapa, malowana na jeden kolor (tak samo jak buildRingBitmap) —
 * emoji "🔔" ma WŁASNY, wbudowany kolor (żółty dzwonek) i całkowicie ignoruje kolor tekstu
 * ustawiony na Text, więc na niektórych motywach kart wyglądał źle dopasowany. Prosty
 * kształt: kopuła (górny łuk) + podstawa + serce dzwonka (małe kółko).
 */
fun buildBellIconBitmap(sizeDp: Int, colorArgb: Int): android.graphics.Bitmap {
    val scale = 3f
    val sizePx = (sizeDp * scale).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = colorArgb
        style = android.graphics.Paint.Style.FILL
    }
    val w = sizePx.toFloat()
    val h = sizePx.toFloat()
    val bodyLeft = w * 0.16f
    val bodyRight = w * 0.84f
    val domeTop = h * 0.10f
    val bodyBottom = h * 0.62f
    val cx = w / 2f

    val path = android.graphics.Path()
    path.addArc(android.graphics.RectF(bodyLeft, domeTop, bodyRight, domeTop + (bodyRight - bodyLeft)), 180f, 180f)
    path.lineTo(bodyRight, bodyBottom)
    path.lineTo(bodyLeft, bodyBottom)
    path.close()
    canvas.drawPath(path, paint)
    canvas.drawRect(bodyLeft - w * 0.04f, bodyBottom, bodyRight + w * 0.04f, bodyBottom + h * 0.07f, paint)
    canvas.drawCircle(cx, bodyBottom + h * 0.18f, h * 0.08f, paint)
    return bitmap
}

/**
 * Tekst przypomnienia — ta sama logika co DetailScreen (dni/godz./min. przed), ale z
 * poprawną polską odmianą "1 dzień" / "N dni" (DetailScreen zawsze mówi "dni", nawet dla
 * 1 — nietknięte tu celowo, to osobna sprawa, nie część tego zadania).
 */
fun buildReminderText(event: com.example.data.Event): String {
    val days = event.reminderDays ?: 0
    val hours = event.reminderHours ?: 0
    val minutes = event.reminderMinutes ?: 0
    val hasReminder = days > 0 || hours > 0 || minutes > 0
    if (!hasReminder) return "Brak przypomnienia"
    val parts = mutableListOf<String>()
    if (days > 0) parts.add(if (days == 1) "1 dzień" else "$days dni")
    if (hours > 0) parts.add("$hours godz.")
    if (minutes > 0) parts.add("$minutes min.")
    return "${parts.joinToString(", ")} przed"
}
