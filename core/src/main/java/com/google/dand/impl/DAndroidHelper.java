package com.google.dand.impl;

import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.errors.HookFailedError;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class DAndroidHelper {

    @SuppressWarnings("UnusedReturnValue")
    public static <T> DAndroidInterface.MethodUnhooker<Method>
    hookMethod(Class<? extends DAndroidInterface.Hooker> hooker, Class<T> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            var method = clazz.getDeclaredMethod(methodName, parameterTypes);
            return DAndroidBridgeImpl.doHook(method, DAndroidInterface.PRIORITY_DEFAULT, hooker);
        } catch (NoSuchMethodException e) {
            throw new HookFailedError(e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <T> Set<DAndroidInterface.MethodUnhooker<Method>>
    hookAllMethods(Class<? extends DAndroidInterface.Hooker> hooker, Class<T> clazz, String methodName) {
        var unhooks = new HashSet<DAndroidInterface.MethodUnhooker<Method>>();
        for (var method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                unhooks.add(DAndroidBridgeImpl.doHook(method, DAndroidInterface.PRIORITY_DEFAULT, hooker));
            }
        }
        return unhooks;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <T> DAndroidInterface.MethodUnhooker<Constructor<T>>
    hookConstructor(Class<? extends DAndroidInterface.Hooker> hooker, Class<T> clazz, Class<?>... parameterTypes) {
        try {
            var constructor = clazz.getDeclaredConstructor(parameterTypes);
            return DAndroidBridgeImpl.doHook(constructor, DAndroidInterface.PRIORITY_DEFAULT, hooker);
        } catch (NoSuchMethodException e) {
            throw new HookFailedError(e);
        }
    }
}
