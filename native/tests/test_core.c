/* Parity + behaviour tests for include/appdimens/appdimens.h (float, double, no-libm, fixed point). */
#include <stdio.h>
#include <string.h>
#include "appdimens/appdimens.h"
#include "golden.h"

/* Reference vectors come from appdimens-dynamic (IEEE-754 float32): compare at float precision. */
#define REL 3e-6
#ifdef AD_NO_LIBM
#undef REL
#define REL 3e-6
#endif

static int fails = 0, checks = 0;
static void near(double got, double want, double rel, const char *what, int a, int b) {
    double d = got - want; if (d < 0) d = -d;
    double s = want < 0 ? -want : want;
    checks++;
    if (d > rel * (s > 1 ? s : 1)) { if (fails < 20) printf("FAIL %s [%d,%d]: got %.9g want %.9g\n", what, a, b, got, want); fails++; }
}
static double fabs_(double x) { return x < 0 ? -x : x; }

int main(void) {
    for (int c = 0; c < AD_GOLDEN_CASES; c++) {
        const ad_golden_case *g = &ad_golden[c];
        ad_input in; ad_input_default(&in);
        in.width = (ad_real)g->w; in.height = (ad_real)g->h; in.density = (ad_real)g->density;
        in.font_scale = (ad_real)g->font_scale; in.multi_window = g->mw;
        ad_metrics m;
        ad_metrics_init(&m, &in);
        for (int s = 0; s < AD_STRATEGY_COUNT; s++)
            for (int i = 0; i < AD_SLOT_COUNT; i++) near(m.table[s][i], g->f[s][i], REL, "table", s, i);
        near(ad_compute_factor_k(&m, AD_SCALED, AD_Q_SMALL_WIDTH, AD_INV_DEFAULT, 0, 1, (ad_real)0.01), g->k_scaled, REL, "k-scaled", c, 0);
        near(ad_compute_factor_k(&m, AD_AUTO, AD_Q_SMALL_WIDTH, AD_INV_DEFAULT, 0, 1, (ad_real)0.01), g->k_auto, REL, "k-auto", c, 0);
        near(ad_sdp(&m, 10), 10 * g->f[0][0], REL, "sdp", c, 0);
        near(ad_assp_a(&m, 10), 10 * g->f[1][1] * g->font_scale, REL, "assp_a", c, 0);
        near(ad_ftwdp_px(&m, 10), 10 * g->f[9][8] * g->density, REL, "ftwdp_px", c, 0);
        near(ad_hem_ia(&m, 10), 10 * g->f[0][7], REL, "hem_ia", c, 0);
#ifdef AD_FIXED_POINT
        for (int s = 0; s < AD_STRATEGY_COUNT; s++) {
            int32_t q = ad_dp_q(&m, (ad_strategy)s, 0, 100);
            double want = 100 * g->f[s][0];
            if (fabs_(q - want) > 0.5001) { printf("FAIL fixed %d %d vs %f\n", s, (int)q, want); fails++; }
            checks++;
        }
#endif
    }
    /* token parser */
    struct { const char *name; int ok, strategy, slot, kind, px; } T[] = {
        {"sdp", 1, 0, 0, 0, 0}, {"asdpa", 1, 1, 1, 0, 0}, {"hdpLw", 1, 0, AD_SLOT(AD_VAR_H_LW, 0), 0, 0},
        {"sdpPxiaPh", 1, 0, AD_SLOT(AD_VAR_S_PH, 3), 0, 1}, {"sdpPhaPx", 1, 0, AD_SLOT(AD_VAR_S_PH, 1), 0, 1},
        {"logssp", 1, 6, 0, 1, 0}, {"pwdp", 1, 2, AD_SLOT(AD_VAR_W, 0), 0, 0}, {"pwsdp", 1, 3, 0, 0, 0},
        {"dgwem", 1, 7, 8, 2, 0}, {"wspPh", 1, 0, AD_SLOT(AD_VAR_W_PH, 0), 1, 0}, {"flsdpi", 1, 10, 2, 0, 0},
        {"hdpPh", 0, 0, 0, 0, 0}, {"sdpaa", 0, 0, 0, 0, 0}, {"xdp", 0, 0, 0, 0, 0}, {"", 0, 0, 0, 0, 0},
        {"sdpPxPx", 0, 0, 0, 0, 0}, {"sdpaPhi", 0, 0, 0, 0, 0}, {"sd", 0, 0, 0, 0, 0},
    };
    for (size_t i = 0; i < sizeof T / sizeof T[0]; i++) {
        ad_token t; int ok = ad_parse_token(T[i].name, &t);
        checks++;
        if (ok != T[i].ok || (ok && ((int)t.strategy != T[i].strategy || t.slot != T[i].slot || (int)t.kind != T[i].kind || t.px != T[i].px))) {
            printf("FAIL token %s ok=%d s=%d slot=%d\n", T[i].name, ok, ok ? (int)t.strategy : -1, ok ? t.slot : -1); fails++;
        }
    }
    /* dimen strings, units, resize, sanitising */
    ad_metrics m; ad_metrics_make(&m, 360, 800, 3, (ad_real)1.2);
    ad_real r = 0;
    checks += 6;
    if (!ad_dimen(&m, "10sdp", &r) || fabs_(r - 12) > 1e-4) { printf("FAIL dimen sdp %f\n", (double)r); fails++; }
    if (!ad_dimen(&m, "10ssp", &r) || fabs_(r - 14.4) > 1e-4) { printf("FAIL dimen ssp\n"); fails++; }
    if (!ad_dimen(&m, "50spaceW", &r) || fabs_(r - 180) > 1e-4) { printf("FAIL dimen space\n"); fails++; }
    if (!ad_dimen(&m, "-3px", &r) || fabs_(r + 1) > 1e-5) { printf("FAIL dimen px\n"); fails++; }
    if (ad_dimen(&m, "10sdpx", &r) || ad_dimen(&m, "sdp", &r)) { printf("FAIL dimen reject\n"); fails++; }
    {
        ad_input in; ad_input_default(&in); in.width = (ad_real)-1; in.height = (ad_real)1e40; in.density = 0;
        ad_metrics z; ad_metrics_init(&z, &in);
        if (z.in.width != 0 || z.in.density != 1 || fabs_(ad_sdp(&z, 10) - 10) > 1e-5) { printf("FAIL sanitise\n"); fails++; }
    }
    printf("%s: %d checks, %d failures\n",
#if defined(AD_FIXED_POINT)
           "fixed"
#elif defined(AD_NO_LIBM)
           "no-libm"
#elif defined(AD_USE_DOUBLE)
           "double"
#else
           "float"
#endif
           , checks, fails);
    return fails != 0;
}
