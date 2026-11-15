#pragma once

#include "CoreMinimal.h"
#include "Engine/DeveloperSettings.h"
#include "AppDimensTypes.h"
#include "AppDimensSettings.generated.h"

/** Project Settings ▸ Plugins ▸ AppDimens. */
UCLASS(config = Game, defaultconfig, meta = (DisplayName = "AppDimens"))
class APPDIMENS_API UAppDimensSettings : public UDeveloperSettings
{
	GENERATED_BODY()
public:
	/** Short side of the resolution your UMG/HUD is authored at (1080 = 1920×1080). */
	UPROPERTY(config, EditAnywhere, Category = "Scaling", meta = (ClampMin = "100"))
	float ReferenceShortSide = 1080.f;

	/** Strategy used by the DPI scaling rule and the default helpers. Auto keeps 4K/ultrawide calm. */
	UPROPERTY(config, EditAnywhere, Category = "Scaling")
	EAppDimensStrategy UIStrategy = EAppDimensStrategy::Auto;

	/** Aspect-ratio correction (the `a` suffix). */
	UPROPERTY(config, EditAnywhere, Category = "Scaling")
	bool bAspectRatio = false;

	/** Accessibility text scale (bind to your options menu at runtime via UAppDimensSubsystem::SetTextScale). */
	UPROPERTY(config, EditAnywhere, Category = "Scaling", meta = (ClampMin = "0.5", ClampMax = "3.0"))
	float TextScale = 1.f;

	virtual FName GetCategoryName() const override { return TEXT("Plugins"); }
};
