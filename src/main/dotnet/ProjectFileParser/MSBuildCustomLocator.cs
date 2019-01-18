using Microsoft.Build.Locator;
using System;

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
                Console.Error.WriteLine("MSBuildLocator cannot detect VS location, falling back to GAC register MSBuild dlls");
                Console.Error.WriteLine("Error was: {0}", ex);

                RegisterFallback();
            }
        }

        private static void RegisterFallback()
        {
            AppDomain.CurrentDomain.AppendPrivatePath("privateDlls");
        }
    }
}