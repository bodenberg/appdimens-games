plugins {
    alias(libs.plugins.android.library)
    // Unifica a versão do compilador Kotlin (o plugin Compose traz o KGP 2.4)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.appdimens.games.units"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":library"))
    // Runtime exigido pelo compilador Compose (compileOnly: não vaza p/ consumidores)
    compileOnly(libs.androidx.compose.runtime)
    testImplementation(libs.androidx.compose.runtime)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
