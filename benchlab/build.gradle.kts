plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.benchlab"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.benchlab"
        minSdk = 25
        targetSdk = 37
        versionCode = 1
        versionName = "3.0.0"
    }

    buildTypes {
        release {
            // Minify OFF: as regras consumer do legado games-2.0.1 quebram o parse do R8.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":library"))
    implementation(libs.appdimens.games.legacy)      // Concorrente A: appdimens-games 2.0.1 (depreciada)
    implementation(libs.appdimens.dynamic)           // Concorrente B: appdimens-dynamic 3.1.9
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
