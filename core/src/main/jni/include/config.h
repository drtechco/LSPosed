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

#pragma once

#include <jni.h>
#include <sys/types.h>
#include <string>
#include "macros.h"
#include "utils.h"
#include "utils/hook_helper.hpp"


template <char... chars>
struct tstring : public std::integer_sequence<char, chars...> {
    inline constexpr static const char *c_str() { return str_; }

    inline constexpr operator std::string_view() const { return {c_str(), sizeof...(chars)}; }

private:
    inline static constexpr char str_[]{chars..., '\0'};
};

template <typename T, T... chars>
inline constexpr tstring<chars...> operator""_tstr() {
    return {};
}

template <char... as, char... bs>
inline constexpr tstring<as..., bs...> operator+(const tstring<as...> &, const tstring<bs...> &) {
    return {};
}

template <char... as>
inline constexpr auto operator+(const std::string &a, const tstring<as...> &) {
    char b[]{as..., '\0'};
    return a + b;
}

template <char... as>
inline constexpr auto operator+(const tstring<as...> &, const std::string &b) {
    char a[]{as..., '\0'};
    return a + b;
}
 
 
namespace dand {

//#define LOG_DISABLED
//#define DEBUG
    //using danlant::operator""_tstr;

    inline bool constexpr Is64() {
#if defined(__LP64__)
        return true;
#else
        return false;
#endif
    }

    inline constexpr bool is64 = Is64();

    inline bool constexpr IsDebug() {
#ifdef NDEBUG
        return false;
#else
        #pragma clang diagnostic push
        #pragma clang diagnostic warning "-W#warnings"
        #warning "NDEBUG is not defined"
        #pragma clang diagnostic pop
        return true;
#endif
    }

    inline constexpr bool isDebug = IsDebug();

#if defined(__LP64__)
# define LP_SELECT(lp32, lp64) lp64
#else
# define LP_SELECT(lp32, lp64) lp32
#endif

    inline static constexpr auto kLibArtName = "libart.so"_tstr;
    inline static constexpr auto kLibFwName = "libandroidfw.so"_tstr;

    inline constexpr const char *BoolToString(bool b) {
        return b ? "true" : "false";
    }

    extern const int versionCode;
    extern const char* const versionName;
}


