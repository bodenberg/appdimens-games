package com.appdimens.games.common;

public record DpQualifierEntry(DpQualifier type, float value) {
    public DpQualifierEntry { if (type == null || !Float.isFinite(value) || value <= 0f) throw new IllegalArgumentException(); }
}
