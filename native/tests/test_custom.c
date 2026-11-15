/* appdimens_custom.h: the cross-platform 540-case fixture (Kotlin reference engine) + API behaviour. */
#define ADC_IMPLEMENTATION
#include "appdimens_custom.h"
#include <math.h>
#include <stdio.h>
#include <string.h>
#include "custom_fixture.h"

static int fails = 0;
#define CHECK(c) do { if (!(c)) { fprintf(stderr, "%s:%d: CHECK(%s)\n", __FILE__, __LINE__, #c); fails++; } } while (0)

static int close_to(double want, double got, double tol) {
    double m = fabs(want) > 1.0 ? fabs(want) : 1.0;
    return fabs(want - got) <= tol * m;
}

static double product_fn(const adc_screen *s, void *user) {
    double f = s->width / 360.0; (void)user;
    return f < 0.85 ? 0.85 : (f > 1.8 ? 1.8 : f);
}
static double hud_fn(double base, const adc_screen *s, void *user) { (void)user; return base * pow(s->smallest_width / 360.0, 0.8); }

static void fixture(void) {
    char err[256] = {0};
    int ok = adc_load_json(ADC_FIX_DOC, err, sizeof err), bad = 0, i;
    if (!ok) { fprintf(stderr, "load: %s\n", err); fails++; return; }
    CHECK(adc_default() == adc_get("brand"));
    for (i = 0; i < ADC_FIX_COUNT; i++) {
        const adc_fix_case *c = &ADC_FIX_CASES[i];
        const adc_strategy *s = adc_get(c->strategy);
        adc_resolved r;
        char rules[256] = {0};
        unsigned k;
        if (!s) { fprintf(stderr, "missing %s\n", c->strategy); bad++; continue; }
        adc_resolve(s, &ADC_FIX_SCREENS[c->screen], &r);
        for (k = 0; k < r.rule_count && k < 32; k++) {
            if (r.applied_rules & (1u << k)) {
                if (rules[0]) strcat(rules, ",");
                strcat(rules, adc_rule_name(r.strategy, k));
            }
        }
        if (!close_to(c->factor, r.factor, ADC_FIX_TOL) || !close_to(c->dp, adc_scale(&r, c->base), ADC_FIX_TOL) ||
            !close_to(c->sp_dp, adc_scale_sp_dp(&r, c->base), ADC_FIX_TOL) || strcmp(rules, c->rules) != 0) {
            if (bad < 10)
                fprintf(stderr, "case %d %s/%d base=%g: factor %g/%g dp %g/%g sp %g/%g rules '%s'/'%s'\n", i, c->strategy,
                        c->screen, c->base, c->factor, r.factor, c->dp, adc_scale(&r, c->base), c->sp_dp,
                        adc_scale_sp_dp(&r, c->base), c->rules, rules);
            bad++;
        }
    }
    if (bad) { fprintf(stderr, "fixture: %d/%d cases differ\n", bad, ADC_FIX_COUNT); fails++; }
    else printf("fixture: %d/%d cases match\n", ADC_FIX_COUNT, ADC_FIX_COUNT);
}

static void api(void) {
    char err[256] = {0};
    adc_screen s = adc_screen_make(720, 1280, 2, 1, ADC_TABLET);
    adc_resolved r;
    adc_strategy *f, *f2, *p, *h;
    p = adc_custom_factor("product", product_fn, NULL);
    adc_resolve(p, &s, &r);
    CHECK(close_to(18.0, adc_scale(&r, 10), 1e-9));
    adc_free(p);
    h = adc_custom_formula("hud", hud_fn, NULL);
    adc_resolve(h, &s, &r);
    CHECK(close_to(16 * pow(2.0, 0.8), adc_scale(&r, 16), 1e-9));
    adc_free(h);
    f = adc_formula("base * pow(sw / 360, 0.8)", err, sizeof err);
    f2 = adc_formula("base * pow(sw / 360, 0.8)", err, sizeof err);
    CHECK(f && f == f2);
    adc_resolve(f, &s, &r);
    CHECK(close_to(16 * pow(2.0, 0.8), adc_scale(&r, 16), 1e-6));
    CHECK(adc_formula("base * (", err, sizeof err) == NULL && err[0]);
    err[0] = 0;
    CHECK(!adc_load_json("{\"version\":1,\"strategies\":[{\"id\":\"x\",\"clamp\":5}]}", err, sizeof err) && err[0]);
    CHECK(adc_get("x") == NULL);
    CHECK(adc_get("scaled") && adc_get("gameHud") && adc_get("tvSafe"));
    {
        ad_input in; ad_input_default(&in);
        ad_metrics m;
        in.width = 360; in.height = 640; in.density = 3;
        ad_metrics_init(&m, &in);
        CHECK(close_to(16.0, adc_dp(adc_get("scaled"), &m, 16) * 300.0 / 360.0, 1e-5));
        CHECK(close_to(adc_dp(adc_get("scaled"), &m, 16) * 3, adc_px(adc_get("scaled"), &m, 16), 1e-5));
    }
    {
        char buf[256];
        adc_explain(adc_get("tvSafe"), &s, buf, sizeof buf);
        CHECK(strlen(buf) > 10);
    }
    adc_reset();
}

int main(void) {
    fixture();
    adc_reset();
    api();
    if (fails) { fprintf(stderr, "%d failure(s)\n", fails); return 1; }
    puts("test_custom: OK");
    return 0;
}
