package com.boancurator.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.boancurator.app.ui.screens.bookmarks.BookmarksRoute
import com.boancurator.app.ui.screens.home.HomeRoute
import com.boancurator.app.ui.screens.profile.ProfileRoute
import com.boancurator.app.ui.screens.search.SearchRoute

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
            HomeRoute()
        }

        composable(Screen.Search.route) {
            SearchRoute()
        }

        composable(Screen.Bookmarks.route) {
            BookmarksRoute()
        }

        composable(Screen.Profile.route) {
            ProfileRoute()
        }
    }
}
