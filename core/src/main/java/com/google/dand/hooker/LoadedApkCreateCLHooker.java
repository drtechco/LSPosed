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

import static com.google.dand.core.ApplicationServiceClient.serviceClient;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.android.dandroid.DAndroidBridge;
import com.google.android.dandroid.DAndroidHelpers;
import com.google.android.dandroid.DAndroidInit;
import com.google.android.dandroid.XC_MethodHook;
import com.google.android.dandroid.XC_MethodReplacement;
import com.google.android.dandroid.callbacks.XC_LoadPackage;
import com.google.dand.impl.DAndroidContext;
import com.google.dand.util.Hookers;
import com.google.dand.util.MetaDataReader;
import com.google.dand.util.Utils;
import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.DAndroidModuleInterface;
import com.google.libdandroid.api.annotations.AfterInvocation;
import com.google.libdandroid.api.annotations.DAndroidHooker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressLint("BlockedPrivateApi")
@DAndroidHooker
public class LoadedApkCreateCLHooker implements DAndroidInterface.Hooker {
    private final static Field defaultClassLoaderField;

    private final static Set<LoadedApk> loadedApks = ConcurrentHashMap.newKeySet();

    static {
        Field field = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                field = LoadedApk.class.getDeclaredField("mDefaultClassLoader");
                field.setAccessible(true);
            } catch (Throwable ignored) {
            }
        }
        defaultClassLoaderField = field;
    }

    static void addLoadedApk(LoadedApk loadedApk) {
        loadedApks.add(loadedApk);
    }

    @AfterInvocation
    public static void afterHookedMethod(DAndroidInterface.AfterHookCallback callback) {
        LoadedApk loadedApk = (LoadedApk) callback.getThisObject();

        if (callback.getArgs()[0] != null || !loadedApks.contains(loadedApk)) {
            return;
        }

        try {
            Hookers.logD("LoadedApk#createClassLoader starts");

            String packageName = ActivityThread.currentPackageName();
            String processName = ActivityThread.currentProcessName();
            boolean isFirstPackage = packageName != null && processName != null && packageName.equals(loadedApk.getPackageName());
            if (!isFirstPackage) {
                packageName = loadedApk.getPackageName();
                processName = ActivityThread.currentPackageName();
            } else if (packageName.equals("android")) {
                packageName = "system";
            }

            Object mAppDir = DAndroidHelpers.getObjectField(loadedApk, "mAppDir");
            ClassLoader classLoader = (ClassLoader) DAndroidHelpers.getObjectField(loadedApk, "mClassLoader");
            Hookers.logD("LoadedApk#createClassLoader ends: " + mAppDir + " -> " + classLoader);

            if (classLoader == null) {
                return;
            }

            if (!isFirstPackage && !DAndroidHelpers.getBooleanField(loadedApk, "mIncludeCode")) {
                Hookers.logD("LoadedApk#<init> mIncludeCode == false: " + mAppDir);
                return;
            }

            if (!isFirstPackage && !DAndroidInit.getLoadedModules().getOrDefault(packageName, Optional.of("")).isPresent()) {
                return;
            }

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                    DAndroidBridge.sLoadedPackageCallbacks);
            lpparam.packageName = packageName;
            lpparam.processName = processName;
            lpparam.classLoader = classLoader;
            lpparam.appInfo = loadedApk.getApplicationInfo();
            lpparam.isFirstApplication = isFirstPackage;

            if (isFirstPackage && DAndroidInit.getLoadedModules().getOrDefault(packageName, Optional.empty()).isPresent()) {
                hookNewXSP(lpparam);
            }

            Hookers.logD("Call handleLoadedPackage: packageName=" + lpparam.packageName + " processName=" + lpparam.processName + " isFirstPackage=" + isFirstPackage + " classLoader=" + lpparam.classLoader + " appInfo=" + lpparam.appInfo);
            XC_LoadPackage.callAll(lpparam);

            DAndroidContext.callOnPackageLoaded(new DAndroidModuleInterface.PackageLoadedParam() {
                @NonNull
                @Override
                public String getPackageName() {
                    return loadedApk.getPackageName();
                }

                @NonNull
                @Override
                public ApplicationInfo getApplicationInfo() {
                    return loadedApk.getApplicationInfo();
                }

                @NonNull
                @Override
                public ClassLoader getDefaultClassLoader() {
                    try {
                        return (ClassLoader) defaultClassLoaderField.get(loadedApk);
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @NonNull
                @Override
                public ClassLoader getClassLoader() {
                    return classLoader;
                }

                @Override
                public boolean isFirstPackage() {
                    return isFirstPackage;
                }
            });
        } catch (Throwable t) {
            Hookers.logE("error when hooking LoadedApk#createClassLoader", t);
        } finally {
            loadedApks.remove(loadedApk);
        }
    }

    private static void hookNewXSP(XC_LoadPackage.LoadPackageParam lpparam) {
        int dandroidminversion = -1;
        boolean dandroidsharedprefs = false;
        try {
            Map<String, Object> metaData = MetaDataReader.getMetaData(new File(lpparam.appInfo.sourceDir));
            Object minVersionRaw = metaData.get("dandroidminversion");
            if (minVersionRaw instanceof Integer) {
                dandroidminversion = (Integer) minVersionRaw;
            } else if (minVersionRaw instanceof String) {
                dandroidminversion = MetaDataReader.extractIntPart((String) minVersionRaw);
            }
            dandroidsharedprefs = metaData.containsKey("dandroidsharedprefs");
        } catch (NumberFormatException | IOException e) {
            Hookers.logE("ApkParser fails", e);
        }

        if (dandroidminversion > 92 || dandroidsharedprefs) {
            Utils.logI("New modules detected, hook preferences");
            DAndroidHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "checkMode", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (((int) param.args[0] & 1/*Context.MODE_WORLD_READABLE*/) != 0) {
                        param.setThrowable(null);
                    }
                }
            });
            DAndroidHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "getPreferencesDir", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return new File(serviceClient.getPrefsPath(lpparam.packageName));
                }
            });
        }
    }
}
