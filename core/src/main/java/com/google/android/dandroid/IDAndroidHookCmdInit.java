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

package com.google.android.dandroid;


/**
 * Hook the initialization of Java-based command-line tools (like pm).
 *
 * @hide DAndroid no longer hooks command-line tools, therefore this interface shouldn't be
 * implemented anymore.
 */
public interface IDAndroidHookCmdInit extends IDAndroidMod {
    /**
     * Called very early during startup of a command-line tool.
     *
     * @param startupParam Details about the module itself and the started process.
     * @throws Throwable Everything is caught, but it will prevent further initialization of the module.
     */
    void initCmdApp(StartupParam startupParam) throws Throwable;

    /**
     * Data holder for {@link #initCmdApp}.
     */
    final class StartupParam {
        /*package*/ StartupParam() {
        }

        /**
         * The path to the module's APK.
         */
        public String modulePath;

        /**
         * The class name of the tools that the hook was invoked for.
         */
        public String startClassName;
    }
}
