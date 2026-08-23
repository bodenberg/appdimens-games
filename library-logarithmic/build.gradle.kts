import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

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

// ─── Publishing (Maven Central) ────────────────────────────────────────────
val isJitPack = System.getenv("JITPACK") == "true"
        || System.getenv("jitpack") == "true"
        || System.getenv("CI") == "true"
        || System.getenv("ci") == "true"

mavenPublishing {
    coordinates(
        "io.github.bodenberg",
        "appdimens-games-logarithmic",
        providers.gradleProperty("appdimens.version").orElse("3.0.0").get()
    )
    configure(AndroidSingleVariantLibrary())
    pom {
        name.set("AppDimens Games — Logarithmic")
        description.set("Logarithmic scaling strategy (logsdp) for games.")
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
