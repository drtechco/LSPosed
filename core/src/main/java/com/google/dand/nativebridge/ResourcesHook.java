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

package com.google.dand.nativebridge;

import android.content.res.Resources;
import android.content.res.XResources;

import dalvik.annotation.optimization.FastNative;

public class ResourcesHook {

    public static native boolean initXResourcesNative();

    public static native boolean makeInheritable(Class<?> clazz);

    public static native ClassLoader buildDummyClassLoader(ClassLoader parent, String resourceSuperClass, String typedArraySuperClass);

    @FastNative
    public static native void rewriteXmlReferencesNative(long parserPtr, XResources origRes, Resources repRes);
}
