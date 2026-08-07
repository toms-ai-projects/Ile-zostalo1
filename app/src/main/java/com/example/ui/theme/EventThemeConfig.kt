package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

data class EventThemeConfig(
    val name: String,
    val backgroundColor: Color,
    // Gradient opcjonalny (używany tylko przez motyw "Nocny") — jeśli null,
    // renderuj płaskie backgroundColor.
    val backgroundGradient: Brush? = null,
    val textColor: Color,
    val secondaryTextColor: Color,
    val labelColor: Color,
    val dividerColor: Color,
    val cardBackgroundColor: Color,
    val iconTint: Color,
    val accentColor: Color,
    val fontFamily: FontFamily = FontFamily.Default,
    val titleFontWeight: FontWeight = FontWeight.Bold,
    val hasDecorativeBorder: Boolean = false
)

object EventThemes {
    val themes = listOf(
        // "Klasyczny" — fiolet, dokładnie #5B21B6 na tle #EFE3FF
        EventThemeConfig(
            name = "Classic",
            backgroundColor = Color(0xFFEFE3FF),
            textColor = Color(0xFF5B21B6),
            secondaryTextColor = Color(0xFF5B21B6).copy(alpha = 0.55f),
            labelColor = Color(0xFF5B21B6).copy(alpha = 0.6f),
            dividerColor = Color(0xFF5B21B6).copy(alpha = 0.15f),
            cardBackgroundColor = Color.White.copy(alpha = 0.5f),
            iconTint = Color(0xFF5B21B6),
            accentColor = Color(0xFF5B21B6),
            fontFamily = QuicksandFontFamily,
            titleFontWeight = FontWeight.SemiBold,
        ),
        // "Elegancki" — Roboto Slab, brąz #7A5B2E na tle #EFE9DC
        EventThemeConfig(
            name = "Elegant",
            backgroundColor = Color(0xFFEFE9DC),
            textColor = Color(0xFF7A5B2E),
            secondaryTextColor = Color(0xFF7A5B2E).copy(alpha = 0.55f),
            labelColor = Color(0xFF7A5B2E).copy(alpha = 0.6f),
            dividerColor = Color(0xFF7A5B2E).copy(alpha = 0.15f),
            cardBackgroundColor = Color.White.copy(alpha = 0.5f),
            iconTint = Color(0xFF7A5B2E),
            accentColor = Color(0xFF7A5B2E),
            fontFamily = RobotoSlabFontFamily,
            titleFontWeight = FontWeight.Normal,
            hasDecorativeBorder = true,
        ),
        // "Ciepły" — pomarańcz #C4501C na tle #FFE7D6
        EventThemeConfig(
            name = "Warm",
            backgroundColor = Color(0xFFFFE7D6),
            textColor = Color(0xFFC4501C),
            secondaryTextColor = Color(0xFFC4501C).copy(alpha = 0.6f),
            labelColor = Color(0xFFC4501C).copy(alpha = 0.6f),
            dividerColor = Color(0xFFC4501C).copy(alpha = 0.15f),
            cardBackgroundColor = Color.White.copy(alpha = 0.55f),
            iconTint = Color(0xFFC4501C),
            accentColor = Color(0xFFC4501C),
            fontFamily = QuicksandFontFamily,
            titleFontWeight = FontWeight.SemiBold,
        ),
        // "Minimalistyczny" — Roboto Mono, szarość #3A3A3A na tle #EDEDED
        EventThemeConfig(
            name = "Minimalist",
            backgroundColor = Color(0xFFEDEDED),
            textColor = Color(0xFF3A3A3A),
            secondaryTextColor = Color(0xFF3A3A3A).copy(alpha = 0.55f),
            labelColor = Color(0xFF3A3A3A).copy(alpha = 0.5f),
            dividerColor = Color(0xFF3A3A3A).copy(alpha = 0.12f),
            cardBackgroundColor = Color.White.copy(alpha = 0.6f),
            iconTint = Color(0xFF3A3A3A),
            accentColor = Color(0xFF3A3A3A),
            fontFamily = RobotoMonoFontFamily,
            titleFontWeight = FontWeight.Medium,
        ),
        // "Nocny" — ciemny gradient granat/indygo z akcentem #8B9CFF.
        // UWAGA: zdefiniowany tu z wyprzedzeniem (Etap 2 designu), ale NIE jest
        // jeszcze podłączony do selektora motywów w formularzu — to celowe,
        // zgodnie z ustaloną kolejnością wdrożenia etapami.
        EventThemeConfig(
            name = "Night",
            backgroundColor = Color(0xFF232946),
            backgroundGradient = Brush.linearGradient(
                colors = listOf(Color(0xFF232946), Color(0xFF12142A)),
            ),
            textColor = Color(0xFFC7D2FE),
            secondaryTextColor = Color(0xFFC7D2FE).copy(alpha = 0.6f),
            labelColor = Color(0xFFC7D2FE).copy(alpha = 0.5f),
            dividerColor = Color(0xFFC7D2FE).copy(alpha = 0.15f),
            cardBackgroundColor = Color.White.copy(alpha = 0.08f),
            iconTint = Color(0xFF8B9CFF),
            accentColor = Color(0xFF8B9CFF),
            fontFamily = QuicksandFontFamily,
            titleFontWeight = FontWeight.SemiBold,
        ),
    )

    fun getTheme(name: String): EventThemeConfig {
        return themes.find { it.name == name } ?: themes.first()
    }
}
