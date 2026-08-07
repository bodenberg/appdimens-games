plugins { alias(libs.plugins.android.library); `maven-publish` }
group = providers.gradleProperty("GROUP").get(); version = providers.gradleProperty("VERSION_NAME").get()
android {
    namespace = "io.github.bodenberg.appdimens.games.ndk"; compileSdk = 36
    ndkVersion = "30.0.15729638"
    defaultConfig {
        minSdk = 23
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64") }
        externalNativeBuild { cmake { cppFlags += listOf("-std=c++17", "-fno-exceptions", "-fno-rtti"); arguments += "-DANDROID_STL=c++_static" } }
    }
    buildFeatures { prefabPublishing = true }
    prefab { create("appdimens_games") { headers = "src/main/cpp/include" } }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "4.1.2" } }
}
