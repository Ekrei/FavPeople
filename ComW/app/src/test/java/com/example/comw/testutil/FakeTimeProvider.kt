package com.example.comw.testutil

import com.example.comw.util.TimeProvider

/** "Часы", которыми управляет тест. */
class FakeTimeProvider(var current: Long = 1_000L) : TimeProvider {
    override fun now(): Long = current
    fun advanceBy(millis: Long) {
        current += millis
    }
}
