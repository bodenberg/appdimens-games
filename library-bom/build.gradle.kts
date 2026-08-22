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
