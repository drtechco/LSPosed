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

package com.google.android.dandroid.callbacks;

import android.content.pm.ApplicationInfo;

import com.google.android.dandroid.IDAndroidHookLoadPackage;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This class is only used for internal purposes, except for the {@link LoadPackageParam}
 * subclass.
 */
public abstract class XC_LoadPackage extends XCallback implements IDAndroidHookLoadPackage {
    /**
     * Creates a new callback with default priority.
     *
     * @hide
     */
    @SuppressWarnings("deprecation")
    public XC_LoadPackage() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     * @hide
     */
    public XC_LoadPackage(int priority) {
        super(priority);
    }

    /**
     * Wraps information about the app being loaded.
     */
    public static final class LoadPackageParam extends Param {
        /**
         * @hide
         */
        public LoadPackageParam(CopyOnWriteArraySet<XC_LoadPackage> callbacks) {
            super(callbacks.toArray(new XCallback[0]));
        }

        /**
         * The name of the package being loaded.
         */
        public String packageName;

        /**
         * The process in which the package is executed.
         */
        public String processName;

        /**
         * The ClassLoader used for this package.
         */
        public ClassLoader classLoader;

        /**
         * More information about the application being loaded.
         */
        public ApplicationInfo appInfo;

        /**
         * Set to {@code true} if this is the first (and main) application for this process.
         */
        public boolean isFirstApplication;
    }

    /**
     * @hide
     */
    @Override
    protected void call(Param param) throws Throwable {
        if (param instanceof LoadPackageParam)
            handleLoadPackage((LoadPackageParam) param);
    }
}
