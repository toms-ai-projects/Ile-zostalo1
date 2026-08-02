package com.example.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.CountdownApplication
import com.example.ui.add.AddEventViewModel
import com.example.ui.detail.DetailViewModel
import com.example.ui.home.HomeViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(countdownApplication().container.eventRepository)
        }
        initializer {
            AddEventViewModel(
                this.createSavedStateHandle(),
                countdownApplication().container.eventRepository
            )
        }
        initializer {
            DetailViewModel(
                this.createSavedStateHandle(),
                countdownApplication().container.eventRepository
            )
        }
    }
}

fun CreationExtras.countdownApplication(): CountdownApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as CountdownApplication)
