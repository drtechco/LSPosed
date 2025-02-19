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

import android.annotation.SuppressLint;

import com.google.dand.deopt.PrebuiltMethodsDeopter;
import com.google.dand.impl.DAndroidHelper;
import com.google.dand.util.Hookers;
import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.annotations.AfterInvocation;
import com.google.libdandroid.api.annotations.DAndroidHooker;

// system_server initialization
@DAndroidHooker
public class HandleSystemServerProcessHooker implements DAndroidInterface.Hooker {

    public static volatile ClassLoader systemServerCL;

    @SuppressLint("PrivateApi")
    @AfterInvocation
    public static void afterHookedMethod() {
        Hookers.logD("ZygoteInit#handleSystemServerProcess() starts");
        try {
            // get system_server classLoader
            systemServerCL = Thread.currentThread().getContextClassLoader();
            // deopt methods in SYSTEMSERVERCLASSPATH
            PrebuiltMethodsDeopter.deoptSystemServerMethods(systemServerCL);
            var clazz = Class.forName("com.android.server.SystemServer", false, systemServerCL);
            DAndroidHelper.hookAllMethods(StartBootstrapServicesHooker.class, clazz, "startBootstrapServices");
        } catch (Throwable t) {
            Hookers.logE("error when hooking systemMain", t);
        }
    }
}
