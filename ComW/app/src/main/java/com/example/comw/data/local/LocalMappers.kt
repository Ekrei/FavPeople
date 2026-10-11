package com.example.comw.data.local

import com.example.comw.model.CachedPerson
import com.example.comw.model.CollectionSummary
import com.example.comw.model.FavoriteEntry
import com.example.comw.model.HistoryEntry
import com.example.comw.model.NoteWithPerson
import com.example.comw.model.PersonNote
import com.example.comw.model.PersonalStatus
import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail

// Преобразования "модель приложения <-> строка таблицы Room".

fun TvmPerson.toFavoriteEntity(addedAt: Long): FavoriteEntity = FavoriteEntity(
    personId = id,
    name = name,
    country = country,
    birthday = birthday,
    gender = gender,
    imageUrl = imageUrl,
    url = url,
    addedAt = addedAt
)

fun FavoriteEntity.toFavoriteEntry(): FavoriteEntry = FavoriteEntry(
    person = TvmPerson(
        id = personId,
        name = name,
        country = country,
        birthday = birthday,
        gender = gender,
        imageUrl = imageUrl,
        url = url
    ),
    addedAtMillis = addedAt
)

fun TvmPerson.toHistoryEntity(viewedAt: Long): HistoryEntity = HistoryEntity(
    personId = id,
    name = name,
    country = country,
    birthday = birthday,
    gender = gender,
    imageUrl = imageUrl,
    url = url,
    viewedAt = viewedAt
)

fun HistoryEntity.toHistoryEntry(): HistoryEntry = HistoryEntry(
    person = TvmPerson(
        id = personId,
        name = name,
        country = country,
        birthday = birthday,
        gender = gender,
        imageUrl = imageUrl,
        url = url
    ),
    viewedAtMillis = viewedAt
)

// ---------- кэш людей ----------

fun PersonEntity.toTvmPerson(): TvmPerson = TvmPerson(
    id = id,
    name = name,
    country = country,
    birthday = birthday,
    gender = gender,
    imageUrl = imageUrl,
    url = url
)

fun PersonEntity.toCachedPerson(): CachedPerson = CachedPerson(
    detail = TvmPersonDetail(
        id = id,
        name = name,
        country = country,
        birthday = birthday,
        deathday = deathday,
        gender = gender,
        imageUrl = imageUrl,
        url = url,
        updated = updated
    ),
    fetchedAt = fetchedAt
)

/**
 * Из краткой модели (например, из результатов поиска) — в строку кэша.
 * Используется с insertIfAbsent, чтобы не затереть более полные данные.
 */
fun TvmPerson.toPersonEntity(fetchedAt: Long): PersonEntity = PersonEntity(
    id = id,
    name = name,
    country = country,
    birthday = birthday,
    deathday = null,
    gender = gender,
    imageUrl = imageUrl,
    url = url,
    updated = null,
    fetchedAt = fetchedAt
)

// ---------- заметки ----------

fun NoteEntity.toPersonNote(): PersonNote = PersonNote(
    personId = personId,
    text = text,
    status = PersonalStatus.fromName(status),
    updatedAt = updatedAt
)

fun NoteWithPersonRow.toNoteWithPerson(): NoteWithPerson = NoteWithPerson(
    note = PersonNote(
        personId = personId,
        text = text,
        status = PersonalStatus.fromName(status),
        updatedAt = updatedAt
    ),
    personName = personName,
    personImageUrl = personImageUrl
)

// ---------- подборки ----------

fun CollectionSummaryRow.toSummary(): CollectionSummary = CollectionSummary(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    itemCount = itemCount
)
