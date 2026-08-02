package com.example.data
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExportedEvent(
    val name: String,
    val targetTimestamp: Long,
    val recurrence: String
)
