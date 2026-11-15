# Unreal plugin checks (no engine required)

`stubs/` is a minimal stand-in for the UE5 headers the plugin uses (CoreMinimal, EngineSubsystem,
DPICustomScalingRule, DeveloperSettings, UnrealClient/FViewport, IConsoleManager…). It catches C++ errors
and checks the plugin logic; it does **not** replace building inside Unreal Engine.

```bash
P=engines/unreal/AppDimens/Source/AppDimens
c++ -std=c++20 -Iengines/unreal/tests/stubs -I$P/Public -I$P/ThirdParty \
    engines/unreal/tests/plugin_logic_test.cpp $P/Private/AppDimensSubsystem.cpp \
    $P/Private/AppDimensLibrary.cpp $P/Private/AppDimensDPIScalingRule.cpp -o ue_check && ./ue_check
```
