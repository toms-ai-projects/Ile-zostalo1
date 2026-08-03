package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Fonty wbudowane jako zasoby (variable fonts pobrane z oficjalnego repozytorium
// google/fonts: Quicksand [OFL], Roboto Slab [Apache 2.0], Roboto Mono [OFL]),
// zgodnie z dokładną specyfikacją designu "Ciepły" z Claude Design.
// Wagi realizowane przez FontVariation.Settings (działa od API 26+; na starszych
// wersjach Androida system użyje domyślnego wariantu wagi danego fontu — projekt
// ma minSdk 24, więc to tylko kosmetyczna różnica na dwóch najstarszych wersjach).
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val QuicksandFontFamily = FontFamily(
    variableFont(R.font.quicksand_variable, FontWeight.Normal),
    variableFont(R.font.quicksand_variable, FontWeight.Medium),
    variableFont(R.font.quicksand_variable, FontWeight.SemiBold),
    variableFont(R.font.quicksand_variable, FontWeight.Bold),
)

val RobotoSlabFontFamily = FontFamily(
    variableFont(R.font.roboto_slab_variable, FontWeight.Normal),
    variableFont(R.font.roboto_slab_variable, FontWeight.Medium),
)

val RobotoMonoFontFamily = FontFamily(
    variableFont(R.font.roboto_mono_variable, FontWeight.Normal),
    variableFont(R.font.roboto_mono_variable, FontWeight.Medium),
    variableFont(R.font.roboto_mono_variable, FontWeight.Bold),
)

// "Roboto" (tekst pomocniczy, daty) — Roboto to domyślny font systemowy Androida,
// więc korzystamy z FontFamily.Default zamiast dublować kolejny plik czcionki.
val RobotoFontFamily = FontFamily.Default

// Typografia Material 3 — domyślny krój to Quicksand dla nagłówków/liczb,
// zgodnie z designem "Ciepły".
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = QuicksandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 31.sp, // 1.2 * 26sp, zgodnie z designem (font:700 26px/1.2)
        letterSpacing = 0.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = QuicksandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
      ),
  )
