package com.example.customerdashapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object PinInput : Screen("pin_input/{phone}") {
        fun createRoute(phone: String) = "pin_input/$phone"
    }
    object OtpVerify : Screen("otp_verify/{phone}") {
        fun createRoute(phone: String) = "otp_verify/$phone"
    }
    object SetPin : Screen("set_pin")
    object Home : Screen("home")
    object CreateDelivery : Screen("create_delivery")
    object DeliveryDetail : Screen("delivery_detail/{deliveryId}") {
        fun createRoute(deliveryId: String) = "delivery_detail/$deliveryId"
    }
    object DeliveryHistory : Screen("delivery_history")
    object ItemPhoto : Screen("item_photo")
    object MapPicker : Screen("map_picker")
    object Profile : Screen("profile")
}
