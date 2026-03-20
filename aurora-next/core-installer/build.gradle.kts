plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.android.library")
    alias(libs.plugins.hilt.android.plugin)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aurora.next.installer"
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
    implementation(project(":aurora-next:core-domain"))
    implementation(libs.hilt.android.core)
    ksp(libs.hilt.android.compiler)
}
