package com.example.comw.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Личная заметка и статус пользователя для человека (одна на человека). */
@Entity(tableName = "person_notes")
data class NoteEntity(
    @PrimaryKey val personId: Long,
    val text: String,
    /** Имя элемента enum PersonalStatus (NONE / LIKED / TO_EXPLORE / HIDDEN). */
    val status: String,
    val updatedAt: Long
)

/** Результат JOIN заметки с кэшем людей (для экрана "Моё"). */
data class NoteWithPersonRow(
    val personId: Long,
    val text: String,
    val status: String,
    val updatedAt: Long,
    val personName: String?,
    val personImageUrl: String?
)
