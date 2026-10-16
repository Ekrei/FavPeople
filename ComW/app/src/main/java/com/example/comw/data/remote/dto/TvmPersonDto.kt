package com.example.comw.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TvmPersonDto(
    @SerializedName("id")
    val id: Long?,

    @SerializedName("url")
    val url: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("country")
    val country: TvmCountryDto?,

    @SerializedName("birthday")
    val birthday: String?,

    @SerializedName("deathday")
    val deathday: String?,

    @SerializedName("gender")
    val gender: String?,

    @SerializedName("image")
    val image: TvmImageDto?,

    @SerializedName("updated")
    val updated: Long?
)

data class TvmCountryDto(
    @SerializedName("name")
    val name: String?,

    @SerializedName("code")
    val code: String?,

    @SerializedName("timezone")
    val timezone: String?
)

data class TvmImageDto(
    @SerializedName("medium")
    val medium: String?,

    @SerializedName("original")
    val original: String?
)

data class TvmSearchPeopleItemDto(
    @SerializedName("score")
    val score: Double?,

    @SerializedName("person")
    val person: TvmPersonDto?
)
