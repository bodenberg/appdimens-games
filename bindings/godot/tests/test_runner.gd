## Headless test (CI): godot --headless --path bindings/godot -s tests/test_runner.gd
extends SceneTree

var fails := 0
var checks := 0

func near(got: float, want: float, tol: float, what: String) -> void:
	checks += 1
	if absf(got - want) > tol:
		fails += 1
		if fails < 20:
			print("FAIL %s: got %.9f want %.9f" % [what, got, want])

func ok(c: bool, what: String) -> void:
	checks += 1
	if not c:
		fails += 1
		print("FAIL " + what)

func _initialize() -> void:
	var g: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://tests/golden.json"))
	for c in g.cases:
		var m := AppDimensMetrics.new(c.w, c.h, c.density, c.fontScale, c.multiWindow)
		for s in g.strategies.size():
			var row: Array = c.factors[g.strategies[s]]
			for i in row.size():
				near(m.factor(s, i), row[i], 2e-6 * maxf(1.0, absf(row[i])), "%s[%d] %sx%s" % [g.strategies[s], i, c.w, c.h])
		near(m.compute_factor(0, 0, 0, false, true, 0.01), c.customK001.scaled, 2e-6, "k scaled")
	ok(AppDimensMetrics.parse_token("sdpPxiaPh").slot == 15, "token sdpPxiaPh")
	ok(AppDimensMetrics.parse_token("pwdp").strategy == 2, "token pwdp → percent")
	ok(AppDimensMetrics.parse_token("hdpPh").is_empty(), "bad token")
	var ref := AppDimensMetrics.new(3840.0 / 3.6, 2160.0 / 3.6, 3.6)
	near(ref.dp(0, 0, 64), 128, 1e-6, "4K reference sdp")
	near(ref.resolve("asdp", 64), 64 * (1.6 + 0.4 * log(1.4)), 1e-6, "4K asdp")
	var vp := AppDimensGames.viewport(AppDimensGames.ViewportMode.INTEGER, Vector2(1366, 768), Vector2(320, 180))
	ok(vp.scale.x == 4.0 and vp.rect.position == Vector2(43, 24), "integer viewport")
	vp = AppDimensGames.viewport(AppDimensGames.ViewportMode.EXPAND, Vector2(2560, 1080), Vector2(1920, 1080))
	near(AppDimensGames.screen_to_design(vp, Vector2(320, 0)).x, 0, 1e-4, "expand mapping")
	ok(AppDimensGames.render_size(AppDimensGames.Upscaler.DLSS, AppDimensGames.Quality.BALANCED, Vector2i(3840, 2160)) == Vector2i(2227, 1253), "dlss balanced")
	near(AppDimensGames.mip_bias(AppDimensGames.Upscaler.FSR3, 1280, 1920), log(1280.0 / 1920.0) / log(2.0) - 1.0, 1e-9, "mip bias")
	var v := AppDimensGames.fov_for_aspect(AppDimensGames.FovPolicy.HYBRID, deg_to_rad(60), 16.0 / 9.0, 4.0 / 3.0)
	near(AppDimensGames.hfov_from_vfov(v, 4.0 / 3.0), AppDimensGames.hfov_from_vfov(deg_to_rad(60), 16.0 / 9.0), 1e-9, "hybrid fov")
	near(AppDimensGames.halton(1, 3), 1.0 / 3.0, 1e-12, "halton")
	var d := AppDimensGames.lod_distance(2.0, 100.0, 1.0, 1080.0)
	near(AppDimensGames.projected_px(2.0, d, 1.0, 1080.0), 100, 1e-6, "lod")
	process_frame.connect(_after_first_frame, CONNECT_ONE_SHOT)

func _after_first_frame() -> void:
	var ad = root.get_node_or_null("AppDimens")
	ok(ad != null, "autoload present")
	if ad != null:
		var seen := [0]
		ad.changed.connect(func(_m): seen[0] += 1)
		ok(ad.publish_surface(Vector2(3840, 2160)), "publish")
		ok(not ad.publish_surface(Vector2(3840, 2160)), "identical publish ignored")
		near(ad.sdp(64), 128, 1e-6, "autoload sdp")
		ok(ad.font_size(28) == int(round(28 * (1.6 + 0.4 * log(1.4)))), "font size")
		ok(seen[0] == 1, "changed signal once")
		ad.publish_surface(Vector2(1280, 800))
		ad.apply_ui_scale(0)
		near(root.content_scale_factor, 800.0 / 1080.0, 1e-5, "content scale factor")
	var sv := SubViewport.new()
	root.add_child(sv)
	AppDimensGames.configure_3d_scaling(sv, AppDimensGames.Upscaler.DLSS, AppDimensGames.Quality.QUALITY)
	near(sv.scaling_3d_scale, 1.0 / 1.5, 1e-6, "3d scaling scale")
	ok(sv.scaling_3d_mode == Viewport.SCALING_3D_MODE_FSR2, "3d scaling mode fsr2")
	print("godot: %d checks, %d failures" % [checks, fails])
	quit(1 if fails > 0 else 0)
