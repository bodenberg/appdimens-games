# AppDimens Games — .NET / C#

| Folder | What |
|---|---|
| [`AppDimens.Games/`](AppDimens.Games) | netstandard2.0 package, pure managed: MonoGame, FNA, Stride, Godot .NET, Unity |
| [`AppDimens.Games.Tests/`](AppDimens.Games.Tests) | parity tests (`dotnet run --project dotnet/AppDimens.Games.Tests -c Release`) |
| [`dropin/AppDimensGames/`](dropin/AppDimensGames) | copy-in sources: single-file core and custom strategies (`AppDimensGamesCustom*.cs`, Unity asmdef) |

Unity users normally take the UPM package in [`../engines/unity`](../engines/unity), which embeds the same core.
Guide: [`../DOCUMENTATION/CSHARP-UNITY.md`](../DOCUMENTATION/CSHARP-UNITY.md).
