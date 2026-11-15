#pragma once
#include "Engine/GameViewportClient.h"
class UEngine { public: UGameViewportClient* GameViewport = nullptr; template <class T> T* GetEngineSubsystem() { static T s; return &s; } };
inline UEngine* GEngine = nullptr;
