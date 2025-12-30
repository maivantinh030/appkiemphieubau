# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.example.ungdungkiemphieu.data.model.LoginResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.SignupResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.UserInfoResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.PollResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.JoinPollResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.PollDetailResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.CandidateStatsResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.UploadResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.RequestRoleUpgradeResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.ApproveRoleResponse { *; }

# Request models - dùng JSON serialization
-keep class com.example.ungdungkiemphieu.data.model.LoginRequest { *; }
-keep class com.example.ungdungkiemphieu.data.model.SignupRequest { *; }
-keep class com.example.ungdungkiemphieu.data.model.JoinPollRequest { *; }
-keep class com.example.ungdungkiemphieu.data.model.RequestRoleUpgradeRequest { *; }
-keep class com.example.ungdungkiemphieu.data.model.RequestRoleBody { *; }
-keep class com.example.ungdungkiemphieu.data.model.ApproveRoleBody { *; }

# Nested models trong response/request
-keep class com.example.ungdungkiemphieu.data.model.User { *; }
-keep class com.example.ungdungkiemphieu.data.model.SignupUser { *; }
-keep class com.example.ungdungkiemphieu.data.model.UserInfo { *; }
-keep class com.example.ungdungkiemphieu.data.model.Poll { *; }
-keep class com.example.ungdungkiemphieu.data.model.PollStats { *; }
-keep class com.example.ungdungkiemphieu.data.model.Memberships { *; }
-keep class com.example.ungdungkiemphieu.data.model.MemberInfo { *; }
-keep class com.example.ungdungkiemphieu.data.model.CandidateResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.candidatestats { *; }
-keep class com.example.ungdungkiemphieu.data.model.UploadResult { *; }
-keep class com.example.ungdungkiemphieu.data.model.RequestRoleResponse { *; }
-keep class com.example.ungdungkiemphieu.data.model.MyPollMembershipResponse{*;}

-keep class *Request { *; }
-keep class *Response { *; }

-keep class org.opencv.dnn.** { *; }
-keep class org.opencv.objdetect.** { *; }

# API Service interface - Retrofit cần reflection
-keep interface com.example.ungdungkiemphieu.data.network.ApiService { *; }

# Retrofit annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }


# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Gson serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable

# Retrofit + Kotlin coroutines: keep continuation metadata so Retrofit can read generic types
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.**

# Keep Retrofit coroutine extensions
-keepclassmembers class retrofit2.KotlinExtensions { *; }
-keepclassmembers class retrofit2.KotlinExtensions$* { *; }

-keep class com.example.test.data.model.** { *; }

# Suppress warnings from test/annotation dependencies (safe to ignore)
-dontwarn androidx.concurrent.futures.**
-dontwarn javax.lang.model.**
-dontwarn com.google.errorprone.**
-dontwarn androidx.test.**
-dontwarn org.checkerframework.**

-dontwarn androidx.concurrent.futures.SuspendToFutureAdapter
-dontwarn javax.lang.model.element.Modifier



# Giữ nguyên data models (cần cho JSON serialization)
-keep class com.example.ungdungkiemphieu.data.model.** { *; }

# Giữ API Service interface
-keep interface com.example.ungdungkiemphieu.data.network.ApiService { *; }

# Obfuscate tất cả các class khác
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OpenCV (quan trọng cho xử lý ảnh)
-keep class org.opencv.** { *; }
-keep class org.opencv.dnn.** { *; }
-keep class org.opencv.objdetect.** { *; }

# Loại bỏ logging trong production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

