using Microsoft.Build.Construction;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.IO;

namespace ProjectFileParser
{
    internal class Program
    {
        private static int Main(string[] args)
        {
            MSBuildCustomLocator.Register();

            using (new MonoHack())
            {
                try
                {
                    var customPropertiesText = Console.In.ReadToEnd();
                    var obj = JObject.Parse(customPropertiesText);
                    var result = Parse(args[0], obj);
                    Console.WriteLine(result);
                }
                catch (Exception e)
                {
                    Console.Error.WriteLine("Error during project file parsing: {0}", e);
                    return -1;
                }
            }
            return 0;
        }

        private static JObject Parse(string file, JObject args)
        {
            var isSolution = Path.GetExtension(file).Equals(".sln", StringComparison.InvariantCultureIgnoreCase);
            return isSolution ? ParseSolution(file, args) : ParseProject(file, args);
        }

        private static JObject ParseSolution(string file, JObject args)
        {
            var projects = ProjectHelpers.GetProjects(SolutionFile.Parse(file), ParamsToDic(args));
            return Jsonify.ToJson(projects);
        }

        private static JObject ParseProject(string file, JObject args)
        {
            var project = ProjectHelpers.LoadProject(file, ParamsToDic(args));
            return Jsonify.ToJson(project);
        }

        private static IDictionary<string, string> ParamsToDic(JObject args)
        {
            var dic = new Dictionary<string, string>();
            foreach (var kvp in args)
            {
                dic[kvp.Key] = kvp.Value.Value<String>();
            }
            return dic;
        }
    }
}
