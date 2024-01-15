using Microsoft.Build.Construction;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;

namespace ProjectFileParser
{
    internal class Program
    {
        private static int Main(string[] args)
        {
            MSBuildCustomLocator.Register();
            try
            {
                var customPropertiesText = Console.In.ReadToEnd();
                var parseArgs = JsonSerializer.Deserialize<Dictionary<string, string>>(args.Length >= 2 ? args[1].Replace('\'', '"') : "{}");
                var result = Parse(args[0], parseArgs);
                Console.WriteLine(result);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Error during project file parsing: {0}", e);
                return -1;
            }
            return 0;
        }

        private static string Parse(string file, Dictionary<string, string> args)
        {
            var isSolution = Path.GetExtension(file).Equals(".sln", StringComparison.InvariantCultureIgnoreCase);
            return isSolution ? ParseSolution(file, args) : ParseProject(file, args);
        }

        private static string ParseSolution(string file, Dictionary<string, string> args)
        {
            var projects = ProjectHelpers.GetProjects(SolutionFile.Parse(file), args);
            return Jsonify.ToJson(projects);
        }

        private static string ParseProject(string file, Dictionary<string, string> args)
        {
            var project = ProjectHelpers.LoadProject(file, args);
            return Jsonify.ToJson(project);
        }
    }
}
