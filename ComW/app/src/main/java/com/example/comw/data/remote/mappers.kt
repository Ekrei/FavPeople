package com.example.comw.data.remote

import com.example.comw.data.local.PersonEntity
import com.example.comw.data.remote.dto.TvmPersonDto
import com.example.comw.data.remote.dto.TvmSearchPeopleItemDto
import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail

fun TvmPersonDto.toPersonUiModel(): TvmPerson? {
    val id = this.id ?: return null
    return TvmPerson(
        id = id,
        name = name?.takeIf { it.isNotBlank() } ?: "(No name)",
        country = country?.name,
        birthday = birthday,
        gender = gender,
        imageUrl = image?.medium ?: image?.original,
        url = url
    )
}

fun TvmPersonDto.toPersonDetailUiModel(): TvmPersonDetail {
    return TvmPersonDetail(
        id = id ?: -1L,
        name = name?.takeIf { it.isNotBlank() } ?: "(No name)",
        country = country?.name,
        birthday = birthday,
        deathday = deathday,
        gender = gender,
        imageUrl = image?.medium ?: image?.original,
        url = url,
        updated = updated
    )
}

fun TvmSearchPeopleItemDto.toPersonUiModel(): TvmPerson? = person?.toPersonUiModel()

/** DTO из сети -> строка кэша Room. Записи без id отбрасываются. */
fun TvmPersonDto.toPersonEntity(fetchedAt: Long): PersonEntity? {
    val id = this.id ?: return null
    return PersonEntity(
        id = id,
        name = name?.takeIf { it.isNotBlank() } ?: "(No name)",
        country = country?.name,
        birthday = birthday,
        deathday = deathday,
        gender = gender,
        imageUrl = image?.medium ?: image?.original,
        url = url,
        updated = updated,
        fetchedAt = fetchedAt
    )
}
