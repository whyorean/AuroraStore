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
        // Only included in huawei variants
        maven("https://developer.huawei.com/repo/") {
            content {
                includeGroup("com.huawei.hms")
                includeGroup("com.huawei.android.hms")
            }
        }
    }
}
include(":app")
include(":aurora-next:core-domain")
include(":aurora-next:core-data")
include(":aurora-next:core-network")
include(":aurora-next:core-auth")
include(":aurora-next:core-installer")
include(":aurora-next:core-navigation")
include(":aurora-next:feature-home")
include(":aurora-next:feature-details")
include(":aurora-next:app")

rootProject.name = "AuroraStore4"
