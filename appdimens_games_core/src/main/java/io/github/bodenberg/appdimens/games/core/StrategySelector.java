package io.github.bodenberg.appdimens.games.core;

/** Deterministic semantic defaults. Explicit strategy selection always takes precedence. */
public final class StrategySelector {
    private StrategySelector() {}
    public static Strategy forElement(ElementType type) {
        if (type == null) throw new NullPointerException("type");
        return switch (type) {
            case HUD_TEXT, TOOLTIP, TYPOGRAPHY -> Strategy.FLUID;
            case HUD_BUTTON, HUD_ICON, HUD_BAR, MENU, DIALOG, INVENTORY -> Strategy.DEFAULT;
            case HUD_MINIMAP, CAMERA_BOUNDS -> Strategy.FIT;
            case BACKGROUND, TERRAIN, BUILDING -> Strategy.FILL;
            case TOUCH_TARGET, CROSSHAIR, TRIGGER_ZONE -> Strategy.DIAGONAL;
            case PLAYER, NPC, ENEMY, BOSS, VEHICLE, WEAPON, PROJECTILE, PICKUP,
                    WORLD_OBJECT, PARTICLE, EFFECT, CONTAINER, GENERIC -> Strategy.BALANCED;
        };
    }
}
