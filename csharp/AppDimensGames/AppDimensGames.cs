// AppDimens Games 3.0 — C# port (Unity / Godot / MAUI games).
// Single-file, allocation-free hot paths, bit-parity with the Kotlin/C++ core.
// Unity usage:
//   AppDimensGames.Screen.Update(widthDp, heightDp, dpi);   // on resolution change
//   float player = AppDimensGames.Math.AutoDp(64f);
#if UNITY_5_3_OR_NEWER
using UnityEngine;
#endif

namespace AppDimensGames
{
    /// Canonical family constants.
    public static class Constants
    {
        public const float BaseWidthDp = 300f;
        public const float BaseHeightDp = 533f;
        // AUDIT: family literal (true sqrt = 611.6281550…), kept for parity.
        public const float BaseDiagonalDp = 611.6305f;
        public const float BasePerimeterDp = 833f;
        public const float ReferenceAr = 1.78f;
        public const float InvBaseRatio = 0.0033333334f;   // 1/300
        public const float AdjustmentScale = 0.0033333334f; // 0.10/30
        public const float SensitivityDefault = 0.0026666667f; // 0.08/30
        public const float FluidMinW = 320f, FluidMaxW = 768f;
        public const float AutoTransition = 480f, SensitivityLog = 0.4f, PowerExpDefault = 0.75f;
    }

    /// Immutable per-window snapshot with precomputed factors.
    public sealed class Metrics
    {
        public float WidthDp, HeightDp, SmallestWidthDp, Density, FontScale;
        public bool Fullscreen;

        public float Scale, WFactor, HFactor, ArMul, ScaledArMul,
                     PowerScale, InterpolatedScale, DiagonalScale, PerimeterScale,
                     LogarithmicScale, AutoScale, FitScale, FillScale;

        public static Metrics Make(float widthDp, float heightDp, float smallestWidthDp,
                                   float densityDpi, float fontScale = 1f, bool fullscreen = true)
        {
            var m = new Metrics();
            float mn = widthDp < heightDp ? widthDp : heightDp;
            float mx = widthDp < heightDp ? heightDp : widthDp;
            m.WidthDp = widthDp; m.HeightDp = heightDp;
            m.SmallestWidthDp = smallestWidthDp > 0f ? smallestWidthDp : mn;
            m.Density = densityDpi / 160f;
            m.FontScale = fontScale > 0f ? fontScale : 1f;
            m.Fullscreen = fullscreen;

            float sw = m.SmallestWidthDp;
            float logAr = (float)System.Math.Log((mx / mn) / Constants.ReferenceAr);

            m.Scale = sw * Constants.InvBaseRatio;
            m.WFactor = widthDp * Constants.InvBaseRatio;
            m.HFactor = heightDp * Constants.InvBaseRatio;
            m.ArMul = 1f + Constants.SensitivityDefault * logAr;
            m.ScaledArMul = 1f + (sw - Constants.BaseWidthDp) *
                (Constants.AdjustmentScale + Constants.SensitivityDefault * logAr);
            m.PowerScale = (float)System.Math.Pow(sw / Constants.BaseWidthDp, Constants.PowerExpDefault);
            m.InterpolatedScale = 1f + (m.Scale - 1f) * 0.5f;
            m.DiagonalScale = (float)System.Math.Sqrt(mn * mn + mx * mx) / Constants.BaseDiagonalDp;
            m.PerimeterScale = (mn + mx) / Constants.BasePerimeterDp;
            m.LogarithmicScale = sw > Constants.BaseWidthDp
                ? 1f + Constants.SensitivityLog * (float)System.Math.Log(sw * Constants.InvBaseRatio)
                : (sw > 0f ? 1f - Constants.SensitivityLog * (float)System.Math.Log(Constants.BaseWidthDp / sw) : 1f);
            m.AutoScale = sw <= Constants.AutoTransition
                ? sw * Constants.InvBaseRatio
                : (Constants.AutoTransition * Constants.InvBaseRatio) +
                  Constants.SensitivityLog * (float)System.Math.Log(1f + (sw - Constants.AutoTransition) * Constants.InvBaseRatio);
            m.FitScale = mn / Constants.BaseWidthDp < mx / Constants.BaseHeightDp
                ? mn / Constants.BaseWidthDp : mx / Constants.BaseHeightDp;
            m.FillScale = mn / Constants.BaseWidthDp > mx / Constants.BaseHeightDp
                ? mn / Constants.BaseWidthDp : mx / Constants.BaseHeightDp;
            return m;
        }
    }

    /// Live screen hub — call Update on resize; `i` variants use the frozen fullscreen snapshot.
    public static class Screen
    {
        private static volatile Metrics _current = Metrics.Make(300f, 533f, 300f, 160f);
        private static volatile Metrics _frozen = _current;

        public static Metrics Current { get { return _current; } }
        public static Metrics Invariant { get { return _current.Fullscreen ? _current : _frozen; } }

