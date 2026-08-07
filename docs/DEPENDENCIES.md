# Auditoria de dependências

Auditoria realizada em 7 de agosto de 2026 diretamente nos metadados oficiais do
[Google Maven](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml),
[Compose BOM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml),
[Maven Central](https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml),
[Gradle](https://services.gradle.org/versions/current) e no repositório do Android SDK.

| Componente | Versão adotada | Decisão |
|---|---:|---|
| Android Gradle Plugin | 9.3.1 | release estável absoluto mais recente no Google Maven |
| Gradle Wrapper | 9.7.0 | release estável absoluto mais recente, com SHA-256 oficial fixado |
| Kotlin/Compose plugin | 2.4.10 | release estável absoluto mais recente; Android usa o suporte Kotlin built-in do AGP 9 |
| Compose BOM | 2026.06.01 | release estável mais recente no Google Maven |
| Android SDK | 36 | API estável usada para compilação |
| Build Tools | 36.1.0 | revisão estável da geração 36 |
| Android NDK | 30.0.15729638 | pacote estável atual, com alinhamento de páginas de 16 KiB nas ABIs 64-bit |
| CMake | 4.1.2 | pacote estável atual do Android SDK |
| JUnit 4 | 4.13.2 | release final mais recente da linha JUnit 4 |

O lint do AGP 9.3.1 já anuncia `compileSdk 37`, porém o repositório estável oficial do
Android SDK ainda não publica `platforms;android-37`. A biblioteca permanece em API 36
até que o pacote possa ser instalado de forma reproduzível, sem depender de preview.

O catálogo foi reduzido às quatro dependências realmente consumidas. Isso evita alertas
falsos, resolução desnecessária e atualizações de bibliotecas que não fazem parte do binário.
O plugin legado `org.jetbrains.kotlin.android` foi removido porque o AGP 9 fornece Kotlin
diretamente. O plugin `org.jetbrains.kotlin.plugin.compose` continua explícito para manter o
compilador Compose sincronizado com Kotlin 2.4.10. Atualizações futuras de major devem ser
acompanhadas de build limpo dos AARs, lint e inspeção do metadata Kotlin.

O wrapper verifica a distribuição antes da execução por meio de `distributionSha256Sum`.
Dependabot monitora Gradle e GitHub Actions semanalmente, mas nenhuma atualização automática
deve ser mesclada sem `clean check lint assembleRelease`.
