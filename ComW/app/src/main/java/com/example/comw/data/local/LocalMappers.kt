package com.example.comw.data.local

import com.example.comw.model.FavoriteEntry
import com.example.comw.model.HistoryEntry
import com.example.comw.model.TvmPerson

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
