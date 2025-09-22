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

# 保留堆栈跟踪中的行号信息
-keepattributes SourceFile,LineNumberTable

# 隐藏源文件名
-renamesourcefileattribute SourceFile

# 不混淆Room数据库相关类
-keep class com.stu.calender2.data.** { *; }
-keepclassmembers class com.stu.calender2.data.** { *; }

# 不混淆ViewModel类
-keep class com.stu.calender2.viewmodel.** { *; }

# 保留水墨屏优化相关的实用程序类
-keep class com.stu.calender2.utils.EInkDisplayHelper { *; }
-keep class com.stu.calender2.utils.EInkAnimationUtils { *; }

# 保留应用程序类
-keep class com.stu.calender2.MyApplication { *; }

# 保留所有的Fragment和Activity类名称
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# 保留服务类
-keep class com.stu.calender2.service.** { *; }

# 保留反射相关的类和方法
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep class androidx.annotation.Keep

# 保留Navigation组件相关的类
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends androidx.navigation.Navigator

# 保留NavArgs参数
-keepclassmembers class * implements androidx.navigation.NavArgs {
    *;
}

# 保留safe args生成的类
-keep class com.stu.calender2.**.FragmentDirections { *; }
-keep class com.stu.calender2.**.FragmentArgs { *; }

# 针对 R8 全模式的优化（减小应用程序大小）
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# 移除Log语句，减少APK大小
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# 保留原生方法名称
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留Room相关注解
-keepattributes *Annotation*

# 防止混淆重要的系统类
-keep class android.view.** { *; }
-keep class android.os.** { *; }
-keep class android.support.v4.** { *; }
-keep class androidx.** { *; }

# 移除未使用的资源（启用了shrinkResources）
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# 针对水墨屏厂商的特殊API
-keep class android.view.EpdController { *; }
-keep class android.hardware.eink.** { *; }
-keep class android.eink.** { *; }
-keep class com.onyx.android.sdk.** { *; }
-keep class com.boyue.booxapi.** { *; }