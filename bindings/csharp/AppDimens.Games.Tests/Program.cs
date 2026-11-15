// Parity + behaviour tests for AppDimens.Games (.NET). Exit code = failures.
using System;
using System.IO;
using System.Text.Json;
using System.Threading;
using AppDimens;
using AppDimens.Games;

static class Program
{
    static int fails, checks;
    static void Near(double got, double want, double tol, string what)
    {
        checks++;
        if (Math.Abs(got - want) > tol) { fails++; if (fails < 30) Console.WriteLine($"FAIL {what}: got {got:R} want {want:R}"); }
    }
    static void True(bool c, string what) { checks++; if (!c) { fails++; Console.WriteLine("FAIL " + what); } }

    static int Main()
    {
        var doc = JsonDocument.Parse(File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "golden.json")));
        var strategies = doc.RootElement.GetProperty("strategies");
        foreach (var c in doc.RootElement.GetProperty("cases").EnumerateArray())
        {
            var inp = new DimenInput(c.GetProperty("w").GetDouble(), c.GetProperty("h").GetDouble(), c.GetProperty("density").GetDouble(), c.GetProperty("fontScale").GetDouble());
            inp.MultiWindow = c.GetProperty("multiWindow").GetBoolean();
            var m = new DimenMetrics(inp);
            int s = 0;
            foreach (var name in strategies.EnumerateArray())
            {
                int slot = 0;
                foreach (var f in c.GetProperty("factors").GetProperty(name.GetString()).EnumerateArray())
                {
                    double want = f.GetDouble();
                    Near(m.Factor((Strategy)s, slot), want, 3e-6 * Math.Max(1, Math.Abs(want)), $"{name}[{slot}] {inp.Width}x{inp.Height}");
                    slot++;
                }
                s++;
            }
            Near(m.ComputeFactor(Strategy.Scaled, Qualifier.SmallWidth, Inverter.Default, false, true, 0.01), c.GetProperty("customK001").GetProperty("scaled").GetDouble(), 1e-12, "k scaled");
        }
        // tokens
        Token t;
        True(Token.TryParse("sdpPxiaPh", out t) && t.Px && t.Slot == 15 && t.Strategy == Strategy.Scaled, "token sdpPxiaPh");
        True(Token.TryParse("pwdp", out t) && t.Strategy == Strategy.Percent && t.Slot == 8, "token pwdp");
        True(Token.TryParse("logssp", out t) && t.Strategy == Strategy.Logarithmic && t.Kind == Kind.Sp, "token logssp");
        True(!Token.TryParse("hdpPh", out t) && !Token.TryParse("sdpaa", out t), "bad tokens");
        // games
        var surf = new Surface(3840, 2160);
        var gm = surf.ToMetrics();
        Near(gm.Sdp(64), 128, 1e-3, "4K reference sdp");
        Near(gm.Asdp(64), 64 * (1.6 + 0.4 * Math.Log(1.4)), 1e-3, "4K reference asdp");
        var v = Viewport.Compute(ViewportMode.Integer, 1366, 768, 320, 180);
        True(v.ScaleX == 4 && v.X == 43 && v.Y == 24, "integer viewport");
        v = Viewport.Compute(ViewportMode.Fill, 2560, 1080, 1920, 1080);
        float dx, dy, sx, sy; v.ScreenToDesign(10, 20, out dx, out dy); v.DesignToScreen(dx, dy, out sx, out sy);
        Near(sx, 10, 1e-3, "fill roundtrip"); Near(sy, 20, 1e-3, "fill roundtrip y");
        var r = Upscaling.ForPreset(Upscaler.Dlss, UpscaleQuality.Balanced, 3840, 2160);
        True(r.Width == 2227 && r.Height == 1253, "dlss balanced");
        Near(Upscaling.MipBias(Upscaler.Fsr3, 1280, 1920), Math.Log(1280.0 / 1920, 2) - 1, 1e-5, "mip bias");
        True(Upscaling.JitterPhaseCount(960, 1920) == 32, "phases");
        var j = Upscaling.JitterFor(0, 960, 540, 1920);
        Near(j.Py, 1.0 / 3 - 0.5, 1e-6, "jitter");
        var vf = Fov.ForAspect(FovPolicy.Hybrid, 60 * Fov.Deg2Rad, 16f / 9, 4f / 3);
        Near(Fov.HorizontalFromVertical(vf, 4f / 3), Fov.HorizontalFromVertical(60 * Fov.Deg2Rad, 16f / 9), 1e-5, "hybrid fov");
        var p = Gfx.Ortho(GraphicsApi.Vulkan, 0, 0, 1920, 1080, 0, 1);
        Near(p[5] * 0 + p[13], -1, 1e-6, "vk ortho top=-1");
        var pp = Gfx.Perspective(GraphicsApi.D3D12, 1f, 1.5f, 0.1f, 100f, true);
        Near((pp[10] * -0.1f + pp[14]) / 0.1f, 1, 1e-4, "reverse z near=1");
        var dr = new DynamicResolution(16.6f);
        for (int i = 0; i < 400; i++) dr.Update(25f * dr.Scale * dr.Scale);
        Near(dr.Scale, Math.Sqrt(16.6 * 0.92 / 25), 0.03, "dynres");
        var band = Hud.Band(new AppDimens.Games.Rect(0, 0, 5120, 1440), 16f / 9);
        Near(band.W, 2560, 1e-3, "hud band");
        // live: concurrent publish/read
        int changed = 0;
        Live.Changed += _ => Interlocked.Increment(ref changed);
        True(GameDimens.Publish(new Surface(1920, 1080)), "publish");
        True(!GameDimens.Publish(new Surface(1920, 1080)), "identical publish ignored");
        Near(GameDimens.Px(64), 64, 1e-4, "1080p identity");
        bool stop = false; long bad = 0, reads = 0;
        var readers = new Thread[3];
        for (int i = 0; i < 3; i++)
        {
            readers[i] = new Thread(() =>
            {
                while (!Volatile.Read(ref stop))
                {
                    var m = Live.Current;
                    if (Math.Abs(m.Factor(Strategy.Scaled, 0) - m.Sw / 300.0) > 1e-5 * m.Sw / 300.0) Interlocked.Increment(ref bad);
                    Interlocked.Increment(ref reads);
                }
            });
            readers[i].Start();
        }
        for (int i = 0; i < 20000; i++) GameDimens.Publish(new Surface(800 + i % 3000, 600 + i % 1700));
        Volatile.Write(ref stop, true);
        foreach (var th in readers) th.Join();
        True(bad == 0, "torn reads");
        Console.WriteLine($"stress: {reads} reads, {bad} inconsistent, {changed} change events");
        Console.WriteLine($"csharp: {checks} checks, {fails} failures");
        return fails;
    }
}
