#pragma once

#include "CoreMinimal.h"
#include "Subsystems/EngineSubsystem.h"
THIRD_PARTY_INCLUDES_START
#include "appdimens/games.h"
THIRD_PARTY_INCLUDES_END
#include "AppDimensTypes.h"
#include "AppDimensSubsystem.generated.h"

DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FOnAppDimensChanged, FIntPoint, ViewportSize);

/** Tracks the game viewport and keeps an AppDimens snapshot (Reference mode). */
UCLASS()
class APPDIMENS_API UAppDimensSubsystem : public UEngineSubsystem
{
	GENERATED_BODY()
public:
	virtual void Initialize(FSubsystemCollectionBase& Collection) override;
	virtual void Deinitialize() override;

	/** Broadcast after the viewport size (or text scale) changed. */
	UPROPERTY(BlueprintAssignable, Category = "AppDimens")
	FOnAppDimensChanged OnChanged;

	/** Publishes a size manually (split-screen panes, render targets, editor previews). */
	UFUNCTION(BlueprintCallable, Category = "AppDimens")
	void SetViewportSize(FIntPoint SizePx);

	UFUNCTION(BlueprintCallable, Category = "AppDimens")
	void SetTextScale(float Scale);

	UFUNCTION(BlueprintPure, Category = "AppDimens")
	FIntPoint GetViewportSize() const { return Size; }

	/** Value in px at the reference resolution → px now, through Strategy. */
	UFUNCTION(BlueprintPure, Category = "AppDimens")
	float Px(EAppDimensStrategy Strategy, float ReferencePx) const;

	/** Token by name: "sdp", "asdp", "ftsdp", "hdpLw", "assp"… (Reference mode: result is px). */
	UFUNCTION(BlueprintPure, Category = "AppDimens")
	float Resolve(const FString& Token, float Value) const;

	UFUNCTION(BlueprintPure, Category = "AppDimens")
	float Factor(EAppDimensStrategy Strategy, bool bAspectRatio = false) const;

	const ad_metrics& Metrics() const { return M; }

	static UAppDimensSubsystem* Get();

private:
	void OnViewportResized(FViewport* Viewport, uint32 Unused);
	void Publish(FIntPoint SizePx);

	ad_metrics M;
	FIntPoint Size = FIntPoint(1920, 1080);
	float TextScale = 1.f;
	FDelegateHandle ResizeHandle;
};
