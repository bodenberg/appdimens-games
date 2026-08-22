pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "AppDimens Games"

include(":library")
include(":library-bom")
include(":library-auto")
include(":library-density")
include(":library-diagonal")
include(":library-fill")
include(":library-fit")
include(":library-fluid")
include(":library-interpolated")
include(":library-logarithmic")
include(":library-percent")
include(":library-perimeter")
include(":library-power")
include(":library-resize")
include(":library-units")
include(":library-native")
include(":sample")
include(":benchlab")
