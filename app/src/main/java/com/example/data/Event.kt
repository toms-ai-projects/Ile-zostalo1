package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrenceType {
    NONE, WEEKLY, MONTHLY, YEARLY
}

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
    // Kiedy wydarzenie zostało utworzone — używane wyłącznie do liczenia paska/pierścienia
    // postępu (ile czasu już minęło względem całego okresu od utworzenia do daty docelowej).
    // Rekordy sprzed tej kolumny dostają wartość targetTimestamp w migracji (patrz
    // AppDatabase.MIGRATION_7_8), co daje bezpieczne "0% minęło" zamiast zgadywania.
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Ułamek [0..1] czasu, jaki upłynął od utworzenia wydarzenia do jego daty docelowej.
     * Używane przez pasek postępu na liście (Home) i pierścień postępu w szczegółach.
     *
     * Uproszczenie: dla wydarzeń cyklicznych liczymy względem oryginalnego
     * [targetTimestamp], nie względem najbliższego wystąpienia z [getNextOccurrence] — po
     * minięciu pierwszego terminu pasek/pierścień po prostu zostaje pełny (100%), zamiast
     * resetować się co cykl. Design nie definiuje semantyki postępu dla cykli, więc to
     * świadomy, bezpieczny wybór (nic się nie psuje, tylko nie "oddaje" cykliczności).
     */
    fun progressFraction(currentTimeMillis: Long = System.currentTimeMillis()): Float {
        val total = (targetTimestamp - createdAt).toFloat()
        if (total <= 0f) return 1f
        return ((currentTimeMillis - createdAt).toFloat() / total).coerceIn(0f, 1f)
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
