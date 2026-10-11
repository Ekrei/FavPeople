package com.example.comw.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Кэш людей из TVmaze (offline-first). Отсюда UI берёт данные и списка, и деталей.
 * fetchedAt — когда строка была скачана из сети (для TTL).
 */
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val country: String?,
    val birthday: String?,
    val deathday: String?,
    val gender: String?,
    val imageUrl: String?,
    val url: String?,
    val updated: Long?,
    val fetchedAt: Long
)

/** Состав и порядок главного списка: позиция -> id человека. */
@Entity(tableName = "main_list")
data class MainListEntryEntity(
    @PrimaryKey val position: Int,
    val personId: Long,
    val fetchedAt: Long
)
