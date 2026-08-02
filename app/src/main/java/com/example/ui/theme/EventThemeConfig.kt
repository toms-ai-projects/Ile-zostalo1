package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

data class EventThemeConfig(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val labelColor: Color,
    val dividerColor: Color,
    val cardBackgroundColor: Color,
    val iconTint: Color,
    val accentColor: Color,
    val fontFamily: FontFamily = FontFamily.Default,
    val titleFontWeight: FontWeight = FontWeight.Bold,
    val hasDecorativeBorder: Boolean = false,
    val hasCornerIcon: Boolean = false
)

object EventThemes {
    val themes = listOf(
        EventThemeConfig(
            name = "Classic",
            backgroundColor = Color.Transparent, // Fallback to user color
            textColor = Color(0xFF001D35),
            secondaryTextColor = Color(0xFF001D35).copy(alpha = 0.6f),
            labelColor = Color(0xFF001D35).copy(alpha = 0.5f),
            dividerColor = Color(0xFF001D35).copy(alpha = 0.1f),
            cardBackgroundColor = Color.White.copy(alpha = 0.4f),
            iconTint = Color(0xFF001D35),
            accentColor = Color(0xFF001D35)
        ),
        EventThemeConfig(
            name = "Elegant",
            backgroundColor = Color(0xFF1C2025),
            textColor = Color(0xFFD4C19C),
            secondaryTextColor = Color(0xFFD4C19C).copy(alpha = 0.7f),
            labelColor = Color(0xFFD4C19C).copy(alpha = 0.6f),
            dividerColor = Color(0xFFD4C19C).copy(alpha = 0.2f),
            cardBackgroundColor = Color(0xFF2A2E35).copy(alpha = 0.6f),
            iconTint = Color(0xFFD4C19C),
            accentColor = Color(0xFFD4C19C),
            fontFamily = FontFamily.Serif,
            titleFontWeight = FontWeight.Normal,
            hasDecorativeBorder = true
        ),
        EventThemeConfig(
            name = "Warm",
            backgroundColor = Color(0xFFFFF3E0),
            textColor = Color(0xFFE65100),
            secondaryTextColor = Color(0xFFE65100).copy(alpha = 0.8f),
            labelColor = Color(0xFFE65100).copy(alpha = 0.6f),
            dividerColor = Color(0xFFE65100).copy(alpha = 0.2f),
            cardBackgroundColor = Color.White.copy(alpha = 0.7f),
            iconTint = Color(0xFFE65100),
            accentColor = Color(0xFFE65100),
            titleFontWeight = FontWeight.Bold,
            hasCornerIcon = true
        ),
        EventThemeConfig(
            name = "Minimalist",
            backgroundColor = Color(0xFFF5F5F5),
            textColor = Color.Black,
            secondaryTextColor = Color.Black.copy(alpha = 0.6f),
            labelColor = Color.Black.copy(alpha = 0.5f),
            dividerColor = Color.Black.copy(alpha = 0.1f),
            cardBackgroundColor = Color.White,
            iconTint = Color.Black,
            accentColor = Color.Black,
            fontFamily = FontFamily.Monospace,
            titleFontWeight = FontWeight.Medium
        )
    )

    fun getTheme(name: String): EventThemeConfig {
        return themes.find { it.name == name } ?: themes.first()
    }
}
