package io.github.bodenberg.appdimens.games.core;

/** Stable strategy ids shared with the C ABI. Never reorder. */
public enum Strategy {
    NONE(0), DEFAULT(1), PERCENTAGE(2), BALANCED(3), LOGARITHMIC(4), POWER(5),
    FLUID(6), INTERPOLATED(7), DIAGONAL(8), PERIMETER(9), FIT(10), FILL(11), AUTOSIZE(12);
    public final int id;
    Strategy(int id) { this.id = id; }
    public static Strategy fromId(int id) {
        for (Strategy value : values()) if (value.id == id) return value;
        throw new IllegalArgumentException("Unknown strategy id: " + id);
    }
}
