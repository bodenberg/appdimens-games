package com.appdimens.games.common

/**
 * [EN] Unified scaling strategies for AppDimens Games 3.0.
 * Math is bit-compatible with `appdimens-dynamic` / `appdimens-kmp`.
 *
 * [PT] Estratégias de escalonamento unificadas do AppDimens Games 3.0.
 * A matemática é compatível bit-a-bit com `appdimens-dynamic` / `appdimens-kmp`.
 */
enum class GameScalingStrategy {
    /** ~97% linear + AR adjustment. HUD, buttons, icons. */
    DEFAULT,
    /** 100% linear proportional (`b·d/300`). Containers, world bounds. */
    PERCENTAGE,
    /** Linear up to 480 dp + log above. RECOMMENDED default for gameplay elements. */
    BALANCED,
    /** Pure Weber-Fechner logarithm. TV/large screens. */
    LOGARITHMIC,
    /** Stevens power law `(d/300)^0.75`. Configurable. */
    POWER,
    /** CSS-clamp-like interpolation in the 320–768 band. Typography. */
    FLUID,
    /** 50% moderated linear. Secondary objects. */
    INTERPOLATED,
    /** Diagonal ratio `√(min²+max²)/611.6305`. Touch targets. */
    DIAGONAL,
    /** Perimeter ratio `(min+max)/833`. Balanced general use. */
    PERIMETER,
    /** Letterbox: `min(min/300, max/533)`. Full-viewport content. */
    FIT,
    /** Cover: `max(min/300, max/533)`. Backgrounds/parallax. */
    FILL,
    /** Container-aware auto fit (binary search). Dynamic HUD text. */
    AUTOSIZE,
    /** No scaling (constant). Pixel-perfect rendering. */
    NONE;

    /** Human-readable formula. / Fórmula legível. */
    fun getFormula(): String = when (this) {
        DEFAULT -> "f(x)=x·(1+(d−300)·0.00333)·arAdj"
        PERCENTAGE -> "f(x)=x·(d/300)"
        BALANCED -> "f(x)=x·d/300 if d≤480 else x·(1.6+k·ln(1+(d−480)/300)) ·arAdj"
        LOGARITHMIC -> "f(x)=x·(1±0.4·ln(d/300))·arAdj"
        POWER -> "f(x)=x·(d/300)^0.75·arAdj"
        FLUID -> "f(x)=lerp(0.8x,1.2x,d,320,768)"
        INTERPOLATED -> "f(x)=x+(x·d/300−x)/2"
        DIAGONAL -> "f(x)=x·√(min²+max²)/611.6305"
        PERIMETER -> "f(x)=x·(min+max)/833"
        FIT -> "f(x)=x·min(min/300,max/533)"
        FILL -> "f(x)=x·max(min/300,max/533)"
        AUTOSIZE -> "f(x)=fitBinarySearch(x,min,max,container)"
        NONE -> "f(x)=x"
    }
}

/** [EN] Game element type used for strategy auto-inference. [PT] Tipo de elemento de jogo para inferência automática. */
enum class GameElementType(val recommended: GameScalingStrategy) {
    PLAYER(GameScalingStrategy.BALANCED),
    ENEMY(GameScalingStrategy.BALANCED),
    PROJECTILE(GameScalingStrategy.BALANCED),
    GAME_OBJECT(GameScalingStrategy.BALANCED),
    WORLD_BOUNDS(GameScalingStrategy.PERCENTAGE),
    BACKGROUND(GameScalingStrategy.FILL),
    PARALLAX_LAYER(GameScalingStrategy.FILL),
    VIEWPORT_CONTENT(GameScalingStrategy.FIT),
    HUD_BUTTON(GameScalingStrategy.DEFAULT),
    HUD_ICON(GameScalingStrategy.DEFAULT),
    HUD_TEXT(GameScalingStrategy.FLUID),
    MENU_ELEMENT(GameScalingStrategy.DEFAULT),
    TOUCH_TARGET(GameScalingStrategy.DIAGONAL),
    PIXEL_ART(GameScalingStrategy.NONE);

    /** [EN] Strategy recommended for this element. [PT] Estratégia recomendada para o elemento. */
    fun getRecommendedStrategy(): GameScalingStrategy = recommended
}

/** [EN] Device class inferred from smallest width. [PT] Classe de dispositivo inferida da menor largura. */
enum class GameDeviceType {
    PHONE, TABLET_SMALL, TABLET_LARGE, TV;

    companion object {
        @JvmStatic
        fun from(smallestWidthDp: Float, uiMode: UiModeType): GameDeviceType {
            if (uiMode == UiModeType.TELEVISION) return TV
            return when {
                smallestWidthDp >= 720f -> TABLET_LARGE
                smallestWidthDp >= 600f -> TABLET_SMALL
                else -> PHONE
            }
        }
    }
}
