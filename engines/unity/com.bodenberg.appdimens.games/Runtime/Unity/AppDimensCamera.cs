// Keeps a camera's framing consistent from 4:3 to 32:9 and portrait (Hor+ / Vert- / hybrid).
using UnityEngine;

namespace AppDimens.Games.Unity
{
    [ExecuteAlways]
    [RequireComponent(typeof(Camera))]
    [AddComponentMenu("AppDimens/AppDimens Camera")]
    public sealed class AppDimensCamera : MonoBehaviour
    {
        public FovPolicy policy = FovPolicy.Hybrid;
        [Tooltip("Vertical FOV (degrees) at the design aspect.")] public float designVerticalFov = 60f;
        public float designAspect = 16f / 9f;
        [Tooltip("Cap for the horizontal FOV in degrees (0 = none), e.g. 120 for 32:9.")] public float maxHorizontalFov = 0f;
        [Header("Orthographic (2D)")]
        public Vector2 designSize = new Vector2(1920, 1080);
        public float pixelsPerUnit = 100f;
        public bool fill = false;
        Camera _cam;
        float _lastAspect;

        void OnEnable() { _cam = GetComponent<Camera>(); Apply(); }
        void LateUpdate() { if (_cam != null && _cam.aspect != _lastAspect) Apply(); }

        public void Apply()
        {
            if (_cam == null) return;
            float a = _cam.aspect;
            _lastAspect = a;
            if (_cam.orthographic)
                _cam.orthographicSize = Fov.OrthoHalfHeight(designSize.x / pixelsPerUnit, designSize.y / pixelsPerUnit, a, fill);
            else
                _cam.fieldOfView = Fov.ForAspect(policy, designVerticalFov * Fov.Deg2Rad, designAspect, a, maxHorizontalFov * Fov.Deg2Rad) * Fov.Rad2Deg;
        }
    }
}
