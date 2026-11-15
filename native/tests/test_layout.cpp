// Compile-time proof that the AppDimens viewport structs are layout-compatible with the
// real graphics API structs (memcpy / reinterpret_cast safe). Built when headers exist.
#include "appdimens/games.h"
#include <cstddef>
#include <cstdio>
#if __has_include(<vulkan/vulkan.h>)
#include <vulkan/vulkan.h>
static_assert(sizeof(adg_viewport_f) == sizeof(VkViewport), "VkViewport size");
static_assert(offsetof(adg_viewport_f, x) == offsetof(VkViewport, x), "VkViewport.x");
static_assert(offsetof(adg_viewport_f, height) == offsetof(VkViewport, height), "VkViewport.height");
static_assert(offsetof(adg_viewport_f, max_depth) == offsetof(VkViewport, maxDepth), "VkViewport.maxDepth");
static_assert(sizeof(adg_rect_i) == sizeof(VkRect2D), "VkRect2D size");
static_assert(offsetof(adg_rect_i, width) == offsetof(VkRect2D, extent.width), "VkRect2D.extent");
#define HAVE_VK 1
#endif
#if defined(_WIN32) && __has_include(<d3d11.h>)
#include <d3d11.h>
#include <d3d12.h>
static_assert(sizeof(adg_viewport_f) == sizeof(D3D11_VIEWPORT), "D3D11_VIEWPORT");
static_assert(offsetof(adg_viewport_f, width) == offsetof(D3D11_VIEWPORT, Width), "D3D11 Width");
static_assert(offsetof(adg_viewport_f, max_depth) == offsetof(D3D11_VIEWPORT, MaxDepth), "D3D11 MaxDepth");
static_assert(sizeof(adg_viewport_f) == sizeof(D3D12_VIEWPORT), "D3D12_VIEWPORT");
static_assert(sizeof(adg_rect_ltrb) == sizeof(D3D12_RECT), "D3D12_RECT");
static_assert(offsetof(adg_rect_ltrb, bottom) == offsetof(RECT, bottom), "RECT.bottom");
#define HAVE_DX 1
#endif
#if defined(__APPLE__) && defined(__OBJC__) && __has_include(<Metal/MTLRenderCommandEncoder.h>)  // Metal headers are Objective-C
#include <Metal/MTLRenderCommandEncoder.h>
static_assert(sizeof(adg_mtl_viewport) == sizeof(MTLViewport), "MTLViewport");
#define HAVE_MTL 1
#endif
int main() {
#ifdef HAVE_VK
    std::puts("Vulkan layout: OK");
#endif
#ifdef HAVE_DX
    std::puts("Direct3D 11/12 layout: OK");
#endif
#ifdef HAVE_MTL
    std::puts("Metal layout: OK");
#endif
    return 0;
}
