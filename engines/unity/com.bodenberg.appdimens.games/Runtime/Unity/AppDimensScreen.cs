// Tracks the Unity screen and publishes AppDimens snapshots. Auto-created before the first scene.
using UnityEngine;

namespace AppDimens.Games.Unity
{
    [DefaultExecutionOrder(-10000)]
    [DisallowMultipleComponent]
    [AddComponentMenu("AppDimens/AppDimens Screen")]
    public sealed class AppDimensScreen : MonoBehaviour
    {
        [Tooltip("Reference: design values are px at the reference resolution (games). Content/Dpi: dp-style.")]
        public ScaleMode mode = ScaleMode.Reference;
        [Tooltip("Reference mode: short side of the reference resolution in px (1080 = design at 1080p).")]
        public float referenceShortSide = 1080f;
        [Tooltip("Accessibility text scale (bind to your options menu).")]
        [Range(0.5f, 3f)] public float textScale = 1f;
        [Tooltip("Treat windowed (non-fullscreen) as multi-window: `i` tokens keep their base value.")]
        public bool windowedIsMultiWindow = false;
        [Tooltip("10-foot UI on TVs / consoles.")]
        public UiMode uiMode = UiMode.Normal;

        public static AppDimensScreen Instance { get; private set; }
        /// <summary>Raised after every published change (resolution, rotation, DPI, safe area).</summary>
        public static event System.Action<DimenMetrics> Changed;
        /// <summary>Current Screen.safeArea in px (top-left origin, like the AppDimens HUD helpers).</summary>
        public static AppDimens.Games.Rect SafeArea { get; private set; }

        int _w, _h; float _dpi, _scale; UnityEngine.Rect _safe; bool _full; ScaleMode _mode; float _ref; UiMode _ui;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        static void Bootstrap()
        {
            if (Instance != null) return;
            var go = new GameObject("AppDimensScreen");
            DontDestroyOnLoad(go);
            go.AddComponent<AppDimensScreen>();
        }

        void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(this); return; }
            Instance = this;
            Publish(true);
        }

        void OnDestroy() { if (Instance == this) Instance = null; }

        void Update() { Publish(false); }   // five compares per frame; publishes only on change

        /// <summary>Forces a re-read (e.g. after changing settings from code).</summary>
        public void Refresh() { Publish(true); }

        void Publish(bool force)
        {
            int w = Screen.width, h = Screen.height;
            float dpi = Screen.dpi;
            var safe = Screen.safeArea;
            bool full = Screen.fullScreen;
            if (!force && w == _w && h == _h && dpi == _dpi && safe == _safe && full == _full && textScale == _scale &&
                mode == _mode && referenceShortSide == _ref && uiMode == _ui) return;
            _w = w; _h = h; _dpi = dpi; _safe = safe; _full = full; _scale = textScale; _mode = mode; _ref = referenceShortSide; _ui = uiMode;

            var s = new Surface(w, h, mode, mode == ScaleMode.Reference ? referenceShortSide : (mode == ScaleMode.Dpi ? dpi : ContentScale(dpi)));
            s.Dpi = dpi > 0 ? dpi : 0;
            s.FontScale = textScale;
            s.MultiWindow = windowedIsMultiWindow && !full;
            s.UiMode = uiMode;
            // Unity's safeArea origin is bottom-left; AppDimens uses top-left.
            SafeArea = new AppDimens.Games.Rect(safe.x, h - (safe.y + safe.height), safe.width, safe.height);
            if (Live.Publish(s.ToInput()) || force)
            {
                var h2 = Changed;
                if (h2 != null) h2(Live.Current);
            }
        }

        static float ContentScale(float dpi)
        {
#if UNITY_STANDALONE_WIN || UNITY_EDITOR_WIN
            return dpi > 0 ? dpi / 96f : 1f;      // Windows logical DPI (100 % = 96); 96 under Proton by default
#elif UNITY_STANDALONE_OSX || UNITY_IOS
            return dpi > 0 ? Mathf.Max(1f, Mathf.Round(dpi / 132f)) : 1f;
#else
            return dpi > 0 ? dpi / 160f : 1f;     // Android / Linux: dp
#endif
        }

        // Convenience (Reference mode: px at the reference → px now). Every family token is also an extension
        // method, the AppDimens.Net way: 320f.ASdp(), 24f.FTSdp(), 28f.Ssp() (AppDimens.Games.Tokens).
        public static float Sdp(float v) { return Live.Current.Sdp(v); }
        public static float ASdp(float v) { return Live.Current.Asdp(v); }
        public static float Ssp(float v) { return Live.Current.Ssp(v); }
        public static float FTSdp(float v) { return Live.Current.Ftsdp(v); }
        public static float Resolve(string token, float v) { return Live.Current.Resolve(token, v); }
    }
}
