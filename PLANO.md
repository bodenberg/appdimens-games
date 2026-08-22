# PLANO DETALHADO — Conversão AppDimens → AppDimens Games 3.0 (Unified)

> Branch: `feat/games-3.0-unified` — substitui a biblioteca `appdimens-games` 2.0.1 (depreciada/WIP).
> Fontes analisadas: `appdimens-kmp` 1.0.1 · `appdimens-dynamic` 3.1.9 · `appdimens-games` 2.0.1 · hub `appdimens` (README/família).

---

## 0. Diagnóstico das fontes

### 0.1 appdimens-dynamic 3.1.9 (Android)
- **14 estratégias** modulares (`library`, `library-auto|density|diagonal|fill|fit|fluid|interpolated|logarithmic|percent|perimeter|power|resize|units` + BOM).
- Núcleo: snapshot imutável `DimenMetrics` com fatores pré-computados (uma multiplicação no hot path), cache lock-free por chave 64-bit particionada por snapshot, fast lanes bit-idênticos à matemática legada, watcher de configuração orientado a eventos.
- Constantes canônicas: `BASE_WIDTH_DP=300`, `BASE_HEIGHT_DP=533`, `BASE_DIAGONAL_DP=611.6305`, `BASE_PERIMETER_DP=833`, `REFERENCE_AR=1.78`, `INV_BASE_RATIO=1/300`, `ADJUSTMENT_SCALE=0.10/30`, `SENSITIVITY_DEFAULT=0.08/30`.
- Sufixos: `a` (aspect ratio), `i` (ignora multi-janela → valor base quando restrito), `ia`.
- BenchLab próprio: fases WARMUP/CORE/T1-T3, mediana/P90, checksum anti-DCE, export TXT/PNG.

### 0.2 appdimens-kmp 1.0.1
- Port fiel do Dynamic para KMP (Android/JVM/iOS/macOS/Linux/Win/JS/WASM), mesma matemática e API; `AppDimensContext` neutro de plataforma; provider Compose multiplataforma.

### 0.3 appdimens-games 2.0.1 (depreciada)
- Kotlin + C++/NDK (~8.9k linhas C++). 13 estratégias (`GameScalingStrategy`), `GameDimensionType` (DYNAMIC/FIXED/GAME_WORLD/UI_OVERLAY), Vector2D/Rect, viewport modes (FIT_WIDTH/FIT_HEIGHT/FIT_ALL/STRETCH/CROP), unidades físicas, cache hash próprio, OpenGL utils.
- **Problemas identificados**: gateway singleton pesado (`getInstance()+initialize(context)`); cálculo via enum+hash a cada chamada (hash 64-bit + cache mesmo para multiplicação trivial); ln-lookup com tolerância 0.005 (perde precisão); sem caminho de fator pré-computado por snapshot; sem sufixos da família (`a/i/ia`); sem integração Compose reativa à janela; C++ acoplado a JNI; sem DirectX/C#; documentação mínima ("Under Development").

### 0.4 Conclusão
A nova **AppDimens Games 3.0** unifica: matemática canônica do Dynamic/KMP (bit-compatível) + conceitos exclusivos de jogos da games 2.x (mundo, viewport, vetores) + arquitetura de performance do Dynamic (fatores pré-computados, fast lanes O(1)) + modularidade e convenções de API da família.

---

## 1. Requisitos

