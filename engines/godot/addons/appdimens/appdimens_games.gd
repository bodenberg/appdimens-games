## AppDimens Games helpers for Godot 4 — viewports, FOV, upscaling, HUD safe areas and LOD.
## Same formulas as native/include/appdimens/games.h.
class_name AppDimensGames
extends RefCounted

enum ViewportMode { FIT, FILL, STRETCH, FIT_WIDTH, FIT_HEIGHT, EXPAND, INTEGER }
enum FovPolicy { HOR_PLUS, VERT_MINUS, HYBRID }
enum Upscaler { NONE, FSR1, FSR2, FSR3, DLSS, XESS, METALFX, TSR, PSSR, SGSR }
enum Quality { NATIVE_AA, ULTRA_QUALITY_PLUS, ULTRA_QUALITY, QUALITY, BALANCED, PERFORMANCE, ULTRA_PERFORMANCE }

const _RATIOS := [
	[1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0], [0.0, 0.0, 1.3, 1.5, 1.7, 2.0, 0.0], [1.0, 0.0, 0.0, 1.5, 1.7, 2.0, 3.0],
	[1.0, 0.0, 0.0, 1.5, 1.7, 2.0, 3.0], [1.0, 0.0, 0.0, 1.5, 1.7241379, 2.0, 3.0], [1.0, 1.3, 1.5, 1.7, 2.0, 2.3, 3.0],
	[1.0, 0.0, 1.3, 1.5, 1.7, 2.0, 3.0], [1.0, 0.0, 0.0, 1.5, 1.7, 2.0, 3.0], [1.0, 0.0, 0.0, 1.5, 1.7, 2.0, 3.0],
	[1.0, 0.0, 1.3, 1.5, 1.7, 2.0, 0.0]]

## Returns {rect: Rect2 (surface px), scale: Vector2, world: Rect2 (visible design units)}.
static func viewport(mode: int, surface: Vector2, design: Vector2) -> Dictionary:
	var sw := maxf(surface.x, 1.0)
	var sh := maxf(surface.y, 1.0)
	var dw := design.x if design.x > 0.0 else 1.0
	var dh := design.y if design.y > 0.0 else 1.0
	var fx := sw / dw
	var fy := sh / dh
	var s := minf(fx, fy)
	var rect := Rect2()
	var sc := Vector2(s, s)
	var world := Rect2(0, 0, dw, dh)
	match mode:
		ViewportMode.FILL:
			s = maxf(fx, fy)
			sc = Vector2(s, s)
			rect = Rect2((sw - dw * s) * 0.5, (sh - dh * s) * 0.5, dw * s, dh * s)
			world = Rect2((dw - sw / s) * 0.5, (dh - sh / s) * 0.5, sw / s, sh / s)
		ViewportMode.STRETCH:
			rect = Rect2(0, 0, sw, sh)
			sc = Vector2(fx, fy)
		ViewportMode.FIT_WIDTH:
			sc = Vector2(fx, fx)
			rect = Rect2(0, 0, sw, sh)
			world = Rect2(0, (dh - sh / fx) * 0.5, dw, sh / fx)
		ViewportMode.FIT_HEIGHT:
			sc = Vector2(fy, fy)
			rect = Rect2(0, 0, sw, sh)
			world = Rect2((dw - sw / fy) * 0.5, 0, sw / fy, dh)
		ViewportMode.EXPAND:
			rect = Rect2(0, 0, sw, sh)
			world = Rect2((dw - sw / s) * 0.5, (dh - sh / s) * 0.5, sw / s, sh / s)
		ViewportMode.INTEGER:
			var k := maxf(floorf(s + 1e-4), 1.0)
			sc = Vector2(k, k)
			rect = Rect2(floorf((sw - dw * k) * 0.5), floorf((sh - dh * k) * 0.5), dw * k, dh * k)
		_:
			rect = Rect2((sw - dw * s) * 0.5, (sh - dh * s) * 0.5, dw * s, dh * s)
	return {"rect": rect, "scale": sc, "world": world}

## Surface px (top-left) → design units (mouse/touch → game coordinates).
static func screen_to_design(vp: Dictionary, p: Vector2) -> Vector2:
	var r: Rect2 = vp.rect
	var o := Vector2(maxf(r.position.x, 0.0), maxf(r.position.y, 0.0))
	return vp.world.position + (p - o) / vp.scale

static func hfov_from_vfov(vfov: float, aspect: float) -> float:
	return 2.0 * atan(tan(vfov * 0.5) * aspect)

static func vfov_from_hfov(hfov: float, aspect: float) -> float:
	return 2.0 * atan(tan(hfov * 0.5) / aspect)

## Vertical FOV (radians) for `aspect`, from the FOV designed at `design_aspect`.
static func fov_for_aspect(policy: int, design_vfov: float, design_aspect: float, aspect: float, max_hfov := 0.0) -> float:
	var design_h := hfov_from_vfov(design_vfov, design_aspect)
	var v := design_vfov
	if policy == FovPolicy.VERT_MINUS or (policy == FovPolicy.HYBRID and aspect < design_aspect):
		v = vfov_from_hfov(design_h, aspect)
	if max_hfov > 0.0 and hfov_from_vfov(v, aspect) > max_hfov:
		v = vfov_from_hfov(max_hfov, aspect)
	return v

