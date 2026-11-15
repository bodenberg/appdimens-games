using System.IO;
using UnrealBuildTool;

public class AppDimens : ModuleRules
{
    public AppDimens(ReadOnlyTargetRules Target) : base(Target)
    {
        PCHUsage = PCHUsageMode.UseExplicitOrSharedPCHs;
        PublicIncludePaths.Add(Path.Combine(ModuleDirectory, "ThirdParty"));
        PublicDependencyModuleNames.AddRange(new string[] { "Core", "CoreUObject", "Engine", "DeveloperSettings" });
        PrivateDependencyModuleNames.AddRange(new string[] { "ApplicationCore", "Slate", "SlateCore" });
    }
}
