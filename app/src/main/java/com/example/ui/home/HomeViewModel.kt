package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Event
import com.example.data.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.map

data class HomeUiState(val eventList: List<Event> = listOf())

class HomeViewModel(private val repository: EventRepository) : ViewModel() {

    suspend fun insertEvent(event: Event) {
        repository.insert(event)
    }


    val homeUiState: StateFlow<HomeUiState> =
        repository.allEvents.map { events ->
            val currentTime = System.currentTimeMillis()
            HomeUiState(events.sortedBy { it.getNextOccurrence(currentTime) })
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HomeUiState()
            )
}
