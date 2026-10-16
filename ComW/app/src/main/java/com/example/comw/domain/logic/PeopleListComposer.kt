package com.example.comw.domain.logic

import com.example.comw.model.AppSettings
import com.example.comw.model.PersonNote
import com.example.comw.model.PersonalStatus
import com.example.comw.model.SortOrder
import com.example.comw.model.TvmPerson

/** Фильтры главного экрана. Часть зависит от личных данных пользователя. */
enum class ListFilter(val label: String) {
    ALL("Все"),
    FAVORITES("★ Избранные"),
    LIKED("❤ Нравится"),
    TO_EXPLORE("🔖 Изучить"),
    MALE("Мужчины"),
    FEMALE("Женщины");

    fun matches(person: TvmPerson, note: PersonNote?, favoriteIds: Set<Long>): Boolean = when (this) {
        ALL -> true
        FAVORITES -> person.id in favoriteIds
        LIKED -> note?.status == PersonalStatus.LIKED
        TO_EXPLORE -> note?.status == PersonalStatus.TO_EXPLORE
        MALE -> person.gender.equals("Male", ignoreCase = true)
        FEMALE -> person.gender.equals("Female", ignoreCase = true)
    }
}

/** Строка списка: человек + всё, что пользователь о нём "знает". */
data class PersonRow(
    val person: TvmPerson,
    val isFavorite: Boolean,
    val status: PersonalStatus = PersonalStatus.NONE,
    val hasNote: Boolean = false
)

/**
 * Бизнес-логика главного экрана: как из "сырого" списка людей и личных данных
 * получить то, что видит пользователь. Чистая функция -> покрыта юнит-тестами.
 */
object PeopleListComposer {

    fun visibleRows(
        items: List<TvmPerson>,
        filter: ListFilter,
        notes: Map<Long, PersonNote>,
        favoriteIds: Set<Long>,
        settings: AppSettings
    ): List<PersonRow> {
        val rows = items
            .distinctBy { it.id }
            .filter { person ->
                settings.showHidden || notes[person.id]?.status != PersonalStatus.HIDDEN
            }
            .filter { person -> filter.matches(person, notes[person.id], favoriteIds) }
            .map { person ->
                val note = notes[person.id]
                PersonRow(
                    person = person,
                    isFavorite = person.id in favoriteIds,
                    status = note?.status ?: PersonalStatus.NONE,
                    hasNote = !note?.text.isNullOrBlank()
                )
            }
        return when (settings.sortOrder) {
            SortOrder.API -> rows
            SortOrder.NAME -> rows.sortedBy { it.person.name.lowercase() }
        }
    }
}
