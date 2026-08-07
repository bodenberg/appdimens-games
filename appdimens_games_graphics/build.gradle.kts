plugins { `java-library` }
group = providers.gradleProperty("GROUP").get(); version = providers.gradleProperty("VERSION_NAME").get()
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies { api(project(":appdimens_games_core")) }
