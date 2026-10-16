package com.example.comw.model

/** Итог попытки обновить данные из сети. */
sealed interface RefreshResult {
    /** Данные скачаны и сохранены в Room. */
    data object Updated : RefreshResult

    /** Кэш ещё свежий (TTL не истёк) — в сеть не ходили. */
    data object SkippedFresh : RefreshResult

    /** Сеть недоступна / ошибка сервера. Кэш при этом остаётся доступен. */
    data class Failed(val error: Throwable) : RefreshResult
}

/** Результат поиска: найденные люди и откуда они (сеть или локальный кэш). */
data class SearchResult(
    val items: List<TvmPerson>,
    val fromCache: Boolean
)

/** Отчёт о предзагрузке деталей для офлайна. */
data class PrefetchReport(
    val updated: Int,
    val skippedFresh: Int,
    val failed: Int
)
