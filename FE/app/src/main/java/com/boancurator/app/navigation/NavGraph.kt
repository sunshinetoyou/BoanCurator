package com.boancurator.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.boancurator.app.ui.screens.bookmarks.BookmarksScreen
import com.boancurator.app.ui.screens.home.HomeScreen
import com.boancurator.app.ui.screens.profile.ProfileScreen
import com.boancurator.app.ui.screens.search.SearchScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Bookmarks : Screen("bookmarks")
    data object Profile : Screen("profile")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Search.route) {
            SearchScreen()
        }

        composable(Screen.Bookmarks.route) {
            BookmarksScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen()
        }
    }
}
