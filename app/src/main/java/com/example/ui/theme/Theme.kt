package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryDark,
    onPrimary = PrimaryLight,
    secondary = PrimaryDark,
    onSecondary = PrimaryLight,
    background = BgLight,
    onBackground = TextLight,
    surface = BgLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant
  )

private val LightColorScheme =
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
    onSurfaceVariant = OnSurfaceVariant
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
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
