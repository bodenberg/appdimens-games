# AppDimens Games for Unreal Engine 5

Copy `AppDimens/` into your project's `Plugins/` folder and enable **AppDimens Games**.

1. **UMG scaling** — *Project Settings ▸ Engine ▸ User Interface*: DPI Scale Rule = **Custom**, Custom Scaling Rule Class =
   **AppDimensDPIScalingRule**. Author widgets at 1920×1080 (*Project Settings ▸ Plugins ▸ AppDimens*: reference short side,
   strategy — `Auto` keeps 4K/ultrawide calm, `Fit` never overflows).
2. **Blueprint / C++ values** — `Sdp`, `Asdp`, `Ssp`, `Ftsdp`, `ResolveToken("hdpLw", 32)`; `UAppDimensSubsystem::OnChanged`.
3. **Camera** — `HorizontalFovForAspect(Hybrid, 90, 16/9, Aspect, 120)` → `UCameraComponent::SetFieldOfView`.
4. **Upscaling** — `ApplyUpscalerPreset(TSR|DLSS|FSR3|XeSS, Quality)` sets `r.ScreenPercentage`; `RenderResolution`, `MipBias`.
5. **LOD** — `LodDistance(Size, TargetPx, FOV, ViewportSize)` keeps LOD switches identical at 720p/4K and any FOV.

The plugin embeds the same C99 core as every AppDimens library (`Source/AppDimens/ThirdParty/appdimens`).
