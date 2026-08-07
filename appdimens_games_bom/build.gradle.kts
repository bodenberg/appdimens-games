plugins {
    `java-platform`
    `maven-publish`
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        val version = providers.gradleProperty("VERSION_NAME").get()
        api("io.github.bodenberg:appdimens-games-core:$version")
        api("io.github.bodenberg:appdimens-games-native:$version")
        api("io.github.bodenberg:appdimens-games-android:$version")
        api("io.github.bodenberg:appdimens-games-graphics:$version")
        api("io.github.bodenberg:appdimens-games-compose:$version")
        api("io.github.bodenberg:appdimens-games-auto:$version")
        api("io.github.bodenberg:appdimens-games-density:$version")
        api("io.github.bodenberg:appdimens-games-diagonal:$version")
        api("io.github.bodenberg:appdimens-games-fill:$version")
        api("io.github.bodenberg:appdimens-games-fit:$version")
        api("io.github.bodenberg:appdimens-games-fluid:$version")
        api("io.github.bodenberg:appdimens-games-interpolated:$version")
        api("io.github.bodenberg:appdimens-games-logarithmic:$version")
        api("io.github.bodenberg:appdimens-games-percent:$version")
        api("io.github.bodenberg:appdimens-games-perimeter:$version")
        api("io.github.bodenberg:appdimens-games-power:$version")
        api("io.github.bodenberg:appdimens-games-resize:$version")
        api("io.github.bodenberg:appdimens-games:$version")
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
            artifactId = "appdimens-games-bom"
            pom {
                name.set("AppDimens Games BOM")
                description.set("Aligned versions for the modular AppDimens Games distribution")
                url.set("https://github.com/bodenberg/appdimens-games")
            }
        }
    }
}
