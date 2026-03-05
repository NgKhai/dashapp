package com.example.customerdashapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.customerdashapp.data.local.TokenManager
import com.example.customerdashapp.navigation.DashNavGraph
import com.example.customerdashapp.navigation.Screen
import com.example.customerdashapp.ui.theme.CustomerDashAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CustomerDashAppTheme {
                // Check login status asynchronously — no main-thread blocking
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val isLoggedIn = tokenManager.isLoggedIn()
                    startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                }

                if (startDestination != null) {
                    val navController = rememberNavController()
                    DashNavGraph(
                        navController = navController,
                        startDestination = startDestination!!
                    )
                } else {
                    // Brief splash while checking login status
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}