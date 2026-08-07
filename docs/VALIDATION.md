# Matriz de validação

| Camada | Validação |
|---|---|
| core Java | Java 17, lint estrito e testes de estratégias/invariantes |
| C ABI | GCC/Clang warnings-as-errors, CTest, erros, overlap e viewport |
| Android/JNI | build das quatro ABIs, ABI version handshake e bulk array |
| distribuição | Gradle `check`, release AAR, Prefab e BOM |
| docs | `git diff --check`; imagens somente SVG |

`tools/validate.sh` fornece a validação host reproduzível. O pipeline Android deve executar
`./gradlew check assembleRelease` com Google Maven, Android SDK 36 e NDK/CMake instalados.

## GitHub Actions

* `ci.yml` executa contratos host e, em job isolado, testes, lint e AARs Android.
* `codeql.yml` audita Java/Kotlin e C++ em pushes, pull requests e semanalmente.
* `release.yml` aceita somente a tag `v3.1.6`, repete todas as validações, coleta AARs/JARs,
  gera `SHA256SUMS` e publica os binários no GitHub Release.
* Dependabot acompanha separadamente Gradle e as actions utilizadas pelos workflows.
