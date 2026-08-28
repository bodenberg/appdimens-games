import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    // Unifica a versão do compilador Kotlin (o plugin Compose traz o KGP 2.4)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.appdimens.games.resize"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom.platform))
    api(project(":library"))
    // Runtime exigido pelo compilador Compose (compileOnly: não vaza p/ consumidores)
    compileOnly(libs.androidx.compose.runtime)
    testImplementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit)
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
        "appdimens-games-resize",
        providers.gradleProperty("appdimens.version").orElse("3.0.0").get()
    )
    configure(AndroidSingleVariantLibrary())
    pom {
        name.set("AppDimens Games — Resize")
        description.set("Container auto-fit helpers (binary-search fit) for game UIs.")
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
