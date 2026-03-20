plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    id("com.android.library")
    alias(libs.plugins.hilt.android.plugin)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aurora.next.feature.details"
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":aurora-next:core-domain"))
    implementation(project(":aurora-next:core-navigation"))
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.hilt.android.core)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.ui.ktx)
}
