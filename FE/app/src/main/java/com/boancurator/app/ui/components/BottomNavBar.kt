package com.boancurator.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
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
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.NeonGreen
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextSecondary

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "홈", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Search, "검색", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.Bookmarks, "북마크", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
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
                            popUpTo(Screen.Home.route) { saveState = true }
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
                    selectedIconColor = NeonGreen,
                    selectedTextColor = NeonGreen,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = DarkBackground
                )
            )
        }
    }
}
