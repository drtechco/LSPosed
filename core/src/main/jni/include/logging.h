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

#ifndef _LOGGING_H
#define _LOGGING_H

#include <android/log.h>
#include <fmt/format.h>
#include <array>

#ifndef LOG_TAG
#define LOG_TAG    "DAndroid"
#endif

#ifdef LOG_DISABLED
#define LOGD(...) 0
#define LOGV(...) 0
#define LOGI(...) 0
#define LOGW(...) 0
#define LOGE(...) 0
#else
template <typename... T>
constexpr inline void DAND_LOG(int prio, const char* tag, fmt::format_string<T...> fmt, T&&... args) {
    std::array<char, 1024> buf{};
    auto s = fmt::format_to_n(buf.data(), buf.size(), fmt, std::forward<T>(args)...).size;
    buf[s] = '\0';
    __android_log_write(prio, tag, buf.data());
}
#ifndef NDEBUG
#define LOGD(fmt, ...) DAND_LOG(ANDROID_LOG_DEBUG, LOG_TAG, "{}:{}#{}" ": " fmt, __FILE_NAME__, __LINE__, __PRETTY_FUNCTION__ __VA_OPT__(,) __VA_ARGS__)
#define LOGV(fmt, ...) DAND_LOG(ANDROID_LOG_VERBOSE, LOG_TAG, "{}:{}#{}" ": " fmt, __FILE_NAME__, __LINE__, __PRETTY_FUNCTION__ __VA_OPT__(,) __VA_ARGS__)
#else
#define LOGD(...) 0
#define LOGV(...) 0
#endif
#define LOGI(...)  DAND_LOG(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...)  DAND_LOG(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  DAND_LOG(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGF(...)  DAND_LOG(ANDROID_LOG_FATAL, LOG_TAG, __VA_ARGS__)
#define PLOGE(fmt, args...) LOGE(fmt " failed with {}: {}", ##args, errno, strerror(errno))
#endif

#pragma clang diagnostic push
#pragma clang diagnostic warning "-W#warnings"
#ifdef NDEBUG
#warning "NDEBUG is defined"
#else
#warning "NDEBUG is not defined"
#endif 
#pragma clang diagnostic pop

#endif // _LOGGING_H
