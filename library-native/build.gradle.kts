import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    // Unifica a versão do compilador Kotlin (o plugin Compose traz o KGP 2.4)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.appdimens.games.jni"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
                cppFlags += "-std=c++20"
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
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

// ─── Publishing (Maven Central) ────────────────────────────────────────────
val isJitPack = System.getenv("JITPACK") == "true"
        || System.getenv("jitpack") == "true"
        || System.getenv("CI") == "true"
        || System.getenv("ci") == "true"

mavenPublishing {
    coordinates(
        "io.github.bodenberg",
        "appdimens-games-native",
        providers.gradleProperty("appdimens.version").orElse("3.0.0").get()
    )
    configure(AndroidSingleVariantLibrary())
    pom {
        name.set("AppDimens Games — Native")
        description.set(
            "AppDimens Games native: C++20 core, C99 header, JNI bridge and render interop " +
                "(OpenGL/Vulkan) for games."
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
