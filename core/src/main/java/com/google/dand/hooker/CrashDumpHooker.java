package com.google.dand.hooker;

import android.util.Log;

import com.google.dand.impl.DAndroidBridgeImpl;
import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.annotations.BeforeInvocation;
import com.google.libdandroid.api.annotations.DAndroidHooker;

@DAndroidHooker
public class CrashDumpHooker implements DAndroidInterface.Hooker {

    @BeforeInvocation
    public static void beforeHookedMethod(DAndroidInterface.BeforeHookCallback callback) {
        try {
            var e = (Throwable) callback.getArgs()[0];
            DAndroidBridgeImpl.log("Crash unexpectedly: " + Log.getStackTraceString(e));
        } catch (Throwable ignored) {
        }
    }
}
