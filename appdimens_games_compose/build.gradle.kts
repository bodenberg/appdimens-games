import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose) }
group = providers.gradleProperty("GROUP").get(); version = providers.gradleProperty("VERSION_NAME").get()
android {
    namespace = "io.github.bodenberg.appdimens.games.compose"; compileSdk = 36
    defaultConfig { minSdk = 23 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
dependencies { api(project(":appdimens_games_core")); implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.compose.runtime); implementation(libs.androidx.compose.ui) }
