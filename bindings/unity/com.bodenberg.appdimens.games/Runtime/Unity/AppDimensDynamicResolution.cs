// GPU-time driven dynamic resolution through ScalableBufferManager (cameras need
// allowDynamicResolution = true; works with DLSS/FSR/STP integrations that read the scale).
using UnityEngine;

namespace AppDimens.Games.Unity
{
    [AddComponentMenu("AppDimens/AppDimens Dynamic Resolution")]
    public sealed class AppDimensDynamicResolution : MonoBehaviour
    {
        public float targetFps = 60f;
        [Range(0.25f, 1f)] public float minScale = 0.5f;
        [Range(0.25f, 1f)] public float maxScale = 1f;
        DynamicResolution _ctl;
        readonly FrameTiming[] _timings = new FrameTiming[1];

        public float CurrentScale { get { return _ctl != null ? _ctl.Scale : 1f; } }

        void OnEnable() { _ctl = new DynamicResolution(1000f / targetFps, minScale, maxScale); }

        void Update()
        {
            FrameTimingManager.CaptureFrameTimings();
            if (FrameTimingManager.GetLatestTimings(1, _timings) < 1) return;
            float gpu = (float)_timings[0].gpuFrameTime;
            if (gpu <= 0) return;
            _ctl.TargetMs = 1000f / targetFps; _ctl.MinScale = minScale; _ctl.MaxScale = maxScale;
            float before = _ctl.Scale, s = _ctl.Update(gpu);
            if (s != before) ScalableBufferManager.ResizeBuffers(s, s);
        }
    }
}
