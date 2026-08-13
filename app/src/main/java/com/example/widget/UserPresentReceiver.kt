package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * NIE deklarować to w AndroidManifest.xml — ACTION_USER_PRESENT i ACTION_SCREEN_ON są
 * jednymi z niewielu intencji, których Android z założenia NIE dostarcza do odbiorników
 * zadeklarowanych statycznie w manifeście, tylko do zarejestrowanych dynamicznie w
 * działającym procesie (Context.registerReceiver). Sprawdzone na żywo przez logcat: po
 * odblokowaniu telefonu dziesiątki komponentów systemowych reagowały na
 * android.intent.action.USER_PRESENT, ale odbiornik zadeklarowany w manifeście — mimo
 * poprawnej rejestracji widocznej w `dumpsys package` — ani razu się nie odpalił. Efekt:
 * ten mechanizm odświeżania nigdy faktycznie nie działał, dla żadnego z 5 widgetów
 * (włącznie z oryginalnym), więc "działał" tylko przypadkiem — gdy dane wydarzenia się
 * zmieniały (Room Flow) albo po ponownym dodaniu widgetu.
 *
 * Rejestrowany dynamicznie w CountdownApplication.onCreate() — działa tylko dopóki proces
 * appki żyje, więc GŁÓWNYM, gwarantowanym mechanizmem odświeżania jest teraz
 * updatePeriodMillis=1800000 (30 min, praktyczne minimum wymuszane przez Androida) w
 * każdym glance_widget_info*.xml. Ten odbiornik to tylko dodatkowy, "szybszy" refresh na
 * wypadek gdy proces akurat żyje.
 */
class UserPresentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT || intent.action == Intent.ACTION_SCREEN_ON) {
            CoroutineScope(Dispatchers.IO).launch {
                CountdownGlanceWidget().updateAll(context)
                CountdownGlanceWidgetPanorama().updateAll(context)
                CountdownGlanceWidgetBar().updateAll(context)
                CountdownGlanceWidgetRing().updateAll(context)
                CountdownGlanceWidgetList().updateAll(context)
            }
        }
    }
}
