package com.appdimens.games.common

/**
 * [EN] Screen dimension qualifier used to select the base axis for scaling.
 * Mirrors `com.appdimens.dynamic.common.DpQualifier` for family parity.
 *
 * [PT] Qualificador de dimensão de tela usado para selecionar o eixo base do escalonamento.
 * Espelha `com.appdimens.dynamic.common.DpQualifier` para paridade com a família.
 */
enum class DpQualifier {
    /** [EN] Smallest screen width (`sw`, rotation-invariant). [PT] Menor largura de tela (invariante à rotação). */
    SMALL_WIDTH,
    /** [EN] Current screen height. [PT] Altura atual da tela. */
    HEIGHT,
    /** [EN] Current screen width. [PT] Largura atual da tela. */
    WIDTH
}

/**
 * [EN] Axis inverter: swaps the qualifier axis when the device is in the opposite orientation.
 * [PT] Inversor de eixo: troca o qualificador quando o dispositivo está na orientação oposta.
 */
enum class Inverter {
    /** No inversion. / Sem inversão. */
    DEFAULT,
    /** Portrait-HEIGHT behaves as LANDSCAPE-WIDTH. */
    PH_TO_LW,
    /** Portrait-WIDTH behaves as LANDSCAPE-HEIGHT. */
    PW_TO_LH,
    /** Landscape-HEIGHT behaves as PORTRAIT-WIDTH. */
    LH_TO_PW,
    /** Landscape-WIDTH behaves as PORTRAIT-HEIGHT. */
    LW_TO_PH,
    /** SMALL_WIDTH behaves as HEIGHT in landscape. */
    SW_TO_LH,
    /** SMALL_WIDTH behaves as WIDTH in landscape. */
    SW_TO_LW,
    /** SMALL_WIDTH behaves as HEIGHT in portrait. */
    SW_TO_PH,
    /** SMALL_WIDTH behaves as WIDTH in portrait. */
    SW_TO_PW
}

/** [EN] Device orientation. [PT] Orientação do dispositivo. */
enum class Orientation { PORTRAIT, LANDSCAPE, DEFAULT }

/** [EN] UI mode type (Android uiMode + foldable states). [PT] Tipo de modo de UI (uiMode Android + estados dobráveis). */
enum class UiModeType(val configValue: Int) {
    UNDEFINED(0), NORMAL(1), TELEVISION(2), CAR(3), WATCH(4), DESK(5),
    APPLIANCE(6), VR_HEADSET(7),
    FOLD_OPEN(-101), FOLD_CLOSED(-102), FOLD_HALF_OPENED(-105),
    FLIP_OPEN(-103), FLIP_CLOSED(-104), FLIP_HALF_OPENED(-106);

    companion object {
        @JvmStatic
        fun fromConfigValue(value: Int): UiModeType =
            entries.firstOrNull { it.configValue == value } ?: NORMAL
    }
}
