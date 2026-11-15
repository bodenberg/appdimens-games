// Drives Canvas.scaleFactor with an AppDimens strategy. Author uGUI at the reference
// resolution (e.g. 1920×1080) with CanvasScaler = Constant Pixel Size (scale 1) or none.
using UnityEngine;

namespace AppDimens.Games.Unity
{
    [ExecuteAlways]
    [RequireComponent(typeof(Canvas))]
    [AddComponentMenu("AppDimens/AppDimens Canvas Scaler")]
    public sealed class AppDimensCanvasScaler : MonoBehaviour
    {
        [Tooltip("Auto (asdp) keeps HUDs calm on 4K/ultrawide; Fit never overflows on unusual aspects.")]
        public Strategy strategy = Strategy.Auto;
        [Tooltip("Aspect-ratio correction (the `a` suffix).")]
        public bool aspectRatio = false;
        [Range(0.25f, 4f)] public float userScale = 1f;
        Canvas _canvas;

        void OnEnable()
        {
            _canvas = GetComponent<Canvas>();
            AppDimensScreen.Changed += OnChanged;
            Apply(Live.Current);
        }
        void OnDisable() { AppDimensScreen.Changed -= OnChanged; }
        void OnChanged(DimenMetrics m) { Apply(m); }

        void Apply(DimenMetrics m)
        {
            if (_canvas == null) return;
            _canvas.scaleFactor = m.Factor(strategy, Constants.Slot(Variant.S, aspectRatio)) * userScale;
        }
    }
}
