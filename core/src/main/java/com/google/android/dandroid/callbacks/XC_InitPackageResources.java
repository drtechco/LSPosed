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

import android.content.res.XResources;

import com.google.android.dandroid.IDAndroidHookInitPackageResources;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This class is only used for internal purposes, except for the {@link InitPackageResourcesParam}
 * subclass.
 */
public abstract class XC_InitPackageResources extends XCallback implements IDAndroidHookInitPackageResources {
    /**
     * Creates a new callback with default priority.
     *
     * @hide
     */
    @SuppressWarnings("deprecation")
    public XC_InitPackageResources() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     * @hide
     */
    public XC_InitPackageResources(int priority) {
        super(priority);
    }

    /**
     * Wraps information about the resources being initialized.
     */
    public static final class InitPackageResourcesParam extends Param {
        /**
         * @hide
         */
        public InitPackageResourcesParam(CopyOnWriteArraySet<XC_InitPackageResources> callbacks) {
            super(callbacks.toArray(new XCallback[0]));
        }

        /**
         * The name of the package for which resources are being loaded.
         */
        public String packageName;

        /**
         * Reference to the resources that can be used for calls to
         * {@link XResources#setReplacement(String, String, String, Object)}.
         */
        public XResources res;
    }

    /**
     * @hide
     */
    @Override
    protected void call(Param param) throws Throwable {
        if (param instanceof InitPackageResourcesParam)
            handleInitPackageResources((InitPackageResourcesParam) param);
    }
}
