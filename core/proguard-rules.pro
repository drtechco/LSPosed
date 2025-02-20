-keep class com.google.android.dandroid.** {*;}
-keep class com.google.libdandroid.** {*;}
-keepattributes RuntimeVisibleAnnotations
-keep class android.** { *; }
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keepclassmembers class com.google.dand.impl.DAndroidContext {
    public <methods>;
}
-keepclassmembers class com.google.dand.impl.DAndroidHookCallback {
    public <methods>;
}
-keep,allowoptimization,allowobfuscation @com.google.libdandroid.api.annotations.* class * {
    @com.google.libdandroid.api.annotations.BeforeInvocation <methods>;
    @com.google.libdandroid.api.annotations.AfterInvocation <methods>;
}
-keepclassmembers class com.google.dand.impl.DAndroidBridge$NativeHulker {
    <init>(java.lang.reflect.Executable);
    callback(...);
}
-keepclassmembers class com.google.dand.impl.DAndroidBridge$HookerCallback {
    final *** beforeInvocation;
    final *** afterInvocation;
    HookerCallback(...);
}
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
-repackageclasses
-allowaccessmodification
-dontwarn org.slf4j.impl.StaticLoggerBinder