| ID | Requisito |
|----|-----------|
| R1 | Substituir `appdimens-games` 2.0.1 em todos os usos (gateway legado mantido para migração) |
| R2 | Superfícies: **JNI/C++ (NDK), C puro, C# (Unity/Godot), Vulkan, DirectX, OpenGL(ES), Compose Games, Kotlin Games, Java Games** |
| R3 | Ajuste automático em redimensionamento de tela/janela; variantes com sufixo **`i` permanecem invariantes** |
| R4 | Mesma família de API: mesmos stems/prefixos/sufixos (`sdp/hdp/wdp/ssp/sem`, `fsdp…`, `a/i/ia`, inversores, facilitadores, builders) |
| R5 | Modularizada (core + satélites por estratégia + native + csharp + sample + benchlab) |
| R6 | Performance ≥ Dynamic/KMP/games-atual: hot path = 1 multiplicação sobre fatores pré-computados por snapshot; zero alocação; cache lock-free |
| R7 | Precisão: `ln()` exato (sem tabela aproximada) nos fatores pré-computados; IEEE-754 float; paridade bit-a-bit com Dynamic nos kernels compartilhados |
| R8 | Documentação nível KMP/Dynamic (badges, tabelas, mermaid, LaTeX, guias por estratégia) |

---

## 2. Arquitetura

```
appdimens-games/
├── library/                     # :games-core  (MÓDULO PRINCIPAL)
│   ├── common/                  # enums compartilhados (qualifiers, inverters, uiMode, element/device types, strategies)
│   ├── core/                    # GameScreenConstants, GameMetrics (snapshot), GameScreen (estado atômico ao vivo),
│   │                            # GameCache (chave 64-bit particionada), MultiWindow, MissingModule
│   ├── math/                    # kernels puros Float das 13 estratégias (sem dependência Android) — fonte única de verdade
│   ├── code/                    # extensões Int/Float.sdp(ctx)… + facade Java + builder fluente + facilitadores
│   ├── compose/                 # @Composable Number.sdp… + AppDimensGamesProvider + remember helpers
│   ├── world/                   # Vec2/Vec3/Rect, ViewportMode (letterbox/crop/stretch), world↔screen, safe area
│   ├── units/                   # mm/cm/inch → px/dp/sp
│   └── resize/                  # auto-fit binário (passos + busca O(log n))
├── library-{auto,density,diagonal,fill,fit,fluid,interpolated,logarithmic,percent,perimeter,power,resize,units}/
│                                # satélites finos (prefixos asdp/dsdp/dgsdp/flsdp/ftsdp/fsdp/isdp/logsdp/prsdp/psdp/pwsdp)
├── library-bom/                 # BOM java-platform
├── library-native/              # :games-native (AAR + C++)
│   └── cpp/include/appdimens/games/*.h   # núcleo header-only cross-plataforma (Win/Linux/Android)
│   ├── cpp/src/*.cpp            # implementações não-inline + JNI
│   └── cpp/c/appdimens_games_c.h/.c     # API C pura p/ engines C
├── csharp/AppDimensGames/       # port C# single-file (Unity asmdef incluído)
├── sample/                      # jogo de teste: menu + demo Compose Canvas + GLSurfaceView + Vulkan mínimo
├── benchlab/                    # laboratório de benchmark (games 3.0 vs games 2.0.1 vs dynamic 3.1.9)
├── scripts/oracle.py            # oráculo Python: valida todas as fórmulas/constantes numericamente
├── DOCUMENTATION/               # docs no padrão da família
└── .github/workflows/ci.yml    # build+test JVM/Android
```

### 2.1 Fluxo de dados (ajuste automático em resize)

```
Activity.onConfigurationChanged / SurfaceView.surfaceChanged / swapchain recreate
        │  (Compose: LocalConfiguration recompõe automaticamente)
        ▼
GameScreen.update(wDp, hDp, swDp, dpi, fontScale, fullscreen?)
        │  publica atomicamente novo GameMetrics (fatores pré-computados p/ TODAS as estratégias)
        ▼
próximas chamadas sdp()/kernels leem o snapshot atual → valores ajustados
        │
        ├─ sem sufixo  : usa métricas da JANELA ATUAL (auto-ajuste total)
        └─ com sufixo i: ignora ajuste de janela redimensionada/multi-janela
                        (ancora na última métrica FULLSCREEN congelada; se multi-janela real,
                         devolve valor base — paridade com Dynamic)
```

### 2.2 Contrato de performance (R6/R7)

