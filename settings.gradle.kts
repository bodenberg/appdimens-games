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
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.maven.apache.org/maven2") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.maven.apache.org/maven2") }
    }
}

rootProject.name = "AppDimens"
include(":appdimens_games_core")
include(":appdimens_games_native")
include(":appdimens_games_android")
include(":appdimens_games_graphics")
include(":appdimens_games_compose")
include(":appdimens_games")
include(":appdimens_games_bom")
