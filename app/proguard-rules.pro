# Keep annotations and metadata used by Hilt/Room/Serialization
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,Record

# Core application & DI entry points
-keep class com.classroom.quizmaster.** { *; }
-dontwarn com.classroom.quizmaster.**
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.internal.**
-keep class javax.inject.** { *; }

# AndroidX lifecycle / ViewModel reflective access
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.DefaultLifecycleObserver
-keep interface androidx.lifecycle.DefaultLifecycleObserver
-keepclassmembers class androidx.lifecycle.ProcessLifecycleOwner { *; }

# Navigation & Compose runtime
-keep class androidx.navigation.NavArgs { *; }
-keep class androidx.navigation.NavDirections { *; }
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }
-keep class androidx.compose.runtime.snapshots.SnapshotStateObserver { *; }
-dontwarn androidx.compose.**

# Room / DataStore / WorkManager
-keep class androidx.room.RoomDatabase_Impl { *; }
-keep class androidx.room.migration.Migration { *; }
-keep class androidx.room.util.** { *; }
-dontwarn androidx.room.**
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep public class * extends androidx.work.ListenableWorker { *; }

# Serialization / coroutines / datetime
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# Ktor networking and protobuf
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# Firebase / Play Services / AppCheck
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Crypto / BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Misc support libs
-dontwarn javax.annotation.**
-dontwarn org.intellij.lang.annotations.**
-dontwarn kotlin.Unit

# Timber logging: strip release logs
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
