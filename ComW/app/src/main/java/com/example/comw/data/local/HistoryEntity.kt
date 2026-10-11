package com.example.comw.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Таблица "view_history": история просмотров экрана деталей.
 * Первичный ключ — personId: при повторном открытии того же человека
 * запись не дублируется, а обновляется время viewedAt.
 */
@Entity(tableName = "view_history")
data class HistoryEntity(
    @PrimaryKey val personId: Long,
    val name: String,
    val country: String?,
    val birthday: String?,
    val gender: String?,
    val imageUrl: String?,
    val url: String?,
    val viewedAt: Long
)
