#include "AppDimensDPIScalingRule.h"
#include "AppDimensSettings.h"
THIRD_PARTY_INCLUDES_START
#include "appdimens/games.h"
THIRD_PARTY_INCLUDES_END

float UAppDimensDPIScalingRule::GetDPIScaleBasedOnSize(FIntPoint Size) const
{
	const UAppDimensSettings* S = GetDefault<UAppDimensSettings>();
	if (Size.X <= 0 || Size.Y <= 0) return 1.f;
	adg_surface Surface;
	adg_surface_default(&Surface, (float)Size.X, (float)Size.Y);
	Surface.param = S ? S->ReferenceShortSide : 1080.f;
	ad_metrics M;
	adg_metrics_from_surface(&M, &Surface);
	const ad_strategy Strategy = S ? (ad_strategy)S->UIStrategy : AD_AUTO;
	const int Slot = AD_SLOT(AD_VAR_S, (S && S->bAspectRatio) ? AD_FLAG_A : 0);
	return (float)ad_factor(&M, Strategy, Slot);
}
