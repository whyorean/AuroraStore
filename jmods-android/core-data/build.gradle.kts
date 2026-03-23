plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.android.library")
    alias(libs.plugins.hilt.android.plugin)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jmods.data"
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
    implementation(project(":jmods:core-network"))
    implementation(project(":jmods:core-database"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android.core)
    implementation(libs.hilt.androidx.work)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.androidx.compiler)
}
