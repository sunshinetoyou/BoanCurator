package com.boancurator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.boancurator.app.navigation.NavGraph
import com.boancurator.app.ui.components.BottomNavBar
import com.boancurator.app.ui.screens.home.HomeViewModel
import com.boancurator.app.ui.theme.BoanCuratorTheme
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.NeonGreen
import com.boancurator.app.ui.theme.TextMuted
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoanCuratorTheme {
                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = hiltViewModel()
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val isReady = !uiState.isLoading && uiState.weekGroups.isNotEmpty()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Main app (behind splash)
                    Scaffold(
                        containerColor = DarkBackground,
                        bottomBar = { BottomNavBar(navController) }
                    ) { innerPadding ->
                        NavGraph(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    // Splash overlay
                    AnimatedVisibility(
                        visible = !isReady,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "BOANCURATOR",
                                    color = NeonGreen,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 3.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "보안 뉴스를 불러오는 중...",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                CircularProgressIndicator(
                                    color = NeonGreen,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
