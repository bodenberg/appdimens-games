import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

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

// ─── Publishing (Maven Central) — family parity with appdimens-dynamic ─────
val isJitPack = System.getenv("JITPACK") == "true"
        || System.getenv("jitpack") == "true"
        || System.getenv("CI") == "true"
        || System.getenv("ci") == "true"

mavenPublishing {
    coordinates(
        "io.github.bodenberg",
        "appdimens-games",
        providers.gradleProperty("appdimens.version").orElse("3.0.0").get()
    )
    configure(AndroidSingleVariantLibrary())
    pom {
        name.set("AppDimens Games — Core")
        description.set(
            "AppDimens Games core: GameScreen/GameMetrics/GameCache, GameMath kernels, " +
                "code API, Compose extensions and world layer for games."
        )
        url.set("https://github.com/bodenberg/appdimens-games")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("bodenberg")
                name.set("Jean Bodenberg")
                email.set("jean.bodenberg2@outlook.com")
            }
        }
        scm {
            connection.set("scm:git:github.com/bodenberg/appdimens-games.git")
            developerConnection.set("scm:git:ssh://github.com/bodenberg/appdimens-games.git")
            url.set("https://github.com/bodenberg/appdimens-games")
        }
    }
    if (!isJitPack) {
        publishToMavenCentral()
        signAllPublications()
    }
}
