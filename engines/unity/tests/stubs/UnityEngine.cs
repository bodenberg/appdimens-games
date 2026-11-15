// Minimal UnityEngine API surface used by the package — compile-check only (CI without Unity).
#pragma warning disable CS0067, CS1591
using System;
namespace UnityEngine
{
    public enum ScaleMode { StretchToFill, ScaleAndCrop, ScaleToFit } // real Unity type: checks name resolution
    public class Object { public static void DontDestroyOnLoad(Object o) { } public static void Destroy(Object o) { } }
    public class Component : Object { public Transform transform => null; public T GetComponent<T>() => default(T); }
    public class Transform : Component { }
    public sealed class RectTransform : Transform { public Vector2 anchorMin, anchorMax; }
    public class Behaviour : Component { public bool enabled; }
    public class MonoBehaviour : Behaviour { }
    public sealed class GameObject : Object { public GameObject(string n) { } public T AddComponent<T>() where T : Component => default(T); }
    public struct Vector2 { public float x, y; public Vector2(float x, float y) { this.x = x; this.y = y; } }
    public struct Rect : IEquatable<Rect>
    {
        public float x, y, width, height;
        public bool Equals(Rect o) => x == o.x && y == o.y && width == o.width && height == o.height;
        public override bool Equals(object o) => o is Rect r && Equals(r);
        public override int GetHashCode() => 0;
        public static bool operator ==(Rect a, Rect b) => a.Equals(b);
        public static bool operator !=(Rect a, Rect b) => !a.Equals(b);
    }
    public static class Screen { public static int width, height; public static float dpi; public static Rect safeArea; public static bool fullScreen; }
    public static class Mathf { public static float Max(float a, float b) => Math.Max(a, b); public static float Round(float f) => (float)Math.Round(f); }
    public sealed class Canvas : Behaviour { public float scaleFactor; }
    public sealed class Camera : Behaviour { public float aspect, fieldOfView, orthographicSize; public bool orthographic, allowDynamicResolution; }
    public struct FrameTiming { public double gpuFrameTime, cpuFrameTime; }
    public static class FrameTimingManager { public static void CaptureFrameTimings() { } public static uint GetLatestTimings(uint n, FrameTiming[] t) => 0; }
    public static class ScalableBufferManager { public static void ResizeBuffers(float w, float h) { } }
    public static class Debug { public static void Log(object o) { } }
    public enum RuntimeInitializeLoadType { AfterSceneLoad, BeforeSceneLoad }
    public sealed class RuntimeInitializeOnLoadMethodAttribute : Attribute { public RuntimeInitializeOnLoadMethodAttribute(RuntimeInitializeLoadType t) { } }
    public sealed class DefaultExecutionOrderAttribute : Attribute { public DefaultExecutionOrderAttribute(int o) { } }
    public sealed class DisallowMultipleComponentAttribute : Attribute { }
    public sealed class ExecuteAlwaysAttribute : Attribute { }
    public sealed class RequireComponentAttribute : Attribute { public RequireComponentAttribute(Type t) { } }
    public sealed class AddComponentMenuAttribute : Attribute { public AddComponentMenuAttribute(string m) { } }
    public sealed class TooltipAttribute : Attribute { public TooltipAttribute(string t) { } }
    public sealed class HeaderAttribute : Attribute { public HeaderAttribute(string t) { } }
    public sealed class RangeAttribute : Attribute { public RangeAttribute(float a, float b) { } }
}
namespace UnityEngine.Rendering
{
    public class RenderPipelineAsset : UnityEngine.Object { }
    public static class GraphicsSettings { public static RenderPipelineAsset currentRenderPipeline; }
}
