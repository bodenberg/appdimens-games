# AppDimens Games for Unity

Install: *Package Manager ▸ + ▸ Add package from git URL* →
`https://github.com/bodenberg/appdimens-games.git?path=/engines/unity/com.bodenberg.appdimens.games`

| Component | What it does |
|-----------|--------------|
| `AppDimensScreen` (auto) | Tracks `Screen.width/height/dpi/safeArea/fullScreen`, publishes snapshots, raises `Changed` |
| `AppDimensCanvasScaler` | `Canvas.scaleFactor` from a strategy (author uGUI at 1920×1080; `Auto` keeps 4K/ultrawide calm) |
| `AppDimensSafeArea` | RectTransform → safe area + TV overscan + ultrawide HUD band |
| `AppDimensCamera` | Hor+ / Vert- / hybrid FOV or orthographic size per aspect |
| `AppDimensDynamicResolution` | GPU-time → `ScalableBufferManager.ResizeBuffers` |
| `AppDimensUpscaling` | DLSS/FSR/XeSS preset → URP `renderScale`, mip bias |

Code: `AppDimensScreen.Asdp(64)` (px at 1080p → px now), `Live.Current.Resolve("ftsdp", 24)`.
All platforms (no native plugin): Windows, macOS, Linux/Steam Deck, Android, iOS, WebGL, consoles, XR.
