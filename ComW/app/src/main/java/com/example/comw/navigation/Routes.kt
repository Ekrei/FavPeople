package com.example.comw.navigation

sealed class Routes(val route: String) {
    data object List : Routes("people_list")

    data object Detail : Routes("people_detail/{id}") {
        const val ARG_ID = "id"
        fun create(id: Long): String = "people_detail/$id"
    }
}