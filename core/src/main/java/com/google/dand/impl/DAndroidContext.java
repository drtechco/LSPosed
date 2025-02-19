package com.google.dand.impl;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.dand.core.BuildConfig;
import com.google.dand.impl.utils.DAndroidDexParser;
import com.google.dand.models.Module;
import com.google.dand.nativebridge.HookBridge;
import com.google.dand.nativebridge.NativeAPI;
import com.google.dand.service.ILSPInjectedModuleService;
import com.google.dand.util.LspModuleClassLoader;
import com.google.libdandroid.api.DAndroidInterface;
import com.google.libdandroid.api.DAndroidModule;
import com.google.libdandroid.api.DAndroidModuleInterface;
import com.google.libdandroid.api.errors.DAndroidFrameworkError;
import com.google.libdandroid.api.utils.DexParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@SuppressLint("NewApi")
public class DAndroidContext implements DAndroidInterface {

    private static final String TAG = "DAndroidContext";

    public static boolean isSystemServer;
    public static String appDir;
    public static String processName;

    static final Set<DAndroidModule> modules = ConcurrentHashMap.newKeySet();

    private final String mPackageName;
    private final ApplicationInfo mApplicationInfo;
    private final ILSPInjectedModuleService service;
    private final Map<String, SharedPreferences> mRemotePrefs = new ConcurrentHashMap<>();

    DAndroidContext(String packageName, ApplicationInfo applicationInfo, ILSPInjectedModuleService service) {
        this.mPackageName = packageName;
        this.mApplicationInfo = applicationInfo;
        this.service = service;
    }

    public static void callOnPackageLoaded(DAndroidModuleInterface.PackageLoadedParam param) {
        for (DAndroidModule module : modules) {
            try {
                module.onPackageLoaded(param);
            } catch (Throwable t) {
                Log.e(TAG, "Error when calling onPackageLoaded of " + module.getApplicationInfo().packageName, t);
            }
        }
    }

