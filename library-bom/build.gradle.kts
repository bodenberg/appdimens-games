import com.vanniktech.maven.publish.JavaPlatform

plugins {
    `java-platform`
    alias(libs.plugins.vanniktech.maven.publish)
}

dependencies {
    constraints {
        val v = providers.gradleProperty("appdimens.version").orElse("3.0.0").get()
        api("io.github.bodenberg:appdimens-games:$v")
        listOf(
            "auto", "density", "diagonal", "fill", "fit", "fluid",
            "interpolated", "logarithmic", "percent", "perimeter", "power",
            "resize", "units"
        ).forEach { s ->
            api("io.github.bodenberg:appdimens-games-$s:$v")
        }
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
        "appdimens-games-bom",
        providers.gradleProperty("appdimens.version").orElse("3.0.0").get()
    )
    configure(JavaPlatform())
    pom {
        name.set("AppDimens Games — BOM")
        description.set(
            "Bill of Materials for AppDimens Games — version constraints for " +
                "appdimens-games and appdimens-games-<strategy> modules."
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
