// AppDimens Games for .NET — port of native/include/appdimens/games.h (identical results).
using System;

namespace AppDimens.Games
{
    public enum ScaleMode { Reference = 0, Content = 1, Dpi = 2 }
    public enum ViewportMode { Fit = 0, Fill, Stretch, FitWidth, FitHeight, Expand, Integer }
    public enum GraphicsApi { OpenGL = 0, OpenGLES, WebGL, Vulkan, D3D11, D3D12, Metal, WebGPU }
    public enum FovPolicy { HorPlus = 0, VertMinus, Hybrid }
    public enum Upscaler { None = 0, Fsr1, Fsr2, Fsr3, Dlss, Xess, MetalFx, Tsr, Pssr, Sgsr }
    public enum UpscaleQuality { NativeAA = 0, UltraQualityPlus, UltraQuality, Quality, Balanced, Performance, UltraPerformance }
    public enum DeviceClass { Phone = 0, Tablet, Handheld, Desktop, Tv, Vr }

    public struct Rect { public float X, Y, W, H; public Rect(float x, float y, float w, float h) { X = x; Y = y; W = w; H = h; } }
    public struct Insets { public float Left, Top, Right, Bottom; public Insets(float l, float t, float r, float b) { Left = l; Top = t; Right = r; Bottom = b; } }

    /// <summary>A drawable surface (swapchain / window) → snapshot.</summary>
    public struct Surface
    {
        public float WidthPx, HeightPx; public ScaleMode Mode; public float Param, Dpi, FontScale; public bool MultiWindow; public UiMode UiMode;

        public Surface(float widthPx, float heightPx, ScaleMode mode = ScaleMode.Reference, float param = 0)
        { WidthPx = widthPx; HeightPx = heightPx; Mode = mode; Param = param; Dpi = 0; FontScale = 0; MultiWindow = false; UiMode = UiMode.Normal; }

        /// <summary>Pixels per logical unit. Reference: short side of the reference (default 1080) / 300.</summary>
        public float Density
        {
            get
            {
                switch (Mode)
                {
                    case ScaleMode.Content: return Param > 0 ? Param : 1f;
                    case ScaleMode.Dpi: return (Param > 0 ? Param : 160f) / 160f;
                    default: return (Param > 0 ? Param : 1080f) / 300f;
                }
            }
        }

        public DimenInput ToInput()
        {
            float d = Density;
            var i = new DimenInput(WidthPx / d, HeightPx / d, d, FontScale > 0 ? FontScale : 1f);
            i.Dpi = Dpi > 0 ? Dpi : (Mode == ScaleMode.Dpi && Param > 0 ? Param : 160f * d);
            i.MultiWindow = MultiWindow; i.UiMode = UiMode;
            return i;
        }
        public DimenMetrics ToMetrics() { return new DimenMetrics(ToInput()); }
    }

    public struct Viewport
    {
        public float X, Y, W, H, ScaleX, ScaleY, WorldX, WorldY, WorldW, WorldH;

        public static Viewport Compute(ViewportMode mode, float surfaceW, float surfaceH, float designW, float designH)
        {
            var v = new Viewport();
            float sw = Math.Max(surfaceW, 1f), sh = Math.Max(surfaceH, 1f), dw = designW > 0 ? designW : 1f, dh = designH > 0 ? designH : 1f;
            float fx = sw / dw, fy = sh / dh, s = Math.Min(fx, fy);
            v.WorldW = dw; v.WorldH = dh;
            switch (mode)
            {
                case ViewportMode.Fill:
                    s = Math.Max(fx, fy);
                    v.W = dw * s; v.H = dh * s; v.X = (sw - v.W) * 0.5f; v.Y = (sh - v.H) * 0.5f; v.ScaleX = v.ScaleY = s;
                    v.WorldW = sw / s; v.WorldH = sh / s; v.WorldX = (dw - v.WorldW) * 0.5f; v.WorldY = (dh - v.WorldH) * 0.5f;
                    break;
                case ViewportMode.Stretch: v.W = sw; v.H = sh; v.ScaleX = fx; v.ScaleY = fy; break;
                case ViewportMode.FitWidth:
                    v.ScaleX = v.ScaleY = fx; v.W = sw; v.H = sh; v.WorldH = sh / fx; v.WorldY = (dh - v.WorldH) * 0.5f; break;
                case ViewportMode.FitHeight:
                    v.ScaleX = v.ScaleY = fy; v.W = sw; v.H = sh; v.WorldW = sw / fy; v.WorldX = (dw - v.WorldW) * 0.5f; break;
                case ViewportMode.Expand:
                    v.ScaleX = v.ScaleY = s; v.W = sw; v.H = sh; v.WorldW = sw / s; v.WorldH = sh / s;
                    v.WorldX = (dw - v.WorldW) * 0.5f; v.WorldY = (dh - v.WorldH) * 0.5f; break;
                case ViewportMode.Integer:
                    float k = (float)Math.Floor(s + 1e-4f); if (k < 1f) k = 1f;
                    v.ScaleX = v.ScaleY = k; v.W = dw * k; v.H = dh * k;
                    v.X = (float)Math.Floor((sw - v.W) * 0.5f); v.Y = (float)Math.Floor((sh - v.H) * 0.5f); break;
                default:
                    v.ScaleX = v.ScaleY = s; v.W = dw * s; v.H = dh * s; v.X = (sw - v.W) * 0.5f; v.Y = (sh - v.H) * 0.5f; break;
            }
            return v;
        }

