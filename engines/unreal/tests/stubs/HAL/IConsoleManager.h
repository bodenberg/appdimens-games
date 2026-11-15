#pragma once
#include "CoreMinimal.h"
enum EConsoleVariableFlags { ECVF_SetByGameSetting = 1 };
struct IConsoleVariable { virtual void Set(float, EConsoleVariableFlags) {} virtual void Set(int, EConsoleVariableFlags) {} };
struct IConsoleManager { static IConsoleManager& Get() { static IConsoleManager m; return m; } IConsoleVariable* FindConsoleVariable(const TCHAR*) { static IConsoleVariable v; return &v; } };
