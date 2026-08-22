# API Quick Reference — AppDimens Games 3.0 (family parity)

## Kotlin code (px out)
Int/Float: sdp sdpa sdpi sdpia · hdp hdpa hdpi hdpia · wdp wdpa wdpi wdpia
Text: ssp sspa sspi sspia · hsp wsp · sem hem wem (fixed)
Inverters: sdpPh sdpLh sdpPw sdpLw · hdpLw hdpPw · wdpLh wdpPh (all with ignoreMultiWindows flag)
Hatches: Number.toDynamicScaledDp/Px(context, qualifier, inverter, ignoreMultiWindows, applyAspectRatio, customSensitivityK)
Facilitators: sdpRotate(ctx, rotationValue, targetOrientation[, qualifier]) · sdpMode(ctx, modeValue, uiModeType) ·
  sdpQualifier(ctx, qualifiedValue, qualifierType, threshold) · sdpScreen(ctx, screenValue, uiModeType, qualifierType, threshold)
Builder: Number.scaledDp() → .aspectRatio(k?) .ignoreMultiWindows() .screen(mode,value|.mode,q,t,value) .qualifier(q,t,value) .orientation(o,value) → .sdp(ctx)|.dp(ctx)
Java: DimenSdp.{sdp,sdpa,sdpi,sdpia,hdp,hdpi,wdp,wdpi}(ctx,v) · getDimensionInPx/Dp(...) · scaled(v) · warmupCache()

## Compose (@get:Composable)
Number.sdp/.sdpa/.sdpi/.sdpia : Dp · Number.sdpPx/.sdpaPx/.sdpiPx : Float
hdp/hdpa/hdpi(+Px) · wdp/wdpa/wdpi(+Px)
Text: Number.ssp/.sspi : TextUnit · Number.semPx : Float (fixed px)
Provider: AppDimensProvider { } · locals LocalDimenMetrics, LocalUiModeType · currentDimenMetrics()

## Satellites prefixes (code + compose, suffixes a/i/ia)
psdp phdp pwdp + spaceW/Sw/H(i) · pwsdp · fsdp · asdp · dgsdp · flsdp · ftsdp · isdp · logsdp · prsdp · dsdp

## Native C++
using namespace appdimens::games;
Metrics::make(wDp,hDp,swDp,dpiDpi,fontScale=1,fullscreen=true) → updateMetrics(m)
Reads: metrics() | invariantMetrics()
math:: scaledDp scaledArDp widthDp heightDp calculateScaledDp percentOfAxisDp powerDp fluidDp autoDp
       logarithmicDp interpolatedDp diagonalDp perimeterDp fitDp fillDp densityDp toPx scaleVecFit
render:: viewportRect glRect vkViewport dxViewport ortho (column-major float[16])
C99: adg_make adg_scaled adg_scaled_a adg_hdp adg_wdp adg_power adg_fluid adg_auto adg_logarithmic
     adg_interpolated adg_diagonal adg_perimeter adg_fit adg_fill adg_density adg_to_px
Kotlin JNI: com.appdimens.games.jni.NativeBridge.updateMetrics/scaled/scaledAr/scaledInvariant/power/fluid/auto/logarithmic/diagonal/fit/fill/toPx

## C#
AppDimensGames.Screen.Update(wDp,hDp,dpi) · MathKernels.Scaled/ScaledAr/ScaledInvariant/Width/Height/
PercentOfWidth/Power/Fluid/Auto/Logarithmic/Interpolated/Diagonal/Fit/Fill/ToPx · World.ViewportRect(mode,w,h,dw,dh) · Units.Mm/Cm/InchToPx
