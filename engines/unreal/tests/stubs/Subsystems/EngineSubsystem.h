#pragma once
#include "CoreMinimal.h"
class FSubsystemCollectionBase {};
class UEngineSubsystem : public UObject { public: using Super = UEngineSubsystem; virtual void Initialize(FSubsystemCollectionBase&) {} virtual void Deinitialize() {} };
