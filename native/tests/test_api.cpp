// C ABI tests + concurrency stress (publisher thread vs reader threads).
#include <atomic>
#include <cmath>
#include <cstdio>
#include <thread>
#include <vector>
#include "appdimens/appdimens_games_api.h"

static int fails = 0;
#define CHECK(c, msg) do { if (!(c)) { std::printf("FAIL %d: %s\n", __LINE__, msg); ++fails; } } while (0)

int main() {
    CHECK(adg_version() == 0x030300u, "version");
    CHECK(std::fabs(adg_live_px(AD_SCALED, 0, 16) - 16) < 1e-5f, "default snapshot identity");
    const uint32_t g0 = adg_generation();
    adg_publish_px(3840, 2160, ADG_SCALE_REFERENCE, 1080, 1, 0);
    CHECK(adg_generation() == g0 + 1, "generation");
    adg_publish_px(3840, 2160, ADG_SCALE_REFERENCE, 1080, 1, 0);
    CHECK(adg_generation() == g0 + 1, "identical publish ignored");
    CHECK(std::fabs(adg_live_ref_px(AD_SCALED, 0, 64) - 128) < 1e-3f, "4K ref px");
    CHECK(std::fabs(adg_live_resolve("sdp", 64) - 128) < 1e-3f, "resolve token (reference mode: dp = px)");
    CHECK(std::fabs(adg_live_px(AD_SCALED, 0, 64) - 64 * 2 * 3.6f) < 1e-2f, "dp → px");
    CHECK(adg_live_resolve("nope", 64) == 0, "bad token");
    int32_t s, slot, kind, px;
    CHECK(adg_token_slot("asspPxa", &s, &slot, &kind, &px) && s == AD_AUTO && slot == 1 && kind == 1 && px == 1, "token slot");
    CHECK(std::fabs(adg_live_dimen("50spaceW", -1) - 0.5f * 3840 / 3.6f) < 1e-2f, "dimen");
    adg_viewport v;
    adg_viewport_get(ADG_VIEW_FIT, 2560, 1080, 1920, 1080, &v);
    CHECK(std::fabs(v.x - 320) < 1e-3f, "viewport ffi");
    adg_render_size rs;
    adg_render_size_get(ADG_UPSCALER_DLSS, ADG_Q_PERFORMANCE, 3840, 2160, &rs);
    CHECK(rs.width == 1920 && rs.height == 1080, "render size ffi");
    std::printf("platform mask: 0x%x, device class for 1280x800@215dpi: %d\n", adg_platform(),
                adg_suggest_device_class(1280, 800, 215));
#if defined(_WIN32)
    CHECK(adg_platform() & ADG_PLATFORM_WINDOWS, "platform windows");
#elif defined(__linux__)
    CHECK(adg_platform() & ADG_PLATFORM_LINUX, "platform linux");
#elif defined(__APPLE__)
    CHECK(adg_platform() & (ADG_PLATFORM_MACOS | ADG_PLATFORM_IOS | ADG_PLATFORM_TVOS), "platform apple");
#endif

    // Stress: one publisher, 4 readers. Every snapshot read must be internally consistent.
    std::atomic<bool> stop{false};
    std::atomic<long> reads{0}, bad{0};
    std::vector<std::thread> readers;
    for (int r = 0; r < 4; ++r)
        readers.emplace_back([&] {
            ad_metrics m;
            while (!stop.load(std::memory_order_relaxed)) {
                adg_read(&m);
                const double sw = m.in.width < m.in.height ? m.in.width : m.in.height;
                if (std::fabs(m.table[AD_SCALED][0] - sw / 300.0) > 1e-4 * (sw / 300.0) ) bad++;
                const ad_metrics *p = adg_acquire();
                volatile float f = p->table[AD_FIT][0];
                (void)f;
                reads++;
            }
        });
    std::thread writer([&] {
        for (int i = 0; i < 200000; ++i) adg_publish_px(800.0f + (i % 3000), 600.0f + (i % 1700), ADG_SCALE_REFERENCE, 1080, 1, 0);
        stop = true;
    });
    writer.join();
    for (auto &t : readers) t.join();
    std::printf("stress: %ld reads, %ld inconsistent\n", reads.load(), bad.load());
    CHECK(bad == 0, "torn reads");
    std::printf("%s\n", fails ? "FAILED" : "OK");
    return fails != 0;
}
