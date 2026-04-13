package com.boancurator.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
<<<<<<< Updated upstream
import com.boancurator.app.ui.screens.bookmarks.BookmarksRoute
import com.boancurator.app.ui.screens.home.HomeRoute
import com.boancurator.app.ui.screens.profile.ProfileRoute
import com.boancurator.app.ui.screens.search.SearchRoute
=======
import androidx.navigation.navArgument
import com.boancurator.app.ui.screens.article.ArticleDetailScreen
import com.boancurator.app.ui.screens.feed.FeedScreen
import com.boancurator.app.ui.screens.home.HomeScreen
import com.boancurator.app.ui.screens.profile.ProfileScreen
import com.boancurator.app.ui.screens.search.SearchScreen
import android.util.Base64
>>>>>>> Stashed changes

sealed class Screen(val route: String) {
    data object Feed : Screen("feed")
    data object Articles : Screen("articles")
    data object Search : Screen("search")
    data object Profile : Screen("profile")
    data object ArticleDetail : Screen("article_detail/{articleId}/{urlBase64}") {
        fun createRoute(articleId: Int?, url: String): String {
            val encoded = Base64.encodeToString(url.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
            return "article_detail/${articleId ?: -1}/$encoded"
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Feed.route,
        modifier = modifier
    ) {
<<<<<<< Updated upstream
        composable(Screen.Home.route) {
            HomeRoute()
        }

        composable(Screen.Search.route) {
            SearchRoute()
        }

        composable(Screen.Bookmarks.route) {
            BookmarksRoute()
=======
        composable(Screen.Feed.route) {
            FeedScreen(navController = navController)
        }

        composable(Screen.Articles.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
>>>>>>> Stashed changes
        }

        composable(Screen.Profile.route) {
            ProfileRoute()
        }

        composable(
            route = Screen.ArticleDetail.route,
            arguments = listOf(
                navArgument("articleId") { type = NavType.IntType },
                navArgument("urlBase64") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getInt("articleId") ?: -1
            val encoded = backStackEntry.arguments?.getString("urlBase64") ?: ""
            val url = String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP))
            ArticleDetailScreen(
                articleId = articleId,
                url = url,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
