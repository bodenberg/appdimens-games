#ifndef APPDIMENS_GAMES_H
#define APPDIMENS_GAMES_H
#include <stddef.h>
#include <stdint.h>
#if defined(__GNUC__)
#define ADG_API __attribute__((visibility("default")))
#else
#define ADG_API
#endif
#ifdef __cplusplus
extern "C" {
#endif
#define ADG_ABI_VERSION 0x00030106u
typedef enum adg_status { ADG_OK=0, ADG_NULL_ARGUMENT=1, ADG_INVALID_ARGUMENT=2, ADG_OUT_OF_RANGE=3 } adg_status;
typedef enum adg_strategy { ADG_NONE=0, ADG_DEFAULT=1, ADG_PERCENTAGE=2, ADG_BALANCED=3,
 ADG_LOGARITHMIC=4, ADG_POWER=5, ADG_FLUID=6, ADG_INTERPOLATED=7, ADG_DIAGONAL=8,
 ADG_PERIMETER=9, ADG_FIT=10, ADG_FILL=11, ADG_AUTOSIZE=12 } adg_strategy;
typedef struct adg_insets { float left, top, right, bottom; } adg_insets;
typedef struct adg_screen { float width_dp, height_dp, density; adg_insets safe_area; } adg_screen;
typedef struct adg_config { float design_width_dp, design_height_dp, sensitivity, exponent,
 transition_dp, min_value, max_value, min_viewport_dp, max_viewport_dp; } adg_config;
typedef struct adg_viewport { float scale_x, scale_y, offset_x, offset_y, width, height; } adg_viewport;
ADG_API uint32_t adg_abi_version(void);
ADG_API adg_config adg_default_config(void);
ADG_API adg_status adg_scale(float value, adg_strategy strategy, const adg_screen* screen,
 const adg_config* config, float* result);
ADG_API adg_status adg_scale_batch(const float* input, size_t input_stride, float* output,
 size_t output_stride, size_t count, adg_strategy strategy, const adg_screen* screen, const adg_config* config);
/** mode: 0 FIT, 1 FILL, 2 STRETCH. */
ADG_API adg_status adg_calculate_viewport(float design_width, float design_height,
 const adg_screen* screen, int mode, adg_viewport* result);
#ifdef __cplusplus
}
#endif
#endif
