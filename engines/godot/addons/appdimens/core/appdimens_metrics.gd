## AppDimens core for GDScript — same constants, strategies, tokens and results as every
## AppDimens library (64-bit floats). Immutable after construction.
class_name AppDimensMetrics
extends RefCounted

enum Strategy { SCALED, AUTO, PERCENT, POWER, FLUID, INTERPOLATED, LOGARITHMIC, DIAGONAL, PERIMETER, FIT, FILL, DENSITY }
enum Kind { DP, SP, EM }
## Axis variants: S, H, W, S_PH, S_LH, S_PW, S_LW, H_LW, H_PW, W_LH, W_PH
enum Variant { S, H, W, S_PH, S_LH, S_PW, S_LW, H_LW, H_PW, W_LH, W_PH }

const VERSION := "3.3.0"
const BASE_WIDTH := 300.0
const BASE_HEIGHT := 533.0
const BASE_DIAGONAL := 611.6305
const BASE_PERIMETER := 833.0
const REFERENCE_AR := 1.78
const INV_BASE := 1.0 / 300.0
const ADJUSTMENT := 0.1 / 30.0
const SENSITIVITY := 0.08 / 30.0
const SLOTS := 44
const _VQ := [0, 1, 2, 0, 0, 0, 0, 1, 1, 2, 2]
const _VINV := [0, 0, 0, 7, 5, 8, 6, 1, 3, 2, 4]
const _PREFIXES := ["", "a", "p", "pw", "f", "i", "log", "dg", "pr", "ft", "fl", "d"]
const _ORDER := [6, 3, 7, 8, 9, 10, 1, 2, 4, 5, 11, 0]

var width := 0.0
var height := 0.0
var smallest_width := 0.0
var density := 1.0
var font_scale := 1.0
var dpi := 160.0
var multi_window := false
var ui_mode := 0
var is_landscape := false
var is_portrait := false
var min_dim := 0.0
var max_dim := 0.0
var sw := 300.0
var scale := 1.0
var log_nar := 0.0
var generation := 0
var _table := PackedFloat64Array()

static func slot(variant: int, aspect_ratio := false, ignore_multi_window := false) -> int:
	return variant * 4 + (1 if aspect_ratio else 0) + (2 if ignore_multi_window else 0)

func _init(w := 0.0, h := 0.0, dens := 1.0, fscale := 1.0, mw := false, phys_dpi := 0.0, mode := 0) -> void:
	width = w if w > 0.0 and is_finite(w) else 0.0
	height = h if h > 0.0 and is_finite(h) else 0.0
	density = dens if dens > 0.0 and is_finite(dens) else 1.0
	font_scale = fscale if fscale > 0.0 and is_finite(fscale) else 1.0
	dpi = phys_dpi if phys_dpi > 0.0 else 160.0 * density
	multi_window = mw
	ui_mode = mode
	min_dim = minf(width, height)
	max_dim = maxf(width, height)
	smallest_width = min_dim
	if width > 0.0 or height > 0.0:
		is_landscape = width > height
		is_portrait = not is_landscape
	sw = smallest_width if smallest_width > 0.0 else BASE_WIDTH
	scale = sw * INV_BASE
	var nar := ((max_dim / min_dim) if min_dim > 0.0 else 1.0) / REFERENCE_AR
	if not (nar > 0.0 and is_finite(nar)):
		nar = 1.0
	log_nar = log(nar)
	_table.resize(12 * SLOTS)
	for s in 12:
		for v in 11:
			var plain := compute_factor(s, _VQ[v], _VINV[v], false, false)
			var ar := compute_factor(s, _VQ[v], _VINV[v], false, true)
			var b: int = s * SLOTS + v * 4
			_table[b] = plain
			_table[b + 1] = ar
			_table[b + 2] = 1.0 if multi_window else plain
			_table[b + 3] = 1.0 if multi_window else ar

static func log_scale(d: float) -> float:
	if d > BASE_WIDTH:
		return 1.0 + 0.4 * log(d * INV_BASE)
	if d > 0.0:
		return 1.0 - 0.4 * log(BASE_WIDTH / d)
	return 1.0

func _effective(q: int, inv: int) -> int:
	match inv:
		1: return 2 if is_landscape and q == 1 else q
		2: return 1 if is_landscape and q == 2 else q
		3: return 2 if is_portrait and q == 1 else q
		4: return 1 if is_portrait and q == 2 else q
		5: return 1 if is_landscape and q == 0 else q
		6: return 2 if is_landscape and q == 0 else q
		7: return 1 if is_portrait and q == 0 else q
		8: return 2 if is_portrait and q == 0 else q
	return q

func _axis(q: int) -> float:
	return height if q == 1 else (width if q == 2 else sw)

