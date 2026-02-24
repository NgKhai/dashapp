package com.example.customerdashapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.customerdashapp.data.local.TokenManager
import com.example.customerdashapp.navigation.DashNavGraph
import com.example.customerdashapp.navigation.Screen
import com.example.customerdashapp.ui.theme.CustomerDashAppTheme
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

        // Check if user has a saved session (blocking, but very fast from DataStore)
        val isLoggedIn = runBlocking { tokenManager.isLoggedIn() }
        val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

        setContent {
            CustomerDashAppTheme {
                val navController = rememberNavController()
                DashNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}