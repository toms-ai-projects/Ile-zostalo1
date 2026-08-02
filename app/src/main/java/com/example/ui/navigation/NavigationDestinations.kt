package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeDestination

@Serializable
object AddEventDestination

@Serializable
data class EditEventDestination(val eventId: Int)

@Serializable
data class DetailDestination(val eventId: Int)
