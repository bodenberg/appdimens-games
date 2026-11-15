// AppDimens core for .NET — same constants, strategies, tokens and results as every
// AppDimens library. C# 7.3 / netstandard2.0 (Unity 2019.4+, Godot 4 .NET, MonoGame, FNA, Stride).
using System;
using System.Collections.Generic;
using System.Threading;

namespace AppDimens
{
    public enum Strategy { Scaled = 0, Auto, Percent, Power, Fluid, Interpolated, Logarithmic, Diagonal, Perimeter, Fit, Fill, Density }
    public enum Qualifier { SmallWidth = 0, Height = 1, Width = 2 }
    public enum Inverter { Default = 0, PhToLw, PwToLh, LhToPw, LwToPh, SwToLh, SwToLw, SwToPh, SwToPw }
    public enum Orientation { Auto = 0, Portrait = 1, Landscape = 2 }
    public enum UiMode { Normal = 0, Television, Car, Watch, Desk, Appliance, VrHeadset, Undefined, FoldOpen, FoldClosed, FlipOpen, FlipClosed, FoldHalfOpened, FlipHalfOpened }
    public enum Kind { Dp = 0, Sp = 1, Em = 2 }
    /// <summary>Axis variants of the precomputed tables (same order in every port).</summary>
    public enum Variant { S = 0, H, W, SPh, SLh, SPw, SLw, HLw, HPw, WLh, WPh }

    public static class Constants
    {
        public const double BaseWidthDp = 300.0, BaseHeightDp = 533.0, BaseDiagonalDp = 611.6305, BasePerimeterDp = 833.0;
        public const double ReferenceAspectRatio = 1.78, InvBaseRatio = 1.0 / 300.0;
        public const double AdjustmentScale = 0.1 / 30.0, SensitivityDefault = 0.08 / 30.0;
        public const double FluidMinWidth = 320.0, FluidMaxWidth = 768.0, FluidMinFactor = 0.8, FluidMaxFactor = 1.2;
        public const double AutoTransitionDp = 480.0, LogSensitivity = 0.4, PowerExponent = 0.75;
        public const int StrategyCount = 12, VariantCount = 11, SlotCount = 44;
        public const string Version = "3.3.0";
        public static int Slot(Variant v, bool aspectRatio = false, bool ignoreMultiWindow = false)
        { return (int)v * 4 + (aspectRatio ? 1 : 0) + (ignoreMultiWindow ? 2 : 0); }
    }

    /// <summary>Inputs of a snapshot (logical units: dp / pt / logical px).</summary>
    public struct DimenInput
    {
        public double Width, Height, SmallestWidth, Density, FontScale, Dpi;
        public Orientation Orientation;
        public UiMode UiMode;
        public bool MultiWindow;
        public DimenInput(double width, double height, double density = 1, double fontScale = 1)
        {
            Width = width; Height = height; Density = density; FontScale = fontScale;
            SmallestWidth = 0; Dpi = 0; Orientation = Orientation.Auto; UiMode = UiMode.Normal; MultiWindow = false;
        }
    }

    /// <summary>Immutable snapshot with every factor precomputed (12 strategies × 44 slots).</summary>
    public sealed class DimenMetrics
    {
        static readonly byte[] VQ = { 0, 1, 2, 0, 0, 0, 0, 1, 1, 2, 2 };
        static readonly byte[] VInv = { 0, 0, 0, 7, 5, 8, 6, 1, 3, 2, 4 };

        public readonly DimenInput Input;
        public readonly double MinDim, MaxDim, Sw, Scale, LogNormalizedAr;
        public readonly double PowerScale, InterpolatedScale, DiagonalScale, PerimeterScale, LogarithmicScale;
        public readonly bool IsLandscape, IsPortrait;
        public readonly uint Generation;
        readonly float[] _table = new float[Constants.StrategyCount * Constants.SlotCount];

        public static readonly DimenMetrics Reference = new DimenMetrics(new DimenInput(0, 0));

