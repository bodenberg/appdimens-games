package io.github.bodenberg.appdimens.games.android;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import io.github.bodenberg.appdimens.games.core.Insets;
import io.github.bodenberg.appdimens.games.core.Screen;

/** Captures one immutable Screen snapshot. Call after surface/insets changes, never per draw. */
public final class AndroidScreens {
    private AndroidScreens() {}
    @SuppressWarnings("deprecation")
    public static Screen capture(Activity activity) {
        if (activity == null) throw new NullPointerException("activity");
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        float density = metrics.density;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics window = activity.getWindowManager().getCurrentWindowMetrics();
            Rect bounds = window.getBounds();
            android.graphics.Insets nativeInsets = window.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new Screen(bounds.width()/density, bounds.height()/density, density,
                    new Insets(nativeInsets.left/density, nativeInsets.top/density,
                            nativeInsets.right/density, nativeInsets.bottom/density));
        }
        return new Screen(metrics.widthPixels/density, metrics.heightPixels/density, density);
    }
}
