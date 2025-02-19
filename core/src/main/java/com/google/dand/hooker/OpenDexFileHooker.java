package com.google.dand.hooker;

import android.os.Build;

import com.google.dand.impl.DAndroidBridgeImpl;
import com.google.dand.nativebridge.HookBridge;
import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.annotations.AfterInvocation;
import com.google.libdandroid.api.annotations.DAndroidHooker;

@DAndroidHooker
public class OpenDexFileHooker implements DAndroidInterface.Hooker {

    @AfterInvocation
    public static void afterHookedMethod(DAndroidInterface.AfterHookCallback callback) {
        ClassLoader classLoader = null;
        for (var arg : callback.getArgs()) {
            if (arg instanceof ClassLoader) {
                classLoader = (ClassLoader) arg;
            }
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P && classLoader == null) {
            classLoader = DAndroidBridgeImpl.class.getClassLoader();
        }
        while (classLoader != null) {
            if (classLoader == DAndroidBridgeImpl.class.getClassLoader()) {
                HookBridge.setTrusted(callback.getResult());
                return;
            } else {
                classLoader = classLoader.getParent();
            }
        }
    }
}
