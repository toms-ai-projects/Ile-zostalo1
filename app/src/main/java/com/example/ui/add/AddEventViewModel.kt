package com.example.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Event
import com.example.data.RecurrenceType
import com.example.data.EventRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddEventViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: EventRepository
) : ViewModel() {

    var eventUiState by mutableStateOf(EventUiState())
        private set

    // eventId can be extracted if editing, but for simplicity let's stick to adding first.
    // We can use the same VM for edit if we extract eventId.
    private val eventId: Int? = savedStateHandle["eventId"]

    init {
        if (eventId != null) {
            viewModelScope.launch {
                val event = repository.getEvent(eventId).filterNotNull().first()
                eventUiState = event.toEventUiState()
            }
        }
    }

    fun updateUiState(eventDetails: EventDetails) {
        eventUiState = EventUiState(eventDetails = eventDetails, isEntryValid = validateInput(eventDetails))
    }

    suspend fun saveEvent(): Long {
        if (validateInput()) {
            if (eventId != null) {
                repository.insert(eventUiState.eventDetails.toEvent().copy(id = eventId))
                return eventId.toLong()
            } else {
                return repository.insert(eventUiState.eventDetails.toEvent())
            }
        }
        return -1L
    }

    suspend fun deleteEvent() {
        if (eventId != null) {
            repository.delete(eventUiState.eventDetails.toEvent().copy(id = eventId))
        }
    }

    private fun validateInput(uiState: EventDetails = eventUiState.eventDetails): Boolean {
        return uiState.name.isNotBlank() && uiState.targetTimestamp > 0L
    }
}

data class EventUiState(
    val eventDetails: EventDetails = EventDetails(),
    val isEntryValid: Boolean = false
)

data class EventDetails(
    val id: Int = 0,
    val name: String = "",
    val targetTimestamp: Long = 0L,
    val colorArgb: Int = 0xFF6200EE.toInt(), // Default color
    val note: String = "",
    val imageUri: String = "",
    val recurrence: String = RecurrenceType.NONE.name,
    val reminderDays: String = "",
    val reminderHours: String = "",
    val reminderMinutes: String = "",
    val theme: String = "Classic"
)

fun EventDetails.toEvent(): Event = Event(
    id = id,
    name = name,
    targetTimestamp = targetTimestamp,
    colorArgb = colorArgb,
    note = note,
    imageUri = imageUri,
    recurrence = recurrence,
    reminderDays = reminderDays.toIntOrNull(),
    reminderHours = reminderHours.toIntOrNull(),
    reminderMinutes = reminderMinutes.toIntOrNull(),
    theme = theme
)

fun Event.toEventUiState(): EventUiState = EventUiState(
    eventDetails = this.toEventDetails(),
    isEntryValid = true
)

fun Event.toEventDetails(): EventDetails = EventDetails(
    id = id,
    name = name,
    targetTimestamp = targetTimestamp,
    colorArgb = colorArgb,
    note = note,
    imageUri = imageUri,
    recurrence = recurrence,
    reminderDays = reminderDays?.toString() ?: "",
    reminderHours = reminderHours?.toString() ?: "",
    reminderMinutes = reminderMinutes?.toString() ?: "",
    theme = theme
)
