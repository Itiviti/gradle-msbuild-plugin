using Microsoft.Build.Locator;
using System;
using System.Linq;

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
                Console.Error.WriteLine("MSBuildLocator cannot detect VS location");
                Console.Error.WriteLine("Error was: {0}", ex);
            }
        }
    }
}