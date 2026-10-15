# ================================================================
# RemixLab - ProGuard Rules
# ================================================================

# حفظ انوتیشنها
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Kotlin Metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# Kotlin Serialization
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# Media3 / ExoPlayer
-dontwarn androidx.media3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# LAME
-keep class com.naman14.androidlame.** { *; }

# Coil
-dontwarn coil.**

# Timber
-dontwarn org.jetbrains.annotations.**

# مدلهای اپ
-keep class com.remixlab.app.model.** { *; }
