package com.appdimens.games.core;

public record GameScreen(float widthDp, float heightDp, float density) {
    public GameScreen {
        if (!Float.isFinite(widthDp) || !Float.isFinite(heightDp) || !Float.isFinite(density)
                || widthDp <= 0f || heightDp <= 0f || density <= 0f) throw new IllegalArgumentException();
    }
    public float smallWidthDp() { return Math.min(widthDp, heightDp); }
}
