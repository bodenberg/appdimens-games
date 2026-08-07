package io.github.bodenberg.appdimens.games.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class CoreContractTest {
 @Test public void allStrategiesAreFinite(){Screen s=new Screen(360,800,3);for(Strategy st:Strategy.values())assertTrue(st.name(),Float.isFinite(Calculator.scale(48,st,s)));}
 @Test public void noneIsIdentity(){assertEquals(48,Calculator.scale(48,Strategy.NONE,new Screen(360,800,3)),0);}
 @Test public void overlapBatchUsesMemmoveSemantics(){float[] v={1,2,3,4,5};Calculator.scale(v,0,v,1,4,Strategy.NONE,new Screen(360,800,3),ScaleConfig.DEFAULT);assertArrayEquals(new float[]{1,1,2,3,4},v,0);}
 @Test public void fitStaysInsideSafeArea(){Screen s=new Screen(360,800,3,new Insets(12,24,12,16));Viewport.Transform v=Viewport.calculate(1920,1080,s,Viewport.Mode.FIT);assertTrue(v.viewportWidth()<=s.usableWidthDp());assertTrue(v.viewportHeight()<=s.usableHeightDp());}
 @Test(expected=IllegalArgumentException.class) public void rejectsNaN(){Calculator.scale(Float.NaN,Strategy.NONE,new Screen(360,800,3));}
 @Test public void inferenceCoversEveryElement(){for(ElementType type:ElementType.values())assertNotNull(StrategySelector.forElement(type));}
}