    public static void callOnSystemServerLoaded(DAndroidModuleInterface.SystemServerLoadedParam param) {
        for (DAndroidModule module : modules) {
            try {
                module.onSystemServerLoaded(param);
            } catch (Throwable t) {
                Log.e(TAG, "Error when calling onSystemServerLoaded of " + module.getApplicationInfo().packageName, t);
            }
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    public static boolean loadModule(ActivityThread at, Module module) {
        try {
            Log.d(TAG, "Loading module " + module.packageName);
            var sb = new StringBuilder();
            var abis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
            for (String abi : abis) {
                sb.append(module.apkPath).append("!/lib/").append(abi).append(File.pathSeparator);
            }
            var librarySearchPath = sb.toString();
            var initLoader = DAndroidModule.class.getClassLoader();
            var mcl = LspModuleClassLoader.loadApk(module.apkPath, module.file.preLoadedDexes, librarySearchPath, initLoader);
            if (mcl.loadClass(DAndroidModule.class.getName()).getClassLoader() != initLoader) {
                Log.e(TAG, "  Cannot load module: " + module.packageName);
                Log.e(TAG, "  The DAndroid API classes are compiled into the module's APK.");
                Log.e(TAG, "  This may cause strange issues and must be fixed by the module developer.");
                return false;
            }
            var ctx = new DAndroidContext(module.packageName, module.applicationInfo, module.service);
            for (var entry : module.file.moduleClassNames) {
                var moduleClass = mcl.loadClass(entry);
                Log.d(TAG, "  Loading class " + moduleClass);
                if (!DAndroidModule.class.isAssignableFrom(moduleClass)) {
                    Log.e(TAG, "    This class doesn't implement any sub-interface of DAndroidModule, skipping it");
                    continue;
                }
                try {
                    var moduleEntry = moduleClass.getConstructor(DAndroidInterface.class, DAndroidModuleInterface.ModuleLoadedParam.class);
                    var moduleContext = (DAndroidModule) moduleEntry.newInstance(ctx, new DAndroidModuleInterface.ModuleLoadedParam() {
                        @Override
                        public boolean isSystemServer() {
                            return isSystemServer;
                        }

                        @NonNull
                        @Override
                        public String getProcessName() {
                            return processName;
                        }
                    });
                    modules.add(moduleContext);
                } catch (Throwable e) {
                    Log.e(TAG, "    Failed to load class " + moduleClass, e);
                }
            }
            module.file.moduleLibraryNames.forEach(NativeAPI::recordNativeEntrypoint);
            Log.d(TAG, "Loaded module " + module.packageName + ": " + ctx);
        } catch (Throwable e) {
            Log.d(TAG, "Loading module " + module.packageName, e);
            return false;
        }
        return true;
    }

    @NonNull
    @Override
    public String getFrameworkName() {
        return BuildConfig.FRAMEWORK_NAME;
    }

    @NonNull
    @Override
    public String getFrameworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public long getFrameworkVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public int getFrameworkPrivilege() {
        try {
            return service.getFrameworkPrivilege();
        } catch (RemoteException ignored) {
            return -1;
        }
    }

    @Override
    @NonNull
    public MethodUnhooker<Method> hook(@NonNull Method origin, @NonNull Class<? extends Hooker> hooker) {
        return DAndroidBridgeImpl.doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @NonNull
    public MethodUnhooker<Method> hook(@NonNull Method origin, int priority, @NonNull Class<? extends Hooker> hooker) {
        return DAndroidBridgeImpl.doHook(origin, priority, hooker);
    }

    @Override
    @NonNull
    public <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull Class<? extends Hooker> hooker) {
        return DAndroidBridgeImpl.doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @NonNull
    public <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Class<? extends Hooker> hooker) {
        return DAndroidBridgeImpl.doHook(origin, priority, hooker);
    }

    private static boolean doDeoptimize(@NonNull Executable method) {
        if (Modifier.isAbstract(method.getModifiers())) {
            throw new IllegalArgumentException("Cannot deoptimize abstract methods: " + method);
        } else if (Proxy.isProxyClass(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot deoptimize methods from proxy class: " + method);
        }
        return HookBridge.deoptimizeMethod(method);
    }

    @Override
    public boolean deoptimize(@NonNull Method method) {
        return doDeoptimize(method);
    }

    @Override
    public <T> boolean deoptimize(@NonNull Constructor<T> constructor) {
        return doDeoptimize(constructor);
    }

    @Nullable
    @Override
    public Object invokeOrigin(@NonNull Method method, @Nullable Object thisObject, Object[] args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        return HookBridge.invokeOriginalMethod(method, thisObject, args);
    }

    private static char getTypeShorty(Class<?> type) {
        if (type == int.class) {
            return 'I';
        } else if (type == long.class) {
            return 'J';
        } else if (type == float.class) {
            return 'F';
        } else if (type == double.class) {
            return 'D';
        } else if (type == boolean.class) {
            return 'Z';
        } else if (type == byte.class) {
            return 'B';
        } else if (type == char.class) {
            return 'C';
        } else if (type == short.class) {
            return 'S';
        } else if (type == void.class) {
            return 'V';
        } else {
            return 'L';
        }
    }

    private static char[] getExecutableShorty(Executable executable) {
        var parameterTypes = executable.getParameterTypes();
        var shorty = new char[parameterTypes.length + 1];
        shorty[0] = getTypeShorty(executable instanceof Method ? ((Method) executable).getReturnType() : void.class);
        for (int i = 1; i < shorty.length; i++) {
            shorty[i] = getTypeShorty(parameterTypes[i - 1]);
        }
        return shorty;
    }

    @Nullable
    @Override
    public Object invokeSpecial(@NonNull Method method, @NonNull Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Cannot invoke special on static method: " + method);
        }
        return HookBridge.invokeSpecialMethod(method, getExecutableShorty(method), method.getDeclaringClass(), thisObject, args);
    }

    @NonNull
    @Override
    public <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, Object... args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        var obj = HookBridge.allocateObject(constructor.getDeclaringClass());
        HookBridge.invokeOriginalMethod(constructor, obj, args);
        return obj;
    }

    @NonNull
    @Override
    public <T, U> U newInstanceSpecial(@NonNull Constructor<T> constructor, @NonNull Class<U> subClass, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        var superClass = constructor.getDeclaringClass();
        if (!superClass.isAssignableFrom(subClass)) {
            throw new IllegalArgumentException(subClass + " is not inherited from " + superClass);
        }
        var obj = HookBridge.allocateObject(subClass);
        HookBridge.invokeSpecialMethod(constructor, getExecutableShorty(constructor), superClass, obj, args);
        return obj;
    }

    @Override
    public void log(@NonNull String message) {
        Log.i(TAG, mPackageName + ": " + message);
    }

    @Override
    public void log(@NonNull String message, @NonNull Throwable throwable) {
        Log.e(TAG, mPackageName + ": " + message, throwable);
    }

    @Override
    public DexParser parseDex(@NonNull ByteBuffer dexData, boolean includeAnnotations) throws IOException {
        return new DAndroidDexParser(dexData, includeAnnotations);
    }

    @NonNull
    @Override
    public ApplicationInfo getApplicationInfo() {
        return mApplicationInfo;
    }

    @NonNull
    @Override
    public SharedPreferences getRemotePreferences(String name) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        return mRemotePrefs.computeIfAbsent(name, n -> {
            try {
                return new DAndroidRemotePreferences(service, n);
            } catch (RemoteException e) {
                log("Failed to get remote preferences", e);
                throw new DAndroidFrameworkError(e);
            }
        });
    }

    @NonNull
    @Override
    public String[] listRemoteFiles() {
        try {
            return service.getRemoteFileList();
        } catch (RemoteException e) {
            log("Failed to list remote files", e);
            throw new DAndroidFrameworkError(e);
        }
    }

    @NonNull
    @Override
    public ParcelFileDescriptor openRemoteFile(String name) throws FileNotFoundException {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        try {
            return service.openRemoteFile(name);
        } catch (RemoteException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
