#pragma once

#include "CoreMinimal.h"
#include "AppDimensTypes.generated.h"

/** Scaling strategies (same order as the AppDimens core). */
UENUM(BlueprintType)
enum class EAppDimensStrategy : uint8
{
	Scaled, Auto, Percent, Power, Fluid, Interpolated, Logarithmic, Diagonal, Perimeter, Fit, Fill, Density
};

UENUM(BlueprintType)
enum class EAppDimensUpscaler : uint8
{
	None, FSR1, FSR2, FSR3, DLSS, XeSS, MetalFX, TSR, PSSR, SGSR
};

UENUM(BlueprintType)
enum class EAppDimensQuality : uint8
{
	NativeAA, UltraQualityPlus, UltraQuality, Quality, Balanced, Performance, UltraPerformance
};

UENUM(BlueprintType)
enum class EAppDimensFovPolicy : uint8
{
	HorPlus, VertMinus, Hybrid
};
