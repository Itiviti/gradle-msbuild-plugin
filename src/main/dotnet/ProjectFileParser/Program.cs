using System;
using System.IO;
using Microsoft.Build.Construction;
using Newtonsoft.Json.Linq;
using System.Collections.Generic;

namespace ProjectFileParser
{
    class Program
    {
        static int Main(string[] args)
        {
            MSBuildCustomLocator.Register();

            using (new MonoHack())
            {
                try
                {
                    var customPropertiesText = "{}";//Console.In.ReadToEnd();
                    var obj = JObject.Parse(customPropertiesText);
                    var result = Parse(args[0], obj);
                    Console.WriteLine(result.ToString());
                }
                catch (Exception e)
                {
                    Console.Error.WriteLine("Error during project file parsing: {0}", e);
                    return -1;
                }
            }
            return 0;
        }

        static JObject Parse(string file, JObject args)
        {
            var isSolution = Path.GetExtension(file).Equals(".sln", StringComparison.InvariantCultureIgnoreCase);
            return isSolution ? ParseSolution(file, args) : ParseProject(file, args);
        }

        static JObject ParseSolution(string file, JObject args)
        {
            var projects = ProjectHelpers.GetProjects(SolutionFile.Parse(file), ParamsToDic(args));
            return Jsonify.ToJson(projects);
        }

        static JObject ParseProject(string file, JObject args)
        {
            var project = ProjectHelpers.LoadProject(file, ParamsToDic(args));
            return Jsonify.ToJson(project);
        }

        static IDictionary<string, string> ParamsToDic(JObject args)
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
