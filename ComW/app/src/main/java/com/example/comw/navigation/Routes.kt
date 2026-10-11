package com.example.comw.navigation

sealed class Routes(val route: String) {
    data object List : Routes("people_list")

    data object Detail : Routes("people_detail/{id}") {
        const val ARG_ID = "id"
        fun create(id: Long): String = "people_detail/$id"
    }

    data object History : Routes("history")

    data object My : Routes("my")

    data object Collection : Routes("collection/{collectionId}") {
        const val ARG_ID = "collectionId"
        fun create(id: Long): String = "collection/$id"
    }

    data object Settings : Routes("settings")

    companion object {
        /** Ключ для передачи запроса из истории обратно на главный экран. */
        const val PENDING_QUERY = "pending_query"
    }
}
