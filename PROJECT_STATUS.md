# Status projektu — Ile zostało

Krótki, zawsze-aktualny stan projektu. Historia iteracji: `docs/last-iteration.md`
(co się właśnie wydarzyło) i `docs/next-iteration.md` (co dalej). Pełna, szczegółowa
historia sesji żyje poza repo, w pamięci roboczej narzędzia (Claude memory).

## Stack
Kotlin, Jetpack Compose, Room, Glance (widgety home-screen). Gradle 9.3.1 / JDK 21.

## Bieżący stan (2026-08-13)
- Aplikacja ma 6 wariantów widgetu home-screen (Karta, Panorama, Pasek, Pierścień,
  Lista, Szczegóły), każdy osobno wybieralny w systemowym pickerze widgetów.
- Ikona aplikacji (launcher, adaptive icon + monochrome layer dla Android 13+) została
  podmieniona na finalną, zatwierdzoną wersję.
- Ekran Home przeprojektowany na "wariant C": duża karta "najbliższe wydarzenie" z
  pierścieniem postępu + oś czasu z resztą wydarzeń (zamiast dawnych 4 równych kart)
  — patrz `docs/last-iteration.md`.
- Build (`./gradlew assembleDebug`) zielony.

## Znane ograniczenia platformy (nie do naprawienia z poziomu kodu aplikacji)
- Na tym telefonie/launcherze (Samsung One UI Home) widget o szerokości 4 kolumn zawsze
  zajmuje 2 wiersze, niezależnie od `targetCellHeight`/`minHeight`.
- `ACTION_USER_PRESENT`/`ACTION_SCREEN_ON` nie docierają do odbiorników zadeklarowanych
  statycznie w manifeście — wymagana dynamiczna rejestracja (`Context.registerReceiver`).