        public static void Update(float widthDp, float heightDp, float densityDpi,
                                  float fontScale = 1f, bool fullscreen = true)
        {
            var m = Metrics.Make(widthDp, heightDp, 0f, densityDpi, fontScale, fullscreen);
            _current = m;
            if (fullscreen) _frozen = m;
        }
    }

    /// Kernels — one multiply fast lanes + full formulas. Suffix semantics match Kotlin (`a`/`i`).
    public static class MathKernels
    {
        public static float Scaled(float b) { return b * Screen.Current.Scale; }
        public static float ScaledAr(float b) { return b * Screen.Current.ScaledArMul; }
        public static float ScaledInvariant(float b)
        {
            var m = Screen.Invariant;
            return m.Fullscreen ? b * m.Scale : b;
        }
        public static float Width(float b) { return b * Screen.Current.WFactor; }
        public static float Height(float b) { return b * Screen.Current.HFactor; }
        public static float PercentOfWidth(float percent) { return percent / 100f * Screen.Current.WidthDp; }
        public static float Power(float b, bool ar = false)
        {
            var o = b * Screen.Current.PowerScale;
            return ar ? o * Screen.Current.ArMul : o;
        }
        public static float Fluid(float b, bool ar = false)
        {
            var m = Screen.Current;
            var v = FluidRaw(b, m.SmallestWidthDp);
            return ar ? v * m.ArMul : v;
        }
        private static float FluidRaw(float b, float d)
        {
            const float loF = 0.8f, hiF = 1.2f;
            if (d <= Constants.FluidMinW) return b * loF;
            if (d >= Constants.FluidMaxW) return b * hiF;
            return b * loF + (b * hiF - b * loF) * (d - Constants.FluidMinW) / (Constants.FluidMaxW - Constants.FluidMinW);
        }
        public static float Auto(float b, bool ar = false)
        {
            var o = b * Screen.Current.AutoScale;
            return ar ? o * Screen.Current.ArMul : o;
        }
        public static float Logarithmic(float b, bool ar = false)
        {
            var o = b * Screen.Current.LogarithmicScale;
            return ar ? o * Screen.Current.ArMul : o;
        }
        public static float Interpolated(float b, bool ar = false)
        {
            var o = b * Screen.Current.InterpolatedScale;
            return ar ? o * Screen.Current.ArMul : o;
        }
        public static float Diagonal(float b, bool ar = false)
        {
            var o = b * Screen.Current.DiagonalScale;
            return ar ? o * Screen.Current.ArMul : o;
        }
        public static float Fit(float b, bool ar = false)
        {
            var o = b * Screen.Current.FitScale;
            return ar ? o * Screen.Current.ArMul : o;
        }
        public static float Fill(float b, bool ar = false)
        {
            var o = b * Screen.Current.FillScale;
            return ar ? o * Screen.Current.ArMul : o;
        }
        public static float ToPx(float dp) { return dp * Screen.Current.Density; }
    }

    /// 2D helpers for game worlds (letterbox-consistent scaling).
    public struct Vec2
    {
        public float x, y;
        public Vec2(float x, float y) { this.x = x; this.y = y; }
    }

    public static class World
    {
        /// Viewport rect in px for a design size under a mode.
        public static Rect ViewportRect(Mode mode, float surfaceWpx, float surfaceHpx,
                                        float designW, float designH)
        {
            float sw = surfaceWpx > 1f ? surfaceWpx : 1f;
            float sh = surfaceHpx > 1f ? surfaceHpx : 1f;
            switch (mode)
            {
                case Mode.FitAll:
                case Mode.Crop:
                {
                    float s = mode == Mode.FitAll
                        ? System.Math.Min(sw / designW, sh / designH)
                        : System.Math.Max(sw / designW, sh / designH);
                    return new Rect((sw - designW * s) * 0.5f, (sh - designH * s) * 0.5f, designW * s, designH * s);
                }
                case Mode.FitWidth:
                {
                    float s = sw / designW;
                    return new Rect(0f, (sh - designH * s) * 0.5f, sw, designH * s);
                }
                case Mode.FitHeight:
                {
                    float s = sh / designH;
                    return new Rect((sw - designW * s) * 0.5f, 0f, designW * s, sh);
                }
                default: return new Rect(0f, 0f, sw, sh); // Stretch
            }
        }
    }

    public enum Mode { FitAll, FitWidth, FitHeight, Stretch, Crop }

    /// Axis-aligned rect helper (engine-agnostic; UnityEngine.Rect-compatible fields).
    public struct Rect
    {
        public float X, Y, W, H;
        public Rect(float x, float y, float w, float h) { X = x; Y = y; W = w; H = h; }
    }

    /// Physical units (mm/cm/inch → px) for physical touch targets.
    public static class Units
    {
        public static float MmToPx(float mm) { return mm * Screen.Current.Density * 160f / 25.4f; }
        public static float CmToPx(float cm) { return MmToPx(cm * 10f); }
        public static float InchToPx(float inch) { return inch * Screen.Current.Density * 160f; }
    }
}
