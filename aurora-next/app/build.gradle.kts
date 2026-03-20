plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.hilt.android.plugin)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aurora.next"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aurora.next"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    implementation(project(":aurora-next:core-data"))
    implementation(project(":aurora-next:core-network"))
    implementation(project(":aurora-next:core-navigation"))
    implementation(project(":aurora-next:feature-home"))
    implementation(project(":aurora-next:feature-details"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.hilt.android.core)
    ksp(libs.hilt.android.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation(libs.androidx.navigation.ui.ktx)
}
