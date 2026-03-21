pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io/") {
            content {
                includeModule("com.github.topjohnwu.libsu", "core")
            }
        }
    }
}
include(":app")
include(":jmods:core-domain")
include(":jmods:core-data")
include(":jmods:core-network")
include(":jmods:core-auth")
include(":jmods:core-installer")
include(":jmods:core-navigation")
include(":jmods:core-database")
include(":jmods:core-ui")
include(":jmods:feature-home")
include(":jmods:feature-details")
include(":jmods:feature-categories")
include(":jmods:feature-search")
include(":jmods:app")

project(":jmods:core-domain").projectDir = file("jmods-android/core-domain")
project(":jmods:core-data").projectDir = file("jmods-android/core-data")
project(":jmods:core-network").projectDir = file("jmods-android/core-network")
project(":jmods:core-auth").projectDir = file("jmods-android/core-auth")
project(":jmods:core-installer").projectDir = file("jmods-android/core-installer")
project(":jmods:core-navigation").projectDir = file("jmods-android/core-navigation")
project(":jmods:core-database").projectDir = file("jmods-android/core-database")
project(":jmods:core-ui").projectDir = file("jmods-android/core-ui")
project(":jmods:feature-home").projectDir = file("jmods-android/feature-home")
project(":jmods:feature-details").projectDir = file("jmods-android/feature-details")
project(":jmods:feature-categories").projectDir = file("jmods-android/feature-categories")
project(":jmods:feature-search").projectDir = file("jmods-android/feature-search")
project(":jmods:app").projectDir = file("jmods-android/app")

rootProject.name = "JMODS"
