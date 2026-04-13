package com.boancurator.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.boancurator.app.navigation.Screen
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextSecondary

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
<<<<<<< Updated upstream
    BottomNavItem(Screen.Home, "피드", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Search, "검색", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.Bookmarks, "저장소", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
=======
    BottomNavItem(Screen.Feed, "피드", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomNavItem(Screen.Articles, "전체기사", Icons.Filled.Article, Icons.Outlined.Article),
    BottomNavItem(Screen.Search, "검색", Icons.Filled.Search, Icons.Outlined.Search),
>>>>>>> Stashed changes
    BottomNavItem(Screen.Profile, "프로필", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextSecondary
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Feed.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Cyan,
                    selectedTextColor = Cyan,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = DarkBackground
                )
            )
        }
    }
}
