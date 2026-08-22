plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.appdimens.games.logarithmic"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":library"))
    // Compose é opcional em runtime (paridade com :library)
    compileOnly(libs.androidx.compose.runtime)
    compileOnly(libs.androidx.compose.ui)
    // O compilador Compose exige o runtime também na compilação de testes
    testImplementation(libs.androidx.compose.runtime)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
