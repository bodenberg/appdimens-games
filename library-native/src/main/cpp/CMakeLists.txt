cmake_minimum_required(VERSION 3.22.1)
project(appdimens_games CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Cross-platform SDK shared with Windows/Linux/macOS/iOS/engines (repo-root native/).
set(ADG_NATIVE_SDK ${CMAKE_CURRENT_SOURCE_DIR}/../../../../native)

add_library(appdimens_games SHARED
    jni/appdimens_games_jni.cpp
    ${ADG_NATIVE_SDK}/src/appdimens_games.cpp)

target_include_directories(appdimens_games PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${ADG_NATIVE_SDK}/include)
target_compile_definitions(appdimens_games PRIVATE ADG_BUILD_SHARED)

# Header-first core: only the JNI bridge is compiled; hot math is inline.
target_compile_options(appdimens_games PRIVATE
    -O3 -fno-fast-math -fno-exceptions -fvisibility=hidden)
