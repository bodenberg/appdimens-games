#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
out="$root/build/validation"
rm -rf "$out" && mkdir -p "$out/java"
find "$root/appdimens_games_core/src/main/java" -name '*.java' -print0 | xargs -0 javac --release 17 -Xlint:all -Werror -d "$out/java"
javac --release 17 -Xlint:all -Werror -cp "$out/java" -d "$out/java" "$root/tools/CoreSelfTest.java"
java -ea -cp "$out/java" CoreSelfTest
find "$root/appdimens_games_android/src/main/java" "$root/appdimens_games_graphics/src/main/java" \
  -name '*.java' ! -name 'AndroidScreens.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all -Werror -cp "$out/java" -d "$out/java"
cmake -S "$root/appdimens_games_native/src/test/cpp" -B "$out/native"
cmake --build "$out/native" --parallel
ctest --test-dir "$out/native" --output-on-failure
jdk=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
g++ -std=c++17 -O3 -fPIC -shared -fno-exceptions -fno-rtti -Wall -Wextra -Werror \
  -Wl,--no-undefined -I"$root/appdimens_games_native/src/main/cpp/include" \
  -I"$jdk/include" -I"$jdk/include/linux" \
  "$root/appdimens_games_native/src/main/cpp/src/appdimens_games.cpp" \
  "$root/appdimens_games_native/src/main/cpp/src/jni_bridge.cpp" \
  -o "$out/libappdimens_games.so"
nm -D --defined-only "$out/libappdimens_games.so" | grep -q ' adg_scale_batch$'
echo "Validation completed"
