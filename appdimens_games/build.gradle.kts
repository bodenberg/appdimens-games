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
}
