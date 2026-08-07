# Auditoria de dependências

Auditoria realizada em 7 de agosto de 2026 diretamente nos metadados oficiais do
[Google Maven](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml),
[Compose BOM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml),
[Maven Central](https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml),
[Gradle](https://services.gradle.org/versions/current) e no repositório do Android SDK.

| Componente | Versão adotada | Decisão |
|---|---:|---|
| Android Gradle Plugin | 8.13.2 | último patch estável da linha 8, sem migração disruptiva ao built-in Kotlin do AGP 9 |
| Gradle Wrapper | 8.14.4 | elimina o aviso de compatibilidade do Kotlin 2.2 e possui SHA-256 fixado |
| Kotlin/Compose plugin | 2.2.21 | último patch 2.2 compatível com AGP 8.13; 2.4.10 falhou na validação por metadata incompatível |
| Compose BOM | 2026.06.01 | release estável mais recente no Google Maven |
| Android SDK | 36 | API estável usada para compilação |
| Build Tools | 36.1.0 | revisão estável da geração 36 |
| Android NDK | 30.0.15729638 | pacote estável atual, com alinhamento de páginas de 16 KiB nas ABIs 64-bit |
| CMake | 4.1.2 | pacote estável atual do Android SDK |
| JUnit 4 | 4.13.2 | release final mais recente da linha JUnit 4 |

O catálogo foi reduzido às quatro dependências realmente consumidas. Isso evita alertas
falsos, resolução desnecessária e atualizações de bibliotecas que não fazem parte do binário.
Atualizações de major do AGP/Kotlin devem ocorrer em uma alteração dedicada, acompanhadas de
build limpo de todos os AARs, lint e inspeção do metadata Kotlin.

O wrapper verifica a distribuição antes da execução por meio de `distributionSha256Sum`.
Dependabot monitora Gradle e GitHub Actions semanalmente, mas nenhuma atualização automática
deve ser mesclada sem `clean check lint assembleRelease`.
