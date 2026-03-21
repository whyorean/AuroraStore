plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.android.library")
}

android {
    namespace = "com.jmods.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("javax.inject:javax.inject:1")
}
