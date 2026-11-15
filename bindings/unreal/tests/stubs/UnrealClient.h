#pragma once
#include "CoreMinimal.h"
class FViewport { public: FIntPoint Size{1920, 1080}; FIntPoint GetSizeXY() const { return Size; } static TMulticast<FViewport*, uint32> ViewportResizedEvent; };
inline TMulticast<FViewport*, uint32> FViewport::ViewportResizedEvent;
