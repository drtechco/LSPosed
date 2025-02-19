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

plugins {
    alias(libs.plugins.agp.lib)
}

android {
    namespace = "com.google.dex2oat"

    buildFeatures {
        androidResources = false
        buildConfig = false
        prefab = true
        prefabPublishing = true
    }

    defaultConfig {
        minSdk = 29
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    prefab {
        register("dex2oat")
    }
}
