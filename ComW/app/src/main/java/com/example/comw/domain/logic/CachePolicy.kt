package com.example.comw.domain.logic

/**
 * Политика актуальности кэша (TTL — time to live, "срок жизни").
 * Чистая функция без Android-зависимостей — легко тестируется.
 */
object CachePolicy {

    /** Люди, на которых ничто не ссылается, удаляются из кэша через неделю. */
    const val PRUNE_AFTER_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

    fun ttlMillis(ttlMinutes: Int): Long = ttlMinutes.coerceAtLeast(0) * 60_000L

    /**
     * @return true, если данные нужно обновить из сети:
     * их никогда не скачивали (fetchedAt == null) или прошло >= TTL.
     */
    fun isStale(fetchedAt: Long?, now: Long, ttlMinutes: Int): Boolean {
        if (fetchedAt == null) return true
        if (fetchedAt > now) return false // часы перевели назад — не считаем устаревшим
        return now - fetchedAt >= ttlMillis(ttlMinutes)
    }

    /** Сколько полных минут назад данные были получены. */
    fun ageMinutes(fetchedAt: Long, now: Long): Long =
        ((now - fetchedAt).coerceAtLeast(0)) / 60_000L
}
