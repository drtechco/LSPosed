package com.google.dand.hooker;

import android.app.ActivityThread;

import com.google.android.dandroid.DAndroidInit;
import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.annotations.AfterInvocation;
import com.google.libdandroid.api.annotations.DAndroidHooker;

@DAndroidHooker
public class AttachHooker implements DAndroidInterface.Hooker {

    @AfterInvocation
    public static void afterHookedMethod(DAndroidInterface.AfterHookCallback callback) {
        DAndroidInit.loadModules((ActivityThread) callback.getThisObject());
    }
}
