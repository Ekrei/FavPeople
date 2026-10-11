package com.example.comw.unit

import com.example.comw.domain.logic.CachePolicy
import com.example.comw.domain.logic.CollectionNameValidator
import com.example.comw.domain.logic.ListFilter
import com.example.comw.domain.logic.PeopleListComposer
import com.example.comw.model.AppSettings
import com.example.comw.model.PersonNote
import com.example.comw.model.PersonalStatus
import com.example.comw.model.SortOrder
import com.example.comw.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Финал: юнит-тесты бизнес-логики (чистые функции, без Android). */
class BusinessLogicTest {

    // ---------- политика актуальности (TTL) ----------
    @Test
    fun `кэш без даты загрузки всегда устаревший`() {
        assertTrue(CachePolicy.isStale(fetchedAt = null, now = 1_000, ttlMinutes = 60))
    }

    @Test
    fun `кэш свежий внутри TTL и устаревший ровно на границе`() {
        val hour = 60 * 60_000L
        assertFalse(CachePolicy.isStale(fetchedAt = 0, now = hour - 1, ttlMinutes = 60))
        assertTrue(CachePolicy.isStale(fetchedAt = 0, now = hour, ttlMinutes = 60))
    }

    @Test
    fun `дата из будущего (часы перевели назад) не считается устаревшей`() {
        assertFalse(CachePolicy.isStale(fetchedAt = 5_000, now = 1_000, ttlMinutes = 0))
        assertEquals(2L, CachePolicy.ageMinutes(fetchedAt = 0, now = 150_000))
    }

    // ---------- правила названия подборки ----------
    @Test
    fun `название подборки - пустое и слишком длинное отклоняются`() {
        assertEquals("Введите название", CollectionNameValidator.validate("   ", emptyList()))
        val long = "a".repeat(CollectionNameValidator.MAX_LENGTH + 1)
        assertEquals("Не длиннее 40 символов", CollectionNameValidator.validate(long, emptyList()))
    }

    @Test
    fun `название подборки - повтор без учёта регистра и пробелов отклоняется`() {
        assertEquals(
            "Подборка «актёры» уже есть",
            CollectionNameValidator.validate("  актёры ", listOf("Актёры"))
        )
        assertNull(CollectionNameValidator.validate("Режиссёры", listOf("Актёры")))
    }

    // ---------- что показывать в списке ----------
    private val people = listOf(TestData.carol, TestData.alice, TestData.bob)

    private fun note(id: Long, status: PersonalStatus, text: String = "") =
        PersonNote(personId = id, text = text, status = status, updatedAt = 0)

    @Test
    fun `скрытые люди не показываются, пока это не включено в настройках`() {
        val notes = mapOf(TestData.bob.id to note(TestData.bob.id, PersonalStatus.HIDDEN))

        val hidden = PeopleListComposer.visibleRows(people, ListFilter.ALL, notes, emptySet(), AppSettings())
        val shown = PeopleListComposer.visibleRows(
            people, ListFilter.ALL, notes, emptySet(), AppSettings(showHidden = true)
        )

        assertEquals(listOf(3L, 1L), hidden.map { it.person.id })
        assertEquals(listOf(3L, 1L, 2L), shown.map { it.person.id })
        assertEquals(PersonalStatus.HIDDEN, shown.last().status)
    }

    @Test
    fun `фильтры по личным данным - Нравится, Изучить, Избранные`() {
        val notes = mapOf(
            1L to note(1, PersonalStatus.LIKED, text = "лучшая роль"),
            3L to note(3, PersonalStatus.TO_EXPLORE)
        )
        val favorites = setOf(2L)

        fun ids(filter: ListFilter) =
            PeopleListComposer.visibleRows(people, filter, notes, favorites, AppSettings()).map { it.person.id }

        assertEquals(listOf(1L), ids(ListFilter.LIKED))
        assertEquals(listOf(3L), ids(ListFilter.TO_EXPLORE))
        assertEquals(listOf(2L), ids(ListFilter.FAVORITES))
        val alice = PeopleListComposer.visibleRows(people, ListFilter.LIKED, notes, favorites, AppSettings()).single()
        assertTrue(alice.hasNote)
        assertFalse(alice.isFavorite)
    }

    @Test
    fun `сортировка из настроек - по имени, дубли id убираются`() {
        val rows = PeopleListComposer.visibleRows(
            items = people + TestData.alice,
            filter = ListFilter.ALL,
            notes = emptyMap(),
            favoriteIds = emptySet(),
            settings = AppSettings(sortOrder = SortOrder.NAME)
        )
        assertEquals(listOf("Alice", "Bob", "Carol"), rows.map { it.person.name })
    }
}
