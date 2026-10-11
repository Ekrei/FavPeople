package com.example.comw.testutil

import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail

/** Готовые тестовые данные, чтобы не повторять их в каждом тесте. */
object TestData {
    fun person(id: Long, name: String = "Person $id") = TvmPerson(
        id = id,
        name = name,
        country = "Russia",
        birthday = "1990-01-01",
        gender = "Male",
        imageUrl = null,
        url = "https://www.tvmaze.com/people/$id"
    )

    fun detail(id: Long, name: String = "Person $id") = TvmPersonDetail(
        id = id,
        name = name,
        country = "Russia",
        birthday = "1990-01-01",
        deathday = null,
        gender = "Male",
        imageUrl = null,
        url = "https://www.tvmaze.com/people/$id",
        updated = 0L
    )

    val alice = person(1, "Alice")
    val bob = person(2, "Bob")
    val carol = person(3, "Carol")
}
