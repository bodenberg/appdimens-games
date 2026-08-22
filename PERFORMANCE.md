# ⚡ AppDimens Games — Performance (BenchLab)

## Design contract

| Path | Cost | Notes |
|---|---|---|
| Fast lane dp→px | **≈2 ns** | `base × precomputedFactor` |
| AR-aware lane | ≈2 ns | same shape (`sdpa`) |
| Kernel full (cold) | 15–40 ns | pure inline math, zero alloc |
| Cache hit | ≈4 ns | packed 64-bit key, lock-free slot |
| Snapshot rebuild | once per resize | exact `ln()` only here |
| JNI kernel | +JNI transition | still allocation-free |

Compared to the deprecated `appdimens-games` 2.0.1, which hashed parameters and consulted a cache on **every** call, the 3.0 engine reduces the common case to a single float multiply — the same fast-lane architecture that makes `appdimens-dynamic`/`kmp` the fastest in family benchmarks.

## Methodology (BenchLab module)

Runs **100% on a background thread** and streams **per-case** results to the UI as each
measurement completes — the main thread is never blocked (no ANR). A failing case is
reported and skipped instead of killing the run.

* Warm-up: 3,000 ops; Measure: 9 samples × 10,000 ops;
* Anti-DCE checksum sink;
* Stats: **median / min / P90** per case; ratios reported vs games-3.0.

Cases compared on-device: `games-3.0 kernel` · `games-3.0 ext (16.sdp)` ·
`games-3.0 AR-aware` · `games-2.0.1 legacy gateway` · `dynamic-3.1.9 facade` ·
`dynamic-3.1.9 sdpa`.

## Running

```bash
./gradlew :benchlab:installDebug
adb shell am start -n com.example.benchlab/.BenchlabActivity
# press RUN — results stream on screen per case (ns/op), summary ratios at the end
```

Expected outcome on modern devices (consistent with family numbers): games-3.0 fast lanes at single-digit ns/op, legacy gateway typically **10–50× slower**, dynamic comparable to games-3.0 (same architecture).

> Numbers are device-specific; always publish your BenchLab capture alongside claims.

## Audit tooling

* [`scripts/oracle.py`](scripts/oracle.py) — validates every formula/constant numerically (30 cases).
* Unit tests with oracle-generated expectations: `library/src/test/...`.
* CI runs JVM tests + oracle on every push.
