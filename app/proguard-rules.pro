# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in F:\android-sdk/tools/proguard/proguard-android.txt
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

-allowaccessmodification
-dontskipnonpubliclibraryclasses
-dontwarn sun.misc.Unsafe
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn com.squareup.okhttp.**
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder
-keep public interface android.content.pm.IPackageInstallObserver { *; }
-keep class android.content.pm.IPackageInstallObserver$Stub { *; }
-keep public class com.dragons.aurora.InstallerPrivileged$* { *; }
-keep public class com.google.protobuf.ExtensionRegistryLite { *; }
-keep public class com.google.protobuf.ExtensionRegistry { *; }
-keep final class com.google.protobuf.ExtensionRegistryFactory { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keepattributes SourceFile,LineNumberTable
-keepattributes LocalVariableTable, LocalVariableTypeTable
-keepattributes *Annotation*, Signature, Exception, InnerClasses
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable,!method/removal/parameter
-optimizationpasses 5
-renamesourcefileattribute SourceFile
-repackageclasses