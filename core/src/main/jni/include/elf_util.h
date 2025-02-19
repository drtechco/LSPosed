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
 * Copyright (C) 2019 Swift Gan
 * Copyright (C) 2021 DAndroid Contributors
 */
#ifndef SANDHOOK_ELF_UTIL_H
#define SANDHOOK_ELF_UTIL_H

#include <string_view>
#include <map>
#include <vector>
#include <concepts>
#include <linux/elf.h>
#include <link.h>
#include <xdl.h>
#include "logging.h"

namespace SandHook {

class ElfImg {
public:
    explicit ElfImg(std::string_view base_name);
    ~ElfImg();

    // Delete copy constructor and assignment
    ElfImg(const ElfImg&) = delete;
    ElfImg& operator=(const ElfImg&) = delete;

    template<typename T = void*>
    requires(std::is_pointer_v<T>)
    constexpr const T getSymbAddress(std::string_view name) const {
        auto offset = getSymbOffset(name, GnuHash(name), ElfHash(name));
        if (offset > 0 && base != nullptr) {
            return reinterpret_cast<T>(static_cast<ElfW(Addr)>((uintptr_t)base + offset));
        }
        return nullptr;
    }

    template<typename T = void*>
    requires(std::is_pointer_v<T>)
    constexpr const T getSymbPrefixFirstAddress(std::string_view prefix) const {
        auto offset = getSymbPrefixOffset(prefix);
        if (offset > 0 && base != nullptr) {
            return reinterpret_cast<T>(static_cast<ElfW(Addr)>((uintptr_t)base + offset ));
        }
        return nullptr;
    }

    template<typename T = void*>
    requires(std::is_pointer_v<T>)
    const std::vector<T> getAllSymbAddress(std::string_view name) const {
        auto offsets = getRangeSymbOffset(name);
        std::vector<T> res;
        res.reserve(offsets.size());
        for (const auto &offset : offsets) {
            res.push_back(reinterpret_cast<T>(static_cast<ElfW(Addr)>((uintptr_t)base + offset)));
        }
        return res;
    }

    bool isValid() const { return base != nullptr && handle != nullptr; }
    const std::string name() const { return elf; }
    void DumpSymbols() const;

private:
    constexpr static uint32_t ElfHash(std::string_view name) {
        uint32_t h = 0, g;
        for (unsigned char p: name) {
            h = (h << 4) + p;
            g = h & 0xf0000000;
            h ^= g;
            h ^= g >> 24;
        }
        return h;
    }

    constexpr static uint32_t GnuHash(std::string_view name) {
        uint32_t h = 5381;
        for (unsigned char p: name) {
            h += (h << 5) + p;
        }
        return h;
    }

    ElfW(Addr) getSymbOffset(std::string_view name, uint32_t gnu_hash, uint32_t elf_hash) const;
    ElfW(Addr) getSymbPrefixOffset(std::string_view prefix) const;
    std::vector<ElfW(Addr)> getRangeSymbOffset(std::string_view name) const;
    
    void initializeTables() const;
    bool findModuleBase();

    void* base = nullptr;
    void* handle = nullptr;
    std::string elf;
    off_t bias = -4396;
    mutable std::map<std::string_view, ElfW(Sym)*> symtabs_;
    mutable void* addr_cache = nullptr;
    mutable bool initialized = false;
};

} // namespace SandHook

#endif //SANDHOOK_ELF_UTIL_H