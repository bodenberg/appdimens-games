#pragma once
class FDefaultModuleImpl {};
#define IMPLEMENT_MODULE(Impl, Name) [[maybe_unused]] static Impl Name##_module_instance;
