package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrenceType {
    NONE, WEEKLY, MONTHLY, YEARLY
}

private const val DAY_MILLIS = 24 * 60 * 60 * 1000f
// Ile dni przed celem pasek/pierścień postępu pokazuje dokładnie 50% — patrz
// Event.progressFraction().
private const val PROGRESS_CURVE_K_DAYS = 14f

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val targetTimestamp: Long,
    val colorArgb: Int,
    val note: String = "",
    val imageUri: String = "",
    val recurrence: String = RecurrenceType.NONE.name,
    val reminderDays: Int? = null,
    val reminderHours: Int? = null,
    val reminderMinutes: Int? = null,
    val theme: String = "Classic",
    // Kiedy wydarzenie zostało utworzone. Nieużywane przez progressFraction() (patrz
    // komentarz tam) od czasu, gdy pasek/pierścień postępu przeszedł na krzywą czasu
    // pozostałego — zostaje w schemacie jako proste, nieszkodliwe metadane (np. do
    // ewentualnego sortowania "od najnowszych" w przyszłości), nie warto robić kolejnej
    // migracji tylko po to, żeby ją usunąć.
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Ułamek [0..1] "pilności" wydarzenia, używany przez pasek postępu na liście (Home)
     * i pierścień postępu w szczegółach.
     *
     * Pierwsza wersja liczyła to jako czas-od-utworzenia / czas-utworzenie-do-celu, ale to
     * dawało niespójne wyniki dla dwóch wydarzeń z bardzo podobną datą docelową, jeśli
     * dodano je w różnym czasie — użytkownik nie widzi "kiedy dodano", więc różnica
     * wyglądała jak błąd. Teraz to krzywa zależna WYŁĄCZNIE od tego, ile dni zostało do
     * celu: K / (K + dni_pozostałe). Właściwości:
     *  - ta sama data celu → zawsze ten sam wynik, niezależnie od daty utworzenia;
     *  - nigdy nie "zamarza" na 0% dla odległych wydarzeń (w przeciwieństwie do np. sztywnego
     *    30-dniowego okna) — zbliża się do zera asymptotycznie i codziennie się porusza;
     *  - w dniu K (domyślnie 14 dni przed celem) wynosi dokładnie 50%.
     * K dobrane empirycznie jako rozsądny środek — łatwo dostroić, jeśli po dłuższym
     * używaniu appki okaże się za szybkie/wolne.
     *
     * Uproszczenie: dla wydarzeń cyklicznych liczymy względem oryginalnego
     * [targetTimestamp], nie względem najbliższego wystąpienia z [getNextOccurrence] — po
     * minięciu pierwszego terminu pasek/pierścień po prostu zostaje pełny (100%), zamiast
     * resetować się co cykl. Design nie definiuje semantyki postępu dla cykli, więc to
     * świadomy, bezpieczny wybór (nic się nie psuje, tylko nie "oddaje" cykliczności).
     */
    fun progressFraction(currentTimeMillis: Long = System.currentTimeMillis()): Float {
        val daysRemaining = (targetTimestamp - currentTimeMillis).toFloat() / DAY_MILLIS
        if (daysRemaining <= 0f) return 1f
        return (PROGRESS_CURVE_K_DAYS / (PROGRESS_CURVE_K_DAYS + daysRemaining)).coerceIn(0f, 1f)
    }

    fun getNextOccurrence(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val recType = try {
            RecurrenceType.valueOf(recurrence)
        } catch (e: IllegalArgumentException) {
            RecurrenceType.NONE
        }

        if (recType == RecurrenceType.NONE) return targetTimestamp
        if (targetTimestamp > currentTimeMillis) return targetTimestamp

        val originalCal = java.util.Calendar.getInstance().apply { timeInMillis = targetTimestamp }
        val currentCal = java.util.Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

        when (recType) {
            RecurrenceType.WEEKLY -> {
                val diff = currentTimeMillis - targetTimestamp
                val weeks = diff / (7 * 24 * 60 * 60 * 1000L)
                var currentWeeks = weeks.toInt()
                var nextTime = (originalCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.WEEK_OF_YEAR, currentWeeks)
                }
                while (nextTime.timeInMillis <= currentTimeMillis) {
                    currentWeeks++
                    nextTime = (originalCal.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.WEEK_OF_YEAR, currentWeeks)
                    }
                }
                return nextTime.timeInMillis
            }
            RecurrenceType.MONTHLY -> {
                val yearDiff = currentCal.get(java.util.Calendar.YEAR) - originalCal.get(java.util.Calendar.YEAR)
                val monthDiff = currentCal.get(java.util.Calendar.MONTH) - originalCal.get(java.util.Calendar.MONTH)
                var totalMonths = yearDiff * 12 + monthDiff
                if (totalMonths < 0) totalMonths = 0
                
                var nextTime = (originalCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.MONTH, totalMonths)
                }
                while (nextTime.timeInMillis <= currentTimeMillis) {
                    totalMonths++
                    nextTime = (originalCal.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.MONTH, totalMonths)
                    }
                }
                return nextTime.timeInMillis
            }
            RecurrenceType.YEARLY -> {
                val yearDiff = currentCal.get(java.util.Calendar.YEAR) - originalCal.get(java.util.Calendar.YEAR)
                var totalYears = if (yearDiff > 0) yearDiff else 0
                
                var nextTime = (originalCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.YEAR, totalYears)
                }
                while (nextTime.timeInMillis <= currentTimeMillis) {
                    totalYears++
                    nextTime = (originalCal.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.YEAR, totalYears)
                    }
                }
                return nextTime.timeInMillis
            }
            RecurrenceType.NONE -> return targetTimestamp
        }
    }
}
