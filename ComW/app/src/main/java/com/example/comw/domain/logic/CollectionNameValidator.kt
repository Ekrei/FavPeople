package com.example.comw.domain.logic

/** Правила для названия подборки. Возвращает текст ошибки или null, если всё хорошо. */
object CollectionNameValidator {

    const val MAX_LENGTH = 40

    fun validate(rawName: String, existingNames: Collection<String>): String? {
        val name = rawName.trim()
        return when {
            name.isEmpty() -> "Введите название"
            name.length > MAX_LENGTH -> "Не длиннее $MAX_LENGTH символов"
            existingNames.any { it.trim().equals(name, ignoreCase = true) } ->
                "Подборка «$name» уже есть"
            else -> null
        }
    }
}
