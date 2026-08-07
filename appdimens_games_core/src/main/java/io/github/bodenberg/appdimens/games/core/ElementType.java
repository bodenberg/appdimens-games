package io.github.bodenberg.appdimens.games.core;

/** Semantic hints shared by every game genre; never used inside the renderer. */
public enum ElementType {
    HUD_TEXT, HUD_ICON, HUD_BUTTON, HUD_BAR, HUD_MINIMAP, CROSSHAIR,
    MENU, DIALOG, TOOLTIP, INVENTORY, TOUCH_TARGET,
    PLAYER, NPC, ENEMY, BOSS, VEHICLE, WEAPON, PROJECTILE, PICKUP,
    TERRAIN, BUILDING, WORLD_OBJECT, BACKGROUND, PARTICLE, EFFECT,
    CAMERA_BOUNDS, TRIGGER_ZONE, TYPOGRAPHY, CONTAINER, GENERIC
}
