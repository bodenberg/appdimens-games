/*
 * AppDimens custom scaling strategies — portable C99 (C++ compatible), stb-style single header.
 * Port of appdimens-dynamic `library-custom`: same JSON document, text-formula language, built-ins and
 * presets as every AppDimens platform; reproduces the 540-case cross-platform fixture.
 *
 *   #define ADC_IMPLEMENTATION   // in exactly one .c/.cpp file
 *   #include "appdimens/appdimens_custom.h"
 *
 *   char err[256];
 *   adc_load_json(json_text, err, sizeof err);                  // registers strategies, applies "default"
 *   const adc_strategy *brand = adc_get("brand");
 *   float pad = adc_dp(brand, &metrics, 16);                     // dp (logical units)
 *   float txt = adc_sp(adc_get("readableText"), &metrics, 14);   // text dp, font-scale policy applied
 *   adc_strategy *f = adc_formula("base * pow(sw / 360, 0.8)", err, sizeof err);   // shared per text
 *   adc_strategy *c = adc_custom_factor("product", my_factor_fn, user);            // code factor
 *
 * Threading: load / register strategies at start-up (or from one thread). Resolution with
 * adc_resolve() into your own adc_resolved is thread-safe; adc_dp()/adc_sp() memoize the last snapshot
 * per strategy and are meant for one UI/game thread.
 * Memory: ADC_MALLOC / ADC_FREE (default malloc/free; e.g. lv_malloc / lv_free on LVGL).
 */
#ifndef APPDIMENS_CUSTOM_H
#define APPDIMENS_CUSTOM_H

#include "appdimens.h"

#ifdef AD_FIXED_POINT
#error "appdimens_custom.h needs floating point (AD_FIXED_POINT builds keep the built-in strategies)"
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define ADC_FORMAT_VERSION 1

typedef enum adc_device { ADC_PHONE = 0, ADC_TABLET, ADC_DESKTOP, ADC_TV, ADC_WATCH, ADC_CAR, ADC_VR, ADC_DEVICE_COUNT } adc_device;
typedef enum adc_axis { ADC_AXIS_SW = 0, ADC_AXIS_WIDTH, ADC_AXIS_HEIGHT, ADC_AXIS_MIN, ADC_AXIS_MAX, ADC_AXIS_DIAGONAL, ADC_AXIS_PERIMETER } adc_axis;

typedef struct adc_screen {
    double width, height, smallest_width, density, font_scale;
    int landscape, multi_window;
    adc_device device;
} adc_screen;

typedef struct adc_strategy adc_strategy;

/** Code factor: `factor(screen)` (CustomScaleStrategy). */
typedef double (*adc_factor_fn)(const adc_screen *screen, void *user);
/** Code formula: final value from `base` (scalingFormula { }). */
typedef double (*adc_formula_fn)(double base, const adc_screen *screen, void *user);

typedef struct adc_resolved {
    const adc_strategy *strategy;
    adc_screen screen;
    double factor, axis_dp, reference_dp, curve_factor, ar_multiplier, coefficient;
    double min_value, max_value, font_max;
    int has_min_value, has_max_value, font_policy; /* 0 follow, 1 ignore, 2 clamp */
    int multi_window_ignored, transform_kind;      /* 0 none, 1 text formula, 2 code formula */
    const void *formula; adc_formula_fn code_fn; void *code_user;
    double ref_w, ref_h;
    unsigned applied_rules;                         /* bit i = rule i applied (first 32 rules) */
    unsigned rule_count;
} adc_resolved;

/** Screen from an AppDimens snapshot; device from the UI mode (Android rules: sw ≥ 600 → tablet). */
adc_screen adc_screen_from_metrics(const ad_metrics *m);
adc_screen adc_screen_make(double width, double height, double density, double font_scale, adc_device device);

/** Registers every strategy of a JSON document (object, array or {"strategies": […]}). 1 on success. */
int adc_load_json(const char *json, char *err, size_t err_len);
/** Parses a single strategy object / document and returns its first strategy (not registered). */
adc_strategy *adc_decode_strategy(const char *json, char *err, size_t err_len);

const adc_strategy *adc_get(const char *id);            /* registered → built-in → preset */
const adc_strategy *adc_default(void);                   /* initially the "scaled" built-in */
void adc_set_default(const adc_strategy *s);              /* NULL restores "scaled" */
int adc_register(adc_strategy *s);                        /* registry takes ownership */
void adc_unregister(const char *id);
void adc_reset(void);                                      /* frees registry + shared formulas */

/** Text formula, shared per text (bounded cache); NULL + err on syntax error. */
adc_strategy *adc_formula(const char *text, char *err, size_t err_len);
adc_strategy *adc_custom_factor(const char *id, adc_factor_fn fn, void *user);
adc_strategy *adc_custom_formula(const char *id, adc_formula_fn fn, void *user);
void adc_free(adc_strategy *s);                            /* only for unregistered strategies */

const char *adc_id(const adc_strategy *s);
int adc_value_dependent(const adc_strategy *s);

/** Resolves for one screen (thread-safe; no allocation). */
void adc_resolve(const adc_strategy *s, const adc_screen *screen, adc_resolved *out);
double adc_scale(const adc_resolved *r, double base);        /* dp */
double adc_scale_sp_dp(const adc_resolved *r, double base);  /* text dp × effective font scale */
double adc_scale_sp_units(const adc_resolved *r, double base);
double adc_effective_font_scale(const adc_resolved *r);
/** Name of applied rule `i` of the resolved strategy ("#i" when unnamed). */
const char *adc_rule_name(const adc_strategy *s, unsigned i);

/** Memoized helpers over an ad_metrics snapshot (pointer + generation). */
float adc_dp(const adc_strategy *s, const ad_metrics *m, float value);
float adc_px(const adc_strategy *s, const ad_metrics *m, float value);
float adc_sp(const adc_strategy *s, const ad_metrics *m, float value);
float adc_sp_px(const adc_strategy *s, const ad_metrics *m, float value);
float adc_factor(const adc_strategy *s, const ad_metrics *m);
/** Writes a one-line explanation into buf (returns buf). */
const char *adc_explain(const adc_strategy *s, const adc_screen *screen, char *buf, size_t len);

#ifdef __cplusplus
}
#endif
#endif /* APPDIMENS_CUSTOM_H */

/* ═══════════════════════════════════════════════════════════════════════════════════════ */
#ifdef ADC_IMPLEMENTATION
#ifndef ADC_IMPLEMENTATION_DONE
#define ADC_IMPLEMENTATION_DONE

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef ADC_MALLOC
#define ADC_MALLOC(n) malloc(n)
#define ADC_FREE(p) free(p)
#endif

