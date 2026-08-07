plugins { `java-library`; `maven-publish` }
group = providers.gradleProperty("GROUP").get(); version = providers.gradleProperty("VERSION_NAME").get()
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }; withSourcesJar(); withJavadocJar() }
tasks.test { useJUnit() }
dependencies { testImplementation(libs.junit) }
