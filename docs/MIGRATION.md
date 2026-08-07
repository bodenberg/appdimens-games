# Migração da biblioteca obsoleta

A v3 é um rewrite e usa o namespace `io.github.bodenberg.appdimens.games`.

| API antiga | v3 |
|---|---|
| singleton nativo global | `Calculator` stateless ou `NativeEngine` explícito |
| enum ordinal implícito | `Strategy.id` / `adg_strategy` estável |
| chamada JNI scalar | `NativeEngine.scaleInPlace` |
| utilitário OpenGL com estado | `GraphicsViewport` renderer-neutral |
| objetos de tela duplicados | `Screen` único |
| headers C++ internos | ABI C publicado por Prefab |

Não misture os binários antigos e v3 no mesmo processo. Verifique `NativeEngine.isAvailable()`
na inicialização; decida explicitamente entre motor JVM e nativo e mantenha essa escolha.
