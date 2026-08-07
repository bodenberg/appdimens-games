import io.github.bodenberg.appdimens.games.core.*;
import java.util.Arrays;
public final class CoreSelfTest {
 public static void main(String[] args){Screen screen=new Screen(360,800,3,new Insets(12,24,12,16));
  for(Strategy strategy:Strategy.values()){float result=Calculator.scale(48,strategy,screen);if(!Float.isFinite(result)||result<0)throw new AssertionError(strategy+": "+result);}
  float[] values={1,2,3,4,5};Calculator.scale(values,0,values,1,4,Strategy.NONE,screen,ScaleConfig.DEFAULT);if(!Arrays.equals(values,new float[]{1,1,2,3,4}))throw new AssertionError(Arrays.toString(values));
  var viewport=Viewport.calculate(1920,1080,screen,Viewport.Mode.FIT);if(viewport.viewportWidth()>screen.usableWidthDp()||viewport.viewportHeight()>screen.usableHeightDp())throw new AssertionError(viewport);
  for(ElementType type:ElementType.values())if(StrategySelector.forElement(type)==null)throw new AssertionError(type);
  try{Calculator.scale(Float.NaN,Strategy.NONE,screen);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){}
 }
}
