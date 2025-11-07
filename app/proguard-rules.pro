# Core app classes
-keep class com.classroom.quizmaster.** { *; }

# Hilt / Dagger generated components
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-dontwarn dagger.hilt.internal.**

# Compose runtime / previews
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Ktor + kotlinx serialization
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keepclassmembers class com.classroom.quizmaster.data.lan.WireMessage$* { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Firebase/AppCheck/AppCompat
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Timber (strip trees only)
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
