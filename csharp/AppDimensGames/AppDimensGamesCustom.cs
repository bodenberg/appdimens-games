// AppDimens Games — custom scaling strategies, engine bridge (Unity / Godot / MAUI games).
// The portable engine lives in AppDimensGamesCustom.Engine.cs (generated, same code as AppDimens.Net.Custom).
// Unity usage:
//   var hud = ScalingStrategy.Define("hud", b => { b.Axis = ScalingAxis.Min; b.Curve = ScalingCurve.Expression("pow(ratio, 0.85)"); });
//   float button = CustomKernels.Cdp(64f, hud);          // dp, live window
//   float label  = CustomKernels.Csp(18f, hud);          // final text size in dp (policy applied)
//   float inline = 64f.Cdp("base * pow(sw / 360, 0.8)");  // just write the formula (shared per text)
//   var product  = CustomScaleStrategy.Create(screen => Math.Clamp(screen.WidthDp / 360f, 0.85f, 1.8f));
#nullable enable
using System;
#if UNITY_5_3_OR_NEWER
using UnityEngine;
#endif

namespace AppDimensGames.Custom
{
    /// <summary>Bridge between <see cref="AppDimensGames.Screen"/> snapshots and the custom engine.</summary>
    public static class CustomGames
    {
        /// <summary>Optional override for the device class (e.g. force <see cref="DeviceClass.Tv"/> on a console build).</summary>
        public static Func<Metrics, DeviceClass?>? DeviceClassResolver { get; set; }

        /// <summary>Unity: Desktop / Console (TV) from <c>SystemInfo.deviceType</c>; otherwise phone/tablet by size.</summary>
        public static DeviceClass DefaultDeviceClass(Metrics m)
        {
#if UNITY_5_3_OR_NEWER
            switch (SystemInfo.deviceType)
            {
                case DeviceType.Desktop: return DeviceClass.Desktop;
                case DeviceType.Console: return DeviceClass.Tv;
            }
#endif
            return CustomNames.FromSmallestWidth(m.SmallestWidthDp);
        }

        /// <summary>Converts a game snapshot; a non-fullscreen surface counts as multi-window.</summary>
        public static ScreenSnapshot ToScreenSnapshot(this Metrics m) => new ScreenSnapshot(
            m.WidthDp, m.HeightDp, m.SmallestWidthDp, m.Density, m.FontScale,
            isLandscape: m.WidthDp > m.HeightDp, isMultiWindow: !m.Fullscreen,
            device: DeviceClassResolver?.Invoke(m) ?? DefaultDeviceClass(m));

        /// <summary>
        /// Strategy whose value is the text <paramref name="formula"/> — the formula <b>is</b> the value
        /// (<c>"base * pow(sw / 360, 0.8)"</c>). Shared per text without an <paramref name="id"/>; write
        /// <c>base * (…)</c> for the one-multiply fast path. Parse errors throw immediately.
        /// </summary>
        public static ScalingStrategy Formula(string formula, string? id = null, bool? register = null) =>
            ScalingStrategy.Formula(formula, id, register);

        /// <summary>Strategy from a code formula, <c>(base, screen) =&gt; dp</c> (not JSON-serializable).</summary>
        public static ScalingStrategy Formula(ScalingFormula formula, string? id = null, bool? register = null) =>
            ScalingStrategy.Formula(formula, id, register);

        /// <summary>Programmable factor, <c>screen =&gt; factor</c>: runs once per window, then one multiply per value.</summary>
        public static ScalingStrategy CustomScaleStrategy(ScreenFactor factor, string? id = null, bool? register = null) =>
            global::AppDimensGames.Custom.CustomScaleStrategy.Create(factor, id, register);

        /// <summary>Resolves for <paramref name="metrics"/>; memoized per snapshot (one multiply on hit).</summary>
        public static ResolvedScaling Resolve(this ScalingStrategy strategy, Metrics metrics) =>
            strategy.Resolve(metrics, () => metrics.ToScreenSnapshot());
    }

