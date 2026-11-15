#pragma once
#include "CoreMinimal.h"
class UDPICustomScalingRule : public UObject { public: virtual float GetDPIScaleBasedOnSize(FIntPoint Size) const { return 1.f; } };
