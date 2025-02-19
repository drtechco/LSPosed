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
 * Copyright (C) 2021 DAndroid Contributors
 */

package com.google.dand.hooker;

import static com.google.dand.util.Utils.logD;

import androidx.annotation.NonNull;

import com.google.android.dandroid.DAndroidBridge;
import com.google.android.dandroid.DAndroidInit;
import com.google.android.dandroid.callbacks.XC_LoadPackage;
import com.google.dand.impl.DAndroidContext;
import com.google.dand.util.Hookers;
import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.DAndroidModuleInterface;
import com.google.libdandroid.api.annotations.BeforeInvocation;
import com.google.libdandroid.api.annotations.DAndroidHooker;

@DAndroidHooker
public class StartBootstrapServicesHooker implements DAndroidInterface.Hooker {

    @BeforeInvocation
    public static void beforeHookedMethod() {
        logD("SystemServer#startBootstrapServices() starts");

        try {
            DAndroidInit.loadedPackagesInProcess.add("android");

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(DAndroidBridge.sLoadedPackageCallbacks);
            lpparam.packageName = "android";
            lpparam.processName = "android"; // it's actually system_server, but other functions return this as well
            lpparam.classLoader = HandleSystemServerProcessHooker.systemServerCL;
            lpparam.appInfo = null;
            lpparam.isFirstApplication = true;
            XC_LoadPackage.callAll(lpparam);

            DAndroidContext.callOnSystemServerLoaded(new DAndroidModuleInterface.SystemServerLoadedParam() {
                @Override
                @NonNull
                public ClassLoader getClassLoader() {
                    return HandleSystemServerProcessHooker.systemServerCL;
                }
            });
        } catch (Throwable t) {
            Hookers.logE("error when hooking startBootstrapServices", t);
        }
    }
}
