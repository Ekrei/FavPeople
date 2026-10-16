package com.example.comw.util

import javax.inject.Inject

/**
 * Источник текущего времени.
 * Вынесен в интерфейс, чтобы в тестах подставлять "фальшивые часы"
 * и получать детерминированный (одинаковый при каждом запуске) результат.
 */
interface TimeProvider {
    fun now(): Long
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
