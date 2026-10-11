package com.example.comw.unit

import com.example.comw.data.remote.dto.TvmCountryDto
import com.example.comw.data.remote.dto.TvmImageDto
import com.example.comw.data.remote.dto.TvmPersonDto
import com.example.comw.data.remote.toPersonDetailUiModel
import com.example.comw.data.remote.toPersonUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Тесты преобразования моделей сети (DTO) в модели приложения. */
class MappersTest {

    private fun dto(
        id: Long? = 10,
        name: String? = "Alice",
        image: TvmImageDto? = TvmImageDto(medium = "m.jpg", original = "o.jpg")
    ) = TvmPersonDto(
        id = id,
        url = "u",
        name = name,
        country = TvmCountryDto(name = "Russia", code = "RU", timezone = null),
        birthday = "1990-01-01",
        deathday = null,
        gender = "Female",
        image = image,
        updated = 1L
    )

    @Test
    fun `DTO без id отбрасывается`() {
        assertNull(dto(id = null).toPersonUiModel())
    }

    @Test
    fun `пустое имя заменяется заглушкой, страна берётся из вложенного объекта`() {
        val person = dto(name = "  ").toPersonUiModel()!!
        assertEquals("(No name)", person.name)
        assertEquals("Russia", person.country)
    }

    @Test
    fun `если нет medium-картинки, берётся original`() {
        val detail = dto(image = TvmImageDto(medium = null, original = "o.jpg")).toPersonDetailUiModel()
        assertEquals("o.jpg", detail.imageUrl)
        assertEquals(10L, detail.id)
    }
}
