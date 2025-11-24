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

# 1. 保持三大组件不被混淆
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# 2. 特别指定你的类（双重保险）
-keep class com.example.myapplication.SmsReceiver { *; }
-keep class com.example.myapplication.KeepAliveService { *; }
-keep class com.example.myapplication.GlobalState { *; }

# 3. 如果用了数据类或JSON解析
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}