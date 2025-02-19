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
 * Copyright (C) 2022 DAndroid Contributors
 */
#pragma once

#include <map>

namespace dand {
    using obfuscation_map_t = std::map<std::string, std::string>;

    class ConfigBridge {
    public:
        inline static ConfigBridge *GetInstance() {
            return instance_.get();
        }

        inline static std::unique_ptr<ConfigBridge> ReleaseInstance() {
            return std::move(instance_);
        }

        virtual obfuscation_map_t &obfuscation_map() = 0;

        virtual void obfuscation_map(obfuscation_map_t) = 0;

        virtual ~ConfigBridge() = default;

    protected:
        static std::unique_ptr<ConfigBridge> instance_;
    };
}
