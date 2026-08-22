cmake_minimum_required(VERSION 3.22.1)
project(appdimens_games CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(appdimens_games SHARED
    jni/appdimens_games_jni.cpp)

target_include_directories(appdimens_games PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR})

# Header-first core: only the JNI bridge is compiled; hot math is inline.
target_compile_options(appdimens_games PRIVATE
    -O3 -fno-fast-math -fno-exceptions -fvisibility=hidden)
