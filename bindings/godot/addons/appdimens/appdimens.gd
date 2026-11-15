## AppDimens autoload (singleton "AppDimens"). Tracks the root window and publishes snapshots.
##   AppDimens.asdp(64)               # Reference mode: px at 1080p → px now
##   AppDimens.resolve("ftsdp", 24)
##   AppDimens.changed.connect(_relayout)
##   AppDimens.apply_ui_scale()       # scales the whole UI (Window.content_scale_factor)
extends Node

signal changed(metrics: AppDimensMetrics)

enum ScaleMode { REFERENCE, CONTENT, DPI }

## REFERENCE: design values are px at the reference resolution (games). CONTENT/DPI: dp-style.
@export var mode: ScaleMode = ScaleMode.REFERENCE
@export var reference_short_side := 1080.0
@export_range(0.5, 3.0) var text_scale := 1.0
@export var ui_strategy: AppDimensMetrics.Strategy = AppDimensMetrics.Strategy.AUTO
## Apply ui_strategy to Window.content_scale_factor automatically (content_scale_mode = DISABLED).
@export var auto_ui_scale := false

var metrics: AppDimensMetrics = AppDimensMetrics.new()

func _ready() -> void:
	get_tree().root.size_changed.connect(refresh)
	refresh()

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_DPI_CHANGE or what == NOTIFICATION_APPLICATION_RESUMED:
		refresh()

func _headless() -> bool:
	return DisplayServer.get_name() == "headless"

func surface_size() -> Vector2:
	var w := get_tree().root
	return Vector2(w.size) if w.size.x > 0 else Vector2(1920, 1080)

func _density() -> float:
	match mode:
		ScaleMode.CONTENT:
			return 1.0 if _headless() else maxf(DisplayServer.screen_get_scale(), 1.0)
		ScaleMode.DPI:
			return 1.0 if _headless() else maxf(float(DisplayServer.screen_get_dpi()), 1.0) / 160.0
	return reference_short_side / 300.0

## Re-reads the window. Returns true when the snapshot changed.
func refresh() -> bool:
	return publish_surface(surface_size())

## Publishes a surface manually (tests, SubViewports, split-screen panes).
func publish_surface(size: Vector2, multi_window := false) -> bool:
	var d := _density()
	var dpi := 0.0 if _headless() else float(DisplayServer.screen_get_dpi())
	var next := AppDimensMetrics.new(size.x / d, size.y / d, d, text_scale, multi_window, dpi)
	if next.same_inputs(metrics):
		return false
	next.generation = metrics.generation + 1
	metrics = next
	if auto_ui_scale:
		apply_ui_scale()
	changed.emit(metrics)
	return true

## Window.content_scale_factor from a strategy: author Control scenes at the reference resolution.
func apply_ui_scale(strategy := -1) -> void:
	var s: int = ui_strategy if strategy < 0 else strategy
	get_tree().root.content_scale_factor = metrics.factor(s, 0)

# ── Tokens (REFERENCE mode: px at the reference → px now; other modes: dp) ─────
func sdp(v: float) -> float: return metrics.dp(0, 0, v)
func hdp(v: float) -> float: return metrics.dp(0, 4, v)
func wdp(v: float) -> float: return metrics.dp(0, 8, v)
func ssp(v: float) -> float: return metrics.sp(0, 0, v)
func sem(v: float) -> float: return metrics.dp(0, 0, v)
func asdp(v: float) -> float: return metrics.dp(1, 0, v)
func assp(v: float) -> float: return metrics.sp(1, 0, v)
func ftsdp(v: float) -> float: return metrics.dp(9, 0, v)
func flsdp(v: float) -> float: return metrics.dp(10, 0, v)
func logsdp(v: float) -> float: return metrics.dp(6, 0, v)
func resolve(token: String, v: float) -> float: return metrics.resolve(token, v)
func factor(strategy: int, slot := 0) -> float: return metrics.factor(strategy, slot)
## Integer font size for Label/RichTextLabel theme overrides (default strategy: auto).
func font_size(v: float, strategy := 1) -> int: return maxi(1, int(round(metrics.sp(strategy, 0, v))))