        public DimenMetrics(DimenInput input, uint generation = 0)
        {
            Generation = generation;
            var i = input;
            if (!Pos(i.Width)) i.Width = 0;
            if (!Pos(i.Height)) i.Height = 0;
            if (!Pos(i.Density)) i.Density = 1;
            if (!Pos(i.FontScale)) i.FontScale = 1;
            if (!Pos(i.Dpi)) i.Dpi = 160 * i.Density;
            MinDim = Math.Min(i.Width, i.Height);
            MaxDim = Math.Max(i.Width, i.Height);
            if (!Pos(i.SmallestWidth)) i.SmallestWidth = MinDim;
            if (i.Orientation == Orientation.Auto && (i.Width > 0 || i.Height > 0))
                i.Orientation = i.Width > i.Height ? Orientation.Landscape : Orientation.Portrait;
            Input = i;
            IsLandscape = i.Orientation == Orientation.Landscape;
            IsPortrait = i.Orientation == Orientation.Portrait;
            Sw = i.SmallestWidth > 0 ? i.SmallestWidth : Constants.BaseWidthDp;
            Scale = Sw * Constants.InvBaseRatio;
            double nar = (MinDim > 0 ? MaxDim / MinDim : 1.0) / Constants.ReferenceAspectRatio;
            if (!Pos(nar)) nar = 1;
            LogNormalizedAr = Math.Log(nar);
            PowerScale = Math.Pow(Sw / Constants.BaseWidthDp, Constants.PowerExponent);
            InterpolatedScale = 1.0 + (Sw * Constants.InvBaseRatio - 1.0) * 0.5;
            DiagonalScale = Math.Sqrt(MinDim * MinDim + MaxDim * MaxDim) / Constants.BaseDiagonalDp;
            PerimeterScale = (MinDim + MaxDim) / Constants.BasePerimeterDp;
            LogarithmicScale = LogScale(Sw);
            for (int s = 0; s < Constants.StrategyCount; s++)
                for (int v = 0; v < Constants.VariantCount; v++)
                {
                    var q = (Qualifier)VQ[v]; var inv = (Inverter)VInv[v];
                    double plain = ComputeFactor((Strategy)s, q, inv, false, false);
                    double ar = ComputeFactor((Strategy)s, q, inv, false, true);
                    int b = s * Constants.SlotCount + v * 4;
                    _table[b] = (float)plain; _table[b + 1] = (float)ar;
                    _table[b + 2] = i.MultiWindow ? 1f : (float)plain; _table[b + 3] = i.MultiWindow ? 1f : (float)ar;
                }
        }

        static bool Pos(double v) { return v > 0 && !double.IsInfinity(v) && !double.IsNaN(v); }

        public static double LogScale(double d)
        {
            if (d > Constants.BaseWidthDp) return 1.0 + Constants.LogSensitivity * Math.Log(d * Constants.InvBaseRatio);
            if (d > 0) return 1.0 - Constants.LogSensitivity * Math.Log(Constants.BaseWidthDp / d);
            return 1.0;
        }

        /// <summary>Precomputed factor (hot path: one array load).</summary>
        public float Factor(Strategy s, int slot) { return _table[(int)s * Constants.SlotCount + slot]; }
        public float Dp(Strategy s, int slot, float value) { return value * _table[(int)s * Constants.SlotCount + slot]; }
        public float Sp(Strategy s, int slot, float value) { return value * _table[(int)s * Constants.SlotCount + slot] * (float)Input.FontScale; }
        public float Px(Strategy s, int slot, float value) { return value * _table[(int)s * Constants.SlotCount + slot] * (float)Input.Density; }
        public float SpPx(Strategy s, int slot, float value) { return Sp(s, slot, value) * (float)Input.Density; }

        public float Sdp(float v) { return v * _table[0]; }
        public float Hdp(float v) { return v * _table[4]; }
        public float Wdp(float v) { return v * _table[8]; }
        public float Ssp(float v) { return v * _table[0] * (float)Input.FontScale; }
        public float Sem(float v) { return v * _table[0]; }
        public float Asdp(float v) { return v * _table[Constants.SlotCount]; }
        public float Assp(float v) { return v * _table[Constants.SlotCount] * (float)Input.FontScale; }
        public float Ftsdp(float v) { return v * _table[(int)Strategy.Fit * Constants.SlotCount]; }
        public float Flsdp(float v) { return v * _table[(int)Strategy.Fill * Constants.SlotCount]; }

        public float SpaceW(float percent) { return percent * 0.01f * (float)Input.Width; }
        public float SpaceH(float percent) { return percent * 0.01f * (float)Input.Height; }
        public float Mm(float v) { return (float)(v / 25.4 * Input.Dpi / Input.Density); }
        public float Inch(float v) { return (float)(v * Input.Dpi / Input.Density); }

