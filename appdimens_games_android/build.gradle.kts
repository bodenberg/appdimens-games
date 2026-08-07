plugins { alias(libs.plugins.android.library) }
group = providers.gradleProperty("GROUP").get(); version = providers.gradleProperty("VERSION_NAME").get()
android { namespace = "io.github.bodenberg.appdimens.games.android"; compileSdk = 36; defaultConfig { minSdk = 23 } }
dependencies { api(project(":appdimens_games_core")); runtimeOnly(project(":appdimens_games_native")) }
