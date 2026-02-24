package com.example.driverdashapp.presentation.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object PinInput : Screen("pin_input")
    data object OtpVerify : Screen("otp_verify/{phone}") {
        fun createRoute(phone: String) = "otp_verify/$phone"
    }
    data object SetPin : Screen("set_pin")
    // Main
    data object Home : Screen("home")
    data object Pending : Screen("pending")
    data object ActiveDelivery : Screen("active_delivery/{deliveryId}") {
        fun createRoute(deliveryId: String) = "active_delivery/$deliveryId"
    }
    data object History : Screen("history")
    data object Earnings : Screen("earnings")
    data object Profile : Screen("profile")
}
