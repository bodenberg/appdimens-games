// Fits a RectTransform to Screen.safeArea (+ optional TV overscan), updating on change.
using UnityEngine;

namespace AppDimens.Games.Unity
{
    [RequireComponent(typeof(RectTransform))]
    [AddComponentMenu("AppDimens/AppDimens Safe Area")]
    public sealed class AppDimensSafeArea : MonoBehaviour
    {
        [Range(0f, 0.1f)] [Tooltip("Extra overscan per side (0.05 = 90 % title-safe for TVs).")]
        public float overscan = 0f;
        [Tooltip("Keep the HUD inside this aspect on ultrawide screens (0 = off; 1.777 = 16:9).")]
        public float maxAspect = 0f;
        RectTransform _rt;

        void OnEnable() { _rt = (RectTransform)transform; AppDimensScreen.Changed += OnChanged; OnChanged(Live.Current); }
        void OnDisable() { AppDimensScreen.Changed -= OnChanged; }

        void OnChanged(DimenMetrics m)
        {
            float w = Screen.width, h = Screen.height;
            if (w <= 0 || h <= 0) return;
            var s = AppDimensScreen.SafeArea;
            var r = Hud.SafeRect(w, h, new Insets(s.X, s.Y, w - (s.X + s.W), h - (s.Y + s.H)), overscan);
            r = Hud.Band(r, maxAspect);
            // top-left → Unity anchors (bottom-left origin)
            _rt.anchorMin = new Vector2(r.X / w, 1f - (r.Y + r.H) / h);
            _rt.anchorMax = new Vector2((r.X + r.W) / w, 1f - r.Y / h);
        }
    }
}
