#pragma once

#include "CoreMinimal.h"
#include "Engine/DPICustomScalingRule.h"
#include "AppDimensDPIScalingRule.generated.h"

/**
 * UMG DPI scale driven by AppDimens. Project Settings ▸ Engine ▸ User Interface:
 *   DPI Scale Rule = Custom, Custom Scaling Rule Class = AppDimensDPIScalingRule.
 * Author widgets at the reference resolution (UAppDimensSettings::ReferenceShortSide).
 */
UCLASS()
class APPDIMENS_API UAppDimensDPIScalingRule : public UDPICustomScalingRule
{
	GENERATED_BODY()
public:
	virtual float GetDPIScaleBasedOnSize(FIntPoint Size) const override;
};
