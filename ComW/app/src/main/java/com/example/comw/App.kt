package com.example.comw

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.comw.navigation.Routes
import com.example.comw.ui.screen.CollectionScreen
import com.example.comw.ui.screen.HistoryScreen
import com.example.comw.ui.screen.MyScreen
import com.example.comw.ui.screen.PeopleDetailScreen
import com.example.comw.ui.screen.PeopleListScreen
import com.example.comw.ui.screen.SettingsScreen

@Composable
fun App() {
    val navController = rememberNavController()
    val openDetail: (Long) -> Unit = { id -> navController.navigate(Routes.Detail.create(id)) }
    val back: () -> Unit = { navController.popBackStack() }

    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.List.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.List.route) { entry ->
                val pendingQuery by entry.savedStateHandle
                    .getStateFlow<String?>(Routes.PENDING_QUERY, null)
                    .collectAsState()
                PeopleListScreen(
                    onOpenDetail = openDetail,
                    onOpenHistory = { navController.navigate(Routes.History.route) },
                    onOpenMy = { navController.navigate(Routes.My.route) },
                    onOpenSettings = { navController.navigate(Routes.Settings.route) },
                    pendingQuery = pendingQuery,
                    onPendingQueryConsumed = { entry.savedStateHandle.set<String?>(Routes.PENDING_QUERY, null) }
                )
            }

            composable(
                route = Routes.Detail.route,
                arguments = listOf(navArgument(Routes.Detail.ARG_ID) { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong(Routes.Detail.ARG_ID)
                if (id != null) {
                    PeopleDetailScreen(itemId = id, onBack = back)
                }
            }

            composable(Routes.History.route) {
                HistoryScreen(
                    onOpenDetail = openDetail,
                    onSearchClick = { query ->
                        // передаём запрос на главный экран и возвращаемся туда
                        navController.previousBackStackEntry?.savedStateHandle?.set(Routes.PENDING_QUERY, query)
                        navController.popBackStack()
                    },
                    onBack = back
                )
            }

            composable(Routes.My.route) {
                MyScreen(
                    onBack = back,
                    onOpenCollection = { id -> navController.navigate(Routes.Collection.create(id)) },
                    onOpenDetail = openDetail
                )
            }

            composable(
                route = Routes.Collection.route,
                arguments = listOf(navArgument(Routes.Collection.ARG_ID) { type = NavType.LongType })
            ) {
                CollectionScreen(onBack = back, onOpenDetail = openDetail)
            }

            composable(Routes.Settings.route) {
                SettingsScreen(onBack = back)
            }
        }
    }
}
