# Keep line numbers for Crashlytics stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# Firebase / Crashlytics
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Play services (transitive)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-dontwarn com.squareup.moshi.**

# Kotlin serialization (used alongside Moshi in places)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# App models deserialized from network
-keep class com.example.f1_kotlin.data.model.** { *; }
-keep class com.example.f1_kotlin.domain.model.** { *; }

# OSMDroid / OSMBonusPack
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-keep class org.osmdroid.bonuspack.** { *; }
-dontwarn org.osmdroid.bonuspack.**

# AppMetrica
-keep class io.appmetrica.** { *; }
-dontwarn io.appmetrica.**

# Coil
-dontwarn coil.**
