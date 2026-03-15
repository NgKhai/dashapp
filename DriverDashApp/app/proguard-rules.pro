# ────────────────────────────────────────────────────────────────
# ProGuard / R8 rules for DriverDashApp
# ────────────────────────────────────────────────────────────────

# ── Retrofit + OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# ── Gson ──
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }

# ── Keep all DTO / domain model classes (used by Gson reflection) ──
-keep class com.example.driverdashapp.data.remote.dto.** { *; }
-keep class com.example.driverdashapp.domain.model.** { *; }

# ── Hilt ──
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── Supabase / Ktor ──
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# ── Kotlin Serialization ──
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# ── Keep line numbers for crash reports ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile