using Microsoft.Build.Locator;
using System;
using System.Linq;
using System.Reflection;

namespace ProjectFileParser
{
    public static class MSBuildCustomLocator
    {
        public static void Register()
        {
            try
            {
                var versions = MSBuildLocator.QueryVisualStudioInstances().OrderBy(vsInstance => vsInstance.Version);
                var latestVsVersion = versions.Last();
                MSBuildLocator.RegisterInstance(latestVsVersion);
                Console.Error.WriteLine($"Registered latest VS Instance: {latestVsVersion.Name} - {latestVsVersion.Version} - {latestVsVersion.MSBuildPath} - {latestVsVersion.DiscoveryType} - {latestVsVersion.VisualStudioRootPath}");
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("MSBuildLocator cannot detect VS location, falling back to GAC registered MSBuild dlls");
                Console.Error.WriteLine("Error was: {0}", ex);
            }
        }
    }
}