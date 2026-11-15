#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintFunctionLibrary.h"
#include "AppDimensTypes.h"
#include "AppDimensLibrary.generated.h"

UCLASS()
class APPDIMENS_API UAppDimensLibrary : public UBlueprintFunctionLibrary
{
	GENERATED_BODY()
public:
	/** Scaled: px at the reference → px now. */
	UFUNCTION(BlueprintPure, Category = "AppDimens") static float Sdp(float ReferencePx);
	/** Auto: calm growth on 4K / ultrawide / TVs. */
	UFUNCTION(BlueprintPure, Category = "AppDimens") static float Asdp(float ReferencePx);
	/** Text (× accessibility text scale). */
	UFUNCTION(BlueprintPure, Category = "AppDimens") static float Ssp(float ReferencePx);
	/** Fit: never overflows on unusual aspects. */
	UFUNCTION(BlueprintPure, Category = "AppDimens") static float Ftsdp(float ReferencePx);
	UFUNCTION(BlueprintPure, Category = "AppDimens") static float ResolveToken(const FString& Token, float Value);

	/** Horizontal FOV (degrees, UE camera convention) for the current/any aspect from the FOV designed at DesignAspect. */
	UFUNCTION(BlueprintPure, Category = "AppDimens|Camera")
	static float HorizontalFovForAspect(EAppDimensFovPolicy Policy, float DesignHorizontalFovDeg, float DesignAspect, float Aspect, float MaxHorizontalFovDeg = 0.f);

	/** Render resolution for an upscaler preset at a display size. */
	UFUNCTION(BlueprintPure, Category = "AppDimens|Upscaling")
	static FIntPoint RenderResolution(EAppDimensUpscaler Upscaler, EAppDimensQuality Quality, FIntPoint Display);

	/** Screen percentage (100 / ratio) for a preset — e.g. DLSS Quality = 66.67. */
	UFUNCTION(BlueprintPure, Category = "AppDimens|Upscaling")
	static float ScreenPercentage(EAppDimensUpscaler Upscaler, EAppDimensQuality Quality);

	/** Sets r.ScreenPercentage (and r.AntiAliasingMethod=4 for TSR). Vendor plugins (DLSS/FSR/XeSS) read the screen percentage or their own quality cvar. */
	UFUNCTION(BlueprintCallable, Category = "AppDimens|Upscaling")
	static void ApplyUpscalerPreset(EAppDimensUpscaler Upscaler, EAppDimensQuality Quality);

	UFUNCTION(BlueprintPure, Category = "AppDimens|Upscaling")
	static float MipBias(EAppDimensUpscaler Upscaler, int32 RenderWidth, int32 DisplayWidth);

	/** Distance at which an object of Size (cm) covers TargetPx — resolution/FOV independent LOD thresholds. */
	UFUNCTION(BlueprintPure, Category = "AppDimens|LOD")
	static float LodDistance(float Size, float TargetPx, float HorizontalFovDeg, FIntPoint Viewport);
};
