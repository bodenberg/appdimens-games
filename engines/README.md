# AppDimens Games — game engines

| Folder | Engine | Install |
|---|---|---|
| [`unity/`](unity/com.bodenberg.appdimens.games) | Unity 2019.4+ (every platform incl. WebGL, consoles, XR) | Package Manager ▸ git URL `https://github.com/bodenberg/appdimens-games.git?path=/engines/unity/com.bodenberg.appdimens.games` |
| [`unreal/`](unreal/AppDimens) | Unreal Engine 5 | copy `engines/unreal/AppDimens` into your project's `Plugins/` |
| [`godot/`](godot/addons/appdimens) | Godot 4 (GDScript; C# via [`../dotnet`](../dotnet)) | copy `engines/godot/addons/appdimens` into your project |

The engines share the math of [`../native`](../native): `python3 tools/sync_bindings.py` copies the C headers
into the Unreal `ThirdParty/` folder, the .NET core into the Unity package and the parity vectors into the Godot
tests (`--check` in CI). Guide: [`../DOCUMENTATION/ENGINES.md`](../DOCUMENTATION/ENGINES.md).
