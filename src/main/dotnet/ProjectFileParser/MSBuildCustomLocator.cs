using System;
using Microsoft.Build.Locator;

namespace ProjectFileParser
{
    internal static class MSBuildCustomLocator
    {
        public static void Register()
        {
            try
            {
                MSBuildLocator.RegisterDefaults();
            }
            catch (InvalidOperationException ex)
            {
                Console.WriteLine("MSBuildLocator cannot detect VS location, falling back to GAC register MSBuild dlls");
                Console.WriteLine("Error was: {0}", ex);

                RegisterFallback();
            }
        }

        private static void RegisterFallback()
        {
            AppDomain.CurrentDomain.AppendPrivatePath("privateDlls");
        }
    }
}
