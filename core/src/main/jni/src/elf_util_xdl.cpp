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

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cinttypes>
#include "elf_util.h"

namespace SandHook {
    
ElfImg::ElfImg(std::string_view base_name) : elf(base_name) {
    LOGD("Initializing ElfImg for {}", base_name);
    
    if (!findModuleBase()) {
        LOGE("Failed to find module base for {}", base_name);
        return;
    }
    
    // Open library using xdl
    handle = xdl_open(elf.c_str(), XDL_DEFAULT);
    if (!handle) {
        LOGE("Failed to open {} with xdl", elf);
        return;
    }

    // Get base address and verify load
    xdl_info_t info;
    if (xdl_info(handle, XDL_DI_DLINFO, &info) < 0) {
        LOGE("Failed to get info for {}", elf);
        xdl_close(handle);
        handle = nullptr;
        return;
    }
    
    if (bias == -4396) {  // Only update bias if it hasn't been set
        bias = static_cast<off_t>(reinterpret_cast<uintptr_t>(info.dli_fbase));
        LOGD("Set bias to {:#x}", bias);
    }
    
    LOGD("Successfully mapped library, base: {:#x}, bias: {:#x}", 
         reinterpret_cast<uintptr_t>(base), bias);
}

ElfImg::~ElfImg() {
    LOGD("Cleaning up ElfImg for {}", elf);
    if (handle) {
        xdl_close(handle);
        handle = nullptr;
    }
    if (addr_cache) {
        xdl_addr_clean(&addr_cache);
    }
}

bool ElfImg::findModuleBase() {
    LOGD("Finding module base for {}", elf);
    off_t load_addr;
    bool found = false;
    FILE *maps = fopen("/proc/self/maps", "r");
    if (!maps) {
        LOGE("Failed to open /proc/self/maps");
        return false;
    }

    char *buff = nullptr;
    size_t len = 0;
    while (getline(&buff, &len, maps) != -1) {
        std::string_view line{buff};

        if ((line.find("r-xp") != std::string_view::npos ||
             line.find("r--p") != std::string_view::npos) &&
             line.find(elf) != std::string_view::npos) {

            LOGD("Found matching maps entry: {}", line);
            if (auto begin = line.find_last_of(' '); begin != std::string_view::npos &&
                                                     line[++begin] == '/') {
                std::string_view new_path = line.substr(begin);
                // Remove newline if present
                if (new_path.back() == '\n') {
                    new_path = new_path.substr(0, new_path.length() - 1);
                }
                elf = std::string(new_path);
                LOGD("Updated path: {}", elf);
            }
            uintptr_t temp_addr;
            if (sscanf(buff, "%" PRIxPTR, &temp_addr) == 1) {
                load_addr = temp_addr;
                base = reinterpret_cast<void*>(load_addr);
                found = true;
                break;
            }
        }
    }

    if (buff) free(buff);
    fclose(maps);

    if (found) {
        LOGD("Found module base at {:#x}", reinterpret_cast<uintptr_t>(base));
    } else {
        LOGE("Failed to find module base for {}", elf);
    }
    return found;
}

void ElfImg::initializeTables() const {
    if (initialized) return;
    LOGD("Initializing symbol tables");
    
    auto callback = [](struct dl_phdr_info* info, size_t size, void* data) {
        auto* self = static_cast<ElfImg*>(data);
        if (info->dlpi_name && self->elf == info->dlpi_name) {
            xdl_info_t sym_info;
            if (xdl_addr4(reinterpret_cast<void*>(info->dlpi_addr), 
                         &sym_info, &self->addr_cache, XDL_DEFAULT) == 0) {
                if (sym_info.dli_sname) {
                    LOGD("Adding symbol {} to table", sym_info.dli_sname);
                    self->symtabs_.emplace(sym_info.dli_sname, 
                        reinterpret_cast<ElfW(Sym)*>(sym_info.dli_saddr));
                }
            }
            return 1;  // Found our target, stop iteration
        }
        return 0;  // Continue searching
    };
    
    xdl_iterate_phdr(callback, const_cast<ElfImg*>(this), XDL_DEFAULT);
    initialized = true;
    LOGD("Initialized symbol tables with {} entries", symtabs_.size());
}

ElfW(Addr) ElfImg::getSymbOffset(std::string_view name, uint32_t gnu_hash, uint32_t elf_hash) const {
    LOGD("Looking up symbol: {}, gnu_hash: {:#x}, elf_hash: {:#x}", name, gnu_hash, elf_hash);
    
    if (!handle) return 0;

    size_t size;
    void* addr = xdl_sym(handle, std::string(name).c_str(), &size);
    if (!addr) {
        LOGD("Symbol not found in normal symbols, trying debug symbols");
        addr = xdl_dsym(handle, std::string(name).c_str(), &size);
    }
    
    if (addr) {
        auto offset = reinterpret_cast<ElfW(Addr)>(addr) - reinterpret_cast<ElfW(Addr)>(base);
        LOGD("Found symbol at offset {:#x} with size {},{}", offset, size,name);
        return offset;
    }
    
    LOGD("Symbol not found:{}",name);
    return 0;
}

std::vector<ElfW(Addr)> ElfImg::getRangeSymbOffset(std::string_view name) const {
    LOGD("Looking up all instances of symbol: {}", name);
    std::vector<ElfW(Addr)> results;
    initializeTables();

    for (const auto& [sym_name, sym] : symtabs_) {
        if (sym_name == name) {
            auto offset = reinterpret_cast<ElfW(Addr)>(sym) - reinterpret_cast<ElfW(Addr)>(base);
            results.push_back(offset);
            LOGD("Found instance at offset {:#x}", offset);
        }
    }
    
    LOGD("Found {} instances of symbol {}", results.size(), name);
    return results;
}

ElfW(Addr) ElfImg::getSymbPrefixOffset(std::string_view prefix) const {
    LOGD("Looking up first symbol with prefix: {}", prefix);
    
    initializeTables();
    for (const auto& [sym_name, sym] : symtabs_) {
        if (sym_name.starts_with(prefix)) {
            auto offset = reinterpret_cast<ElfW(Addr)>(sym) - reinterpret_cast<ElfW(Addr)>(base);
            LOGD("Found matching symbol {} at offset {:#x}", sym_name, offset);
            return offset;
        }
    }
    
    LOGD("No symbols found with prefix {}", prefix);
    return 0;
}

void ElfImg::DumpSymbols() const {
    LOGD("=========== Dumping All Symbols ===========");
    
    initializeTables();
    
    LOGD("Total symbols in map: {}", symtabs_.size());
    for (const auto& [name, sym] : symtabs_) {
        auto offset = reinterpret_cast<ElfW(Addr)>(sym) - reinterpret_cast<ElfW(Addr)>(base);
        LOGD("Symbol: {}, Offset: {:#x}, Address: {:#x}", 
             name, offset, reinterpret_cast<uintptr_t>(sym));
    }
    
    // Get additional symbol information using xdl
    if (handle) {
        xdl_info_t info;
        // Dump dynamic symbols
        LOGD("\nDynamic symbols:");
        if (xdl_info(handle, XDL_DI_DLINFO, &info) == 0 && info.dli_sname) {
            LOGD("  {}: addr={:#x}, size={}, base={:#x}", 
                 info.dli_sname, 
                 reinterpret_cast<uintptr_t>(info.dli_saddr),
                 info.dli_ssize,
                 reinterpret_cast<uintptr_t>(info.dli_fbase));
        }
    }
    
    LOGD("=========== Symbol Dump End ===========");
}

} // namespace SandHook