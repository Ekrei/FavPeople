package com.example.comw.model

/**
 * Элемент избранного: человек + момент, когда его добавили.
 * По addedAtMillis список избранного сортируется (модификатор варианта MOD_E25_FAV_SORT).
 */
data class FavoriteEntry(
    val person: TvmPerson,
    val addedAtMillis: Long
)