    /// <summary>Kernels over the live <see cref="AppDimensGames.Screen"/>. Without a strategy, <see cref="ScalingStrategies.Default"/> is used.</summary>
    public static class CustomKernels
    {
        /// <summary>Scaled value in dp.</summary>
        public static float Cdp(float b, ScalingStrategy? s = null) => (s ?? ScalingStrategies.Default).Resolve(Screen.Current).Scale(b);

        /// <summary>Resize-invariant (<c>i</c>): frozen fullscreen reference; base value under multi-window.</summary>
        public static float CdpInvariant(float b, ScalingStrategy? s = null)
        {
            var m = Screen.Invariant;
            return m.Fullscreen ? (s ?? ScalingStrategies.Default).Resolve(m).Scale(b) : b;
        }

        /// <summary>Scaled value in pixels.</summary>
        public static float CdpPx(float b, ScalingStrategy? s = null) => Cdp(b, s) * Screen.Current.Density;

        /// <summary>Final text size in dp (engines draw text themselves; the font-scale policy is applied).</summary>
        public static float Csp(float b, ScalingStrategy? s = null) => (s ?? ScalingStrategies.Default).Resolve(Screen.Current).ScaleSpDp(b);

        /// <summary>Final text size in pixels.</summary>
        public static float CspPx(float b, ScalingStrategy? s = null) => Csp(b, s) * Screen.Current.Density;

        // Inline formulas: the formula *is* the value (shared per text; base * (…) is the fast path).
        // PT Fórmulas inline: a fórmula *é* o valor (compartilhada por texto).

        /// <summary><c>CustomKernels.Cdp(64f, "base * pow(sw / 360, 0.8)")</c> → dp.</summary>
        public static float Cdp(float b, string formula) => Cdp(b, ScalingStrategies.Formula(formula));

        /// <summary>Resize-invariant formula value in dp.</summary>
        public static float CdpInvariant(float b, string formula) => CdpInvariant(b, ScalingStrategies.Formula(formula));

        /// <summary>Formula value in pixels.</summary>
        public static float CdpPx(float b, string formula) => CdpPx(b, ScalingStrategies.Formula(formula));

        /// <summary>Final text size in dp from a formula (font-scale policy applied).</summary>
        public static float Csp(float b, string formula) => Csp(b, ScalingStrategies.Formula(formula));

        /// <summary>Final text size in pixels from a formula.</summary>
        public static float CspPx(float b, string formula) => CspPx(b, ScalingStrategies.Formula(formula));

        /// <summary>Current factor (debug overlays).</summary>
        public static float Factor(ScalingStrategy? s = null) => (s ?? ScalingStrategies.Default).Resolve(Screen.Current).Factor;

        /// <summary>Human-readable breakdown for the live window.</summary>
        public static string Explain(ScalingStrategy? s = null)
        {
            var strategy = s ?? ScalingStrategies.Default;
            return strategy.Resolve(Screen.Current).Explain(strategy.Id);
        }
    }

    /// <summary><c>64f.Cdp(hud)</c>-style helpers.</summary>
    public static class CustomFloatExtensions
    {
        public static float Cdp(this float b, ScalingStrategy? s = null) => CustomKernels.Cdp(b, s);
        public static float Cdpi(this float b, ScalingStrategy? s = null) => CustomKernels.CdpInvariant(b, s);
        public static float CdpPx(this float b, ScalingStrategy? s = null) => CustomKernels.CdpPx(b, s);
        public static float Csp(this float b, ScalingStrategy? s = null) => CustomKernels.Csp(b, s);
        public static float CspPx(this float b, ScalingStrategy? s = null) => CustomKernels.CspPx(b, s);

        /// <summary><c>64f.Cdp("base * pow(sw / 360, 0.8)")</c> — the formula is the value.</summary>
        public static float Cdp(this float b, string formula) => CustomKernels.Cdp(b, formula);
        public static float Cdpi(this float b, string formula) => CustomKernels.CdpInvariant(b, formula);
        public static float CdpPx(this float b, string formula) => CustomKernels.CdpPx(b, formula);
        public static float Csp(this float b, string formula) => CustomKernels.Csp(b, formula);
        public static float CspPx(this float b, string formula) => CustomKernels.CspPx(b, formula);
    }
}
