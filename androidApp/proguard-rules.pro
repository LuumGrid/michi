# ============================================================
# kotlinx.serialization 1.8.x
# Source: https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** {
    *;
}

# Keep @Serializable classes and their generated helpers under the app namespace
-keep @kotlinx.serialization.Serializable class com.luum.michi.app.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.luum.michi.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.luum.michi.app.core.anilist.dto.** { *; }
-keepclassmembers class com.luum.michi.app.core.anilist.dto.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated $serializer inner classes
-keepnames class **$$serializer
-keepclassmembers class **$$serializer {
    *;
}

-dontwarn kotlinx.serialization.**

# ============================================================
# Ktor 3.x + OkHttp + Okio
# Source: Ktor documentation / community-maintained rules
# ============================================================
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-dontwarn io.ktor.**

-keep class okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class okio.** { *; }
-keepclassmembers class okio.** { *; }
-keep interface okio.** { *; }
-dontwarn okio.**

# ============================================================
# Coil 3.x (io.coil-kt.coil3)
# Source: https://github.com/coil-kt/coil/blob/main/coil-core/src/androidMain/shrinker/shrinker-rules-coil-core.pro
# ============================================================
-keep class coil3.** { *; }
-keepclassmembers class coil3.** { *; }
-keep interface coil3.** { *; }
-dontwarn coil3.**

# ServiceLoader: keep Coil component registries
-keep class * implements coil3.ComponentRegistry { *; }
-keep class * implements coil3.fetch.Fetcher { *; }
-keep class * implements coil3.decode.Decoder { *; }

# ============================================================
# Kotlin coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# Kotlin metadata / reflection
# ============================================================
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**
