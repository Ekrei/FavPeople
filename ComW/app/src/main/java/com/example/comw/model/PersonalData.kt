package com.example.comw.model

/**
 * Личный статус, который пользователь ставит человеку.
 * Статус ВЛИЯЕТ на приложение: HIDDEN убирает человека из ленты и поиска,
 * LIKED / TO_EXPLORE доступны как фильтры на главном экране.
 */
enum class PersonalStatus(val label: String, val badge: String) {
    NONE("Без статуса", ""),
    LIKED("Нравится", "❤"),
    TO_EXPLORE("Изучить позже", "🔖"),
    HIDDEN("Скрыть из ленты", "🚫");

    companion object {
        fun fromName(name: String?): PersonalStatus =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** Личная заметка + статус для человека. */
data class PersonNote(
    val personId: Long,
    val text: String,
    val status: PersonalStatus,
    val updatedAt: Long
)

/** Заметка вместе с именем человека — для экрана "Моё". */
data class NoteWithPerson(
    val note: PersonNote,
    val personName: String?,
    val personImageUrl: String?
)

/** Подборка (коллекция) пользователя: краткая информация для списка. */
data class CollectionSummary(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val itemCount: Int
)

/** Подборка целиком: описание + люди в ней. */
data class CollectionDetails(
    val id: Long,
    val name: String,
    val description: String,
    val members: List<TvmPerson>
)

/** Детали человека из локального кэша + когда они были получены из сети. */
data class CachedPerson(
    val detail: TvmPersonDetail,
    val fetchedAt: Long
)
