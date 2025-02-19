-keep class com.google.libdandroid.** { *; }
-keepclassmembers,allowoptimization class ** implements com.google.libdandroid.api.DAndroidInterface$Hooker {
    public static *** before();
    public static *** before(com.google.libdandroid.api.DAndroidInterface$BeforeHookCallback);
    public static void after();
    public static void after(com.google.libdandroid.api.DAndroidInterface$AfterHookCallback);
    public static void after(com.google.libdandroid.api.DAndroidInterface$AfterHookCallback, ***);
}
