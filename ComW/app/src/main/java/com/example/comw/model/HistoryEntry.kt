package com.example.comw.model

/** Запись истории просмотров: кого открывали и когда (последний раз). */
data class HistoryEntry(
    val person: TvmPerson,
    val viewedAtMillis: Long
)
