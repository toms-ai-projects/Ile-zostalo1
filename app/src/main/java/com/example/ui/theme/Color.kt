package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Wartości 1:1 z pliku designu "Ile Zostalo - Finalny design (cieply).dc.html"
// (Claude Design) — nie przybliżenia, tylko dokładne kody hex ze źródła, z jednym
// świadomym odstępstwem: patrz komentarz przy BgLight.

// Tło ekranów aplikacji. Plik designu podaje tu #FFF8F0 dla samych ekranów, ale
// odczucie "za jasno/sterylnie" na żywym telefonie (vs. pamięć designu) skłoniło do
// świadomego przejścia na wyraźniej beżowy odcień — wybrany po porównaniu kilku
// próbek na żywo z aplikacją.
val BgLight = Color(0xFFEFE3D0)
val TextLight = Color(0xFF2B241D)

// Kolor akcentu głównego: FAB, przycisk "Zapisz", aktywne elementy segmentowanych
// przełączników
val AccentOrange = Color(0xFFFF8A5B)

// Drugi, głębszy odcień pomarańczu (rdzawy) — design świadomie używa DWÓCH pomarańczy:
// AccentOrange do głównych CTA, i tego do drugorzędnych akcentów w formularzu (ikony
// pól daty/godziny, obrys przycisku "Wybierz zdjęcie"). Nie mylić z AccentOrange.
val AccentRust = Color(0xFFC4501C)

// Tło pól formularza (input, textarea)
val FormFieldBg = Color(0xFFF6EEE3)
val FormFieldPlaceholder = Color(0xFFB8A995)
val FormLabelColor = Color(0xFF2B241D) // używany z alpha ~0.45 na etykietach sekcji

// Tło przycisków ikon w nagłówkach (okrągłe, subtelne)
val IconButtonBg = Color(0x0F2B241D) // rgba(43,36,29,0.06)

// Pozostawione dla kompatybilności z Material3 ColorScheme (Theme.kt)
val PrimaryDark = Color(0xFF2B241D)
val PrimaryLight = Color(0xFFFFE7D6)

val SurfaceVariant = Color(0xFFF6EEE3)
val OnSurfaceVariant = Color(0xFF8A7A63)
