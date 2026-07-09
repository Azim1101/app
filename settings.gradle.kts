pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "VDub"
include(":app")
include(":core")
include(":domain")
include(":data")
include(":ml")
include(":media")
include(":database")
include(":feature-home")
include(":feature-models")
include(":feature-processing")
include(":feature-history")
include(":feature-settings")
