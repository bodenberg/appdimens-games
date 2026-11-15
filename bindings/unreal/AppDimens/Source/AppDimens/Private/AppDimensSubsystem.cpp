#include "AppDimensSubsystem.h"
#include "AppDimensSettings.h"
#include "Engine/Engine.h"
#include "Engine/GameViewportClient.h"
#include "UnrealClient.h"

static ad_metrics MakeMetrics(FIntPoint SizePx, float TextScale)
{
	const UAppDimensSettings* S = GetDefault<UAppDimensSettings>();
	adg_surface Surface;
	adg_surface_default(&Surface, (float)FMath::Max(SizePx.X, 1), (float)FMath::Max(SizePx.Y, 1));
	Surface.mode = ADG_SCALE_REFERENCE;
	Surface.param = S ? S->ReferenceShortSide : 1080.f;
	Surface.font_scale = TextScale;
	ad_metrics M;
	adg_metrics_from_surface(&M, &Surface);
	return M;
}

void UAppDimensSubsystem::Initialize(FSubsystemCollectionBase& Collection)
{
	Super::Initialize(Collection);
	const UAppDimensSettings* S = GetDefault<UAppDimensSettings>();
	TextScale = S ? S->TextScale : 1.f;
	M = MakeMetrics(Size, TextScale);
	ResizeHandle = FViewport::ViewportResizedEvent.AddUObject(this, &UAppDimensSubsystem::OnViewportResized);
	if (GEngine && GEngine->GameViewport && GEngine->GameViewport->Viewport)
	{
		Publish(GEngine->GameViewport->Viewport->GetSizeXY());
	}
}

void UAppDimensSubsystem::Deinitialize()
{
	FViewport::ViewportResizedEvent.Remove(ResizeHandle);
	Super::Deinitialize();
}

void UAppDimensSubsystem::OnViewportResized(FViewport* Viewport, uint32 /*Unused*/)
{
	// Only follow the game viewport (editor viewports resize too).
	if (Viewport && GEngine && GEngine->GameViewport && Viewport == GEngine->GameViewport->Viewport)
	{
		Publish(Viewport->GetSizeXY());
	}
}

void UAppDimensSubsystem::Publish(FIntPoint SizePx)
{
	if (SizePx.X <= 0 || SizePx.Y <= 0) return;
	const ad_metrics Next = MakeMetrics(SizePx, TextScale);
	if (ad_metrics_same(&Next, &M)) return;
	M = Next;
	Size = SizePx;
	OnChanged.Broadcast(Size);
}

void UAppDimensSubsystem::SetViewportSize(FIntPoint SizePx) { Publish(SizePx); }

void UAppDimensSubsystem::SetTextScale(float Scale)
{
	TextScale = Scale > 0.f ? Scale : 1.f;
	M = MakeMetrics(Size, TextScale);
	OnChanged.Broadcast(Size);
}

float UAppDimensSubsystem::Px(EAppDimensStrategy Strategy, float ReferencePx) const
{
	return adg_ref_px(&M, (ad_strategy)Strategy, ReferencePx);
}

float UAppDimensSubsystem::Resolve(const FString& Token, float Value) const
{
	ad_token T;
	const FTCHARToUTF8 Utf8(*Token);
	if (!ad_parse_token(Utf8.Get(), &T)) return Value;
	return (float)ad_token_resolve(&M, &T, Value);
}

float UAppDimensSubsystem::Factor(EAppDimensStrategy Strategy, bool bAspectRatio) const
{
	return (float)ad_factor(&M, (ad_strategy)Strategy, AD_SLOT(AD_VAR_S, bAspectRatio ? AD_FLAG_A : 0));
}

UAppDimensSubsystem* UAppDimensSubsystem::Get()
{
	return GEngine ? GEngine->GetEngineSubsystem<UAppDimensSubsystem>() : nullptr;
}
