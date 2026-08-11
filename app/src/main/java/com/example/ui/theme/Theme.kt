package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// JEDEN, stały schemat kolorów — appka ma własną, zaprojektowaną ciepłą paletę
// (design "Ciepły") i świadomie ignoruje systemowy tryb ciemny telefonu, nie tylko
// dynamic color. Wcześniej istniał osobny "DarkColorScheme" budowany przez
// darkColorScheme(...) z tymi samymi 10 parametrami co poniżej — ale ta funkcja
// wypełnia WSZYSTKIE pozostałe, nie wymienione tu role (surfaceContainer,
// surfaceContainerHigh itd. — używane m.in. jako tło DatePickerDialog/TimePickera/
// rozwijanych list) własnymi, GENUINE ciemnymi domyślnymi kolorami Material3. Gdy
// telefon miał włączony tryb ciemny, dawało to ciemne tło dialogu + nasz ciemny
// TextLight na tekście = praktycznie nieczytelne. Dlatego teraz zawsze budujemy
// przez lightColorScheme(...) i explicit nadpisujemy też role "container"/"outline",
// żeby nic nie wracało do domyślnych (jasnych ANI ciemnych) kolorów Material3.
private val AppColorScheme =
  lightColorScheme(
    primary = PrimaryDark,
    onPrimary = PrimaryLight,
    secondary = PrimaryDark,
    onSecondary = PrimaryLight,
    background = BgLight,
    onBackground = TextLight,
    surface = BgLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    // Tło dialogów (DatePickerDialog, TimePicker-owy AlertDialog) i rozwijanych list
    // (ExposedDropdownMenu) — bez tego wracały do ciemnoszarych domyślnych M3.
    surfaceContainer = FormFieldBg,
    surfaceContainerLow = BgLight,
    surfaceContainerLowest = BgLight,
    surfaceContainerHigh = FormFieldBg,
    surfaceContainerHighest = FormFieldBg,
    outline = OnSurfaceVariant,
    outlineVariant = FormFieldPlaceholder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Świadomie wyłączone: appka ma własną, zaprojektowaną ciepłą paletę kolorów
  // (design "Ciepły" z Claude Design). Dynamic color (Material You) nadpisywałby
  // ją kolorami wyciągniętymi z tapety telefonu użytkownika, co uniemożliwiało
  // dotąd faktyczne wyświetlenie zaprojektowanych kolorów na Androidzie 12+.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
      AppColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