        public void ScreenToDesign(float sx, float sy, out float dx, out float dy)
        { dx = WorldX + (sx - Math.Max(X, 0)) / ScaleX; dy = WorldY + (sy - Math.Max(Y, 0)) / ScaleY; }
        public void DesignToScreen(float dx, float dy, out float sx, out float sy)
        { sx = Math.Max(X, 0) + (dx - WorldX) * ScaleX; sy = Math.Max(Y, 0) + (dy - WorldY) * ScaleY; }
        public bool Contains(float sx, float sy)
        { float x0 = Math.Max(X, 0), y0 = Math.Max(Y, 0); return sx >= x0 && sy >= y0 && sx < X + W && sy < Y + H; }
        /// <summary>glViewport/glScissor y (bottom-left origin).</summary>
        public int GLY(float surfaceH) { return (int)Math.Round(surfaceH - (Y + H)); }
    }

    public static class Gfx
    {
        public static bool OriginBottomLeft(GraphicsApi a) { return a <= GraphicsApi.WebGL; }
        public static bool DepthMinusOne(GraphicsApi a) { return a <= GraphicsApi.WebGL; }
        public static bool NdcYDown(GraphicsApi a) { return a == GraphicsApi.Vulkan; }

        /// <summary>Design space (Y down) → clip space of <paramref name="api"/>. Column-major unless rowMajor.</summary>
        public static float[] Ortho(GraphicsApi api, float left, float top, float right, float bottom, float zn, float zf, bool rowMajor = false)
        {
            var m = new float[16];
            float rl = right - left, bt = bottom - top, fn = zf - zn, ys = NdcYDown(api) ? 1f : -1f;
            m[0] = 2f / rl; m[12] = -(right + left) / rl; m[5] = ys * 2f / bt; m[13] = -ys * (bottom + top) / bt;
            if (DepthMinusOne(api)) { m[10] = 2f / fn; m[14] = -(zf + zn) / fn; } else { m[10] = 1f / fn; m[14] = -zn / fn; }
            m[15] = 1f;
            return rowMajor ? Transpose(m) : m;
        }

        /// <summary>Right-handed perspective. zf ≤ 0 → infinite far plane. reverseZ for [0,1] depth APIs.</summary>
        public static float[] Perspective(GraphicsApi api, float vfovRad, float aspect, float zn, float zf, bool reverseZ = false, bool rowMajor = false)
        {
            var m = new float[16];
            float f = 1f / (float)Math.Tan(vfovRad * 0.5f);
            bool gl = DepthMinusOne(api);
            m[0] = f / aspect; m[5] = NdcYDown(api) ? -f : f; m[11] = -1f;
            if (zf <= 0) { if (gl && !reverseZ) { m[10] = -1; m[14] = -2 * zn; } else if (reverseZ) { m[10] = 0; m[14] = zn; } else { m[10] = -1; m[14] = -zn; } }
            else if (gl && !reverseZ) { m[10] = -(zf + zn) / (zf - zn); m[14] = -2f * zf * zn / (zf - zn); }
            else if (reverseZ) { m[10] = zn / (zf - zn); m[14] = zf * zn / (zf - zn); }
            else { m[10] = zf / (zn - zf); m[14] = zf * zn / (zn - zf); }
            return rowMajor ? Transpose(m) : m;
        }

        static float[] Transpose(float[] m)
        {
            var t = new float[16];
            for (int c = 0; c < 4; c++) for (int r = 0; r < 4; r++) t[r * 4 + c] = m[c * 4 + r];
            return t;
        }
    }

