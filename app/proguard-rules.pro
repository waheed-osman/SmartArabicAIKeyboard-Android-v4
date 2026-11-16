# Keep Gemini API Retrofit Models
-keep class com.qalamai.keyboard.ai.** { *; }
-keepclassmembers class com.qalamai.keyboard.ai.** { *; }

# Keep Clipboard JSON Models
-keep class com.qalamai.keyboard.data.ClipboardEntry { *; }

# Retrofit & Gson
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson: الحفاظ على معلومات الأنواع العامة (TypeToken) بعد التصغير
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Retrofit مع دوال suspend في Kotlin
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep interface com.qalamai.keyboard.ai.GeminiRetrofitApi { *; }