        static Qualifier Effective(Qualifier q, Inverter inv, bool land, bool port)
        {
            switch (inv)
            {
                case Inverter.PhToLw: return land && q == Qualifier.Height ? Qualifier.Width : q;
                case Inverter.PwToLh: return land && q == Qualifier.Width ? Qualifier.Height : q;
                case Inverter.LhToPw: return port && q == Qualifier.Height ? Qualifier.Width : q;
                case Inverter.LwToPh: return port && q == Qualifier.Width ? Qualifier.Height : q;
                case Inverter.SwToLh: return land && q == Qualifier.SmallWidth ? Qualifier.Height : q;
                case Inverter.SwToLw: return land && q == Qualifier.SmallWidth ? Qualifier.Width : q;
                case Inverter.SwToPh: return port && q == Qualifier.SmallWidth ? Qualifier.Height : q;
                case Inverter.SwToPw: return port && q == Qualifier.SmallWidth ? Qualifier.Width : q;
                default: return q;
            }
        }

        double Axis(Qualifier q) { return q == Qualifier.Height ? Input.Height : q == Qualifier.Width ? Input.Width : Sw; }

        /// <summary>General path (any inverter, custom aspect-ratio sensitivity k).</summary>
        public double ComputeFactor(Strategy s, Qualifier qual, Inverter inv, bool ignoreMw, bool aspect, double k = Constants.SensitivityDefault)
        {
            if (ignoreMw && Input.MultiWindow) return 1.0;
            bool defSw = qual == Qualifier.SmallWidth && inv == Inverter.Default;
            var q = s >= Strategy.Diagonal ? qual : Effective(qual, inv, IsLandscape, IsPortrait);
            if (s == Strategy.Scaled || s == Strategy.Percent)
            {
                double d = defSw ? Sw : Axis(q);
                if (!aspect) return defSw ? Scale : d * Constants.InvBaseRatio;
                return 1.0 + (d - Constants.BaseWidthDp) * (Constants.AdjustmentScale + k * LogNormalizedAr);
            }
            double b = Base(s, q, defSw);
            return aspect ? b * (1.0 + k * LogNormalizedAr) : b;
        }

        double Base(Strategy s, Qualifier q, bool defSw)
        {
            double d;
            switch (s)
            {
                case Strategy.Auto:
                    d = Axis(q);
                    return d <= Constants.AutoTransitionDp ? d * Constants.InvBaseRatio
                        : Constants.AutoTransitionDp * Constants.InvBaseRatio + Constants.LogSensitivity * Math.Log(1.0 + (d - Constants.AutoTransitionDp) * Constants.InvBaseRatio);
                case Strategy.Power: return defSw ? PowerScale : Math.Pow(Axis(q) / Constants.BaseWidthDp, Constants.PowerExponent);
                case Strategy.Fluid:
                    d = Axis(q);
                    if (d <= Constants.FluidMinWidth) return Constants.FluidMinFactor;
                    if (d >= Constants.FluidMaxWidth) return Constants.FluidMaxFactor;
                    return Constants.FluidMinFactor + (Constants.FluidMaxFactor - Constants.FluidMinFactor) * (d - Constants.FluidMinWidth) / (Constants.FluidMaxWidth - Constants.FluidMinWidth);
                case Strategy.Interpolated: return defSw ? InterpolatedScale : 1.0 + (Axis(q) * Constants.InvBaseRatio - 1.0) * 0.5;
                case Strategy.Logarithmic: return defSw ? LogarithmicScale : LogScale(Axis(q));
                case Strategy.Diagonal: return DiagonalScale;
                case Strategy.Perimeter: return PerimeterScale;
                case Strategy.Fit: return Math.Min(MinDim / Constants.BaseWidthDp, MaxDim / Constants.BaseHeightDp);
                case Strategy.Fill: return Math.Max(MinDim / Constants.BaseWidthDp, MaxDim / Constants.BaseHeightDp);
                case Strategy.Density: return Input.Density;
                default: return defSw ? Scale : Axis(q) * Constants.InvBaseRatio;
            }
        }

        public bool SameInputs(DimenMetrics o)
        {
            var a = Input; var b = o.Input;
            return a.Width == b.Width && a.Height == b.Height && a.SmallestWidth == b.SmallestWidth && a.Density == b.Density &&
                   a.FontScale == b.FontScale && a.Dpi == b.Dpi && a.Orientation == b.Orientation && a.UiMode == b.UiMode && a.MultiWindow == b.MultiWindow;
        }

        public float Resolve(Token t, float value)
        {
            float r = value * _table[(int)t.Strategy * Constants.SlotCount + t.Slot];
            if (t.Kind == Kind.Sp) r *= (float)Input.FontScale;
            return t.Px ? r * (float)Input.Density : r;
        }
        public float Resolve(string token, float value)
        {
            Token t;
            return Token.TryParse(token, out t) ? Resolve(t, value) : value;
        }
    }