    public static class Fov
    {
        public const float Deg2Rad = (float)(Math.PI / 180.0), Rad2Deg = (float)(180.0 / Math.PI);
        public static float HorizontalFromVertical(float vfov, float aspect) { return 2f * (float)Math.Atan(Math.Tan(vfov * 0.5f) * aspect); }
        public static float VerticalFromHorizontal(float hfov, float aspect) { return 2f * (float)Math.Atan(Math.Tan(hfov * 0.5f) / aspect); }

        public static float ForAspect(FovPolicy policy, float designVfov, float designAspect, float aspect, float maxHfov = 0)
        {
            float designH = HorizontalFromVertical(designVfov, designAspect), v = designVfov;
            if (policy == FovPolicy.VertMinus || (policy == FovPolicy.Hybrid && aspect < designAspect)) v = VerticalFromHorizontal(designH, aspect);
            if (maxHfov > 0 && HorizontalFromVertical(v, aspect) > maxHfov) v = VerticalFromHorizontal(maxHfov, aspect);
            return v;
        }

        public static float OrthoHalfHeight(float designW, float designH, float aspect, bool fill = false)
        {
            bool wider = aspect > designW / designH;
            return (fill ? wider : !wider) ? 0.5f * designW / aspect : 0.5f * designH;
        }

        public static float SensitivityScale(float oldHfov, float newHfov, float coefficient = 0)
        {
            if (coefficient <= 0) return (float)(Math.Tan(newHfov * 0.5) / Math.Tan(oldHfov * 0.5));
            return (float)(Math.Atan(coefficient * Math.Tan(newHfov * 0.5)) / Math.Atan(coefficient * Math.Tan(oldHfov * 0.5)));
        }
    }

    public struct RenderSize { public int Width, Height; public float Scale; }
    public struct Jitter { public float Px, Py, ClipX, ClipY; }

    public static class Upscaling
    {
        static readonly float[,] Ratios = {
            {1f,1f,1f,1f,1f,1f,1f}, {0f,0f,1.3f,1.5f,1.7f,2f,0f}, {1f,0f,0f,1.5f,1.7f,2f,3f}, {1f,0f,0f,1.5f,1.7f,2f,3f},
            {1f,0f,0f,1.5f,1.7241379f,2f,3f}, {1f,1.3f,1.5f,1.7f,2f,2.3f,3f}, {1f,0f,1.3f,1.5f,1.7f,2f,3f},
            {1f,0f,0f,1.5f,1.7f,2f,3f}, {1f,0f,0f,1.5f,1.7f,2f,3f}, {1f,0f,1.3f,1.5f,1.7f,2f,0f} };

        public static float Ratio(Upscaler u, UpscaleQuality q)
        {
            int ui = (int)u, qi = (int)q;
            if (ui < 0 || ui > 9) ui = 0;
            if (qi < 0 || qi > 6) qi = 3;
            for (int d = 0; d < 7; d++)
            {
                int a = qi - d, b = qi + d;
                if (a >= 0 && Ratios[ui, a] > 0) return Ratios[ui, a];
                if (b < 7 && Ratios[ui, b] > 0) return Ratios[ui, b];
            }
            return 1f;
        }

        public static RenderSize ForScale(int displayW, int displayH, float scale)
        {
            if (!(scale > 0)) scale = 1; if (scale > 1) scale = 1;
            var r = new RenderSize { Width = Math.Max(1, (int)(displayW * scale + 0.5f)), Height = Math.Max(1, (int)(displayH * scale + 0.5f)), Scale = scale };
            return r;
        }
        public static RenderSize ForPreset(Upscaler u, UpscaleQuality q, int displayW, int displayH) { return ForScale(displayW, displayH, 1f / Ratio(u, q)); }

        public static float MipBias(Upscaler u, int renderW, int displayW)
        {
            if (u == Upscaler.None || renderW <= 0 || displayW <= 0 || renderW >= displayW) return 0;
            float b = (float)(Math.Log((double)renderW / displayW) / Math.Log(2.0));
            bool temporal = u == Upscaler.Fsr2 || u == Upscaler.Fsr3 || u == Upscaler.Dlss || u == Upscaler.Tsr || u == Upscaler.MetalFx || u == Upscaler.Pssr;
            return temporal ? b - 1f : b;
        }

        public static float Halton(int index, int b)
        {
            float f = 1f, r = 0f;
            while (index > 0) { f /= b; r += f * (index % b); index /= b; }
            return r;
        }
        public static int JitterPhaseCount(int renderW, int displayW)
        {
            if (renderW <= 0) return 8;
            float r = (float)displayW / renderW;
            return Math.Max(1, (int)(8f * r * r));
        }
        public static Jitter JitterFor(uint frame, int renderW, int renderH, int displayW)
        {
            int phases = JitterPhaseCount(renderW, displayW);
            int i = (int)(frame % (uint)phases) + 1;
            var j = new Jitter { Px = Halton(i, 2) - 0.5f, Py = Halton(i, 3) - 0.5f };
            j.ClipX = 2f * j.Px / Math.Max(renderW, 1); j.ClipY = -2f * j.Py / Math.Max(renderH, 1);
            return j;
        }
    }

