# Ostatnia iteracja — Home: wariant C, oś czasu z wyróżnionym wydarzeniem (2026-08-13)

Zadanie: przeprojektowanie ekranu Home z układu czterech równorzędnych kart na
hierarchię "najbliższe wydarzenie + oś czasu z resztą", wg makiety wklejonej przez
użytkownika ("Home — wariant C").

## Ustalenia poprzedzające implementację (przez AskUserQuestion)
- **Zakres:** wariant C **zastępuje** dotychczasowy Home całkowicie (nie eksperyment obok).
- **Przeterminowane wydarzenia:** zachowanie **bez zmian** względem starego Home — brak
  dodatkowego filtrowania, mogą zostać "najbliższe" albo trafić na oś z "X dni temu".
- **Pigułka przypomnienia na karcie "najbliższe":** **ukryta**, gdy wydarzenie nie ma
  ustawionego przypomnienia (nie pokazujemy "Brak przypomnienia" jak na DetailScreen).
- **Gramatyka tekstu przypomnienia:** naprawiona **wszędzie** (Home + DetailScreen +
  widgety), nie tylko na nowym Home.

## Dodane / zmienione pliki
- `data/Event.kt` — nowa `Event.reminderText()`, jedno źródło prawdy dla tekstu
  pigułki przypomnienia (dawniej trzy kopie tej samej logiki: DetailScreen miała błąd
  gramatyczny "1 dni", widgety miały już poprawkę "1 dzień").
- `ui/detail/DetailScreen.kt` — używa teraz `event.reminderText()` zamiast własnej,
  wadliwej logiki inline.
- `widget/WidgetShared.kt` — `buildReminderText()` to teraz cienka nakładka nad
  `Event.reminderText()` (zero zmian w zachowaniu widgetów, tylko deduplikacja).
- `ui/home/HomeViewModel.kt` — `HomeUiState` rozbity na `featured: Event?` +
  `laterEvents: List<Event>`, ta sama kolejność sortowania co dawniej (bez filtrowania).
- `ui/home/HomeScreen.kt` — przepisany: `FeaturedEventCard` (pierścień postępu
  Canvas/`drawArc`, nazwa/data, opcjonalna pigułka przypomnienia) + `TimelineSection`/
  `TimelineEventRow` (pionowa oś z ciągłą linią i kropką w `accentColor` motywu
  wydarzenia). Stary `EventCard` (4 równe bloki) usunięty — nic innego go nie używało.

## Weryfikacja na urządzeniu
Zainstalowane przez `adb`/`gradlew installDebug`, sprawdzone zrzutami ekranu: górna i
dolna część listy Home (7 testowych wydarzeń, różne motywy — kropki i tła kart
poprawnie kolorowane), nawigacja z karty "najbliższe" do Detail, poprawiona gramatyka
pigułki na Detail ("1 dzień przed" zamiast dawnego "1 dni przed").

## Wynik builda
`./gradlew assembleDebug` / `compileDebugKotlin` — **BUILD SUCCESSFUL**.

## Brak nieodwracalnych operacji
Ta iteracja to wyłącznie zmiany w kodzie Kotlina (żadnych usuniętych zasobów binarnych
jak przy poprzedniej iteracji — ikona launchera).
