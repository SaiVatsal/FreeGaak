# Keep Hilt-generated components
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Keep Retrofit service interfaces
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Keep Moshi adapters and models
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep class * extends com.squareup.moshi.JsonAdapter
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.saivatsal.soundorbit.core.source.**.model.** { *; }
-keep class com.saivatsal.soundorbit.core.source.**.dto.** { *; }

# Keep Room entities
-keep class com.saivatsal.soundorbit.core.database.entity.** { *; }

# Keep data classes used in JSON serialization
-keep class com.saivatsal.soundorbit.core.model.** { *; }

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
