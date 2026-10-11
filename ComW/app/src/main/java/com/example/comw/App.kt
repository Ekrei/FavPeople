package com.example.comw

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.comw.navigation.Routes
import com.example.comw.ui.screen.HistoryScreen
import com.example.comw.ui.screen.PeopleDetailScreen
import com.example.comw.ui.screen.PeopleListScreen

@Composable
fun App() {
    val navController = rememberNavController()

    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.List.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.List.route) {
                PeopleListScreen(
                    onOpenDetail = { id ->
                        navController.navigate(Routes.Detail.create(id))
                    },
                    onOpenHistory = { navController.navigate(Routes.History.route) }
                )
            }

            composable(Routes.History.route) {
                HistoryScreen(
                    onOpenDetail = { id -> navController.navigate(Routes.Detail.create(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.Detail.route,
                arguments = listOf(
                    navArgument(Routes.Detail.ARG_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong(Routes.Detail.ARG_ID)
                if (id != null) {
                    PeopleDetailScreen(
                        itemId = id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}