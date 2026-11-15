// Root build file. Plugin versions are managed in gradle/libs.versions.toml.
plugins {
    // Necessário com configuration cache: o plugin vanniktech usa um build service
    // compartilhado entre módulos irmãos e precisa de AGP + plugin no classpath do
    // root; declarar com `apply false` evita o conflito de classloaders
    // (mesma correção usada no appdimens-dynamic).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    // O AGP 9 traz o KGP embutido (2.2.x) para o classloader do root; sem declarar o
    // plugin Compose aqui, os módulos enxergam essa versão e o :sample tenta resolver
    // org.jetbrains.kotlin:compose-group-mapping:2.2.10 (inexistente) no release.
    alias(libs.plugins.kotlin.compose) apply false
}
