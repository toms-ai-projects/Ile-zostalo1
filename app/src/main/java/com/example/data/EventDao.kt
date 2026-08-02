package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY targetTimestamp ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY targetTimestamp ASC")
    suspend fun getAllEventsSnapshot(): List<Event>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Flow<Event?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Delete
    suspend fun deleteEvent(event: Event)
}
