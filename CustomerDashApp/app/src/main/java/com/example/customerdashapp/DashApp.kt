package com.example.customerdashapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class DashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = "CustomerDashApp/1.0"
            osmdroidTileCache = cacheDir.resolve("osmdroid")
            load(this@DashApp, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
        }
    }
}
