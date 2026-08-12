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
