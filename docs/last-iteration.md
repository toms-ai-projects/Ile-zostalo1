# Ostatnia iteracja — podmiana ikony aplikacji (2026-08-13)

Zadanie: wprowadzenie gotowych, zatwierdzonych zasobów ikony launchera z
`ile-zostalo-icon.zip` do `app/src/main/res`, bez zmian w kodzie Kotlina i bez
modyfikacji dostarczonych plików XML.

## Usunięte pliki (jedyna nieodwracalna część tej iteracji)
Stare zasoby ikony wygenerowane przez szablon Android Studio, usunięte przed
skopiowaniem nowych:
- `mipmap-anydpi-v26/ic_launcher.xml`
- `mipmap-anydpi-v26/ic_launcher_round.xml`
- `mipmap-hdpi/ic_launcher.webp`, `mipmap-hdpi/ic_launcher_round.webp`
- `mipmap-mdpi/ic_launcher.webp`, `mipmap-mdpi/ic_launcher_round.webp`
- `mipmap-xhdpi/ic_launcher.webp`, `mipmap-xhdpi/ic_launcher_round.webp`
- `mipmap-xxhdpi/ic_launcher.webp`, `mipmap-xxhdpi/ic_launcher_round.webp`
- `mipmap-xxxhdpi/ic_launcher.webp`, `mipmap-xxxhdpi/ic_launcher_round.webp`
- `drawable/ic_launcher_background.xml`
- `drawable/ic_launcher_foreground.xml`

(Odzyskiwalne z historii gita, jeśli kiedykolwiek potrzebne — usunięcie dotyczy
tylko plików roboczych, nie commitów.)

## Dodane / zmienione pliki
- `drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml` —
  nowe warstwy vector (zastępują usunięte, ta sama nazwa).
- `drawable/ic_launcher_monochrome.xml` — nowa warstwa monochrome (Android 13+
  themed icons), poprzednio nie istniała osobno.
- `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` — nowa definicja
  adaptive icon, referencjonuje `@drawable/ic_launcher_monochrome`.
- `mipmap-{h,m,xh,xxh,xxxh}dpi/ic_launcher.png`, `ic_launcher_round.png` — nowe PNG
  (format zmieniony z `.webp` na `.png` — tak dostarczył zestaw ikon; brak `.webp` w
  `mipmap-*dpi` był jednym z kryteriów akceptacji).
- `values/colors.xml` — dodano `ic_launcher_graphite` (`#FF2C2C2A`) i
  `ic_launcher_amber` (`#FFEF9F27`), scalone z dostarczonego `ic_launcher_colors.xml`
  (ten plik następnie usunięty, bo `colors.xml` już istniał w projekcie).

## Pominięte celowo
- `store/play_store_icon_512.png` (z ZIP-a) i osobno wskazany przez użytkownika
  `C:\Users\Admin\Downloads\play_store_icon_512.png` — materiał na listing Google
  Play, poza zakresem repozytorium zgodnie z treścią zadania.
- `AndroidManifest.xml` — `android:icon`/`android:roundIcon` już wskazywały na
  `@mipmap/ic_launcher{,_round}`, więc nie wymagał zmian.

## Wynik builda
`./gradlew assembleDebug` — **BUILD SUCCESSFUL**, brak ostrzeżeń o duplikacie
zasobów.

## Odstępstwa od specyfikacji zadania
- `PROJECT_STATUS.md`, `docs/last-iteration.md`, `docs/next-iteration.md` nie
  istniały wcześniej w repo (stan sesji był dotąd śledzony wyłącznie w pamięci
  roboczej Claude, poza repo) — utworzone od zera tą iteracją.
