# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\fung.lam\Dropbox\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }
-keep public class com.google.android.gms.**
-dontwarn android.support.v7.**
-dontwarn com.google.android.gms.**
-dontwarn android.support.**
-dontwarn com.github.**
-dontwarn com.squareup.picasso.**
-dontwarn com.etsy.android.grid.**

-keep class org.apache.http.** { *; }
-keep class org.apache.commons.codec.** { *; }
-keep class org.apache.commons.logging.** { *; }
-keep class android.net.compatibility.** { *; }
-keep class android.net.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn android.webkit.**