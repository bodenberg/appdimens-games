"""Copies the single sources of truth into every binding (run after editing them).

  native/include/appdimens/{appdimens.h,appdimens_custom.h,games.h} → Unreal ThirdParty
  dotnet/AppDimens.Games/{Core,Games}.cs  → Unity package Runtime/Core
  native/tests/golden.json                         → Godot tests
Use --check in CI to fail when a copy is stale.
"""
import filecmp
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PAIRS = [
    ("native/include/appdimens/appdimens.h", "engines/unreal/AppDimens/Source/AppDimens/ThirdParty/appdimens/appdimens.h"),
    ("native/include/appdimens/appdimens_custom.h", "engines/unreal/AppDimens/Source/AppDimens/ThirdParty/appdimens/appdimens_custom.h"),
    ("native/include/appdimens/games.h", "engines/unreal/AppDimens/Source/AppDimens/ThirdParty/appdimens/games.h"),
    ("dotnet/AppDimens.Games/Core.cs", "engines/unity/com.bodenberg.appdimens.games/Runtime/Core/Core.cs"),
    ("dotnet/AppDimens.Games/Tokens.cs", "engines/unity/com.bodenberg.appdimens.games/Runtime/Core/Tokens.cs"),
    ("dotnet/AppDimens.Games/Games.cs", "engines/unity/com.bodenberg.appdimens.games/Runtime/Core/Games.cs"),
    ("native/tests/golden.json", "engines/godot/tests/golden.json"),
]
check = "--check" in sys.argv
stale = []
for src, dst in PAIRS:
    s, d = os.path.join(ROOT, src), os.path.join(ROOT, dst)
    if os.path.exists(d) and filecmp.cmp(s, d, shallow=False):
        continue
    if check:
        stale.append(dst)
    else:
        os.makedirs(os.path.dirname(d), exist_ok=True)
        shutil.copyfile(s, d)
        print("synced", dst)
if stale:
    print("stale copies:\n  " + "\n  ".join(stale))
    sys.exit(1)
print("engine copies in sync")
