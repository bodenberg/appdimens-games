using UnityEngine;
using AppDimens.Games.Unity;

// Attach to any GameObject: logs sizes when the screen changes.
public sealed class HudExample : MonoBehaviour
{
    void OnEnable() { AppDimensScreen.Changed += Log; }
    void OnDisable() { AppDimensScreen.Changed -= Log; }
    void Log(AppDimens.DimenMetrics m)
    {
        Debug.Log($"{Screen.width}x{Screen.height}: health bar {AppDimensScreen.Asdp(320)} px, crosshair {AppDimensScreen.Ftsdp(24)} px, text {AppDimensScreen.Ssp(28)} px");
    }
}
