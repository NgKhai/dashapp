package com.example.customerdashapp.util

val String.formattedPhone: String
    get() = if (this.startsWith("0")) "+84" + this.drop(1) else this
