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
 * Copyright (C) 2022 DAndroid Contributors
 */

package com.google.dand.core;

import android.os.IBinder;
import android.os.Process;

import com.google.dand.BuildConfig;
import com.google.dand.service.ILSPApplicationService;
import com.google.dand.util.ParasiticManagerHooker;
import com.google.dand.util.Utils;

public class Main {

    public static void forkCommon(boolean isSystem, String niceName, String appDir, IBinder binder) {
        Startup.initDAndroid(isSystem, niceName, appDir, ILSPApplicationService.Stub.asInterface(binder));
        if ((niceName.equals(BuildConfig.MANAGER_INJECTED_PKG_NAME) || niceName.equals(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME))
                && ParasiticManagerHooker.start()) {
            Utils.logI("Loaded manager, skipping next steps");
            return;
        }
        Utils.logI("Loading dandroid for " + niceName + "/" + Process.myUid());
        Startup.bootstrapDAndroid();
    }
}
