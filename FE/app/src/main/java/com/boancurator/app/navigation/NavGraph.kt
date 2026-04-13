package com.boancurator.app.navigation

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.boancurator.app.ui.screens.article.ArticleDetailScreen
import com.boancurator.app.ui.screens.feed.FeedScreen
import com.boancurator.app.ui.screens.home.HomeScreen
import com.boancurator.app.ui.screens.profile.ProfileRoute
import com.boancurator.app.ui.screens.search.SearchScreen

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
        composable(Screen.Feed.route) {
            FeedScreen(navController = navController)
        }

        composable(Screen.Articles.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
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
