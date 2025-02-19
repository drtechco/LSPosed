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
 * Copyright (C) 2021 - 2022 DAndroid Contributors
 */

#include <jni.h>
#include <cstring>
#include <cstdlib>
#include <array>
#include "logging.h"
#include "loader.h"
#include "config_impl.h"
#include "magisk_loader.h"
#include "symbol_cache.h"

#define RIRU_MODULE
#include "riru.h"

namespace dand {
    int *allowUnload = nullptr;
    namespace {
        std::string magiskPath;

        jstring nice_name = nullptr;
        jstring app_dir = nullptr;

        void onModuleLoaded() {
            LOGI("onModuleLoaded: welcome to DAndroid!");
            LOGI("onModuleLoaded: version v{} ({})", versionName, versionCode);
            MagiskLoader::Init();
            ConfigImpl::Init();
        }

        void nativeForkAndSpecializePre(JNIEnv *env, jclass, jint *_uid, jint *,
                                        jintArray *gids, jint *,
                                        jobjectArray *, jint *,
                                        jstring *, jstring *_nice_name,
                                        jintArray *, jintArray *,
                                        jboolean *start_child_zygote, jstring *,
                                        jstring *_app_data_dir, jboolean *,
                                        jobjectArray *,
                                        jobjectArray *,
                                        jboolean *,
                                        jboolean *) {
            nice_name = *_nice_name;
            app_dir = *_app_data_dir;
            MagiskLoader::GetInstance()->OnNativeForkAndSpecializePre(env, *_uid, *gids,
                                                                 nice_name,
                                                                 *start_child_zygote,
                                                                 *_app_data_dir);
        }

        void nativeForkAndSpecializePost(JNIEnv *env, jclass, jint res) {
            if (res == 0)
                MagiskLoader::GetInstance()->OnNativeForkAndSpecializePost(env, nice_name, app_dir);
        }

        void nativeForkSystemServerPre(JNIEnv *env, jclass, uid_t *, gid_t *,
                                       jintArray *, jint *,
                                       jobjectArray *, jlong *,
                                       jlong *) {
            MagiskLoader::GetInstance()->OnNativeForkSystemServerPre(env);
        }

        void nativeForkSystemServerPost(JNIEnv *env, jclass, jint res) {
            if (res == 0)
                MagiskLoader::GetInstance()->OnNativeForkSystemServerPost(env);
        }

        /* method added in Android Q */
        void specializeAppProcessPre(JNIEnv *env, jclass, jint *_uid, jint *,
                                     jintArray *gids, jint *,
                                     jobjectArray *, jint *,
                                     jstring *, jstring *_nice_name,
                                     jboolean *start_child_zygote, jstring *,
                                     jstring *_app_data_dir, jboolean *,
                                     jobjectArray *,
                                     jobjectArray *,
                                     jboolean *,
                                     jboolean *) {
            nice_name = *_nice_name;
            app_dir = *_app_data_dir;
            MagiskLoader::GetInstance()->OnNativeForkAndSpecializePre(env, *_uid, *gids,
                                                                 nice_name,
                                                                 *start_child_zygote,
                                                                 *_app_data_dir);
        }

        void specializeAppProcessPost(JNIEnv *env, jclass) {
            MagiskLoader::GetInstance()->OnNativeForkAndSpecializePost(env, nice_name, app_dir);
        }
    }

    RiruVersionedModuleInfo module{
            .moduleApiVersion = apiVersion,
            .moduleInfo = RiruModuleInfo{
                    .supportHide = !isDebug,
                    .version = versionCode,
                    .versionName = versionName,
                    .onModuleLoaded = dand::onModuleLoaded,
                    .forkAndSpecializePre = dand::nativeForkAndSpecializePre,
                    .forkAndSpecializePost = dand::nativeForkAndSpecializePost,
                    .forkSystemServerPre = dand::nativeForkSystemServerPre,
                    .forkSystemServerPost = dand::nativeForkSystemServerPost,
                    .specializeAppProcessPre = dand::specializeAppProcessPre,
                    .specializeAppProcessPost = dand::specializeAppProcessPost,
            }
    };
}

RIRU_EXPORT RiruVersionedModuleInfo *init(Riru *riru) {
    LOGD("using riru {}", riru->riruApiVersion);
    LOGD("module path: {}", riru->magiskModulePath);
    dand::magiskPath = riru->magiskModulePath;
    if (!dand::isDebug && dand::magiskPath.find(dand::moduleName) == std::string::npos) {
        LOGE("who am i");
        return nullptr;
    }
    dand::allowUnload = riru->allowUnload;
    return &dand::module;
}
