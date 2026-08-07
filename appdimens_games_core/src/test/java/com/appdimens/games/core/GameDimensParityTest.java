package com.appdimens.games.core;

import com.appdimens.games.common.DpQualifier;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameDimensParityTest {
    private final GameScreen design = new GameScreen(300, 533, 1);
    @Test public void everyStrategyIsIdentityAtDesignViewport() {
        for (GameStrategy strategy : GameStrategy.values()) assertEquals(strategy.name(), 24f,
                GameDimens.calculate(24f, strategy, DpQualifier.SMALL_WIDTH, design), .001f);
    }
    @Test public void batchMatchesScalar() {
        float[] values = {8, 16, 32};
        GameDimens.calculate(values, 0, values.length, GameStrategy.FIT, DpQualifier.WIDTH, new GameScreen(600, 1066, 2));
        assertArrayEquals(new float[]{16, 32, 64}, values, .001f);
    }
}
