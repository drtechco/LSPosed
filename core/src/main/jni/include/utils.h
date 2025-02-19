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

#pragma once

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wgnu-string-literal-operator-template"

#include <string>
#include <filesystem>
#ifdef __ANDROID__
#include <sys/system_properties.h>
#else 
// 定义一个空的实现或者替代实现
#define PROP_NAME_MAX   32
#define PROP_VALUE_MAX  92

__attribute__((weak))
int __system_property_get(const char *name, char *value)
{
    // 主机构建时使用的替代实现
    return -1;
}
#endif
#include <unistd.h>
#include <sys/stat.h>
#include "logging.h"


namespace dand {
    using namespace std::literals::string_literals;

    inline int32_t GetAndroidApiLevel() {
        static int32_t api_level = []() {
            char prop_value[PROP_VALUE_MAX];
            __system_property_get("ro.build.version.sdk", prop_value);
            int base = atoi(prop_value);
            __system_property_get("ro.build.version.preview_sdk", prop_value);
            return base + atoi(prop_value);
        }();
        return api_level;
    }

    inline std::string JavaNameToSignature(std::string s) {
        std::replace(s.begin(), s.end(), '.', '/');
        return "L" + s;
    }
}

#pragma clang diagnostic pop
