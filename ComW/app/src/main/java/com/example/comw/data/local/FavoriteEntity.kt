package com.example.comw.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Таблица "favorites" в Room.
 * Первичный ключ — id человека из TVmaze, поэтому один человек не может
 * оказаться в избранном дважды.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val personId: Long,
    val name: String,
    val country: String?,
    val birthday: String?,
    val gender: String?,
    val imageUrl: String?,
    val url: String?,
    val addedAt: Long
)
