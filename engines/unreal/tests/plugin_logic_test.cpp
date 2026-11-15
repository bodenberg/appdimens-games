// Behaviour smoke test of the plugin logic on the stubs.
#include "AppDimensSubsystem.h"
#include "AppDimensLibrary.h"
#include "AppDimensDPIScalingRule.h"
#include "Engine/Engine.h"
#include <cstdio>
#include <cmath>
int main() {
    static UEngine eng; static UGameViewportClient gvc; static FViewport vp; gvc.Viewport = &vp; eng.GameViewport = &gvc; GEngine = &eng;
    UAppDimensSubsystem* s = UAppDimensSubsystem::Get();
    FSubsystemCollectionBase c; s->Initialize(c);
    int fails = 0;
    auto chk = [&](bool ok, const char* w) { if (!ok) { std::printf("FAIL %s\n", w); fails++; } };
    chk(std::fabs(UAppDimensLibrary::Sdp(64) - 64) < 1e-3f, "1080p identity");
    vp.Size = FIntPoint(3840, 2160); FViewport::ViewportResizedEvent.Broadcast(&vp, 0);
    chk(std::fabs(UAppDimensLibrary::Sdp(64) - 128) < 1e-3f, "4K after resize event");
    chk(std::fabs(UAppDimensLibrary::ASdp(64) - 64 * (1.6 + 0.4 * std::log(1.4))) < 1e-3f, "4K asdp");
    chk(std::fabs(UAppDimensLibrary::ResolveToken(TEXT("ftsdp"), 100) - s->Px(EAppDimensStrategy::Fit, 100)) < 1e-4f, "token");
    chk(std::fabs(UAppDimensLibrary::FTSdp(100) - UAppDimensLibrary::ResolveToken(TEXT("ftsdp"), 100)) < 1e-4f, "FTSdp");
    chk(std::fabs(UAppDimensLibrary::Hdp(10) - UAppDimensLibrary::ResolveToken(TEXT("hdp"), 10)) < 1e-4f, "Hdp");
    chk(std::fabs(UAppDimensLibrary::LOGSsp(10) - UAppDimensLibrary::ResolveToken(TEXT("logssp"), 10)) < 1e-4f, "LOGSsp");
    chk(std::fabs(UAppDimensLibrary::FLSdp(10) - UAppDimensLibrary::ResolveToken(TEXT("flsdp"), 10)) < 1e-4f, "FLSdp");
    UAppDimensDPIScalingRule rule;
    chk(std::fabs(rule.GetDPIScaleBasedOnSize(FIntPoint(3840, 2160)) - (1.6 + 0.4 * std::log(1.4))) < 1e-4f, "DPI rule auto");
    chk(std::fabs(UAppDimensLibrary::ScreenPercentage(EAppDimensUpscaler::DLSS, EAppDimensQuality::Quality) - 66.6667f) < 1e-3f, "screen %");
    FIntPoint r = UAppDimensLibrary::RenderResolution(EAppDimensUpscaler::DLSS, EAppDimensQuality::Balanced, FIntPoint(3840, 2160));
    chk(r.X == 2227 && r.Y == 1253, "render res");
    float h = UAppDimensLibrary::HorizontalFovForAspect(EAppDimensFovPolicy::HorPlus, 90, 16.f / 9, 21.f / 9);
    chk(h > 90 && h < 120, "hor+ wider on 21:9");
    std::printf("unreal plugin logic: %s\n", fails ? "FAILED" : "OK");
    return fails;
}