#ifdef __cplusplus
extern "C" {
#endif

static const char *const adc__device_names[ADC_DEVICE_COUNT] = {"phone", "tablet", "desktop", "tv", "watch", "car", "vr"};
static const char *const adc__axis_names[7] = {"smallestWidth", "width", "height", "min", "max", "diagonal", "perimeter"};

static void *adc__alloc(size_t n) { void *p = ADC_MALLOC(n ? n : 1); if (p) memset(p, 0, n ? n : 1); return p; }
static char *adc__strdup(const char *s) { size_t n = strlen(s) + 1; char *d = (char *)adc__alloc(n); if (d) memcpy(d, s, n); return d; }
static int adc__ieq(const char *a, const char *b) {
    for (; *a && *b; a++, b++) {
        char x = *a, y = *b;
        if (x >= 'A' && x <= 'Z') x = (char)(x + 32);
        if (y >= 'A' && y <= 'Z') y = (char)(y + 32);
        if (x != y) return 0;
    }
    return *a == *b;
}
static void adc__err(char *err, size_t n, const char *msg) { if (err && n) { strncpy(err, msg, n - 1); err[n - 1] = 0; } }

/* ── screen ─────────────────────────────────────────────────────────────────────────── */
static double adc__read(const adc_screen *s, adc_axis a) {
    const double mn = s->width < s->height ? s->width : s->height, mx = s->width < s->height ? s->height : s->width;
    switch (a) {
    case ADC_AXIS_SW: return s->smallest_width;
    case ADC_AXIS_WIDTH: return s->width;
    case ADC_AXIS_HEIGHT: return s->height;
    case ADC_AXIS_MIN: return mn;
    case ADC_AXIS_MAX: return mx;
    case ADC_AXIS_DIAGONAL: return sqrt(mn * mn + mx * mx);
    default: return mn + mx;
    }
}
static double adc__aspect(const adc_screen *s) {
    const double mn = s->width < s->height ? s->width : s->height, mx = s->width < s->height ? s->height : s->width;
    return mn > 0 ? mx / mn : 1.0;
}
static double adc__ref_read(double w, double h, adc_axis a) {
    switch (a) {
    case ADC_AXIS_SW: case ADC_AXIS_MIN: return w < h ? w : h;
    case ADC_AXIS_WIDTH: return w;
    case ADC_AXIS_HEIGHT: return h;
    case ADC_AXIS_MAX: return w > h ? w : h;
    case ADC_AXIS_DIAGONAL: return sqrt(w * w + h * h);
    default: return w + h;
    }
}

adc_screen adc_screen_make(double width, double height, double density, double font_scale, adc_device device) {
    adc_screen s;
    s.width = width; s.height = height;
    s.smallest_width = width < height ? width : height;
    s.density = density; s.font_scale = font_scale;
    s.landscape = width > height; s.multi_window = 0; s.device = device;
    return s;
}

adc_screen adc_screen_from_metrics(const ad_metrics *m) {
    adc_screen s;
    s.width = m->in.width; s.height = m->in.height; s.smallest_width = m->sw;
    s.density = m->in.density; s.font_scale = m->in.font_scale;
    s.landscape = m->is_landscape ? 1 : (m->is_portrait ? 0 : m->in.width > m->in.height);
    s.multi_window = m->in.multi_window;
    switch (m->in.ui_mode) {
    case AD_UI_DESK: s.device = ADC_DESKTOP; break;
    case AD_UI_CAR: s.device = ADC_CAR; break;
    case AD_UI_TELEVISION: s.device = ADC_TV; break;
    case AD_UI_WATCH: s.device = ADC_WATCH; break;
    case AD_UI_VR_HEADSET: s.device = ADC_VR; break;
    default: s.device = m->sw >= 600 ? ADC_TABLET : ADC_PHONE; break;
    }
    return s;
}

/* ── expressions ────────────────────────────────────────────────────────────────────── */
enum {
    V_SW, V_WIDTH, V_HEIGHT, V_SHORT, V_LONG, V_DIAG, V_PERIM, V_AR, V_DENSITY, V_FONT, V_LAND, V_PORT, V_MW,
    V_PHONE, V_TABLET, V_DESKTOP, V_TV, V_WATCH, V_CAR, V_VR, V_PI, V_E, V_SCREEN_END = V_E,
    V_RATIO, V_AXIS, V_REF, V_REFW, V_REFH, V_CURVE_END = V_REFH,
    V_BASE, V_SCALED, V_FACTOR, V_COUNT
};
static const char *const adc__var_names[V_COUNT] = {
    "sw", "width", "height", "shortSide", "longSide", "diagonal", "perimeter", "aspectRatio", "density", "fontScale",
    "landscape", "portrait", "multiWindow", "phone", "tablet", "desktop", "tv", "watch", "car", "vr", "pi", "e",
    "ratio", "axis", "ref", "refWidth", "refHeight", "base", "scaled", "factor"};

enum { N_NUM, N_VAR, N_NEG, N_NOT, N_BIN, N_TERN, N_CALL };
enum { OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD, OP_POW, OP_EQ, OP_NE, OP_LT, OP_LE, OP_GT, OP_GE, OP_AND, OP_OR };
enum { F_MIN, F_MAX, F_CLAMP, F_POW, F_SQRT, F_LN, F_LOG, F_LOG10, F_LOG2, F_EXP, F_ABS, F_SIGN, F_FLOOR, F_CEIL, F_ROUND,
       F_LERP, F_STEP, F_SMOOTH, F_COUNT };
static const char *const adc__fn_names[F_COUNT] = {"min", "max", "clamp", "pow", "sqrt", "ln", "log", "log10", "log2", "exp",
                                                   "abs", "sign", "floor", "ceil", "round", "lerp", "step", "smoothstep"};
static const int adc__fn_min[F_COUNT] = {1, 1, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 2, 3};
static const int adc__fn_max[F_COUNT] = {1 << 20, 1 << 20, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 2, 3};

typedef struct adc__node { int kind, op, a, b, c, nargs; double num; int *args; } adc__node;
typedef struct adc_expr { char *source; adc__node *nodes; int count, cap, root, coef; /* coef: -2 none, -1 const 1 */ } adc_expr;

typedef struct adc__parser { const char *s; size_t i, n; int allowed_end; adc_expr *e; char *err; size_t err_len; int failed; } adc__parser;

static int adc__fail(adc__parser *p, const char *msg) {
    if (!p->failed) {
        char buf[320];
        snprintf(buf, sizeof buf, "Invalid expression at %u: %s in \"%s\"", (unsigned)p->i, msg, p->s);
        adc__err(p->err, p->err_len, buf);
        p->failed = 1;
    }
    return -1;
}
static int adc__push(adc__parser *p, int kind) {
    adc_expr *e = p->e;
    if (e->count == e->cap) {
        int nc = e->cap ? e->cap * 2 : 16;
        adc__node *nn = (adc__node *)adc__alloc(sizeof(adc__node) * (size_t)nc);
        if (!nn) return adc__fail(p, "out of memory");
        if (e->nodes) { memcpy(nn, e->nodes, sizeof(adc__node) * (size_t)e->count); ADC_FREE(e->nodes); }
        e->nodes = nn; e->cap = nc;
    }
    memset(&e->nodes[e->count], 0, sizeof(adc__node));
    e->nodes[e->count].kind = kind;
    return e->count++;
}
static void adc__ws(adc__parser *p) { while (p->i < p->n && (p->s[p->i] == ' ' || p->s[p->i] == '\t' || p->s[p->i] == '\n' || p->s[p->i] == '\r')) p->i++; }
static int adc__peek(adc__parser *p, const char *t) { size_t l = strlen(t); adc__ws(p); return p->i + l <= p->n && strncmp(p->s + p->i, t, l) == 0; }
static int adc__eat(adc__parser *p, const char *t) { if (!adc__peek(p, t)) return 0; p->i += strlen(t); return 1; }
static int adc__bin(adc__parser *p, int op, int a, int b) {
    if (a < 0 || b < 0) return -1;
    int n = adc__push(p, N_BIN);
    if (n < 0) return -1;
    p->e->nodes[n].op = op; p->e->nodes[n].a = a; p->e->nodes[n].b = b;
    return n;
}
static int adc__ternary(adc__parser *p);
static int adc__unary(adc__parser *p);
static int adc__isalpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
static int adc__isdigit(char c) { return c >= '0' && c <= '9'; }

static int adc__primary(adc__parser *p) {
    adc__ws(p);
    if (p->failed) return -1;
    if (p->i >= p->n) return adc__fail(p, "unexpected end");
    char c = p->s[p->i];
    if (c == '(') {
        p->i++;
        int n = adc__ternary(p);
        if (n < 0) return -1;
        if (!adc__eat(p, ")")) return adc__fail(p, "expected ')'");
        return n;
    }
    if (adc__isdigit(c) || c == '.') {
        size_t start = p->i;
        while (p->i < p->n && (adc__isdigit(p->s[p->i]) || p->s[p->i] == '.')) p->i++;
        if (p->i < p->n && (p->s[p->i] == 'e' || p->s[p->i] == 'E')) {
            size_t save = p->i++;
            if (p->i < p->n && (p->s[p->i] == '+' || p->s[p->i] == '-')) p->i++;
            if (p->i < p->n && adc__isdigit(p->s[p->i])) { while (p->i < p->n && adc__isdigit(p->s[p->i])) p->i++; }
            else p->i = save;
        }
        char buf[64];
        size_t len = p->i - start;
        if (len >= sizeof buf) len = sizeof buf - 1;
        memcpy(buf, p->s + start, len); buf[len] = 0;
        char *end = NULL;
        double v = strtod(buf, &end);
        int dots = 0;
        for (size_t k = 0; k < len; k++) dots += buf[k] == '.';
        if (!end || *end || dots > 1 || len == 0 || (len == 1 && buf[0] == '.')) { p->i = start; return adc__fail(p, "bad number"); }
        int n = adc__push(p, N_NUM);
        if (n >= 0) p->e->nodes[n].num = v;
        return n;
    }
    if (adc__isalpha(c) || c == '_') {
        size_t start = p->i;
        while (p->i < p->n && (adc__isalpha(p->s[p->i]) || adc__isdigit(p->s[p->i]) || p->s[p->i] == '_')) p->i++;
        char name[48];
        size_t len = p->i - start;
        if (len >= sizeof name) len = sizeof name - 1;
        memcpy(name, p->s + start, len); name[len] = 0;
        if (adc__eat(p, "(")) {
            int args[64], na = 0;
            if (!adc__eat(p, ")")) {
                do {
                    int a = adc__ternary(p);
                    if (a < 0) return -1;
                    if (na < 64) args[na++] = a;
                } while (adc__eat(p, ","));
                if (!adc__eat(p, ")")) return adc__fail(p, "expected ')'");
            }
            int f = -1;
            for (int k = 0; k < F_COUNT; k++) if (!strcmp(name, adc__fn_names[k])) { f = k; break; }
            if (f < 0) { p->i = start; char m[96]; snprintf(m, sizeof m, "unknown function '%s'", name); return adc__fail(p, m); }
            if (na < adc__fn_min[f] || na > adc__fn_max[f]) {
                p->i = start;
                char m[96];
                if (adc__fn_min[f] == adc__fn_max[f]) snprintf(m, sizeof m, "'%s' takes %d argument(s), got %d", name, adc__fn_min[f], na);
                else snprintf(m, sizeof m, "'%s' takes %d+ argument(s), got %d", name, adc__fn_min[f], na);
                return adc__fail(p, m);
            }
            int n = adc__push(p, N_CALL);
            if (n < 0) return -1;
            int *a = (int *)adc__alloc(sizeof(int) * (size_t)na);
            if (!a) return adc__fail(p, "out of memory");
            memcpy(a, args, sizeof(int) * (size_t)na);
            p->e->nodes[n].op = f; p->e->nodes[n].nargs = na; p->e->nodes[n].args = a;
            return n;
        }
        if (!strcmp(name, "true") || !strcmp(name, "false")) {
            int n = adc__push(p, N_NUM);
            if (n >= 0) p->e->nodes[n].num = name[0] == 't' ? 1.0 : 0.0;
            return n;
        }
        for (int v = 0; v <= p->allowed_end; v++) {
            if (!strcmp(name, adc__var_names[v])) {
                int n = adc__push(p, N_VAR);
                if (n >= 0) p->e->nodes[n].op = v;
                return n;
            }
        }
        p->i = start;
        char m[96];
        snprintf(m, sizeof m, "unknown variable '%s'", name);
        return adc__fail(p, m);
    }
    char m[32];
    snprintf(m, sizeof m, "unexpected '%c'", c);
    return adc__fail(p, m);
}
static int adc__power(adc__parser *p) {
    int n = adc__primary(p);
    if (n < 0) return -1;
    if (adc__eat(p, "^")) return adc__bin(p, OP_POW, n, adc__unary(p));
    return n;
}
static int adc__unary(adc__parser *p) {
    if (adc__eat(p, "-")) { int a = adc__unary(p); if (a < 0) return -1; int n = adc__push(p, N_NEG); if (n >= 0) p->e->nodes[n].a = a; return n; }
    if (adc__eat(p, "+")) return adc__unary(p);
    if (adc__peek(p, "!") && !adc__peek(p, "!=")) {
        adc__eat(p, "!");
        int a = adc__unary(p);
        if (a < 0) return -1;
        int n = adc__push(p, N_NOT);
        if (n >= 0) p->e->nodes[n].a = a;
        return n;
    }
    return adc__power(p);
}
static int adc__mul(adc__parser *p) {
    int n = adc__unary(p);
    while (n >= 0) {
        if (adc__eat(p, "*")) n = adc__bin(p, OP_MUL, n, adc__unary(p));
        else if (adc__eat(p, "/")) n = adc__bin(p, OP_DIV, n, adc__unary(p));
        else if (adc__eat(p, "%")) n = adc__bin(p, OP_MOD, n, adc__unary(p));
        else break;
    }
    return n;
}
static int adc__add(adc__parser *p) {
    int n = adc__mul(p);
    while (n >= 0) {
        if (adc__eat(p, "+")) n = adc__bin(p, OP_ADD, n, adc__mul(p));
        else if (adc__eat(p, "-")) n = adc__bin(p, OP_SUB, n, adc__mul(p));
        else break;
    }
    return n;
}
static int adc__cmp(adc__parser *p) {
    int n = adc__add(p);
    if (n < 0) return -1;
    static const char *const ops[6] = {"==", "!=", "<=", ">=", "<", ">"};
    static const int codes[6] = {OP_EQ, OP_NE, OP_LE, OP_GE, OP_LT, OP_GT};
    for (int k = 0; k < 6; k++) if (adc__eat(p, ops[k])) return adc__bin(p, codes[k], n, adc__add(p));
    return n;
}
static int adc__and(adc__parser *p) { int n = adc__cmp(p); while (n >= 0 && adc__eat(p, "&&")) n = adc__bin(p, OP_AND, n, adc__cmp(p)); return n; }
static int adc__or(adc__parser *p) { int n = adc__and(p); while (n >= 0 && adc__eat(p, "||")) n = adc__bin(p, OP_OR, n, adc__and(p)); return n; }
static int adc__ternary(adc__parser *p) {
    int c = adc__or(p);
    if (c < 0 || !adc__eat(p, "?")) return c;
    int a = adc__ternary(p);
    if (a < 0) return -1;
    if (!adc__eat(p, ":")) return adc__fail(p, "expected ':'");
    int b = adc__ternary(p);
    if (b < 0) return -1;
    int n = adc__push(p, N_TERN);
    if (n >= 0) { p->e->nodes[n].c = c; p->e->nodes[n].a = a; p->e->nodes[n].b = b; }
    return n;
}
static int adc__reads_value(const adc_expr *e, int n) {
    const adc__node *x = &e->nodes[n];
    switch (x->kind) {
    case N_NUM: return 0;
    case N_VAR: return x->op >= V_BASE;
    case N_NEG: case N_NOT: return adc__reads_value(e, x->a);
    case N_BIN: return adc__reads_value(e, x->a) || adc__reads_value(e, x->b);
    case N_TERN: return adc__reads_value(e, x->c) || adc__reads_value(e, x->a) || adc__reads_value(e, x->b);
    default:
        for (int k = 0; k < x->nargs; k++) if (adc__reads_value(e, x->args[k])) return 1;
        return 0;
    }
}
static void adc__expr_free(adc_expr *e) {
    if (!e) return;
    for (int k = 0; k < e->count; k++) if (e->nodes[k].args) ADC_FREE(e->nodes[k].args);
    ADC_FREE(e->nodes);
    ADC_FREE(e->source);
    ADC_FREE(e);
}
/* level: 0 screen, 1 curve, 2 formula */
static adc_expr *adc__expr_parse(const char *src, int level, char *err, size_t err_len) {
    adc_expr *e = (adc_expr *)adc__alloc(sizeof *e);
    if (!e) return NULL;
    e->source = adc__strdup(src);
    adc__parser p;
    p.s = src; p.i = 0; p.n = strlen(src); p.e = e; p.err = err; p.err_len = err_len; p.failed = 0;
    p.allowed_end = level == 0 ? V_SCREEN_END : (level == 1 ? V_CURVE_END : V_COUNT - 1);
    size_t k = 0;
    while (k < p.n && (src[k] == ' ' || src[k] == '\t' || src[k] == '\n' || src[k] == '\r')) k++;
    if (k == p.n) { adc__fail(&p, "empty expression"); adc__expr_free(e); return NULL; }
    e->root = adc__ternary(&p);
    adc__ws(&p);
    if (e->root >= 0 && p.i < p.n) { char m[32]; snprintf(m, sizeof m, "unexpected '%c'", src[p.i]); adc__fail(&p, m); }
    if (p.failed || e->root < 0) { adc__expr_free(e); return NULL; }
    e->coef = -2;
    const adc__node *r = &e->nodes[e->root];
    if (r->kind == N_VAR && r->op == V_BASE) e->coef = -1;
    else if (r->kind == N_BIN && r->op == OP_MUL) {
        const adc__node *a = &e->nodes[r->a], *b = &e->nodes[r->b];
        if (a->kind == N_VAR && a->op == V_BASE && !adc__reads_value(e, r->b)) e->coef = r->b;
        else if (b->kind == N_VAR && b->op == V_BASE && !adc__reads_value(e, r->a)) e->coef = r->a;
    }
    return e;
}
static double adc__fmod(double x, double y) { return (y == 0 || isinf(x)) ? NAN : fmod(x, y); }
static double adc__eval(const adc_expr *e, int n, const double *v) {
    const adc__node *x = &e->nodes[n];
    switch (x->kind) {
    case N_NUM: return x->num;
    case N_VAR: return v[x->op];
    case N_NEG: return -adc__eval(e, x->a, v);
    case N_NOT: return adc__eval(e, x->a, v) != 0.0 ? 0.0 : 1.0;
    case N_TERN: return adc__eval(e, x->c, v) != 0.0 ? adc__eval(e, x->a, v) : adc__eval(e, x->b, v);
    case N_BIN: {
        if (x->op == OP_AND) return (adc__eval(e, x->a, v) != 0.0 && adc__eval(e, x->b, v) != 0.0) ? 1.0 : 0.0;
        if (x->op == OP_OR) return (adc__eval(e, x->a, v) != 0.0 || adc__eval(e, x->b, v) != 0.0) ? 1.0 : 0.0;
        const double a = adc__eval(e, x->a, v), b = adc__eval(e, x->b, v);
        switch (x->op) {
        case OP_ADD: return a + b;
        case OP_SUB: return a - b;
        case OP_MUL: return a * b;
        case OP_DIV: return a / b;
        case OP_MOD: return adc__fmod(a, b);
        case OP_POW: return pow(a, b);
        case OP_EQ: return a == b;
        case OP_NE: return a != b;
        case OP_LT: return a < b;
        case OP_LE: return a <= b;
        case OP_GT: return a > b;
        default: return a >= b;
        }
    }
    default: {
        double a[64] = {0};
        const int na = x->nargs < 64 ? x->nargs : 64;
        for (int k = 0; k < na; k++) a[k] = adc__eval(e, x->args[k], v);
        switch (x->op) {
        case F_MIN: { double m = a[0]; for (int k = 1; k < na; k++) if (a[k] < m) m = a[k]; return m; }
        case F_MAX: { double m = a[0]; for (int k = 1; k < na; k++) if (a[k] > m) m = a[k]; return m; }
        case F_CLAMP: { double t = a[0] < a[2] ? a[0] : a[2]; return a[1] > t ? a[1] : t; }
        case F_POW: return pow(a[0], a[1]);
        case F_SQRT: return sqrt(a[0]);
        case F_LN: case F_LOG: return log(a[0]);
        case F_LOG10: return log10(a[0]);
        case F_LOG2: return log(a[0]) / log(2.0);
        case F_EXP: return exp(a[0]);
        case F_ABS: return fabs(a[0]);
        case F_SIGN: return a[0] > 0 ? 1.0 : (a[0] < 0 ? -1.0 : a[0]);
        case F_FLOOR: return floor(a[0]);
        case F_CEIL: return ceil(a[0]);
        case F_ROUND: return floor(a[0] + 0.5);
        case F_LERP: return a[0] + (a[1] - a[0]) * a[2];
        case F_STEP: return a[1] < a[0] ? 0.0 : 1.0;
        default: { double t = (a[2] - a[0]) / (a[1] - a[0]); t = t < 0 ? 0 : (t > 1 ? 1 : t); return t * t * (3 - 2 * t); }
        }
    }
    }
}
static void adc__vars(double *v, const adc_screen *s, double ratio, double axis, double ref, double rw, double rh,
                      double base, double scaled, double factor) {
    v[V_SW] = s->smallest_width; v[V_WIDTH] = s->width; v[V_HEIGHT] = s->height;
    v[V_SHORT] = adc__read(s, ADC_AXIS_MIN); v[V_LONG] = adc__read(s, ADC_AXIS_MAX);
    v[V_DIAG] = adc__read(s, ADC_AXIS_DIAGONAL); v[V_PERIM] = adc__read(s, ADC_AXIS_PERIMETER);
    v[V_AR] = adc__aspect(s); v[V_DENSITY] = s->density; v[V_FONT] = s->font_scale;
    v[V_LAND] = s->landscape ? 1 : 0; v[V_PORT] = s->landscape ? 0 : 1; v[V_MW] = s->multi_window ? 1 : 0;
    for (int d = 0; d < ADC_DEVICE_COUNT; d++) v[V_PHONE + d] = (int)s->device == d ? 1.0 : 0.0;
    v[V_PI] = 3.14159265358979323846; v[V_E] = 2.71828182845904523536;
    v[V_RATIO] = ratio; v[V_AXIS] = axis; v[V_REF] = ref; v[V_REFW] = rw; v[V_REFH] = rh;
    v[V_BASE] = base; v[V_SCALED] = scaled; v[V_FACTOR] = factor;
}

/* ── model ──────────────────────────────────────────────────────────────────────────── */
enum { C_LINEAR, C_POWER, C_LOG, C_INTERP, C_CONST, C_BREAK, C_EXPR, C_CODE };
typedef struct adc__curve { int kind; double value; int npoints; double *at, *fac; adc_expr *expr; adc_factor_fn fn; void *user; } adc__curve;

enum { P_REF = 1, P_AXIS = 2, P_CURVE = 4, P_MUL = 8, P_MINF = 16, P_MAXF = 32, P_MINV = 64, P_MAXV = 128, P_AR = 256,
       P_SENS = 512, P_FONT = 1024, P_MW = 2048, P_XF = 4096 };
typedef struct adc__patch {
    unsigned has;
    double ref_w, ref_h; adc_axis axis; adc__curve *curve; double mul, min_f, max_f, min_v, max_v; int ar; double sens;
    int font; double font_max; int mw; int xf_kind; adc_expr *xf; adc_formula_fn xf_fn; void *xf_user;
} adc__patch;

typedef struct adc__cond {
    int has_devices; unsigned devices; int orientation; /* 0 any, 1 portrait, 2 landscape */
    double lo[4], hi[4]; int has_lo[4], has_hi[4]; int mw; /* -1 any */
} adc__cond;

typedef struct adc__rule { adc__cond when; adc__patch set; char *name; } adc__rule;

struct adc_strategy {
    char *id;
    adc__patch cfg;           /* every field present */
    adc__rule *rules; unsigned nrules;
    int value_dependent;
    /* objects owned (freed with the strategy) */
    adc__curve **curves; unsigned ncurves; adc_expr **exprs; unsigned nexprs;
    /* memo */
    const ad_metrics *memo_m; uint32_t memo_gen; double memo_w, memo_h; adc_resolved memo;
};

static void adc__own_curve(adc_strategy *s, adc__curve *c) {
    adc__curve **n = (adc__curve **)adc__alloc(sizeof(*n) * (s->ncurves + 1));
    if (s->curves) { memcpy(n, s->curves, sizeof(*n) * s->ncurves); ADC_FREE(s->curves); }
    n[s->ncurves++] = c; s->curves = n;
}
static void adc__own_expr(adc_strategy *s, adc_expr *e) {
    adc_expr **n = (adc_expr **)adc__alloc(sizeof(*n) * (s->nexprs + 1));
    if (s->exprs) { memcpy(n, s->exprs, sizeof(*n) * s->nexprs); ADC_FREE(s->exprs); }
    n[s->nexprs++] = e; s->exprs = n;
}
static adc__patch adc__default_cfg(void) {
    static adc__curve linear = {C_LINEAR, 0, 0, NULL, NULL, NULL, NULL, NULL};
    adc__patch p;
    memset(&p, 0, sizeof p);
    p.has = P_REF | P_AXIS | P_CURVE | P_MUL | P_AR | P_FONT | P_MW | P_XF;
    p.ref_w = 300; p.ref_h = 533; p.axis = ADC_AXIS_SW; p.curve = &linear; p.mul = 1; p.ar = 0; p.font = 0; p.mw = 0; p.xf_kind = 0;
    return p;
}
static void adc__apply(adc__patch *cfg, const adc__patch *p) {
    if (p->has & P_REF) { cfg->ref_w = p->ref_w; cfg->ref_h = p->ref_h; cfg->has |= P_REF; }
    if (p->has & P_AXIS) { cfg->axis = p->axis; cfg->has |= P_AXIS; }
    if (p->has & P_CURVE) { cfg->curve = p->curve; cfg->has |= P_CURVE; }
    if (p->has & P_MUL) { cfg->mul = p->mul; cfg->has |= P_MUL; }
    if (p->has & P_MINF) { cfg->min_f = p->min_f; cfg->has |= P_MINF; }
    if (p->has & P_MAXF) { cfg->max_f = p->max_f; cfg->has |= P_MAXF; }
    if (p->has & P_MINV) { cfg->min_v = p->min_v; cfg->has |= P_MINV; }
    if (p->has & P_MAXV) { cfg->max_v = p->max_v; cfg->has |= P_MAXV; }
    if (p->has & P_AR) { cfg->ar = p->ar; cfg->has |= P_AR; }
    if (p->has & P_SENS) { cfg->sens = p->sens; cfg->has |= P_SENS; }
    if (p->has & P_FONT) { cfg->font = p->font; cfg->font_max = p->font_max; cfg->has |= P_FONT; }
    if (p->has & P_MW) { cfg->mw = p->mw; cfg->has |= P_MW; }
    if (p->has & P_XF) { cfg->xf_kind = p->xf_kind; cfg->xf = p->xf; cfg->xf_fn = p->xf_fn; cfg->xf_user = p->xf_user; cfg->has |= P_XF; }
}
static int adc__patch_value_dependent(const adc__patch *p) {
    if (p->has & (P_MINV | P_MAXV)) return 1;
    if ((p->has & P_XF) && p->xf_kind == 2) return 1;
    if ((p->has & P_XF) && p->xf_kind == 1 && p->xf->coef == -2) return 1;
    return 0;
}
static int adc__validate(const adc__patch *c, char *err, size_t n) {
    if ((c->has & P_MINF) && (c->has & P_MAXF) && c->min_f > c->max_f) { adc__err(err, n, "minFactor must be <= maxFactor"); return 0; }
    if ((c->has & P_MINV) && (c->has & P_MAXV) && c->min_v > c->max_v) { adc__err(err, n, "minValue must be <= maxValue"); return 0; }
    if ((c->has & P_MINV) && c->min_v < 0) { adc__err(err, n, "minValue must be >= 0"); return 0; }
    if ((c->has & P_MAXV) && c->max_v < 0) { adc__err(err, n, "maxValue must be >= 0"); return 0; }
    return 1;
}
static int adc__cond_matches(const adc__cond *c, const adc_screen *s) {
    if (c->has_devices && !(c->devices & (1u << s->device))) return 0;
    if (c->orientation == 2 && !s->landscape) return 0;
    if (c->orientation == 1 && s->landscape) return 0;
    const double vals[4] = {s->smallest_width, s->width, s->height, adc__aspect(s)};
    for (int k = 0; k < 4; k++) {
        if (c->has_lo[k] && vals[k] < c->lo[k]) return 0;
        if (c->has_hi[k] && vals[k] >= c->hi[k]) return 0;
    }
    if (c->mw >= 0 && (s->multi_window ? 1 : 0) != c->mw) return 0;
    return 1;
}

/* ── engine ─────────────────────────────────────────────────────────────────────────── */
static double adc__curve_factor(const adc__curve *c, double ratio, double axis, double ref, double rw, double rh, const adc_screen *s) {
    switch (c->kind) {
    case C_LINEAR: return ratio;
    case C_POWER: return ratio > 0 ? pow(ratio, c->value) : 0.0;
    case C_LOG: return ratio > 0 ? 1.0 + c->value * log(ratio) : 1.0;
    case C_INTERP: return 1.0 + (ratio - 1.0) * c->value;
    case C_CONST: return c->value;
    case C_BREAK: {
        if (axis <= c->at[0]) return c->fac[0];
        for (int k = 1; k < c->npoints; k++) {
            if (axis <= c->at[k]) {
                const double span = c->at[k] - c->at[k - 1];
                return span <= 0 ? c->fac[k] : c->fac[k - 1] + (c->fac[k] - c->fac[k - 1]) * ((axis - c->at[k - 1]) / span);
            }
        }
        return c->fac[c->npoints - 1];
    }
    case C_CODE: return c->fn(s, c->user);
    default: {
        double v[V_COUNT];
        adc__vars(v, s, ratio, axis, ref, rw, rh, NAN, NAN, NAN);
        return adc__eval(c->expr, c->expr->root, v);
    }
    }
}

void adc_resolve(const adc_strategy *st, const adc_screen *s, adc_resolved *r) {
    adc__patch cfg = st->cfg;
    memset(r, 0, sizeof *r);
    r->strategy = st;
    r->screen = *s;
    r->rule_count = st->nrules;
    for (unsigned k = 0; k < st->nrules; k++) {
        if (adc__cond_matches(&st->rules[k].when, s)) {
            adc__apply(&cfg, &st->rules[k].set);
            if (k < 32) r->applied_rules |= 1u << k;
        }
    }
    r->axis_dp = adc__read(s, cfg.axis);
    r->reference_dp = adc__ref_read(cfg.ref_w, cfg.ref_h, cfg.axis);
    r->ref_w = cfg.ref_w; r->ref_h = cfg.ref_h;
    r->has_min_value = (cfg.has & P_MINV) != 0; r->min_value = cfg.min_v;
    r->has_max_value = (cfg.has & P_MAXV) != 0; r->max_value = cfg.max_v;
    r->font_policy = cfg.font; r->font_max = cfg.font_max;
    r->transform_kind = cfg.xf_kind; r->formula = cfg.xf; r->code_fn = cfg.xf_fn; r->code_user = cfg.xf_user;
    r->ar_multiplier = 1.0;
    if (cfg.mw && s->multi_window) {
        r->factor = 1.0; r->curve_factor = 1.0; r->multi_window_ignored = 1;
        return;
    }
    const double ratio = r->reference_dp > 0 ? r->axis_dp / r->reference_dp : 1.0;
    r->curve_factor = adc__curve_factor(cfg.curve, ratio, r->axis_dp, r->reference_dp, cfg.ref_w, cfg.ref_h, s);
    double f = r->curve_factor * cfg.mul;
    if (cfg.ar) {
        const double k = (cfg.has & P_SENS) ? cfg.sens : 0.08 / 30.0;
        r->ar_multiplier = 1.0 + k * log(adc__aspect(s) / 1.78);
        f *= r->ar_multiplier;
    }
    if ((cfg.has & P_MINF) && f < cfg.min_f) f = cfg.min_f;
    if ((cfg.has & P_MAXF) && f > cfg.max_f) f = cfg.max_f;
    r->factor = f;
    r->coefficient = NAN;
    if (cfg.xf_kind == 1 && cfg.xf->coef != -2) {
        if (cfg.xf->coef == -1) r->coefficient = 1.0;
        else {
            double v[V_COUNT];
            adc__vars(v, s, ratio, r->axis_dp, r->reference_dp, cfg.ref_w, cfg.ref_h, NAN, NAN, NAN);
            r->coefficient = adc__eval(cfg.xf, cfg.xf->coef, v);
        }
    }
}

double adc_scale(const adc_resolved *r, double base) {
    double out = base * r->factor;
    if ((r->has_min_value || r->has_max_value) && out != 0) {
        double mag = fabs(out);
        if (r->has_min_value && mag < r->min_value) mag = r->min_value;
        if (r->has_max_value && mag > r->max_value) mag = r->max_value;
        out = out < 0 ? -mag : mag;
    }
    if (r->multi_window_ignored || r->transform_kind == 0) return out;
    if (r->transform_kind == 2) return r->code_fn(base, &r->screen, r->code_user);
    const adc_expr *e = (const adc_expr *)r->formula;
    if (e->coef != -2) return base * r->coefficient;
    double v[V_COUNT];
    const double ratio = r->reference_dp > 0 ? r->axis_dp / r->reference_dp : 1.0;
    adc__vars(v, &r->screen, ratio, r->axis_dp, r->reference_dp, r->ref_w, r->ref_h, base, out, r->factor);
    return adc__eval(e, e->root, v);
}
double adc_effective_font_scale(const adc_resolved *r) {
    const double sys = r->screen.font_scale;
    if (r->font_policy == 1) return 1.0;
    if (r->font_policy == 2) return sys < r->font_max ? sys : r->font_max;
    return sys;
}
double adc_scale_sp_dp(const adc_resolved *r, double base) { return adc_scale(r, base) * adc_effective_font_scale(r); }
double adc_scale_sp_units(const adc_resolved *r, double base) {
    const double sys = r->screen.font_scale;
    return adc_scale(r, base) * (sys > 0 ? adc_effective_font_scale(r) / sys : 1.0);
}
const char *adc_rule_name(const adc_strategy *s, unsigned i) { return i < s->nrules ? s->rules[i].name : NULL; }
const char *adc_id(const adc_strategy *s) { return s ? s->id : NULL; }
int adc_value_dependent(const adc_strategy *s) { return s ? s->value_dependent : 0; }

static const adc_resolved *adc__memo(const adc_strategy *cs, const ad_metrics *m) {
    adc_strategy *s = (adc_strategy *)cs;
    if (s->memo_m != m || s->memo_gen != m->generation || s->memo_w != (double)m->in.width || s->memo_h != (double)m->in.height ||
        s->memo.screen.font_scale != (double)m->in.font_scale || s->memo.screen.density != (double)m->in.density) {
        adc_screen sc = adc_screen_from_metrics(m);
        adc_resolve(s, &sc, &s->memo);
        s->memo_m = m; s->memo_gen = m->generation; s->memo_w = m->in.width; s->memo_h = m->in.height;
    }
    return &s->memo;
}
float adc_factor(const adc_strategy *s, const ad_metrics *m) { return (float)adc__memo(s, m)->factor; }
float adc_dp(const adc_strategy *s, const ad_metrics *m, float v) { return (float)adc_scale(adc__memo(s, m), v); }
float adc_px(const adc_strategy *s, const ad_metrics *m, float v) { return (float)(adc_scale(adc__memo(s, m), v) * m->in.density); }
float adc_sp(const adc_strategy *s, const ad_metrics *m, float v) { return (float)adc_scale_sp_dp(adc__memo(s, m), v); }
float adc_sp_px(const adc_strategy *s, const ad_metrics *m, float v) { return (float)(adc_scale_sp_dp(adc__memo(s, m), v) * m->in.density); }

const char *adc_explain(const adc_strategy *s, const adc_screen *screen, char *buf, size_t len) {
    adc_resolved r;
    adc_resolve(s, screen, &r);
    int w = snprintf(buf, len, "[%s] %s %s=%.4gdp / ref %.4g => factor %.4g", s->id, adc__device_names[screen->device],
                     adc__axis_names[s->cfg.axis], r.axis_dp, r.reference_dp, r.factor);
    for (unsigned k = 0; k < s->nrules && k < 32 && w > 0 && (size_t)w < len; k++)
        if (r.applied_rules & (1u << k)) w += snprintf(buf + w, len - (size_t)w, " rule=%s", s->rules[k].name);
    return buf;
}

/* ── JSON ───────────────────────────────────────────────────────────────────────────── */
enum { J_NULL, J_BOOL, J_NUM, J_STR, J_ARR, J_OBJ };
typedef struct adc__json { int type; double num; int b; char *str; struct adc__json *items; char **keys; int count; } adc__json;
typedef struct adc__jp { const char *s; size_t i; char *err; size_t err_len; int failed; } adc__jp;

static void adc__jfree(adc__json *v) {
    if (!v) return;
    if (v->str) ADC_FREE(v->str);
    for (int k = 0; k < v->count; k++) { adc__jfree(&v->items[k]); if (v->keys) ADC_FREE(v->keys[k]); }
    if (v->items) ADC_FREE(v->items);
    if (v->keys) ADC_FREE(v->keys);
}
static void adc__jfail(adc__jp *p, const char *m) {
    if (!p->failed) { char b[160]; snprintf(b, sizeof b, "Invalid JSON at %u: %s", (unsigned)p->i, m); adc__err(p->err, p->err_len, b); p->failed = 1; }
}
static void adc__jws(adc__jp *p) { while (p->s[p->i] == ' ' || p->s[p->i] == '\n' || p->s[p->i] == '\r' || p->s[p->i] == '\t') p->i++; }
static int adc__jvalue(adc__jp *p, adc__json *out);
static char *adc__jstr(adc__jp *p) {
    size_t cap = 32, n = 0;
    char *b = (char *)adc__alloc(cap);
    p->i++;
    for (;;) {
        char c = p->s[p->i++];
        if (!c) { adc__jfail(p, "Unterminated string"); ADC_FREE(b); return NULL; }
        if (c == '"') break;
        if (c == '\\') {
            char e = p->s[p->i++];
            unsigned cp = 0;
            switch (e) {
            case '"': case '\\': case '/': c = e; break;
            case 'b': c = '\b'; break;
            case 'f': c = '\f'; break;
            case 'n': c = '\n'; break;
            case 'r': c = '\r'; break;
            case 't': c = '\t'; break;
            case 'u':
                for (int k = 0; k < 4; k++) {
                    char h = p->s[p->i++];
                    cp <<= 4;
                    if (h >= '0' && h <= '9') cp |= (unsigned)(h - '0');
                    else if (h >= 'a' && h <= 'f') cp |= (unsigned)(h - 'a' + 10);
                    else if (h >= 'A' && h <= 'F') cp |= (unsigned)(h - 'A' + 10);
                    else { adc__jfail(p, "Bad unicode escape"); ADC_FREE(b); return NULL; }
                }
                if (n + 4 >= cap) { char *nb = (char *)adc__alloc(cap * 2); memcpy(nb, b, n); ADC_FREE(b); b = nb; cap *= 2; }
                if (cp < 0x80) b[n++] = (char)cp;
                else if (cp < 0x800) { b[n++] = (char)(0xC0 | (cp >> 6)); b[n++] = (char)(0x80 | (cp & 0x3F)); }
                else { b[n++] = (char)(0xE0 | (cp >> 12)); b[n++] = (char)(0x80 | ((cp >> 6) & 0x3F)); b[n++] = (char)(0x80 | (cp & 0x3F)); }
                continue;
            default: adc__jfail(p, "Bad escape"); ADC_FREE(b); return NULL;
            }
        }
        if (n + 2 >= cap) { char *nb = (char *)adc__alloc(cap * 2); memcpy(nb, b, n); ADC_FREE(b); b = nb; cap *= 2; }
        b[n++] = c;
    }
    b[n] = 0;
    return b;
}
static int adc__jpush(adc__json *v) {
    if ((v->count & (v->count - 1)) == 0) {
        int cap = v->count ? v->count * 2 : 4;
        adc__json *ni = (adc__json *)adc__alloc(sizeof(adc__json) * (size_t)cap);
        char **nk = (char **)adc__alloc(sizeof(char *) * (size_t)cap);
        if (v->count) { memcpy(ni, v->items, sizeof(adc__json) * (size_t)v->count); memcpy(nk, v->keys, sizeof(char *) * (size_t)v->count); }
        if (v->items) ADC_FREE(v->items);
        if (v->keys) ADC_FREE(v->keys);
        v->items = ni; v->keys = nk;
    }
    return v->count++;
}
static int adc__jvalue(adc__jp *p, adc__json *out) {
    memset(out, 0, sizeof *out);
    adc__jws(p);
    char c = p->s[p->i];
    if (c == '{' || c == '[') {
        const char close = c == '{' ? '}' : ']';
        out->type = c == '{' ? J_OBJ : J_ARR;
        p->i++;
        adc__jws(p);
        if (p->s[p->i] == close) { p->i++; return 1; }
        for (;;) {
            adc__jws(p);
            char *key = NULL;
            if (out->type == J_OBJ) {
                if (p->s[p->i] != '"') { adc__jfail(p, "Expected a string key"); return 0; }
                key = adc__jstr(p);
                if (!key) return 0;
                adc__jws(p);
                if (p->s[p->i] != ':') { ADC_FREE(key); adc__jfail(p, "Expected ':'"); return 0; }
                p->i++;
            }
            int k = adc__jpush(out);
            out->keys[k] = key;
            if (!adc__jvalue(p, &out->items[k])) return 0;
            adc__jws(p);
            if (p->s[p->i] == ',') { p->i++; continue; }
            if (p->s[p->i] == close) { p->i++; return 1; }
            adc__jfail(p, out->type == J_OBJ ? "Expected ',' or '}'" : "Expected ',' or ']'");
            return 0;
        }
    }
    if (c == '"') { out->type = J_STR; out->str = adc__jstr(p); return out->str != NULL; }
    if (!strncmp(p->s + p->i, "true", 4)) { out->type = J_BOOL; out->b = 1; p->i += 4; return 1; }
    if (!strncmp(p->s + p->i, "false", 5)) { out->type = J_BOOL; out->b = 0; p->i += 5; return 1; }
    if (!strncmp(p->s + p->i, "null", 4)) { out->type = J_NULL; p->i += 4; return 1; }
    if (c == '-' || adc__isdigit(c)) {
        char *end = NULL;
        out->type = J_NUM;
        out->num = strtod(p->s + p->i, &end);
        if (end == p->s + p->i) { adc__jfail(p, "Bad number"); return 0; }
        p->i = (size_t)(end - p->s);
        return 1;
    }
    adc__jfail(p, c ? "Unexpected character" : "Unexpected end of JSON");
    return 0;
}
static const adc__json *adc__jget(const adc__json *o, const char *key) {
    if (!o || o->type != J_OBJ) return NULL;
    for (int k = 0; k < o->count; k++) if (!strcmp(o->keys[k], key)) return &o->items[k];
    return NULL;
}

/* ── reading strategies ─────────────────────────────────────────────────────────────── */
typedef struct adc__rd { char *err; size_t err_len; int failed; adc_strategy *owner; } adc__rd;
static int adc__rfail(adc__rd *r, const char *path, const char *msg) {
    if (!r->failed) { char b[320]; snprintf(b, sizeof b, "%.200s%s%.100s", path, *path ? " " : "", msg); adc__err(r->err, r->err_len, b); r->failed = 1; }
    return 0;
}
/* returns 1 if present (writes *out), 0 if absent; sets failed on type error */
static int adc__rnum(adc__rd *r, const adc__json *v, const char *path, double *out) {
    if (!v || v->type == J_NULL) return 0;
    if (v->type != J_NUM) { adc__rfail(r, path, "must be a number"); return 0; }
    if (!isfinite(v->num)) { adc__rfail(r, path, "must be finite"); return 0; }
    *out = (double)(float)v->num; /* Float precision like the reference */
    return 1;
}
static int adc__rbool(adc__rd *r, const adc__json *v, const char *path, int *out) {
    if (!v || v->type == J_NULL) return 0;
    if (v->type != J_BOOL) { adc__rfail(r, path, "must be a boolean"); return 0; }
    *out = v->b;
    return 1;
}
static adc__curve *adc__rcurve(adc__rd *r, const adc__json *v, const char *path) {
    char p2[256];
    const char *type = NULL;
    const adc__json *obj = NULL;
    if (v->type == J_STR) type = v->str;
    else if (v->type == J_OBJ) { obj = v; const adc__json *t = adc__jget(v, "type"); if (t && t->type == J_STR) type = t->str; }
    else { adc__rfail(r, path, "must be an object or string"); return NULL; }
    if (!type) { snprintf(p2, sizeof p2, "%.200s.type", path); adc__rfail(r, p2, "is required"); return NULL; }
    adc__curve *c = (adc__curve *)adc__alloc(sizeof *c);
    adc__own_curve(r->owner, c);
    double d = 0;
#define ADC_PARAM(key, def) (snprintf(p2, sizeof p2, "%.200s." key, path), adc__rnum(r, adc__jget(obj, key), p2, &d) ? d : (def))
    if (!strcmp(type, "linear")) c->kind = C_LINEAR;
    else if (!strcmp(type, "power")) { c->kind = C_POWER; c->value = ADC_PARAM("exponent", (double)0.75f); }
    else if (!strcmp(type, "logarithmic")) { c->kind = C_LOG; c->value = ADC_PARAM("k", (double)0.4f); }
    else if (!strcmp(type, "interpolated")) { c->kind = C_INTERP; c->value = ADC_PARAM("t", 0.5); }
    else if (!strcmp(type, "constant")) { c->kind = C_CONST; c->value = ADC_PARAM("factor", 1.0); }
    else if (!strcmp(type, "expression")) {
        const adc__json *e = adc__jget(obj, "expression");
        snprintf(p2, sizeof p2, "%.200s.expression", path);
        if (!e || e->type != J_STR) { adc__rfail(r, p2, "is required"); return NULL; }
        char err[300];
        c->kind = C_EXPR;
        c->expr = adc__expr_parse(e->str, 1, err, sizeof err);
        if (!c->expr) { char m[340]; snprintf(m, sizeof m, ": %s", err); adc__rfail(r, p2, m); return NULL; }
        adc__own_expr(r->owner, c->expr);
    } else if (!strcmp(type, "breakpoints")) {
        const adc__json *pts = adc__jget(obj, "points");
        snprintf(p2, sizeof p2, "%.200s.points", path);
        if (!pts || pts->type != J_ARR) { adc__rfail(r, p2, "must be an array"); return NULL; }
        if (pts->count == 0) { adc__rfail(r, p2, "breakpoints need at least one point"); return NULL; }
        c->kind = C_BREAK; c->npoints = pts->count;
        c->at = (double *)adc__alloc(sizeof(double) * (size_t)pts->count);
        c->fac = (double *)adc__alloc(sizeof(double) * (size_t)pts->count);
        for (int k = 0; k < pts->count; k++) {
            char pp[280];
            snprintf(pp, sizeof pp, "%.200s.points[%d]", path, k);
            if (pts->items[k].type != J_OBJ) { adc__rfail(r, pp, "must be an object"); return NULL; }
            char q[300];
            snprintf(q, sizeof q, "%.200s.at", pp);
            if (!adc__rnum(r, adc__jget(&pts->items[k], "at"), q, &c->at[k])) { if (!r->failed) adc__rfail(r, q, "is required"); return NULL; }
            snprintf(q, sizeof q, "%.200s.factor", pp);
            if (!adc__rnum(r, adc__jget(&pts->items[k], "factor"), q, &c->fac[k])) { if (!r->failed) adc__rfail(r, q, "is required"); return NULL; }
            if (c->at[k] < 0) { adc__rfail(r, pp, "breakpoint 'at' must be finite and >= 0"); return NULL; }
        }
        for (int a = 1; a < c->npoints; a++) /* stable insertion sort by `at` */
            for (int b = a; b > 0 && c->at[b - 1] > c->at[b]; b--) {
                double t = c->at[b]; c->at[b] = c->at[b - 1]; c->at[b - 1] = t;
                t = c->fac[b]; c->fac[b] = c->fac[b - 1]; c->fac[b - 1] = t;
            }
    } else {
        char m[200];
        snprintf(m, sizeof m, "type '%s' is not one of linear, power, logarithmic, interpolated, constant, breakpoints, expression", type);
        adc__rfail(r, path, m);
        return NULL;
    }
#undef ADC_PARAM
    return r->failed ? NULL : c;
}
static int adc__axis_from(const char *s, adc_axis *out) {
    for (int k = 0; k < 7; k++) if (adc__ieq(s, adc__axis_names[k])) { *out = (adc_axis)k; return 1; }
    if (adc__ieq(s, "smallest_width")) { *out = ADC_AXIS_SW; return 1; }
    return 0;
}
static int adc__device_from(const char *s, adc_device *out) {
    for (int k = 0; k < ADC_DEVICE_COUNT; k++) if (adc__ieq(s, adc__device_names[k])) { *out = (adc_device)k; return 1; }
    return 0;
}
static void adc__rpatch(adc__rd *r, const adc__json *o, const char *path, adc__patch *p) {
    char q[300];
    double d = 0;
    int b;
    memset(p, 0, sizeof *p);
    const adc__json *ref = adc__jget(o, "reference");
    if (ref && ref->type == J_OBJ) {
        p->has |= P_REF; p->ref_w = 300; p->ref_h = 533;
        snprintf(q, sizeof q, "%.200s.reference.width", path); if (adc__rnum(r, adc__jget(ref, "width"), q, &d)) p->ref_w = d;
        snprintf(q, sizeof q, "%.200s.reference.height", path); if (adc__rnum(r, adc__jget(ref, "height"), q, &d)) p->ref_h = d;
        if (!(p->ref_w > 0) || !(p->ref_h > 0)) adc__rfail(r, path, "reference width/height must be > 0");
    }
    const adc__json *ax = adc__jget(o, "axis");
    if (ax && ax->type == J_STR) {
        if (!adc__axis_from(ax->str, &p->axis)) { char m[160]; snprintf(m, sizeof m, "Unknown axis '%s'", ax->str); adc__rfail(r, path, m); }
        p->has |= P_AXIS;
    }
    const adc__json *cv = adc__jget(o, "curve");
    if (cv && cv->type != J_NULL) { snprintf(q, sizeof q, "%.200s.curve", path); p->curve = adc__rcurve(r, cv, q); p->has |= P_CURVE; }
    snprintf(q, sizeof q, "%.200s.multiplier", path); if (adc__rnum(r, adc__jget(o, "multiplier"), q, &d)) { p->mul = d; p->has |= P_MUL; }
    const adc__json *cl = adc__jget(o, "clamp"), *vc = adc__jget(o, "valueClamp");
    if (cl && cl->type != J_NULL && cl->type != J_OBJ) { snprintf(q, sizeof q, "%.200s.clamp", path); adc__rfail(r, q, "must be an object"); }
    if (vc && vc->type != J_NULL && vc->type != J_OBJ) { snprintf(q, sizeof q, "%.200s.valueClamp", path); adc__rfail(r, q, "must be an object"); }
    snprintf(q, sizeof q, "%.200s.clamp.min", path); if (cl && adc__rnum(r, adc__jget(cl, "min"), q, &d)) { p->min_f = d; p->has |= P_MINF; }
    snprintf(q, sizeof q, "%.200s.clamp.max", path); if (cl && adc__rnum(r, adc__jget(cl, "max"), q, &d)) { p->max_f = d; p->has |= P_MAXF; }
    snprintf(q, sizeof q, "%.200s.valueClamp.min", path); if (vc && adc__rnum(r, adc__jget(vc, "min"), q, &d)) { p->min_v = d; p->has |= P_MINV; }
    snprintf(q, sizeof q, "%.200s.valueClamp.max", path); if (vc && adc__rnum(r, adc__jget(vc, "max"), q, &d)) { p->max_v = d; p->has |= P_MAXV; }
    const adc__json *ar = adc__jget(o, "aspectRatio");
    if (ar && ar->type == J_BOOL) { p->ar = ar->b; p->has |= P_AR; }
    else if (ar && ar->type == J_OBJ) {
        p->ar = 1; p->has |= P_AR;
        snprintf(q, sizeof q, "%.200s.aspectRatio.enabled", path); if (adc__rbool(r, adc__jget(ar, "enabled"), q, &b)) p->ar = b;
        snprintf(q, sizeof q, "%.200s.aspectRatio.sensitivity", path); if (adc__rnum(r, adc__jget(ar, "sensitivity"), q, &d)) { p->sens = d; p->has |= P_SENS; }
    } else if (ar && ar->type != J_NULL) { snprintf(q, sizeof q, "%.200s.aspectRatio", path); adc__rfail(r, q, "must be a boolean or object"); }
    if (!(p->has & P_SENS)) { snprintf(q, sizeof q, "%.200s.sensitivity", path); if (adc__rnum(r, adc__jget(o, "sensitivity"), q, &d)) { p->sens = d; p->has |= P_SENS; } }
    const adc__json *fs = adc__jget(o, "fontScale");
    if (fs && fs->type != J_NULL) {
        snprintf(q, sizeof q, "%.200s.fontScale", path);
        p->has |= P_FONT;
        if (fs->type == J_STR && !strcmp(fs->str, "follow")) p->font = 0;
        else if (fs->type == J_STR && !strcmp(fs->str, "ignore")) p->font = 1;
        else if (fs->type == J_OBJ) {
            char q2[320];
            snprintf(q2, sizeof q2, "%.200s.max", q);
            if (!adc__rnum(r, adc__jget(fs, "max"), q2, &d)) { if (!r->failed) adc__rfail(r, q2, "is required"); }
            else if (d <= 0) adc__rfail(r, q, "max must be > 0");
            p->font = 2; p->font_max = d;
        } else adc__rfail(r, q, "must be \"follow\", \"ignore\" or {\"max\": n}");
    }
    snprintf(q, sizeof q, "%.200s.ignoreMultiWindow", path); if (adc__rbool(r, adc__jget(o, "ignoreMultiWindow"), q, &b)) { p->mw = b; p->has |= P_MW; }
    const adc__json *f = adc__jget(o, "formula");
    if (f && f->type != J_NULL) {
        snprintf(q, sizeof q, "%.200s.formula", path);
        if (f->type != J_STR) { adc__rfail(r, q, "must be a string"); return; }
        char err[300];
        p->xf = adc__expr_parse(f->str, 2, err, sizeof err);
        if (!p->xf) { char m[340]; snprintf(m, sizeof m, ": %s", err); adc__rfail(r, q, m); return; }
        adc__own_expr(r->owner, p->xf);
        p->xf_kind = 1; p->has |= P_XF;
    }
}
static void adc__rcond(adc__rd *r, const adc__json *o, const char *path, adc__cond *c) {
    char q[300];
    double d = 0;
    int b;
    memset(c, 0, sizeof *c);
    c->mw = -1;
    const adc__json *dv = adc__jget(o, "device");
    if (dv && dv->type != J_NULL) {
        c->has_devices = 1;
        adc_device dd;
        if (dv->type == J_STR) { if (adc__device_from(dv->str, &dd)) c->devices |= 1u << dd; else adc__rfail(r, path, "unknown device"); }
        else if (dv->type == J_ARR) {
            for (int k = 0; k < dv->count; k++) {
                if (dv->items[k].type != J_STR) { adc__rfail(r, path, ".device entries must be strings"); break; }
                if (adc__device_from(dv->items[k].str, &dd)) c->devices |= 1u << dd; else adc__rfail(r, path, "unknown device");
            }
        } else adc__rfail(r, path, ".device must be a string or array");
    }
    const adc__json *ori = adc__jget(o, "orientation");
    if (ori && ori->type == J_STR) {
        if (!strcmp(ori->str, "portrait")) c->orientation = 1;
        else if (!strcmp(ori->str, "landscape")) c->orientation = 2;
        else adc__rfail(r, path, ".orientation must be portrait or landscape");
    }
    static const char *const keys[4] = {"smallestWidth", "width", "height", "aspectRatio"};
    for (int k = 0; k < 4; k++) {
        const adc__json *rg = adc__jget(o, keys[k]);
        if (!rg || rg->type == J_NULL) continue;
        snprintf(q, sizeof q, "%.200s.%.40s", path, keys[k]);
        if (rg->type != J_OBJ) { adc__rfail(r, q, "must be an object with min/max"); continue; }
        char q2[320];
        snprintf(q2, sizeof q2, "%.200s.min", q); if (adc__rnum(r, adc__jget(rg, "min"), q2, &d)) { c->lo[k] = d; c->has_lo[k] = 1; }
        snprintf(q2, sizeof q2, "%.200s.max", q); if (adc__rnum(r, adc__jget(rg, "max"), q2, &d)) { c->hi[k] = d; c->has_hi[k] = 1; }
    }
    snprintf(q, sizeof q, "%.200s.multiWindow", path);
    if (adc__rbool(r, adc__jget(o, "multiWindow"), q, &b)) c->mw = b;
}

/* ── registry ───────────────────────────────────────────────────────────────────────── */
static adc_strategy **adc__reg; static unsigned adc__nreg;
static adc_strategy **adc__formulas; static unsigned adc__nformulas;
static adc_strategy **adc__builtins; static unsigned adc__nbuiltins; /* built-ins then presets */
static const adc_strategy *adc__default_strategy;
static int adc__lookup_builtins_only;

void adc_free(adc_strategy *s) {
    if (!s) return;
    for (unsigned k = 0; k < s->ncurves; k++) { ADC_FREE(s->curves[k]->at); ADC_FREE(s->curves[k]->fac); ADC_FREE(s->curves[k]); }
    for (unsigned k = 0; k < s->nexprs; k++) adc__expr_free(s->exprs[k]);
    for (unsigned k = 0; k < s->nrules; k++) ADC_FREE(s->rules[k].name);
    ADC_FREE(s->curves); ADC_FREE(s->exprs); ADC_FREE(s->rules); ADC_FREE(s->id); ADC_FREE(s);
}
static adc_strategy *adc__new(const char *id) {
    adc_strategy *s = (adc_strategy *)adc__alloc(sizeof *s);
    s->id = adc__strdup(id);
    s->cfg = adc__default_cfg();
    return s;
}
static void adc__finish(adc_strategy *s) {
    s->value_dependent = adc__patch_value_dependent(&s->cfg);
    for (unsigned k = 0; k < s->nrules; k++) s->value_dependent |= adc__patch_value_dependent(&s->rules[k].set);
}
/* deep-copies the configuration of `parent` into `s` (curves/exprs are shared by pointer, so the
 * child keeps the parent alive: parents live in the registry / built-ins, which outlive children). */
static void adc__inherit(adc_strategy *s, const adc_strategy *parent) {
    s->cfg = parent->cfg;
    if (parent->nrules) {
        s->rules = (adc__rule *)adc__alloc(sizeof(adc__rule) * parent->nrules);
        for (unsigned k = 0; k < parent->nrules; k++) {
            s->rules[k] = parent->rules[k];
            s->rules[k].name = adc__strdup(parent->rules[k].name);
        }
        s->nrules = parent->nrules;
    }
}
static const adc_strategy *adc__find(adc_strategy **list, unsigned n, const char *id, int ci) {
    for (unsigned k = 0; k < n; k++) if (ci ? adc__ieq(list[k]->id, id) : !strcmp(list[k]->id, id)) return list[k];
    return NULL;
}
static void adc__ensure_builtins(void);
const adc_strategy *adc_get(const char *id) {
    if (!id) return NULL;
    adc__ensure_builtins();
    const adc_strategy *s = adc__lookup_builtins_only ? NULL : adc__find(adc__reg, adc__nreg, id, 0);
    return s ? s : adc__find(adc__builtins, adc__nbuiltins, id, 1);
}
static void adc__list_add(adc_strategy ***list, unsigned *n, adc_strategy *s) {
    for (unsigned k = 0; k < *n; k++) {
        if (!strcmp((*list)[k]->id, s->id)) { (*list)[k] = s; return; } /* replaced: old one stays alive (children may point to it) */
    }
    adc_strategy **nl = (adc_strategy **)adc__alloc(sizeof(*nl) * (*n + 1));
    if (*list) { memcpy(nl, *list, sizeof(*nl) * *n); ADC_FREE(*list); }
    nl[(*n)++] = s; *list = nl;
}
int adc_register(adc_strategy *s) { if (!s) return 0; adc__list_add(&adc__reg, &adc__nreg, s); return 1; }
void adc_unregister(const char *id) {
    for (unsigned k = 0; k < adc__nreg; k++)
        if (!strcmp(adc__reg[k]->id, id)) { if (adc__default_strategy == adc__reg[k]) adc__default_strategy = NULL; adc__reg[k] = adc__reg[--adc__nreg]; return; }
}
const adc_strategy *adc_default(void) { adc__ensure_builtins(); return adc__default_strategy ? adc__default_strategy : adc__builtins[0]; }
void adc_set_default(const adc_strategy *s) { adc__default_strategy = s; }

/* Parses a document; on success appends the strategies to *out (caller registers). */
static int adc__decode(const char *json, adc_strategy ***out, unsigned *nout, char **default_id, char *err, size_t err_len) {
    adc__jp jp;
    jp.s = json; jp.i = 0; jp.err = err; jp.err_len = err_len; jp.failed = 0;
    adc__json root;
    if (!adc__jvalue(&jp, &root)) { adc__jfree(&root); return 0; }
    adc__jws(&jp);
    if (json[jp.i]) { adc__jfail(&jp, "Unexpected trailing content"); adc__jfree(&root); return 0; }
    const adc__json *items = NULL;
    adc__json single;
    int single_used = 0;
    if (default_id) *default_id = NULL;
    if (root.type == J_OBJ && adc__jget(&root, "strategies")) {
        const adc__json *ver = adc__jget(&root, "version");
        if (ver && ver->type == J_NUM && ver->num > ADC_FORMAT_VERSION) { adc__err(err, err_len, "Unsupported strategies format version"); adc__jfree(&root); return 0; }
        items = adc__jget(&root, "strategies");
        if (items->type != J_ARR) { adc__err(err, err_len, "strategies must be an array"); adc__jfree(&root); return 0; }
        const adc__json *d = adc__jget(&root, "default");
        if (default_id && d && d->type == J_STR) *default_id = adc__strdup(d->str);
    } else if (root.type == J_OBJ) {
        memset(&single, 0, sizeof single);
        single.type = J_ARR; single.count = 1; single.items = &root;
        items = &single; single_used = 1;
    } else if (root.type == J_ARR) items = &root;
    else { adc__err(err, err_len, "Expected a JSON object or array"); adc__jfree(&root); return 0; }
    const int n = items->count;
    adc_strategy **built = (adc_strategy **)adc__alloc(sizeof(adc_strategy *) * (size_t)(n ? n : 1));
    int *state = (int *)adc__alloc(sizeof(int) * (size_t)(n ? n : 1)); /* 0 todo, 1 building, 2 done */
    int ok = 1;
    for (int k = 0; k < n && ok; k++) {
        const adc__json *it = &items->items[k];
        const adc__json *id = adc__jget(it, "id");
        char q[64];
        snprintf(q, sizeof q, "strategies[%d]", k);
        if (it->type != J_OBJ) { char m[96]; snprintf(m, sizeof m, "%.200s must be an object", q); adc__err(err, err_len, m); ok = 0; break; }
        if (!id || id->type != J_STR) { char m[96]; snprintf(m, sizeof m, "%.200s.id is required", q); adc__err(err, err_len, m); ok = 0; break; }
        for (int j = 0; j < k; j++) {
            if (!strcmp(adc__jget(&items->items[j], "id")->str, id->str)) { char m[160]; snprintf(m, sizeof m, "Duplicate strategy id '%s'", id->str); adc__err(err, err_len, m); ok = 0; }
        }
    }
    /* iterative build with dependency resolution (extends) */
    for (int pass = 0; ok && pass <= n; pass++) {
        int progressed = 0, remaining = 0;
        for (int k = 0; k < n && ok; k++) {
            if (state[k] == 2) continue;
            const adc__json *it = &items->items[k];
            const char *sid = adc__jget(it, "id")->str;
            const adc__json *ext = adc__jget(it, "extends");
            const adc_strategy *parent = NULL;
            if (ext && ext->type == J_STR) {
                int local = -1;
                for (int j = 0; j < n; j++) if (!strcmp(adc__jget(&items->items[j], "id")->str, ext->str)) local = j;
                if (local >= 0) {
                    if (state[local] != 2) { remaining++; continue; }
                    parent = built[local];
                } else {
                    parent = adc_get(ext->str);
                    if (!parent) { char m[220]; snprintf(m, sizeof m, "'%s' extends unknown strategy '%s'", sid, ext->str); adc__err(err, err_len, m); ok = 0; break; }
                }
            }
            adc_strategy *s = adc__new(sid);
            if (parent) adc__inherit(s, parent);
            adc__rd rd;
            rd.err = err; rd.err_len = err_len; rd.failed = 0; rd.owner = s;
            adc__patch p;
            adc__rpatch(&rd, it, sid, &p);
            adc__apply(&s->cfg, &p);
            const adc__json *cl = adc__jget(it, "clamp"), *vc = adc__jget(it, "valueClamp"), *fm = adc__jget(it, "formula");
            if (cl && cl->type == J_NULL) s->cfg.has &= ~(unsigned)(P_MINF | P_MAXF);
            if (vc && vc->type == J_NULL) s->cfg.has &= ~(unsigned)(P_MINV | P_MAXV);
            if (fm && fm->type == J_NULL) { s->cfg.xf_kind = 0; s->cfg.xf = NULL; }
            const adc__json *rules = adc__jget(it, "rules");
            if (rules && rules->type != J_NULL) {
                char rp[300];
                snprintf(rp, sizeof rp, "%.200s.rules", sid);
                if (rules->type != J_ARR) adc__rfail(&rd, rp, "must be an array");
                else {
                    adc__rule *nr = (adc__rule *)adc__alloc(sizeof(adc__rule) * (s->nrules + (unsigned)rules->count + 1));
                    if (s->rules) { memcpy(nr, s->rules, sizeof(adc__rule) * s->nrules); ADC_FREE(s->rules); }
                    s->rules = nr;
                    for (int j = 0; j < rules->count && !rd.failed; j++) {
                        const adc__json *ru = &rules->items[j];
                        snprintf(rp, sizeof rp, "%.200s.rules[%d]", sid, j);
                        if (ru->type != J_OBJ) { adc__rfail(&rd, rp, "must be an object"); break; }
                        adc__rule *R = &s->rules[s->nrules];
                        memset(R, 0, sizeof *R);
                        char wp[320];
                        snprintf(wp, sizeof wp, "%.200s.when", rp);
                        const adc__json *w = adc__jget(ru, "when");
                        if (w && w->type != J_NULL && w->type != J_OBJ) adc__rfail(&rd, wp, "must be an object");
                        adc__rcond(&rd, (w && w->type == J_OBJ) ? w : NULL, wp, &R->when);
                        const adc__json *st = adc__jget(ru, "set");
                        snprintf(wp, sizeof wp, "%.200s.set", rp);
                        if (!st || st->type != J_OBJ) { adc__rfail(&rd, wp, "is required"); break; }
                        adc__rpatch(&rd, st, wp, &R->set);
                        const adc__json *nm = adc__jget(ru, "name");
                        char def[24];
                        snprintf(def, sizeof def, "#%u", s->nrules);
                        R->name = adc__strdup(nm && nm->type == J_STR ? nm->str : def);
                        s->nrules++;
                    }
                }
            }
            if (!rd.failed) {
                if (!adc__validate(&s->cfg, err, err_len)) rd.failed = 1;
                for (unsigned j = 0; j < s->nrules && !rd.failed; j++) {
                    adc__patch t = s->cfg;
                    adc__apply(&t, &s->rules[j].set);
                    if (!adc__validate(&t, err, err_len)) rd.failed = 1;
                }
            }
            if (rd.failed) { adc_free(s); ok = 0; break; }
            adc__finish(s);
            built[k] = s; state[k] = 2; progressed = 1;
        }
        if (!ok || !remaining) break;
        if (!progressed) { adc__err(err, err_len, "Cyclic 'extends'"); ok = 0; }
    }
    if (ok) {
        for (int k = 0; k < n; k++) adc__list_add(out, nout, built[k]);
    } else {
        for (int k = 0; k < n; k++) if (state[k] == 2) adc_free(built[k]);
        if (default_id && *default_id) { ADC_FREE(*default_id); *default_id = NULL; }
    }
    ADC_FREE(built); ADC_FREE(state);
    if (single_used) single.items = NULL;
    adc__jfree(&root);
    return ok;
}

static const char *const adc__builtin_json =
    "{\"strategies\":["
    "{\"id\":\"scaled\"},"
    "{\"id\":\"power\",\"curve\":{\"type\":\"power\",\"exponent\":0.75}},"
    "{\"id\":\"logarithmic\",\"curve\":{\"type\":\"logarithmic\",\"k\":0.4}},"
    "{\"id\":\"interpolated\",\"curve\":{\"type\":\"interpolated\",\"t\":0.5}},"
    "{\"id\":\"diagonal\",\"axis\":\"diagonal\"},"
    "{\"id\":\"perimeter\",\"axis\":\"perimeter\"},"
    "{\"id\":\"fixed\",\"curve\":{\"type\":\"constant\",\"factor\":1}},"
    "{\"id\":\"tabletFriendly\",\"clamp\":{\"max\":1.8},\"rules\":[{\"name\":\"large\",\"when\":{\"device\":[\"tablet\",\"desktop\"]},\"set\":{\"curve\":{\"type\":\"power\",\"exponent\":0.5}}}]},"
    "{\"id\":\"compactFirst\",\"reference\":{\"width\":360,\"height\":640},\"curve\":{\"type\":\"interpolated\",\"t\":0.5},\"clamp\":{\"min\":0.9,\"max\":1.4}},"
    "{\"id\":\"tvSafe\",\"rules\":[{\"name\":\"tv\",\"when\":{\"device\":\"tv\"},\"set\":{\"axis\":\"height\",\"reference\":{\"width\":960,\"height\":540},\"clamp\":{\"min\":1,\"max\":2}}}]},"
    "{\"id\":\"wearable\",\"rules\":[{\"name\":\"watch\",\"when\":{\"device\":\"watch\"},\"set\":{\"axis\":\"min\",\"reference\":{\"width\":192,\"height\":192},\"clamp\":{\"min\":0.85,\"max\":1.15}}}]},"
    "{\"id\":\"gameHud\",\"axis\":\"min\",\"curve\":{\"type\":\"power\",\"exponent\":0.85},\"aspectRatio\":true,\"clamp\":{\"min\":0.75,\"max\":2.5},\"fontScale\":\"ignore\"},"
    "{\"id\":\"readableText\",\"curve\":{\"type\":\"logarithmic\",\"k\":0.4},\"fontScale\":{\"max\":1.3}}"
    "]}";

static void adc__ensure_builtins(void) {
    if (adc__builtins) return;
    char err[128];
    adc__builtins = (adc_strategy **)adc__alloc(sizeof(adc_strategy *)); /* non-NULL sentinel during decode */
    adc__lookup_builtins_only = 1;
    adc_strategy **list = NULL;
    unsigned n = 0;
    adc__decode(adc__builtin_json, &list, &n, NULL, err, sizeof err);
    adc__lookup_builtins_only = 0;
    ADC_FREE(adc__builtins);
    adc__builtins = list; adc__nbuiltins = n;
}

int adc_load_json(const char *json, char *err, size_t err_len) {
    adc__ensure_builtins();
    adc_strategy **list = NULL;
    unsigned n = 0;
    char *def = NULL;
    if (!json || !adc__decode(json, &list, &n, &def, err, err_len)) return 0;
    const adc_strategy *d = NULL;
    if (def) {
        d = adc__find(list, n, def, 0);
        if (!d) d = adc_get(def);
        if (!d) { char m[160]; snprintf(m, sizeof m, "No scaling strategy '%s'", def); adc__err(err, err_len, m); for (unsigned k = 0; k < n; k++) adc_free(list[k]); ADC_FREE(list); ADC_FREE(def); return 0; }
    }
    for (unsigned k = 0; k < n; k++) adc_register(list[k]);
    if (d) adc__default_strategy = d;
    ADC_FREE(list);
    if (def) ADC_FREE(def);
    return 1;
}

adc_strategy *adc_decode_strategy(const char *json, char *err, size_t err_len) {
    adc__ensure_builtins();
    adc_strategy **list = NULL;
    unsigned n = 0;
    if (!json || !adc__decode(json, &list, &n, NULL, err, err_len) || !n) { ADC_FREE(list); return NULL; }
    adc_strategy *s = list[0];
    for (unsigned k = 1; k < n; k++) adc_free(list[k]);
    ADC_FREE(list);
    return s;
}

adc_strategy *adc_formula(const char *text, char *err, size_t err_len) {
    for (unsigned k = 0; k < adc__nformulas; k++) if (!strcmp(adc__formulas[k]->id + 8, text)) return adc__formulas[k];
    adc_expr *e = adc__expr_parse(text, 2, err, err_len);
    if (!e) return NULL;
    size_t n = strlen(text) + 9;
    char *id = (char *)adc__alloc(n);
    snprintf(id, n, "formula:%s", text);
    adc_strategy *s = adc__new(id);
    ADC_FREE(id);
    adc__own_expr(s, e);
    s->cfg.xf_kind = 1; s->cfg.xf = e;
    adc__finish(s);
    if (adc__nformulas < 256) adc__list_add(&adc__formulas, &adc__nformulas, s);
    return s;
}
adc_strategy *adc_custom_factor(const char *id, adc_factor_fn fn, void *user) {
    static unsigned seq;
    char buf[32];
    if (!id) { snprintf(buf, sizeof buf, "scale#%u", ++seq); id = buf; }
    adc_strategy *s = adc__new(id);
    adc__curve *c = (adc__curve *)adc__alloc(sizeof *c);
    c->kind = C_CODE; c->fn = fn; c->user = user;
    adc__own_curve(s, c);
    s->cfg.curve = c;
    adc__finish(s);
    return s;
}
adc_strategy *adc_custom_formula(const char *id, adc_formula_fn fn, void *user) {
    static unsigned seq;
    char buf[32];
    if (!id) { snprintf(buf, sizeof buf, "formula#%u", ++seq); id = buf; }
    adc_strategy *s = adc__new(id);
    s->cfg.xf_kind = 2; s->cfg.xf_fn = fn; s->cfg.xf_user = user;
    adc__finish(s);
    return s;
}
void adc_reset(void) {
    for (unsigned k = 0; k < adc__nformulas; k++) adc_free(adc__formulas[k]);
    ADC_FREE(adc__formulas); adc__formulas = NULL; adc__nformulas = 0;
    /* registered strategies may be parents of others: free all together */
    for (unsigned k = 0; k < adc__nreg; k++) adc_free(adc__reg[k]);
    ADC_FREE(adc__reg); adc__reg = NULL; adc__nreg = 0;
    adc__default_strategy = NULL;
}

#ifdef __cplusplus
}
#endif
#endif /* ADC_IMPLEMENTATION_DONE */
#endif /* ADC_IMPLEMENTATION */
