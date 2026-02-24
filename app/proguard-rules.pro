# ─── App Keep Rules (com.cebo.bustracker) ─────────────────────────────────────

# Keep all data/model classes from R8 stripping fields used by Gson
-keepclassmembers class com.cebo.bustracker.** { *; }
-keepclassmembers class com.example.bustrackingapp.** { *; }

# ─── Gson ─────────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# ─── Retrofit ─────────────────────────────────────────────────────────────────
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ─── OkHttp ───────────────────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# ─── Hilt / Dagger ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# ─── Coroutines / Kotlin ─────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ─── OSMDroid ─────────────────────────────────────────────────────────────────
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ─── Socket.IO ────────────────────────────────────────────────────────────────
-keep class io.socket.** { *; }
-dontwarn io.socket.**

# ─── Debug symbols (line numbers for crash reports) ───────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile