package com.example.comw.ui.viewmodel

import java.io.IOException

/** Внутренний статус обновления из сети. */
sealed interface RefreshStatus {
    data object Idle : RefreshStatus
    data object Loading : RefreshStatus
    data object Done : RefreshStatus
    data class Failed(val message: String) : RefreshStatus
}

fun Throwable.toUserMessage(): String =
    if (this is IOException) "Нет сети или проблема с подключением"
    else "Не удалось загрузить данные"

const val OFFLINE_NOTICE = "Нет сети — показаны сохранённые данные"
const val OFFLINE_SEARCH_NOTICE = "Нет сети — поиск по сохранённым данным"