## General path: any inverter and custom aspect-ratio sensitivity k.
func compute_factor(s: int, qual: int, inv: int, ignore_mw: bool, aspect: bool, k := SENSITIVITY) -> float:
	if ignore_mw and multi_window:
		return 1.0
	var def_sw := qual == 0 and inv == 0
	var q := qual if s >= Strategy.DIAGONAL else _effective(qual, inv)
	if s == Strategy.SCALED or s == Strategy.PERCENT:
		var d := sw if def_sw else _axis(q)
		if not aspect:
			return scale if def_sw else d * INV_BASE
		return 1.0 + (d - BASE_WIDTH) * (ADJUSTMENT + k * log_nar)
	var b := _base(s, q, def_sw)
	return b * (1.0 + k * log_nar) if aspect else b

func _base(s: int, q: int, def_sw: bool) -> float:
	var d := _axis(q)
	match s:
		Strategy.AUTO:
			return d * INV_BASE if d <= 480.0 else 480.0 * INV_BASE + 0.4 * log(1.0 + (d - 480.0) * INV_BASE)
		Strategy.POWER:
			return pow(d / BASE_WIDTH, 0.75)
		Strategy.FLUID:
			if d <= 320.0: return 0.8
			if d >= 768.0: return 1.2
			return 0.8 + 0.4 * (d - 320.0) / 448.0
		Strategy.INTERPOLATED:
			return 1.0 + (d * INV_BASE - 1.0) * 0.5
		Strategy.LOGARITHMIC:
			return log_scale(d)
		Strategy.DIAGONAL:
			return sqrt(min_dim * min_dim + max_dim * max_dim) / BASE_DIAGONAL
		Strategy.PERIMETER:
			return (min_dim + max_dim) / BASE_PERIMETER
		Strategy.FIT:
			return minf(min_dim / BASE_WIDTH, max_dim / BASE_HEIGHT)
		Strategy.FILL:
			return maxf(min_dim / BASE_WIDTH, max_dim / BASE_HEIGHT)
		Strategy.DENSITY:
			return density
	return d * INV_BASE

## Precomputed factor (hot path).
func factor(s: int, slot_index := 0) -> float:
	return _table[s * SLOTS + slot_index]

func dp(s: int, slot_index: int, value: float) -> float:
	return value * _table[s * SLOTS + slot_index]

func sp(s: int, slot_index: int, value: float) -> float:
	return value * _table[s * SLOTS + slot_index] * font_scale

func px(s: int, slot_index: int, value: float) -> float:
	return value * _table[s * SLOTS + slot_index] * density

func same_inputs(o: AppDimensMetrics) -> bool:
	return o != null and o.width == width and o.height == height and o.density == density and \
		o.font_scale == font_scale and o.dpi == dpi and o.multi_window == multi_window and o.ui_mode == ui_mode

## Parses "sdp", "asdpa", "hdpLw", "sdpPxiaPh", "logssp"… → {strategy, slot, kind, px} or {} if invalid.
static func parse_token(name: String) -> Dictionary:
	for s in _ORDER:
		var p: String = _PREFIXES[s]
		if not name.begins_with(p):
			continue
		var r := _parse_rest(name.substr(p.length()))
		if not r.is_empty():
			r["strategy"] = s
			return r
	return {}

static func _parse_rest(t: String) -> Dictionary:
	if t.length() < 3:
		return {}
	var axis := t[0]
	var q := 0 if axis == "s" else (1 if axis == "h" else (2 if axis == "w" else -1))
	if q < 0:
		return {}
	var k := t.substr(1, 2)
	var kind := Kind.DP if k == "dp" else (Kind.SP if k == "sp" else (Kind.EM if k == "em" else -1))
	if kind < 0:
		return {}
	var i := 3
	var n := t.length()
	var is_px := false
	var flags := -1
	var inv := ""
	while i < n:
		if i + 1 < n and t[i] == "P" and t[i + 1] == "x":
			if is_px: return {}
			is_px = true
			i += 2
		elif i + 1 < n and (t[i] == "P" or t[i] == "L") and (t[i + 1] == "h" or t[i + 1] == "w"):
			if inv != "": return {}
			inv = t.substr(i, 2)
			i += 2
		elif t[i] == "i" or t[i] == "a":
			if flags >= 0: return {}
			if t[i] == "i" and i + 1 < n and t[i + 1] == "a":
				flags = 3
				i += 2
			else:
				flags = 1 if t[i] == "a" else 2
				i += 1
		else:
			return {}
	if flags < 0:
		flags = 0
	var v := q
	if inv != "":
		var P := inv[0] == "P"
		var H := inv[1] == "h"
		if axis == "s":
			v = (3 if H else 5) if P else (4 if H else 6)
		elif axis == "h" and not H:
			v = 8 if P else 7
		elif axis == "w" and H:
			v = 10 if P else 9
		else:
			return {}
	return {"slot": v * 4 + flags, "kind": kind, "px": is_px}

func resolve(token: String, value: float) -> float:
	var t := parse_token(token)
	if t.is_empty():
		return value
	var r: float = value * _table[t.strategy * SLOTS + t.slot]
	if t.kind == Kind.SP:
		r *= font_scale
	return r * density if t.px else r
