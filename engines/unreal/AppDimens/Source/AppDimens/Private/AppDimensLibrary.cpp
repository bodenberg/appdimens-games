#include "AppDimensLibrary.h"
#include "AppDimensSubsystem.h"
#include "HAL/IConsoleManager.h"

static const ad_metrics* Live()
{
	static ad_metrics Fallback;
	static bool bInit = false;
	if (UAppDimensSubsystem* S = UAppDimensSubsystem::Get()) return &S->Metrics();
	if (!bInit) { ad_metrics_make(&Fallback, 300, 533, 1, 1); bInit = true; }
	return &Fallback;
}

float UAppDimensLibrary::Sdp(float V) { return adg_ref_px(Live(), AD_SCALED, V); }
float UAppDimensLibrary::Asdp(float V) { return adg_ref_px(Live(), AD_AUTO, V); }
float UAppDimensLibrary::Ssp(float V) { return adg_ref_text_px(Live(), AD_SCALED, V); }
float UAppDimensLibrary::Ftsdp(float V) { return adg_ref_px(Live(), AD_FIT, V); }

float UAppDimensLibrary::ResolveToken(const FString& Token, float Value)
{
	UAppDimensSubsystem* S = UAppDimensSubsystem::Get();
	return S ? S->Resolve(Token, Value) : Value;
}

float UAppDimensLibrary::HorizontalFovForAspect(EAppDimensFovPolicy Policy, float DesignHFovDeg, float DesignAspect, float Aspect, float MaxHFovDeg)
{
	if (DesignAspect <= 0.f || Aspect <= 0.f) return DesignHFovDeg;
	const float DesignV = adg_vfov_from_hfov(DesignHFovDeg * ADG_DEG2RAD, DesignAspect);
	const float V = adg_fov_for_aspect((adg_fov_policy)Policy, DesignV, DesignAspect, Aspect, MaxHFovDeg * ADG_DEG2RAD);
	return adg_hfov_from_vfov(V, Aspect) * ADG_RAD2DEG;
}

FIntPoint UAppDimensLibrary::RenderResolution(EAppDimensUpscaler U, EAppDimensQuality Q, FIntPoint Display)
{
	const adg_render_size R = adg_upscale_render_size((adg_upscaler)U, (adg_upscale_quality)Q, (uint32_t)FMath::Max(Display.X, 1), (uint32_t)FMath::Max(Display.Y, 1));
	return FIntPoint((int32)R.width, (int32)R.height);
}

float UAppDimensLibrary::ScreenPercentage(EAppDimensUpscaler U, EAppDimensQuality Q)
{
	return 100.f / adg_upscale_ratio((adg_upscaler)U, (adg_upscale_quality)Q);
}

void UAppDimensLibrary::ApplyUpscalerPreset(EAppDimensUpscaler U, EAppDimensQuality Q)
{
	if (IConsoleVariable* SP = IConsoleManager::Get().FindConsoleVariable(TEXT("r.ScreenPercentage")))
	{
		SP->Set(ScreenPercentage(U, Q), ECVF_SetByGameSetting);
	}
	if (U == EAppDimensUpscaler::TSR)
	{
		if (IConsoleVariable* AA = IConsoleManager::Get().FindConsoleVariable(TEXT("r.AntiAliasingMethod")))
		{
			AA->Set(4, ECVF_SetByGameSetting);
		}
	}
}

float UAppDimensLibrary::MipBias(EAppDimensUpscaler U, int32 RenderWidth, int32 DisplayWidth)
{
	return adg_upscale_mip_bias((adg_upscaler)U, (uint32_t)FMath::Max(RenderWidth, 0), (uint32_t)FMath::Max(DisplayWidth, 0));
}

float UAppDimensLibrary::LodDistance(float Size, float TargetPx, float HFovDeg, FIntPoint Viewport)
{
	if (Viewport.Y <= 0 || Viewport.X <= 0) return 0.f;
	const float Aspect = (float)Viewport.X / (float)Viewport.Y;
	const float V = adg_vfov_from_hfov(HFovDeg * ADG_DEG2RAD, Aspect);
	return adg_lod_distance(Size, TargetPx, V, (float)Viewport.Y);
}
