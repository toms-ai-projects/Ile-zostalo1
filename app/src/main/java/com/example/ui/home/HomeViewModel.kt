package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Event
import com.example.data.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.map

// featured = najbliższe wydarzenie (pierwsze w tej samej kolejności co dawniej —
// posortowane po getNextOccurrence, bez filtrowania przeterminowanych: jeśli
// najbliższe w czasie wydarzenie już minęło, to ono zostaje wyróżnione, tak jak
// zachowywała się lista przed wprowadzeniem hierarchii "najbliższe/później").
// laterEvents = reszta, w tej samej kolejności, renderowana na osi czasu.
data class HomeUiState(
    val eventList: List<Event> = listOf(),
    val featured: Event? = null,
    val laterEvents: List<Event> = listOf()
)

class HomeViewModel(private val repository: EventRepository) : ViewModel() {

    suspend fun insertEvent(event: Event) {
        repository.insert(event)
    }


    val homeUiState: StateFlow<HomeUiState> =
        repository.allEvents.map { events ->
            val currentTime = System.currentTimeMillis()
            val sorted = events.sortedBy { it.getNextOccurrence(currentTime) }
            HomeUiState(
                eventList = sorted,
                featured = sorted.firstOrNull(),
                laterEvents = sorted.drop(1)
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HomeUiState()
            )
}
