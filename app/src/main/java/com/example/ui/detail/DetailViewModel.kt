package com.example.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Event
import com.example.data.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: EventRepository
) : ViewModel() {

    private val eventId: Int = checkNotNull(savedStateHandle["eventId"])

    val uiState: StateFlow<DetailUiState> =
        repository.getEvent(eventId)
            .filterNotNull()
            .map { DetailUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = DetailUiState()
            )

    fun deleteEvent() {
        viewModelScope.launch {
            uiState.value.event?.let {
                repository.delete(it)
            }
        }
    }
}

data class DetailUiState(
    val event: Event? = null
)
