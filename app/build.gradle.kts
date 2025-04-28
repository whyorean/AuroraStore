/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
 *  Copyright (C) 2023, grrfe <grrfe@420blaze.it>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

@file:OptIn(KspExperimental::class)

import com.google.devtools.ksp.KspExperimental
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.navigation)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.rikka.tools.refine.plugin)
    alias(libs.plugins.hilt.android.plugin)
}

val lastCommitHash = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.aurora.store"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aurora.store"
        minSdk = 21
        targetSdk = 36

        versionCode = 68
        versionName = "4.7.2"

        testInstrumentationRunner = "com.aurora.store.HiltInstrumentationTestRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        buildConfigField("String", "EXODUS_API_KEY", "\"bbe6ebae4ad45a9cbacb17d69739799b8df2c7ae\"")

        missingDimensionStrategy("device", "vanilla")
    }

    signingConfigs {
        if (File("signing.properties").exists()) {
            create("release") {
                val properties = Properties().apply {
                    File("signing.properties").inputStream().use { load(it) }
                }

                keyAlias = properties["KEY_ALIAS"] as String
                keyPassword = properties["KEY_PASSWORD"] as String
                storeFile = file(properties["STORE_FILE"] as String)
                storePassword = properties["KEY_PASSWORD"] as String
            }
        }
        create("aosp") {
            // Generated from the AOSP test key:
            // https://android.googlesource.com/platform/build/+/refs/tags/android-11.0.0_r29/target/product/security/testkey.pk8
            keyAlias = "testkey"
            keyPassword = "testkey"
            storeFile = file("testkey.jks")
            storePassword = "testkey"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (File("signing.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        register("nightly") {
            initWith(getByName("release"))
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-${lastCommitHash.get()}"
        }

        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("aosp")
        }
    }

    flavorDimensions += "device"

    productFlavors {
        create("vanilla") {
            isDefault = true
            dimension = "device"
        }

        create("huawei") {
            dimension = "device"
            versionNameSuffix = "-hw"
        }

        // This flavor is only for preloaded devices / users who push the app to system
        create("preload") {
            dimension = "device"
            versionNameSuffix = "-preload"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        aidl = true
        compose = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        lintConfig = file("lint.xml")
    }

    androidResources {
        generateLocaleConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        val flavour = variant.flavorName
        if ((flavour == "huawei" || flavour == "preload") && variant.buildType == "nightly") {
            variant.enable = false
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    useKsp2 = false // TODO: Drop after getting rid of epoxy
}

dependencies {

    //Google's Goodies
    implementation(libs.google.android.material)
    implementation(libs.google.protobuf.javalite)

    //AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.paging.runtime)

    implementation(libs.androidx.adaptive.core)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Coil
    implementation(libs.coil.kt)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    //Shimmer
    implementation(libs.facebook.shimmer)

    //Epoxy
    implementation(libs.airbnb.epoxy.android)
    ksp(libs.airbnb.epoxy.processor)

    //HTTP Clients
    implementation(libs.squareup.okhttp)

    //Lib-SU
    implementation(libs.github.topjohnwu.libsu)

    //GPlayApi
    implementation(libs.auroraoss.gplayapi)

    //Shizuku
    compileOnly(libs.rikka.hidden.stub)
    implementation(libs.rikka.tools.refine.runtime)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.lsposed.hiddenapibypass)

    //Test
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.espresso.core)

    //Hilt
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.androidx.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.hilt.android.core)
    implementation(libs.hilt.androidx.work)

    kspAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.hilt.android.testing)

    //Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.process.phoenix)

    // LeakCanary
    debugImplementation(libs.squareup.leakcanary.android)
}
