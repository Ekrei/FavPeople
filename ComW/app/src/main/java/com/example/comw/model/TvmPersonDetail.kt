package com.example.comw.model

data class TvmPersonDetail(
    val id: Long,
    val name: String,
    val country: String?,
    val birthday: String?,
    val deathday: String?,
    val gender: String?,
    val imageUrl: String?,
    val url: String?,
    val updated: Long?
) {
    fun toTvmPerson(): TvmPerson = TvmPerson(
        id = id,
        name = name,
        country = country,
        birthday = birthday,
        gender = gender,
        imageUrl = imageUrl,
        url = url
    )
}
