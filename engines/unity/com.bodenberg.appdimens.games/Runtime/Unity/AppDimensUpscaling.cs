// Upscaler presets → render scale for URP/HDRP/Built-in, without a hard SRP dependency.
using System.Reflection;
using UnityEngine;
using UnityEngine.Rendering;

namespace AppDimens.Games.Unity
{
    public static class AppDimensUpscaling
    {
        /// <summary>Per-axis render scale for a vendor preset (DLSS Quality = 0.667, FSR Performance = 0.5…).</summary>
        public static float RenderScale(Upscaler upscaler, UpscaleQuality quality) { return 1f / Upscaling.Ratio(upscaler, quality); }

        /// <summary>Sets URP's renderScale (or any pipeline asset exposing a float `renderScale`). Returns false if unsupported.</summary>
        public static bool ApplyToPipeline(Upscaler upscaler, UpscaleQuality quality)
        {
            var asset = GraphicsSettings.currentRenderPipeline;
            if (asset == null) return false;
            var p = asset.GetType().GetProperty("renderScale", BindingFlags.Public | BindingFlags.Instance);
            if (p == null || !p.CanWrite || p.PropertyType != typeof(float)) return false;
            p.SetValue(asset, RenderScale(upscaler, quality), null);
            return true;
        }

        /// <summary>Texture mip bias your materials/samplers should use at this scale (log2(render/display) − 1 for temporal upscalers).</summary>
        public static float MipBias(Upscaler upscaler, int renderWidth, int displayWidth) { return Upscaling.MipBias(upscaler, renderWidth, displayWidth); }
    }
}
