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

//
// Created by 双草酸酯 on 2/7/21.
//
#include "native_api.h"
#include "native_util.h"
#include "utils/jni_helper.hpp"
#include "../native_api.h"

using namespace danlant;

namespace dand {
    LSP_DEF_NATIVE_METHOD(void, NativeAPI, recordNativeEntrypoint, jstring jstr) {
        danlant::JUTFString str(env, jstr);
        RegisterNativeLib(str);
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(NativeAPI, recordNativeEntrypoint, "(Ljava/lang/String;)V")
    };

    void RegisterNativeAPI(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(NativeAPI);
    }
}
