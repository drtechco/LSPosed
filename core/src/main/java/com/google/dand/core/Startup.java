/*
 * This file is part of DAndroid.
 *
 * DAndroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DAndroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DAndroid.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdDAndroid Contributors
 * Copyright (C) 2021 - 2022 DAndroid Contributors
 */

package com.google.dand.core;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.android.internal.os.ZygoteInit;
import com.google.android.dandroid.DAndroidBridge;
import com.google.android.dandroid.DAndroidInit;
import com.google.dand.deopt.PrebuiltMethodsDeopter;
import com.google.dand.hooker.AttachHooker;
import com.google.dand.hooker.CrashDumpHooker;
import com.google.dand.hooker.HandleSystemServerProcessHooker;
import com.google.dand.hooker.LoadedApkCreateCLHooker;
import com.google.dand.hooker.LoadedApkCtorHooker;
import com.google.dand.hooker.OpenDexFileHooker;
import com.google.dand.impl.DAndroidContext;
import com.google.dand.impl.DAndroidHelper;
import com.google.dand.service.ILSPApplicationService;
import com.google.dand.util.Utils;

import java.util.List;

import dalvik.system.DexFile;

public class Startup {
    private static void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        DAndroidHelper.hookMethod(CrashDumpHooker.class, Thread.class, "dispatchUncaughtException", Throwable.class);
        if (isSystem) {
            DAndroidHelper.hookAllMethods(HandleSystemServerProcessHooker.class, ZygoteInit.class, "handleSystemServerProcess");
        } else {
            DAndroidHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openDexFile");
            DAndroidHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openInMemoryDexFile");
            DAndroidHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openInMemoryDexFiles");
        }
        DAndroidHelper.hookConstructor(LoadedApkCtorHooker.class, LoadedApk.class,
                ActivityThread.class, ApplicationInfo.class, CompatibilityInfo.class,
                ClassLoader.class, boolean.class, boolean.class, boolean.class);
        DAndroidHelper.hookMethod(LoadedApkCreateCLHooker.class, LoadedApk.class, "createOrUpdateClassLoaderLocked", List.class);
        DAndroidHelper.hookAllMethods(AttachHooker.class, ActivityThread.class, "attach");
    }

    public static void bootstrapDAndroid() {
        // Initialize the DAndroid framework
        try {
            startBootstrapHook(DAndroidInit.startsSystemServer);
            DAndroidInit.loadLegacyModules();
        } catch (Throwable t) {
            Utils.logE("error during DAndroid initialization", t);
        }
    }

    public static void initDAndroid(boolean isSystem, String processName, String appDir, ILSPApplicationService service) {
        // init logger
        ApplicationServiceClient.Init(service, processName);
        DAndroidBridge.initXResources();
        DAndroidInit.startsSystemServer = isSystem;
        DAndroidContext.isSystemServer = isSystem;
        DAndroidContext.appDir = appDir;
        DAndroidContext.processName = processName;
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
    }
}