| Caminho | Custo | Implementação |
|---|---|---|
| Fast lane dp→px | ~2 ns | `base * metrics.<fator>` (fator pré-computado por snapshot) |
| Fast lane cache hit | ~4 ns | chave 64-bit empacotada + slot atômico |
| Kernel completo (cold) | ~15–40 ns | função pura inline, zero alocação |
| Snapshot rebuild | 1×/resize | lazy factors (`by lazy`) por métrica |
| ln() | exato | `ln` stdlib apenas na criação do snapshot (não no frame) |

---

## 3. Fórmulas canônicas (paridade com Dynamic/KMP)

Constantes: `W₀=300`, `H₀=533`, `D₀=611.6305`, `P₀=833`, `ARref=1.78`, `inv300=1/300`.

| Estratégia | Fórmula |
|---|---|
| SCALED (sdp/hdp/wdp) | `b·(d/300)`; com AR: `b·[1+(d−300)(0.10/30 + K·ln((max/min)/1.78))]`, K=0.08/30 |
| PERCENT (psdp) + space* | igual SCALED; literal `(p/100)·d` |
| POWER (pwsdp) | `b·(d/300)^0.75` |
| FLUID (fsdp) | banda 320–768: lerp entre `b·0.8` e `b·1.2`, patamar fora |
| AUTO (asdp) | `d≤480: b·d/300`; senão `b·(480/300 + 0.4·ln(1+(d−480)/300))` |
| DIAGONAL (dgsdp) | `b·√(min²+max²)/611.6305` |
| FILL (flsdp) | `b·max(min/300, max/533)` |
| FIT (ftsdp) | `b·min(min/300, max/533)` |
| INTERPOLATED (isdp) | `b+(b·d/300−b)/2` |
| LOGARITHMIC (logsdp) | `d>300: b·(1+0.4·ln(d/300))`; senão `b·(1−0.4·ln(300/d))` |
| PERIMETER (prsdp) | `b·(min+max)/833` |
| DENSITY (dsdp) | `b·(dpi/160)` |
| AUTOSIZE/NONE | fit binário / `b` |
| AR multiplier | `1+K·ln((max/min)/1.78)` aplicado opcionalmente às acima |

Jogos adiciona: viewport letterbox (`FIT_ALL/FIT_WIDTH/FIT_HEIGHT/STRETCH/CROP`), escala de mundo (`worldScale = min(w/Ww, h/Hw)` etc.), vetores.

---

## 4. Cronograma de execução (ordem solicitada)

1. **Fase 1 — Módulo principal** (`:games-core`): common, core, math, code, compose, world, units, resize + testes com expectativas do oráculo.
2. **Fase 2 — Variações**: satélites (13) + BOM → `library-native` (C++20 header-first + JNI + C API + helpers GL/Vulkan/DirectX) → `csharp/`.
3. **Fase 3 — Sample app**: menu + demo Compose (HUD escalado, simulação de resize) + demo GLSurfaceView (JNI nativo) + demo Vulkan mínimo.
4. **Fase 4 — BenchLab**: comparativo on-device (novo vs legacy 2.0.1 vs dynamic 3.1.9) com mediana/P90/checksum/export.
5. **Fase 5 — Auditoria & Docs**: oráculo Python validando constantes/fórmulas; revisão estática; README + DOCUMENTATION/* (estilo família) + LLMS.txt.

## 5. Critérios de aceite

- [ ] Todos os kernels batem com o oráculo Python (Δ ≤ 1e-6 relativo em float64; Δ ≤ 0.05 em float32).
- [ ] Paridade semântica dos sufixos `a/i/ia` com Dynamic.
- [ ] Hot path sem alocação e ≤ 2 multiplicações.
- [ ] Resize automático verificado nos 3 motores (Compose/GL/Vulkan path).
- [ ] Sample compila e executa os 3 backends (CI + instruções locais).
- [ ] Docs completas: README + ≥14 docs de módulo/estratégia + matemática + nativo + C# + performance.
