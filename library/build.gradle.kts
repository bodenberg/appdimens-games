plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    // Namespace ≠ pacote da API pública de propósito: o legado games-2.0.1 usa
    // `com.appdimens.games` como package de manifesto e o AGP 9 proíbe duplicatas.
    namespace = "com.appdimens.games.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    api(libs.androidx.annotation)
    // Compose é opcional em runtime: a API code (Kotlin/Java) funciona sem Compose.
    compileOnly(libs.androidx.compose.runtime)
    compileOnly(libs.androidx.compose.ui)
    testImplementation(libs.junit)
    // O compilador Compose exige o runtime visível na compilação dos testes
    testImplementation(libs.androidx.compose.runtime)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
