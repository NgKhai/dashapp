package com.example.driverdashapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class DriverDashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize osmdroid configuration BEFORE any MapView is created
        Configuration.getInstance().apply {
            userAgentValue = "DriverDashApp/1.0"
            osmdroidTileCache = cacheDir.resolve("osmdroid")
            load(this@DriverDashApp, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
        }
    }
}
