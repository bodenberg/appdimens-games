plugins { alias(libs.plugins.android.library) }
group = providers.gradleProperty("GROUP").get(); version = providers.gradleProperty("VERSION_NAME").get()
android {
    namespace = "io.github.bodenberg.appdimens.games"; compileSdk = 36
    defaultConfig { minSdk = 23 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    api(project(":appdimens_games_android"))
    api(project(":appdimens_games_graphics"))
    api(project(":appdimens_games_compose"))
    api(project(":appdimens_games_auto"))
    api(project(":appdimens_games_density"))
    api(project(":appdimens_games_diagonal"))
    api(project(":appdimens_games_fill"))
    api(project(":appdimens_games_fit"))
    api(project(":appdimens_games_fluid"))
    api(project(":appdimens_games_interpolated"))
    api(project(":appdimens_games_logarithmic"))
    api(project(":appdimens_games_percent"))
    api(project(":appdimens_games_perimeter"))
    api(project(":appdimens_games_power"))
    api(project(":appdimens_games_resize"))
}
