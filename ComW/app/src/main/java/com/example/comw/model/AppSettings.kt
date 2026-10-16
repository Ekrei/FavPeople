package com.example.comw.model

enum class SortOrder(val label: String) {
    API("Как на сервере"),
    NAME("По имени")
}

enum class ThemeMode(val label: String) {
    SYSTEM("Как в системе"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

/**
 * Настройки приложения — хранятся в DataStore (не в Room: это предпочтения, а не данные).
 */
data class AppSettings(
    /** Сколько минут кэш считается свежим (политика актуальности данных). */
    val cacheTtlMinutes: Int = DEFAULT_TTL_MINUTES,
    val backgroundSyncEnabled: Boolean = true,
    val syncOnlyOnWifi: Boolean = false,
    /** Показывать ли людей со статусом "Скрыть из ленты". */
    val showHidden: Boolean = false,
    val sortOrder: SortOrder = SortOrder.API,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Время последней успешной фоновой синхронизации. */
    val lastSyncAt: Long? = null
) {
    companion object {
        const val DEFAULT_TTL_MINUTES = 60
        val TTL_OPTIONS = listOf(15, 60, 360, 1440)
    }
}
