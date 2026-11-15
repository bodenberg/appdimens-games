#pragma once
#include "CoreMinimal.h"
class FName { public: FName(const TCHAR*) {} };
class UDeveloperSettings : public UObject { public: virtual FName GetCategoryName() const { return FName(TEXT("Game")); } };
