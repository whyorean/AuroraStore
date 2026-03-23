plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.android.library")
    alias(libs.plugins.hilt.android.plugin)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jmods.network"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":jmods:core-domain"))
    implementation(project(":jmods:core-auth"))
    implementation(libs.auroraoss.gplayapi)
    implementation(libs.squareup.okhttp)
    implementation(libs.google.gson)
    implementation(libs.hilt.android.core)
    ksp(libs.hilt.android.compiler)
}
