package com.google.dand.util;

import static com.google.dand.core.ApplicationServiceClient.serviceClient;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.CompatibilityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.webkit.WebViewDelegate;
import android.webkit.WebViewFactory;
import android.webkit.WebViewFactoryProvider;

import com.google.android.dandroid.DAndroidBridge;
import com.google.android.dandroid.DAndroidHelpers;
import com.google.android.dandroid.XC_MethodHook;
import com.google.android.dandroid.XC_MethodReplacement;
import com.google.dand.BuildConfig;
import com.google.dand.ILSPManagerService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.PathClassLoader;
import hidden.HiddenApiBridge;

public class ParasiticManagerHooker {
    private static final String CHROMIUM_WEBVIEW_FACTORY_METHOD = "create";
    
    private static PackageInfo managerPkgInfo = null;
    private static int managerFd = -1;
    private final static Map<String, Bundle> states = new ConcurrentHashMap<>();
    private final static Map<String, PersistableBundle> persistentStates = new ConcurrentHashMap<>();
    
    private synchronized static PackageInfo getManagerPkgInfo(ApplicationInfo appInfo) {
        // Log detailed appInfo at start
        if (appInfo != null) {
            Utils.logD("getManagerPkgInfo: AppInfo Details:" + "\n  packageName=" + appInfo.packageName + "\n  dataDir=" + appInfo.dataDir + "\n  nativeLibraryDir=" + appInfo.nativeLibraryDir + "\n  sourceDir=" + appInfo.sourceDir + "\n  publicSourceDir=" + appInfo.publicSourceDir + "\n  processName=" + appInfo.processName + "\n  uid=" + appInfo.uid);
        }
        
        Utils.logD("getManagerPkgInfo: Starting method, appInfo=" + (appInfo != null ? appInfo.packageName : "null"));
        
        if (managerPkgInfo == null && appInfo != null) {
            Utils.logD("getManagerPkgInfo: managerPkgInfo is null and appInfo is valid, proceeding with initialization");
            try {
                Context ctx = ActivityThread.currentActivityThread().getSystemContext();
                Utils.logD("getManagerPkgInfo: Got system context");
                
                var sourceDir = "/proc/self/fd/" + managerFd;
                Utils.logD("getManagerPkgInfo: Initial sourceDir=" + sourceDir);
                debugZipFile(sourceDir);
                
                // Log sourceDir tree structure
                DirectoryUtils.printProcFdTree(managerFd, "getManagerPkgInfo");
                
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    Utils.logD("getManagerPkgInfo: Android P or lower detected, copying APK");
                    var dstDir = appInfo.dataDir + "/cache/lsposed.apk";
                    Utils.logD("getManagerPkgInfo: Target destination=" + dstDir);
                    
                    try (var inStream = new FileInputStream(sourceDir); var outStream = new FileOutputStream(dstDir)) {
                        FileChannel inChannel = inStream.getChannel();
                        FileChannel outChannel = outStream.getChannel();
                        long size = inChannel.size();
                        Utils.logD("getManagerPkgInfo: Copying file, size=" + size + " bytes");
                        inChannel.transferTo(0, size, outChannel);
                        sourceDir = dstDir;
                        Utils.logD("getManagerPkgInfo: File copy completed, new sourceDir=" + sourceDir);
                    } catch (Throwable e) {
                        Utils.logE("getManagerPkgInfo: Failed to copy APK", e);
                    }
                }
                
                Utils.logD("getManagerPkgInfo: Getting package archive info from " + sourceDir);
                managerPkgInfo = ctx.getPackageManager().getPackageArchiveInfo(sourceDir, PackageManager.GET_ACTIVITIES);
                
                if (managerPkgInfo != null) {
                    Utils.logD("getManagerPkgInfo: Successfully got package info");
                    var newAppInfo = managerPkgInfo.applicationInfo;
                    
                    // Log detailed newAppInfo before configuration
                    Utils.logD("getManagerPkgInfo: New ApplicationInfo before configuration:" + "\n  packageName=" + newAppInfo.packageName + "\n  dataDir=" + newAppInfo.dataDir + "\n  nativeLibraryDir=" + newAppInfo.nativeLibraryDir + "\n  sourceDir=" + newAppInfo.sourceDir + "\n  publicSourceDir=" + newAppInfo.publicSourceDir + "\n  processName=" + newAppInfo.processName + "\n  uid=" + newAppInfo.uid);
                    
                    Utils.logD("getManagerPkgInfo: Configuring new ApplicationInfo");
                    newAppInfo.sourceDir = sourceDir;
                    newAppInfo.publicSourceDir = sourceDir;
                    newAppInfo.nativeLibraryDir = appInfo.nativeLibraryDir;
                    newAppInfo.packageName = appInfo.packageName;
                    newAppInfo.dataDir = HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(appInfo);
                    newAppInfo.deviceProtectedDataDir = appInfo.deviceProtectedDataDir;
                    newAppInfo.processName = appInfo.processName;
                    
                    // Log detailed newAppInfo after configuration
                    Utils.logD("getManagerPkgInfo: New ApplicationInfo after configuration:" + "\n  packageName=" + newAppInfo.packageName + "\n  dataDir=" + newAppInfo.dataDir + "\n  nativeLibraryDir=" + newAppInfo.nativeLibraryDir + "\n  sourceDir=" + newAppInfo.sourceDir + "\n  publicSourceDir=" + newAppInfo.publicSourceDir + "\n  processName=" + newAppInfo.processName + "\n  uid=" + newAppInfo.uid);
                    
                    Utils.logD("getManagerPkgInfo: Setting credential protected data dir");
                    HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(newAppInfo, HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(appInfo));
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Utils.logD("getManagerPkgInfo: Android S or higher detected, setting overlay paths");
                        HiddenApiBridge.ApplicationInfo_overlayPaths(newAppInfo, HiddenApiBridge.ApplicationInfo_overlayPaths(appInfo));
                    }
                    
                    Utils.logD("getManagerPkgInfo: Setting resource dirs");
                    HiddenApiBridge.ApplicationInfo_resourceDirs(newAppInfo, HiddenApiBridge.ApplicationInfo_resourceDirs(appInfo));
                    
                    newAppInfo.uid = appInfo.uid;
                    Utils.logD("getManagerPkgInfo: ApplicationInfo configuration completed");
                } else {
                    Utils.logE("getManagerPkgInfo: Failed to get package archive info, result is null");
                }
            } catch (Throwable e) {
                Utils.logE("getManagerPkgInfo: Failed to initialize package info", e);
            }
        } else {
            Utils.logD("getManagerPkgInfo: Using cached managerPkgInfo");
        }
        
        Utils.logD("getManagerPkgInfo: Returning " + (managerPkgInfo != null ? "valid package info" : "null"));
        return managerPkgInfo;
    }
    
    private static void dumpClassLoaderPaths(ClassLoader classLoader) {
        if (classLoader instanceof PathClassLoader) {
            try {
                // 获取 pathList 字段
                Field pathListField = Class.forName("dalvik.system.BaseDexClassLoader").getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathList = pathListField.get(classLoader);
                
                // 获取 dexElements 字段
                Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object[] dexElements = (Object[]) dexElementsField.get(pathList);
                
                Utils.logD("DexElements count: " + dexElements.length);
                
                // 打印每个 element 的详细信息
                for (Object element : dexElements) {
                    // 获取 dexFile 字段
                    Field dexFileField = element.getClass().getDeclaredField("dexFile");
                    dexFileField.setAccessible(true);
                    Object dexFile = dexFileField.get(element);
                    
                    // 获取 path 字段
                    Field pathField = element.getClass().getDeclaredField("path");
                    pathField.setAccessible(true);
                    String path = (String) pathField.get(element);
                    
                    Utils.logD("DexElement path: " + path);
                    Utils.logD("DexElement dexFile: " + dexFile);
                }
            } catch (Exception e) {
                Utils.logE("Failed to dump ClassLoader paths", e);
                e.printStackTrace();
            }
        }
    }
    
    private static void debugZipFile(String path) {
        try {
            Utils.logD("Checking zip file: " + path);
            
            // 先检查文件基本信息
            File file = new File(path);
            Utils.logD("File exists: " + file.exists());
            Utils.logD("File length: " + file.length());
            
            // 尝试打开为ZIP
            try (ZipFile zipFile = new ZipFile(path)) {
                Utils.logD("Successfully opened as ZIP");
                Utils.logD("ZIP entry count: " + zipFile.size());
                
                // 检查是否包含classes.dex
                boolean hasClassesDex = false;
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().equals("classes.dex")) {
                        hasClassesDex = true;
                        Utils.logD("Found classes.dex, size: " + entry.getSize());
                    }
                }
                Utils.logD("Contains classes.dex: " + hasClassesDex);
                
            } catch (ZipException e) {
                Utils.logE("Failed to open as ZIP", e);
            }
            
            // 检查文件头
            try (FileInputStream fis = new FileInputStream(path)) {
                byte[] magic = new byte[4];
                int read = fis.read(magic);
                Utils.logD("File header: " + Arrays.toString(magic));
                // ZIP文件应该以 PK\003\004 开头
                boolean isPKZip = magic[0] == 0x50 && magic[1] == 0x4B && magic[2] == 0x03 && magic[3] == 0x04;
                Utils.logD("Has valid ZIP header: " + isPKZip);
            }
            
        } catch (Exception e) {
            Utils.logE("Failed to check zip file", e);
        }
    }
    
    private static void sendBinderToManager(final ClassLoader classLoader, IBinder binder) {
        try {
            var clazz = DAndroidHelpers.findClass("com.google.xmanager.Constants", classLoader);
            var ok = (boolean) DAndroidHelpers.callStaticMethod(clazz, "setBinder", new Class[]{IBinder.class}, binder);
            if (ok) return;
            throw new RuntimeException("setBinder: " + false);
        } catch (Throwable t) {
            Utils.logW("Could not send binder to DAndroid Manager", t);
        }
    }
    
    private static void hookApplicationLoaders(ILSPManagerService managerService) {
        try {
            Class<?> applicationLoadersClass = Class.forName("android.app.ApplicationLoaders");
            
            // Hook 7参数版本的getClassLoader
            DAndroidHelpers.findAndHookMethod(applicationLoadersClass, "getClassLoader", String.class,     // zip
                                              int.class,        // targetSdkVersion
                                              boolean.class,    // isBundled
                                              String.class,     // librarySearchPath
                                              String.class,     // libraryPermittedPath
                                              ClassLoader.class,// parent
                                              String.class,     // classLoaderName
                                              new XC_MethodHook() {
                                                  @Override
                                                  protected void beforeHookedMethod(MethodHookParam param) {
                                                      Utils.logD("ApplicationLoaders.getClassLoader(7 args) called:");
                                                      Utils.logD("  zip path: " + param.args[0]);
                                                      Utils.logD("  targetSdkVersion: " + param.args[1]);
                                                      Utils.logD("  isBundled: " + param.args[2]);
                                                      Utils.logD("  librarySearchPath: " + param.args[3]);
                                                      Utils.logD("  libraryPermittedPath: " + param.args[4]);
                                                      Utils.logD("  parent: " + param.args[5]);
                                                      Utils.logD("  classLoaderName: " + param.args[6]);
                                                  }
                                              }
            );
            
            // Hook 11参数版本的getClassLoader
            DAndroidHelpers.findAndHookMethod(applicationLoadersClass, "getClassLoader", String.class,     // zip
                                              int.class,        // targetSdkVersion
                                              boolean.class,    // isBundled
                                              String.class,     // librarySearchPath
                                              String.class,     // libraryPermittedPath
                                              ClassLoader.class,// parent
                                              String.class,     // cacheKey
                                              String.class,     // classLoaderName
                                              List.class,       // sharedLibraries
                                              List.class,       // nativeSharedLibraries
                                              List.class,       // sharedLibrariesLoadedAfterApp
                                              new XC_MethodHook() {
                                                  @Override
                                                  protected void beforeHookedMethod(MethodHookParam param) {
                                                      Utils.logD("ApplicationLoaders.getClassLoader(11 args) called:");
                                                      Utils.logD("  zip path: " + param.args[0]);
                                                      Utils.logD("  targetSdkVersion: " + param.args[1]);
                                                      Utils.logD("  isBundled: " + param.args[2]);
                                                      Utils.logD("  librarySearchPath: " + param.args[3]);
                                                      Utils.logD("  libraryPermittedPath: " + param.args[4]);
                                                      Utils.logD("  parent: " + param.args[5]);
                                                      Utils.logD("  cacheKey: " + param.args[6]);
                                                      Utils.logD("  classLoaderName: " + param.args[7]);
                                                      Utils.logD("  sharedLibraries: " + param.args[8]);
                                                      Utils.logD("  nativeSharedLibraries: " + param.args[9]);
                                                      Utils.logD("  sharedLibrariesLoadedAfterApp: " + param.args[10]);
                                                  }
                                                  
                                                  @Override
                                                  protected void afterHookedMethod(MethodHookParam param) {
                                                      ClassLoader result = (ClassLoader) param.getResult();
                                                      if (result instanceof BaseDexClassLoader) {
                                                          try {
                                                              Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
                                                              pathListField.setAccessible(true);
                                                              Object pathList = pathListField.get(result);
                                                              Utils.logD("  Created ClassLoader pathList: " + pathList);
                                                          } catch (Exception e) {
                                                              Utils.logE("Failed to get pathList", e);
                                                          }
                                                      }
                                                      
                                                      dumpClassLoaderPaths(result);
                                                      
                                                  }
                                              }
            );
            
        } catch (Throwable e) {
            Utils.logE("Failed to hook class loader creation", e);
        }
    }
    
    private static void hookPathClassLoader() {
        try {
            // Hook 标准的三参数构造
            XC_MethodHook constructorHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Utils.logD("PathClassLoader constructor called with " + param.args.length + " args:");
                    for (int i = 0; i < param.args.length; i++) {
                        Utils.logD("  arg[" + i + "]: " + param.args[i]);
                    }
                    
                    // 打印调用栈
                    Utils.logD("Stack trace:");
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (StackTraceElement element : stackTrace) {
                        Utils.logD("    " + element.toString());
                    }
                }
            };
            
            // Hook 所有构造函数
            DAndroidBridge.hookAllConstructors(PathClassLoader.class, constructorHook);
            
            Utils.logD("Successfully hooked all PathClassLoader constructors");
            
            // 同时 hook ClassLoader 的创建方法
            Class<?> classLoaderFactory = Class.forName("dalvik.system.ClassLoaderFactory");
            DAndroidHelpers.findAndHookMethod(classLoaderFactory, "createClassLoader", String.class,  // dexPath
                                              String.class,  // librarySearchPath
                                              String.class,  // libraryPermittedPath
                                              ClassLoader.class,  // parent
                                              int.class,     // targetSdkVersion
                                              boolean.class, // isNamespaceShared
                                              String.class,  // classLoaderName
                                              List.class,    // sharedLibraries
                                              new XC_MethodHook() {
                                                  @Override
                                                  protected void beforeHookedMethod(MethodHookParam param) {
                                                      Utils.logD("ClassLoaderFactory.createClassLoader called:");
                                                      Utils.logD("  dexPath: " + param.args[0]);
                                                      Utils.logD("  librarySearchPath: " + param.args[1]);
                                                      Utils.logD("  libraryPermittedPath: " + param.args[2]);
                                                      Utils.logD("  parent: " + param.args[3]);
                                                      Utils.logD("  targetSdkVersion: " + param.args[4]);
                                                      Utils.logD("  isNamespaceShared: " + param.args[5]);
                                                      Utils.logD("  classLoaderName: " + param.args[6]);
                                                      Utils.logD("  sharedLibraries: " + param.args[7]);
                                                  }
                                              }
            );
            
        } catch (Exception e) {
            Utils.logE("Failed to hook PathClassLoader constructors", e);
        }
    }
    
    @SuppressLint("SoonBlockedPrivateApi")
    private static void hookMakeDexElements() {
        try {
            // 获取 DexPathList 类
            Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
            
            // 获取 makeDexElements 方法
            Method makeDexElements = dexPathListClass.getDeclaredMethod("makeDexElements", List.class,  // files
                                                                        File.class,  // optimizedDirectory
                                                                        List.class,  // suppressedExceptions
                                                                        ClassLoader.class, // loader
                                                                        boolean.class // isTrusted
            );
            makeDexElements.setAccessible(true);
            
            // 创建 hook
            XC_MethodHook dexElementsHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    @SuppressWarnings("unchecked") List<File> files = (List<File>) param.args[0];
                    File optimizedDirectory = (File) param.args[1];
                    ClassLoader loader = (ClassLoader) param.args[3];
                    
                    Utils.logD("makeDexElements called:");
                    Utils.logD("optimizedDirectory: " + optimizedDirectory);
                    Utils.logD("classLoader: " + loader);
                    
                    // 打印所有文件信息
                    for (File file : files) {
                        Utils.logD("Processing file: " + file.getPath());
                        Utils.logD("  exists: " + file.exists());
                        Utils.logD("  isFile: " + file.isFile());
                        Utils.logD("  length: " + file.length());
                        Utils.logD("  canRead: " + file.canRead());
                        
                        // 如果是符号链接，打印真实路径
                        try {
                            if (Files.isSymbolicLink(file.toPath())) {
                                Utils.logD("  symlink target: " + Files.readSymbolicLink(file.toPath()));
                            }
                        } catch (Exception e) {
                            Utils.logE("  Failed to check symlink", e);
                        }
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object[] elements = (Object[]) param.getResult();
                    Utils.logD("Created elements count: " + (elements != null ? elements.length : 0));
                    
                    // 打印每个Element的信息
                    if (elements != null) {
                        for (int i = 0; i < elements.length; i++) {
                            Object element = elements[i];
                            Utils.logD("Element[" + i + "]: " + element);
                            // 尝试获取Element的file字段
                            try {
                                Field fileField = element.getClass().getDeclaredField("file");
                                fileField.setAccessible(true);
                                File file = (File) fileField.get(element);
                                Utils.logD("  file: " + file);
                            } catch (Exception e) {
                                Utils.logE("  Failed to get file field", e);
                            }
                            
                            // 尝试获取Element的dexFile字段
                            try {
                                Field dexFileField = element.getClass().getDeclaredField("dexFile");
                                dexFileField.setAccessible(true);
                                Object dexFile = dexFileField.get(element);
                                Utils.logD("  dexFile: " + dexFile);
                            } catch (Exception e) {
                                Utils.logE("  Failed to get dexFile field", e);
                            }
                        }
                    }
                }
            };
            
            // 应用 hook
            DAndroidHelpers.findAndHookMethod(dexPathListClass, "makeDexElements", List.class, File.class, List.class, ClassLoader.class, boolean.class, dexElementsHook);
            
            Utils.logD("Successfully hooked makeDexElements");
            
        } catch (Exception e) {
            Utils.logE("Failed to hook makeDexElements", e);
            e.printStackTrace();
        }
    }
    
    private static void hookLoadedApk(ILSPManagerService managerService) {
        
        DAndroidHelpers.findAndHookMethod(LoadedApk.class, "createOrUpdateClassLoaderLocked", List.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam<?> param) throws Throwable {
                List<String> addedPaths = (List<String>) param.args[0];
                Utils.logD("LoadedApk#createOrUpdateClassLoaderLocked() starts, addedPaths: " + addedPaths);
                Utils.logD("  addedPaths: " + addedPaths);
                printLoadedApk((LoadedApk) param.thisObject);
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam<?> param) throws Throwable {
                Utils.logD("LoadedApk#createOrUpdateClassLoaderLocked() ends");
                printLoadedApk((LoadedApk) param.thisObject);
            }
        });
        
        DAndroidHelpers.findAndHookMethod(LoadedApk.class, "makePaths", ActivityThread.class, boolean.class,    // isBundledApp
                                          ApplicationInfo.class, List.class,       // outZipPaths
                                          List.class,       // outLibPaths
                                          new XC_MethodHook() {
                                              @Override
                                              protected void beforeHookedMethod(MethodHookParam param) {
                                                  ActivityThread thread = (ActivityThread) param.args[0];
                                                  boolean isBundledApp = (boolean) param.args[1];
                                                  ApplicationInfo aInfo = (ApplicationInfo) param.args[2];
                                                  Utils.logD("LoadedApk makePaths called with:");
                                                  Utils.logD("  thread: " + thread);
                                                  Utils.logD("  isBundledApp: " + isBundledApp);
                                                  Utils.logD("  appDir: " + aInfo.sourceDir);
                                                  Utils.logD("  libDir: " + aInfo.nativeLibraryDir);
                                                  Utils.logD("  processName: " + aInfo.processName);
                                                  Utils.logD("  targetSdkVersion: " + aInfo.targetSdkVersion);
                                                  
                                                  // 检查传入的List是否为空
                                                  List<String> outZipPaths = (List<String>) param.args[3];
                                                  List<String> outLibPaths = (List<String>) param.args[4];
                                                  Utils.logD("  Initial outZipPaths: " + (outZipPaths != null ? outZipPaths : "null"));
                                                  Utils.logD("  Initial outLibPaths: " + (outLibPaths != null ? outLibPaths : "null"));
                                                  printLoadedApk((LoadedApk) param.thisObject);
                                              }
                                              
                                              @Override
                                              protected void afterHookedMethod(MethodHookParam param) {
                                                  List<String> outZipPaths = (List<String>) param.args[3];
                                                  List<String> outLibPaths = (List<String>) param.args[4];
                                                  Utils.logD("LoadedApk makePaths results:");
                                                  Utils.logD("  Final outZipPaths: " + outZipPaths);
                                                  Utils.logD("  Final outLibPaths: " + outLibPaths);
                                                  printLoadedApk((LoadedApk) param.thisObject);
                                              }
                                          }
        );
        
        
    }
    
    @SuppressLint("SoonBlockedPrivateApi")
    private static void printLoadedApk(LoadedApk loadedApk) {
        try {
            if (loadedApk == null) {
                Utils.logD("LoadedApk is null");
                return;
            }
            ApplicationInfo info = (ApplicationInfo) DAndroidHelpers.getObjectField(loadedApk, "mApplicationInfo");
            Utils.logD("  mApplicationInfo: " + info);
            Utils.logD("  sourceDir: " + (info != null ? info.sourceDir : "null"));
            
            // 检查mPackageName字段
            String pkgName = (String) DAndroidHelpers.getObjectField(loadedApk, "mPackageName");
            Utils.logD("  mPackageName: " + pkgName);
            
            // 显式打印LoadedApk的关键字段
            Utils.logD("LoadedApk fields:");
            Utils.logD("  mBaseClassLoader: " + DAndroidHelpers.getObjectField(loadedApk, "mBaseClassLoader"));
            Utils.logD("  mDefaultClassLoader: " + DAndroidHelpers.getObjectField(loadedApk, "mDefaultClassLoader"));
            
            Field mClassLoaderField = LoadedApk.class.getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            ClassLoader current = (ClassLoader) mClassLoaderField.get(loadedApk);
            Utils.logD("LoadedApk classLoader: " + current);
            Field mClassLoademApplicationInfoField = LoadedApk.class.getDeclaredField("mApplicationInfo");
            mClassLoademApplicationInfoField.setAccessible(true);
            ApplicationInfo appInfo = (ApplicationInfo) mClassLoademApplicationInfoField.get(loadedApk);
            Utils.logD("LoadedApk applicationInfo: " + appInfo);
            
            
            Field mIncludeCodeField = LoadedApk.class.getDeclaredField("mIncludeCode");
            mIncludeCodeField.setAccessible(true);
            boolean includeCode = mIncludeCodeField.getBoolean(loadedApk);
            Utils.logD("LoadedApk mIncludeCode: " + includeCode);
            
            printApplicationInfo(appInfo);
            
            Utils.logD("===================================");
            
            
        } catch (Throwable e) {
            Utils.logE("Failed to dump ClassLoader hierarchy", e);
        }
    }
    
    private static void printApplicationInfo(ApplicationInfo appInfo) {
        if (appInfo == null) {
            Utils.logD("ApplicationInfo is null");
            return;
        }
        Utils.logD("ApplicationInfo: " + appInfo);
        Utils.logD("  packageName: " + appInfo.packageName);
        Utils.logD("  sourceDir: " + appInfo.sourceDir);
        Utils.logD("  publicSourceDir: " + appInfo.publicSourceDir);
        Utils.logD("  dataDir: " + appInfo.dataDir);
        Utils.logD("  nativeLibraryDir: " + appInfo.nativeLibraryDir);
        Utils.logD("  processName: " + appInfo.processName);
        Utils.logD("  uid: " + appInfo.uid);
        
    }
    
    private static void hookActivityThread(ILSPManagerService managerService) {
        DAndroidHelpers.findAndHookMethod(ActivityThread.class, "getPackageInfo", ApplicationInfo.class, CompatibilityInfo.class, ClassLoader.class, boolean.class,    // securityViolation
                                          boolean.class,    // includeCode
                                          boolean.class,    // registerPackage
                                          boolean.class,    // isSdkSandbox
                                          new XC_MethodHook() {
                                              @Override
                                              protected void beforeHookedMethod(MethodHookParam param) {
                                                  ApplicationInfo aInfo = (ApplicationInfo) param.args[0];
                                                  boolean includeCode = (boolean) param.args[4];
                                                  
                                                  Utils.logD("getPackageInfo called:");
                                                  Utils.logD("  packageName: " + aInfo.packageName);
                                                  Utils.logD("  includeCode: " + includeCode);
                                                  Utils.logD("  FLAG_HAS_CODE: " + ((aInfo.flags & ApplicationInfo.FLAG_HAS_CODE) != 0));
                                                  Utils.logD("  flags: " + aInfo.flags);
                                                  
                                                  boolean result = includeCode && (aInfo.flags & ApplicationInfo.FLAG_HAS_CODE) != 0;
                                                  Utils.logD("  Result: " + result);
                                                  
                                              }
                                              
                                              @Override
                                              protected void afterHookedMethod(MethodHookParam<?> param) throws Throwable {
                                                  var info = (LoadedApk) param.getResult();
                                                  DAndroidHelpers.setBooleanField(info, "mIncludeCode", true);
                                                  param.setResult(info);
                                                  printLoadedApk(info);
                                                  
                                              }
                                          }
        );
    }
    
    
    private static void hookForManager(ILSPManagerService managerService) {
        
        hookMakeDexElements();
        hookPathClassLoader();
        hookApplicationLoaders(managerService);
        hookLoadedApk(managerService);
        hookActivityThread(managerService);
        var managerApkHooker = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Hookers.logD("ActivityThread#handleBindApplication() starts");
                Object bindData = param.args[0];
                ApplicationInfo appInfo = (ApplicationInfo) DAndroidHelpers.getObjectField(bindData, "appInfo");
                DAndroidHelpers.setObjectField(bindData, "appInfo", getManagerPkgInfo(appInfo).applicationInfo);
                
                appInfo = (ApplicationInfo) DAndroidHelpers.getObjectField(bindData, "appInfo");
                Hookers.logD("ActivityThread#handleBindApplication() starts,applicationInfo.sourceDir: " + appInfo.sourceDir);
                LoadedApk info = (LoadedApk) DAndroidHelpers.getObjectField(bindData, "info");
                printLoadedApk(info);
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam<?> param) throws Throwable {
                Object bindData = param.args[0];
                LoadedApk info = (LoadedApk) DAndroidHelpers.getObjectField(bindData, "info");
                Hookers.logD("ActivityThread#handleBindApplication() ends");
                printLoadedApk(info);
            }
        };
        DAndroidHelpers.findAndHookMethod(ActivityThread.class, "handleBindApplication", "android.app.ActivityThread$AppBindData", managerApkHooker);
        
        var unhooks = new XC_MethodHook.Unhook[]{null};
        unhooks[0] = DAndroidHelpers.findAndHookMethod(LoadedApk.class, "getClassLoader", new XC_MethodHook() {
            
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                LoadedApk loadedApk = (LoadedApk) param.thisObject;
                ApplicationInfo appInfo = (ApplicationInfo) DAndroidHelpers.getObjectField(loadedApk, "mApplicationInfo");
                Utils.logD("LoadedApk getClassLoader start ");
                
                // 安全地打印当前ClassLoader树
                printLoadedApk(loadedApk);
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                ClassLoader result = (ClassLoader) param.getResult();
                Utils.logD("Created ClassLoader: " + result);
                dumpClassLoaderPaths(result);
                var pkgInfo = getManagerPkgInfo(null);
                var memAppInfo = (ApplicationInfo) DAndroidHelpers.getObjectField(param.thisObject, "mApplicationInfo");
                if (pkgInfo != null && memAppInfo == pkgInfo.applicationInfo) {
                    sendBinderToManager((ClassLoader) param.getResult(), managerService.asBinder());
                    unhooks[0].unhook();
                }
                Utils.logD("LoadedApk getClassLoader end ");
                printLoadedApk((LoadedApk) param.thisObject);
            }
        });
        
        var activityClientRecordClass = DAndroidHelpers.findClass("android.app.ActivityThread$ActivityClientRecord", ActivityThread.class.getClassLoader());
        var activityHooker = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                for (var i = 0; i < param.args.length; ++i) {
                    if (param.args[i] instanceof ActivityInfo) {
                        var aInfo = (ActivityInfo) param.args[i];
                        var pkgInfo = getManagerPkgInfo(aInfo.applicationInfo);
                        if (pkgInfo == null) return;
                        for (var activity : pkgInfo.activities) {
                            if ("com.google.xmanager.ui.activity.MainActivity".equals(activity.name)) {
                                activity.applicationInfo = pkgInfo.applicationInfo;
                                param.args[i] = activity;
                            }
                        }
                    }
                    if (param.args[i] instanceof Intent) {
                        var intent = (Intent) param.args[i];
                        checkIntent(managerService, intent);
                        intent.setComponent(new ComponentName(intent.getComponent().getPackageName(), "com.google.xmanager.ui.activity.MainActivity"));
                    }
                }
                if (param.method.getName().equals("scheduleLaunchActivity")) {
                    ActivityInfo aInfo = null;
                    var parameters = ((Method) param.method).getParameterTypes();
                    for (var i = 0; i < parameters.length; ++i) {
                        if (parameters[i] == ActivityInfo.class) {
                            aInfo = (ActivityInfo) param.args[i];
                            Hookers.logD("loading state of " + aInfo.name);
                        } else if (parameters[i] == Bundle.class && aInfo != null) {
                            final int idx = i;
                            states.computeIfPresent(aInfo.name, (k, v) -> {
                                param.args[idx] = v;
                                return v;
                            });
                        } else if (parameters[i] == PersistableBundle.class && aInfo != null) {
                            final int idx = i;
                            persistentStates.computeIfPresent(aInfo.name, (k, v) -> {
                                param.args[idx] = v;
                                return v;
                            });
                        }
                    }
                    
                }
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                for (var i = 0; i < param.args.length && activityClientRecordClass.isInstance(param.thisObject); ++i) {
                    if (param.args[i] instanceof ActivityInfo) {
                        var aInfo = (ActivityInfo) param.args[i];
                        Hookers.logD("loading state of " + aInfo.name);
                        states.computeIfPresent(aInfo.name, (k, v) -> {
                            DAndroidHelpers.setObjectField(param.thisObject, "state", v);
                            return v;
                        });
                        persistentStates.computeIfPresent(aInfo.name, (k, v) -> {
                            DAndroidHelpers.setObjectField(param.thisObject, "persistentState", v);
                            return v;
                        });
                    }
                }
            }
        };
        DAndroidBridge.hookAllConstructors(activityClientRecordClass, activityHooker);
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            DAndroidBridge.hookAllMethods(DAndroidHelpers.findClass("android.app.ActivityThread$ApplicationThread", ActivityThread.class.getClassLoader()), "scheduleLaunchActivity", activityHooker);
        }
        
        DAndroidBridge.hookAllMethods(ActivityThread.class, "handleReceiver", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                for (var arg : param.args) {
                    if (arg instanceof BroadcastReceiver.PendingResult) {
                        ((BroadcastReceiver.PendingResult) arg).finish();
                    }
                }
                return null;
            }
        });
        
        DAndroidBridge.hookAllMethods(ActivityThread.class, "installProvider", new XC_MethodHook() {
            private Context originalContext = null;
            
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Hookers.logD("before install provider");
                Context ctx = null;
                ProviderInfo info = null;
                int ctxIdx = -1;
                for (var i = 0; i < param.args.length; ++i) {
                    var arg = param.args[i];
                    if (arg instanceof Context) {
                        ctx = (Context) arg;
                        ctxIdx = i;
                    } else if (arg instanceof ProviderInfo) info = (ProviderInfo) arg;
                }
                var pkgInfo = getManagerPkgInfo(null);
                if (ctx != null && info != null && pkgInfo != null) {
                    var packageName = pkgInfo.applicationInfo.packageName;
                    if (!info.applicationInfo.packageName.equals(packageName)) return;
                    if (originalContext == null) {
                        info.applicationInfo.packageName = packageName + ".origin";
                        var originalPkgInfo = ActivityThread.currentActivityThread().getPackageInfoNoCheck(info.applicationInfo, HiddenApiBridge.Resources_getCompatibilityInfo(ctx.getResources()));
                        DAndroidHelpers.setObjectField(originalPkgInfo, "mPackageName", packageName);
                        originalContext = (Context) DAndroidHelpers.callStaticMethod(DAndroidHelpers.findClass("android.app.ContextImpl", null), "createAppContext", ActivityThread.currentActivityThread(), originalPkgInfo);
                        info.applicationInfo.packageName = packageName;
                    }
                    param.args[ctxIdx] = originalContext;
                } else {
                    Hookers.logE("Failed to reload provider", new RuntimeException());
                }
            }
        });
        
        DAndroidHelpers.findAndHookMethod(ActivityThread.class, "deliverNewIntents", activityClientRecordClass, List.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[1] == null) return;
                for (var intent : (List<?>) param.args[1]) {
                    checkIntent(managerService, (Intent) intent);
                }
            }
        });
        
        DAndroidHelpers.findAndHookMethod(WebViewFactory.class, "getProvider", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                var sProviderInstance = DAndroidHelpers.getStaticObjectField(WebViewFactory.class, "sProviderInstance");
                if (sProviderInstance != null) return sProviderInstance;
                //noinspection unchecked
                var providerClass = (Class<WebViewFactoryProvider>) DAndroidHelpers.callStaticMethod(WebViewFactory.class, "getProviderClass");
                Method staticFactory = null;
                try {
                    staticFactory = providerClass.getMethod(CHROMIUM_WEBVIEW_FACTORY_METHOD, WebViewDelegate.class);
                } catch (Exception e) {
                    Hookers.logE("error instantiating provider with static factory method", e);
                }
                
                try {
                    var webViewDelegateConstructor = WebViewDelegate.class.getDeclaredConstructor();
                    webViewDelegateConstructor.setAccessible(true);
                    if (staticFactory != null) {
                        sProviderInstance = staticFactory.invoke(null, webViewDelegateConstructor.newInstance());
                    }
                    DAndroidHelpers.setStaticObjectField(WebViewFactory.class, "sProviderInstance", sProviderInstance);
                    Hookers.logD("Loaded provider: " + sProviderInstance);
                    return sProviderInstance;
                } catch (Exception e) {
                    Hookers.logE("error instantiating provider", e);
                    throw new AndroidRuntimeException(e);
                }
            }
        });
        var stateHooker = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    var record = param.args[0];
                    if (record instanceof IBinder) {
                        record = ((ArrayMap<?, ?>) DAndroidHelpers.getObjectField(param.thisObject, "mActivities")).get(record);
                        if (record == null) return;
                    }
                    DAndroidHelpers.callMethod(param.thisObject, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? "callActivityOnSaveInstanceState" : "callCallActivityOnSaveInstanceState", record);
                    var state = (Bundle) DAndroidHelpers.getObjectField(record, "state");
                    var persistentState = (PersistableBundle) DAndroidHelpers.getObjectField(record, "persistentState");
                    var aInfo = (ActivityInfo) DAndroidHelpers.getObjectField(record, "activityInfo");
                    states.compute(aInfo.name, (k, v) -> state);
                    persistentStates.compute(aInfo.name, (k, v) -> persistentState);
                    Hookers.logD("saving state of " + aInfo.name);
                } catch (Throwable e) {
                    Hookers.logE("save state", e);
                }
            }
        };
        DAndroidBridge.hookAllMethods(ActivityThread.class, "performStopActivityInner", stateHooker);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1)
            DAndroidHelpers.findAndHookMethod(ActivityThread.class, "performDestroyActivity", IBinder.class, boolean.class, int.class, boolean.class, stateHooker);
    }
    
    
    private static void checkIntent(ILSPManagerService managerService, Intent intent) {
        if (managerService == null) return;
        if (Process.myUid() != BuildConfig.MANAGER_INJECTED_UID) return;
        if (intent.getCategories() == null || !intent.getCategories().contains("com.google.xmanager.LAUNCH_MANAGER")) {
            Hookers.logD("Launching the original app, restarting");
            try {
                managerService.restartFor(intent);
            } catch (RemoteException e) {
                Hookers.logE("restart failed", e);
            } finally {
                Process.killProcess(Process.myPid());
            }
        }
    }
    
    
    static public boolean start() {
        List<IBinder> binder = new ArrayList<>(1);
        Utils.logD("start parasitic manager");
        try (var managerParcelFd = serviceClient.requestInjectedManagerBinder(binder)) {
            if (binder.size() > 0 && binder.get(0) != null && managerParcelFd != null) {
                managerFd = managerParcelFd.detachFd();
                var managerService = ILSPManagerService.Stub.asInterface(binder.get(0));
                hookForManager(managerService);
                Utils.logD("injected manager");
                return true;
            } else {
                Utils.logE("failed to inject manager");
                // Not manager
                return false;
            }
        } catch (Throwable e) {
            Utils.logE("failed to inject manager", e);
            return false;
        }
    }
}
