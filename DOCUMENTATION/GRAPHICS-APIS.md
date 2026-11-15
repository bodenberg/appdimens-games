# Graphics APIs

| | OpenGL / GLES / WebGL | Vulkan | D3D11 / D3D12 | Metal | WebGPU |
|---|---|---|---|---|---|
| Framebuffer origin | bottom-left | top-left | top-left | top-left | top-left |
| NDC +Y | up | **down** | up | up | up |
| Clip depth | [-1, 1] | [0, 1] | [0, 1] | [0, 1] | [0, 1] |
| Viewport struct | `adg_gl_viewport()` → `glViewport(x, y, w, h)` | `adg_native_viewport(v, flip)` ≡ `VkViewport` | ≡ `D3D11_VIEWPORT` / `D3D12_VIEWPORT` | `adg_metal_viewport()` ≡ `MTLViewport` | ≡ `setViewport(x,y,w,h,min,max)` |
| Scissor | `adg_scissor(ADG_GFX_OPENGL, …)` (y from bottom) | `adg_rect_i` ≡ `VkRect2D` | `adg_rect_ltrb` ≡ `D3D12_RECT` | `MTLScissorRect` (cast to uint) | `setScissorRect` |

Layout compatibility with `VkViewport`, `VkRect2D`, `D3D11_VIEWPORT`, `D3D12_VIEWPORT`, `D3D12_RECT` is enforced with
`static_assert` in `native/tests/test_layout.cpp` whenever the SDK headers are present, so `memcpy` / casts are safe.

## Projections

```c
float proj[16];
adg_ortho(ADG_GFX_VULKAN, 0, 0, design_w, design_h, 0, 1, /*row_major*/0, proj);          // 2D / UI, Y down
adg_perspective(ADG_GFX_D3D12, vfov, aspect, 0.1f, 0.0f /*infinite*/, /*reverse_z*/1, 0, proj);
```

- Matrices are **column-major** (GLSL, Vulkan, Metal, WGSL, HLSL `column_major` default for cbuffers); `row_major = 1`
  gives DirectXMath / HLSL `row_major` layout.
- Vulkan: either use the matrix as is (NDC Y down handled) or flip with a negative-height viewport
  (`adg_native_viewport(v, 1)`, VK_KHR_maintenance1 / Vulkan 1.1) and build the matrix for a Y-up API.
- Reverse-Z (`reverse_z = 1`, depth clear 0, `GREATER` test) + infinite far plane gives the best depth precision for
  open worlds. On OpenGL the reverse-Z matrix targets a [0, 1] depth range, so enable it with
  `glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)` (GL 4.5 / ARB_clip_control); without clip control keep `reverse_z = 0`.

## Viewports

`adg_viewport_compute(mode, surface_w, surface_h, design_w, design_h)`:

| Mode | Behaviour | Typical use |
|------|-----------|-------------|
| `FIT` | letterbox / pillarbox | fixed-board puzzle, cutscenes |
| `FILL` | cover, crops edges | backgrounds, menus with bleed |
| `EXPAND` | uniform scale, wider/taller screens see more world, no bars | 2D action, platformers (Godot "expand") |
| `FIT_WIDTH` / `FIT_HEIGHT` | one axis fixed, the other expands | vertical shooters / side scrollers |
| `INTEGER` | largest integer scale, centred | pixel art (no shimmering) |
| `STRETCH` | non-uniform | debug only |

`adg_screen_to_design()` / `adg_design_to_screen()` convert mouse, touch and gamepad cursors; `adg_viewport_contains()`
rejects clicks on bars.