    /// <summary>GPU-time driven dynamic resolution (damped, quantised).</summary>
    public sealed class DynamicResolution
    {
        public float TargetMs, MinScale, MaxScale, Smoothing = 0.2f, Headroom = 0.92f, Step = 1f / 64f;
        public float Scale { get; private set; }
        float _avg;
        public DynamicResolution(float targetMs, float minScale = 0.5f, float maxScale = 1f) { TargetMs = targetMs; MinScale = minScale; MaxScale = maxScale; Scale = maxScale; }
        public float Update(float gpuMs)
        {
            if (!(gpuMs > 0)) return Scale;
            _avg = _avg > 0 ? _avg + Smoothing * (gpuMs - _avg) : gpuMs;
            float want = Scale * (float)Math.Sqrt(TargetMs * Headroom / _avg);
            if (want > Scale) want = Scale + (want - Scale) * 0.25f;
            want = Math.Max(MinScale, Math.Min(MaxScale, want));
            float q = (float)Math.Floor(want / Step + 0.5f) * Step;
            if (Math.Abs(q - Scale) > Step * 0.5f) Scale = q;
            Scale = Math.Max(MinScale, Math.Min(MaxScale, Scale));
            return Scale;
        }
    }

    public static class Hud
    {
        public static Rect SafeRect(float surfaceW, float surfaceH, Insets os, float overscan = 0)
        {
            float ox = surfaceW * overscan, oy = surfaceH * overscan;
            float l = Math.Max(os.Left, ox), t = Math.Max(os.Top, oy), r = Math.Max(os.Right, ox), b = Math.Max(os.Bottom, oy);
            return new Rect(l, t, Math.Max(0, surfaceW - l - r), Math.Max(0, surfaceH - t - b));
        }
        public static Rect Band(Rect safe, float maxAspect)
        {
            if (maxAspect > 0 && safe.H > 0 && safe.W / safe.H > maxAspect) { float w = safe.H * maxAspect; safe.X += (safe.W - w) * 0.5f; safe.W = w; }
            return safe;
        }
        public static float MinTextPx(DeviceClass c, float surfaceHPx, DimenMetrics m = null)
        {
            switch (c)
            {
                case DeviceClass.Tv: return surfaceHPx * (26f / 1080f);
                case DeviceClass.Handheld: return surfaceHPx * (10f / 800f);
                case DeviceClass.Desktop: return surfaceHPx * (12f / 1080f);
                case DeviceClass.Vr: return surfaceHPx * (20f / 1080f);
                default: return m != null ? (float)(2.0 / 25.4 * m.Input.Dpi) : 12f;
            }
        }
        public static float MinTouchPx(DimenMetrics m) { return (float)(9.0 / 25.4 * m.Input.Dpi); }
    }

    public static class Lod
    {
        public static float ProjectedPx(float size, float distance, float vfov, float surfaceHPx)
        { return distance > 0 ? size * surfaceHPx / (2f * distance * (float)Math.Tan(vfov * 0.5f)) : float.MaxValue; }
        public static float Distance(float size, float targetPx, float vfov, float surfaceHPx)
        { return targetPx > 0 ? size * surfaceHPx / (2f * targetPx * (float)Math.Tan(vfov * 0.5f)) : 0f; }
        public static float DistanceScale(float surfaceHPx, float vfov, float refHPx, float refVfov)
        { return (surfaceHPx / refHPx) * (float)(Math.Tan(refVfov * 0.5) / Math.Tan(vfov * 0.5)); }
        public static float PixelSnap(float v, float scale) { return (float)Math.Floor(v * scale + 0.5f) / scale; }
    }

    /// <summary>Game-facing helpers over <see cref="Live"/> (Reference mode: values are px at the reference resolution).</summary>
    public static class GameDimens
    {
        public static bool Publish(Surface s) { return Live.Publish(s.ToInput()); }
        /// <summary>Reference mode: px at the reference → final px (scaled strategy).</summary>
        public static float Px(float referencePx) { return Live.Current.Sdp(referencePx); }
        public static float Px(Strategy s, float referencePx) { return Live.Current.Dp(s, 0, referencePx); }
        public static float TextPx(Strategy s, float referencePx) { return Live.Current.Sp(s, 0, referencePx); }
    }
}
