package com.example.driverdashapp

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
import com.example.driverdashapp.data.local.TokenManager
import com.example.driverdashapp.presentation.navigation.DriverNavGraph
import com.example.driverdashapp.presentation.navigation.Screen
import com.example.driverdashapp.ui.theme.DriverDashAppTheme
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
            DriverDashAppTheme {
                // Check login status asynchronously — no main-thread blocking
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val isLoggedIn = tokenManager.isLoggedIn()
                    startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                }

                if (startDestination != null) {
                    val navController = rememberNavController()
                    DriverNavGraph(
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