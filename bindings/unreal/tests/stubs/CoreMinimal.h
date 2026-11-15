// Minimal UE5 API surface for compile-checking the AppDimens plugin (not the engine).
#pragma once
#include <cstdint>
#include <string>
#include <vector>
#include <functional>
#include <algorithm>
typedef int32_t int32; typedef uint32_t uint32; typedef uint8_t uint8; typedef char16_t TCHAR;
#define TEXT(x) u##x
#define UCLASS(...)
#define UENUM(...)
#define UFUNCTION(...)
#define UPROPERTY(...)
#define GENERATED_BODY()
#define THIRD_PARTY_INCLUDES_START
#define THIRD_PARTY_INCLUDES_END
#define APPDIMENS_API
struct FIntPoint { int32 X = 0, Y = 0; FIntPoint() {} FIntPoint(int32 x, int32 y) : X(x), Y(y) {} };
struct FMath { template <class T> static T Max(T a, T b) { return a > b ? a : b; } };
class FString { std::u16string s; public: FString() {} FString(const TCHAR* t) : s(t) {} const TCHAR* operator*() const { return s.c_str(); } };
class FTCHARToUTF8 { std::string u; public: explicit FTCHARToUTF8(const TCHAR* t) { while (*t) u.push_back((char)*t++); } const char* Get() const { return u.c_str(); } };
struct FDelegateHandle { int id = 0; };
class UObject { public: virtual ~UObject() {} };
template <class T> const T* GetDefault() { static T d; return &d; }
template <class... A> struct TMulticast {
  std::vector<std::function<void(A...)>> fns;
  template <class O, class F> FDelegateHandle AddUObject(O* o, F f) { fns.push_back([o, f](A... a) { (o->*f)(a...); }); return {(int)fns.size()}; }
  void Remove(FDelegateHandle) {}
  void Broadcast(A... a) { for (auto& f : fns) f(a...); }
};
#define DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(Name, T1, P1) struct Name : TMulticast<T1> {};
