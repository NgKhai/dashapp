package com.example.driverdashapp.util

val String.formattedPhone: String
    get() = if (this.startsWith("0")) "+84" + this.drop(1) else this
