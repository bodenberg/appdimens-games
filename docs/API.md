# API e estratégias

O grid padrão é 300 × 533 dp. O motor normaliza a menor dimensão como largura de design,
portanto a mesma configuração funciona em portrait e landscape. `ScaleConfig` permite trocar
grid, sensibilidade, expoente, transição, breakpoints e constraints.

| Estratégia | Fórmula resumida | Uso em jogos |
|---|---|---|
| `NONE` | `v` | coordenadas já normalizadas |
| `DEFAULT` | crescimento linear moderado + aspecto | HUD e menus |
| `PERCENTAGE` | `v × W/W₀` | containers proporcionais |
| `BALANCED` | linear até transição; log após | entidades e uso geral |
| `LOGARITHMIC` | `v × (1+s×ln(W/W₀))` | TVs e telas grandes |
| `POWER` | `v × (W/W₀)^e` | ajuste perceptual configurável |
| `FLUID` | interpolação limitada por breakpoints | tipografia |
| `INTERPOLATED` | metade do delta linear | objetos secundários |
| `DIAGONAL` | razão das diagonais | targets físicos |
| `PERIMETER` | razão dos perímetros | escala geral 2D |
| `FIT` | `min(W/W₀,H/H₀)` | canvas inteiro/letterbox |
| `FILL` | `max(W/W₀,H/H₀)` | backgrounds/crop |
| `AUTOSIZE` | interpolação limitada | elementos com bounds |

Toda saída passa por `[minValue,maxValue]`. Entradas NaN, infinitas, negativas ou telas
inválidas lançam `IllegalArgumentException` no core e retornam `ADG_INVALID_ARGUMENT` no C.

## Viewport

`Viewport.calculate` oferece FIT, FILL e STRETCH, considera cutouts/system bars e retorna
scale, offsets e área final. O tipo não depende de Android nem de uma API gráfica.

## Inferência

`StrategySelector` é determinístico. HUD usa DEFAULT/FLUID, mundo e backgrounds usam
BALANCED/FILL e regiões físicas usam DIAGONAL. A seleção explícita sempre é preferível
quando a direção de arte exige comportamento específico.