## Applies the FOV policy to a Camera3D (keep_aspect = KEEP_HEIGHT → `fov` is vertical, degrees).
static func apply_camera_fov(cam: Camera3D, policy: int, design_vfov_deg: float, design_aspect: float, max_hfov_deg := 0.0) -> void:
	var size := cam.get_viewport().get_visible_rect().size
	if size.y <= 0.0:
		return
	cam.keep_aspect = Camera3D.KEEP_HEIGHT
	cam.fov = rad_to_deg(fov_for_aspect(policy, deg_to_rad(design_vfov_deg), design_aspect, size.x / size.y, deg_to_rad(max_hfov_deg)))

static func upscale_ratio(upscaler: int, quality: int) -> float:
	var u := clampi(upscaler, 0, 9)
	var q := clampi(quality, 0, 6)
	for d in 7:
		var a := q - d
		var b := q + d
		if a >= 0 and _RATIOS[u][a] > 0.0:
			return _RATIOS[u][a]
		if b < 7 and _RATIOS[u][b] > 0.0:
			return _RATIOS[u][b]
	return 1.0

static func render_size(upscaler: int, quality: int, display: Vector2i) -> Vector2i:
	var s := 1.0 / upscale_ratio(upscaler, quality)
	return Vector2i(maxi(1, int(display.x * s + 0.5)), maxi(1, int(display.y * s + 0.5)))

static func mip_bias(upscaler: int, render_w: int, display_w: int) -> float:
	if upscaler == Upscaler.NONE or render_w <= 0 or display_w <= 0 or render_w >= display_w:
		return 0.0
	var b := log(float(render_w) / float(display_w)) / log(2.0)
	var temporal := upscaler in [Upscaler.FSR2, Upscaler.FSR3, Upscaler.DLSS, Upscaler.TSR, Upscaler.METALFX, Upscaler.PSSR]
	return b - 1.0 if temporal else b

static func halton(index: int, base: int) -> float:
	var f := 1.0
	var r := 0.0
	while index > 0:
		f /= base
		r += f * (index % base)
		index = index / base
	return r

## Configures a Viewport's 3D scaling for a preset: FSR1/SGSR → FSR, FSR2/3/DLSS/XeSS/TSR → FSR2
## (Godot's built-in temporal upscaler), MetalFX → METALFX_TEMPORAL when the engine has it (4.4+).
static func configure_3d_scaling(vp: Viewport, upscaler: int, quality: int, sharpness := 0.2) -> void:
	vp.scaling_3d_scale = 1.0 / upscale_ratio(upscaler, quality)
	var mode: int = Viewport.SCALING_3D_MODE_FSR2
	match upscaler:
		Upscaler.NONE:
			mode = Viewport.SCALING_3D_MODE_BILINEAR
		Upscaler.FSR1, Upscaler.SGSR:
			mode = Viewport.SCALING_3D_MODE_FSR
		Upscaler.METALFX:
			if ClassDB.class_has_integer_constant("Viewport", "SCALING_3D_MODE_METALFX_TEMPORAL"):
				mode = ClassDB.class_get_integer_constant("Viewport", "SCALING_3D_MODE_METALFX_TEMPORAL")
	vp.scaling_3d_mode = mode
	vp.fsr_sharpness = sharpness

## Safe rect (px) from DisplayServer + optional TV overscan; `max_aspect` keeps the HUD in a band on ultrawide.
static func safe_rect(surface: Vector2, overscan := 0.0, max_aspect := 0.0) -> Rect2:
	var safe := Rect2(Vector2.ZERO, surface)
	if DisplayServer.get_name() != "headless":
		var s := DisplayServer.get_display_safe_area()
		var screen := Vector2(DisplayServer.screen_get_size())
		if s.size.x > 0 and screen.x > 0:
			var k := surface / screen
			safe = Rect2(Vector2(s.position) * k, Vector2(s.size) * k)
	var ox := surface.x * overscan
	var oy := surface.y * overscan
	var l := maxf(safe.position.x, ox)
	var t := maxf(safe.position.y, oy)
	var r := maxf(surface.x - safe.end.x, ox)
	var b := maxf(surface.y - safe.end.y, oy)
	var out := Rect2(l, t, maxf(0.0, surface.x - l - r), maxf(0.0, surface.y - t - b))
	if max_aspect > 0.0 and out.size.y > 0.0 and out.size.x / out.size.y > max_aspect:
		var w := out.size.y * max_aspect
		out.position.x += (out.size.x - w) * 0.5
		out.size.x = w
	return out

static func lod_distance(size: float, target_px: float, vfov: float, surface_h: float) -> float:
	return size * surface_h / (2.0 * target_px * tan(vfov * 0.5)) if target_px > 0.0 else 0.0

static func projected_px(size: float, distance: float, vfov: float, surface_h: float) -> float:
	return size * surface_h / (2.0 * distance * tan(vfov * 0.5)) if distance > 0.0 else INF
