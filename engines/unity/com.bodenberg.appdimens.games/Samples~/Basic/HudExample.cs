using UnityEngine;
using AppDimens.Games;
using AppDimens.Games.Unity;

// Attach to any GameObject: logs sizes when the screen changes.
public sealed class HudExample : MonoBehaviour
{
    void OnEnable() { AppDimensScreen.Changed += Log; }
    void OnDisable() { AppDimensScreen.Changed -= Log; }
    void Log(AppDimens.DimenMetrics m)
    {
        Debug.Log($"{Screen.width}x{Screen.height}: health bar {320f.ASdp()} px, crosshair {24f.FTSdp()} px, text {28f.Ssp()} px");
    }
}