    /// <summary>Parsed family token ("sdp", "asdpa", "hdpLw", "sdpPxiaPh", "logssp"…).</summary>
    public struct Token
    {
        public Strategy Strategy; public Qualifier Qualifier; public Inverter Inverter; public Kind Kind; public bool Px; public int Slot;
        static readonly string[] Prefixes = { "", "a", "p", "pw", "f", "i", "log", "dg", "pr", "ft", "fl", "d" };
        static readonly int[] Order = { 6, 3, 7, 8, 9, 10, 1, 2, 4, 5, 11, 0 };
        static readonly byte[] VInv = { 0, 0, 0, 7, 5, 8, 6, 1, 3, 2, 4 };
        static readonly Dictionary<string, Token> Cache = new Dictionary<string, Token>();
        static readonly object CacheLock = new object();

        public static bool TryParse(string name, out Token token)
        {
            token = default(Token);
            if (string.IsNullOrEmpty(name)) return false;
            lock (CacheLock) { if (Cache.TryGetValue(name, out token)) return true; }
            foreach (int s in Order)
            {
                string p = Prefixes[s];
                if (!name.StartsWith(p, StringComparison.Ordinal)) continue;
                Token t;
                if (ParseRest(name, p.Length, out t))
                {
                    t.Strategy = (Strategy)s;
                    lock (CacheLock) { if (Cache.Count < 4096) Cache[name] = t; }
                    token = t;
                    return true;
                }
            }
            return false;
        }

        static bool ParseRest(string s, int i, out Token t)
        {
            t = default(Token);
            int n = s.Length;
            if (n - i < 3) return false;
            char axis = s[i];
            if (axis == 's') t.Qualifier = Qualifier.SmallWidth; else if (axis == 'h') t.Qualifier = Qualifier.Height; else if (axis == 'w') t.Qualifier = Qualifier.Width; else return false;
            string k = s.Substring(i + 1, 2);
            if (k == "dp") t.Kind = Kind.Dp; else if (k == "sp") t.Kind = Kind.Sp; else if (k == "em") t.Kind = Kind.Em; else return false;
            i += 3;
            bool px = false, invSeen = false; int flags = -1; char i0 = '\0', i1 = '\0';
            while (i < n)
            {
                if (n - i >= 2 && s[i] == 'P' && s[i + 1] == 'x') { if (px) return false; px = true; i += 2; continue; }
                if (n - i >= 2 && (s[i] == 'P' || s[i] == 'L') && (s[i + 1] == 'h' || s[i + 1] == 'w')) { if (invSeen) return false; invSeen = true; i0 = s[i]; i1 = s[i + 1]; i += 2; continue; }
                if (s[i] == 'i' || s[i] == 'a')
                {
                    if (flags >= 0) return false;
                    if (s[i] == 'i' && i + 1 < n && s[i + 1] == 'a') { flags = 3; i += 2; } else { flags = s[i] == 'a' ? 1 : 2; i++; }
                    continue;
                }
                return false;
            }
            if (flags < 0) flags = 0;
            int v = (int)t.Qualifier;
            if (invSeen)
            {
                bool P = i0 == 'P', H = i1 == 'h';
                if (axis == 's') v = P ? (H ? 3 : 5) : (H ? 4 : 6);
                else if (axis == 'h' && !H) v = P ? 8 : 7;
                else if (axis == 'w' && H) v = P ? 10 : 9;
                else return false;
            }
            t.Inverter = (Inverter)VInv[v];
            t.Px = px;
            t.Slot = v * 4 + flags;
            return true;
        }
    }

    /// <summary>Process-wide live snapshot. Publishing swaps one immutable reference
    /// (atomic in .NET), so any thread can read without locks or tearing.</summary>
    public static class Live
    {
        static DimenMetrics _current = DimenMetrics.Reference;
        static int _generation;
        public static event Action<DimenMetrics> Changed;

        public static DimenMetrics Current { get { return Volatile.Read(ref _current); } }
        public static int Generation { get { return Volatile.Read(ref _generation); } }

        /// <summary>Publishes a snapshot; returns false (no event) when the inputs did not change.</summary>
        public static bool Publish(DimenInput input)
        {
            var next = new DimenMetrics(input, (uint)(Generation + 1));
            if (Current.SameInputs(next) && Generation > 0) return false;
            Volatile.Write(ref _current, next);
            Interlocked.Increment(ref _generation);
            var h = Changed;
            if (h != null) h(next);
            return true;
        }
    }
}
