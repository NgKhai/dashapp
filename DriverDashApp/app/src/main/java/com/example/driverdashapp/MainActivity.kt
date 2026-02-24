package com.example.driverdashapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.driverdashapp.data.local.TokenManager
import com.example.driverdashapp.presentation.navigation.DriverNavGraph
import com.example.driverdashapp.presentation.navigation.Screen
import com.example.driverdashapp.ui.theme.DriverDashAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isLoggedIn = runBlocking { tokenManager.isLoggedIn() }

        setContent {
            DriverDashAppTheme {
                val navController = rememberNavController()
                DriverNavGraph(
                    navController = navController,
                    startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                )
            }
        }
    }
}