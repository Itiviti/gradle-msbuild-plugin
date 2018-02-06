using System;
using System.IO;
using Microsoft.Build.Construction;
using Newtonsoft.Json.Linq;
using System.Collections.Generic;
using System.Reflection;

namespace ProjectFileParser
{
    class Program
    {
        static int Main(string[] args)
        {
            SetIsMonoExplicitly();
            try
            {
                var obj = JObject.Parse(Console.In.ReadToEnd());
                var result = Parse(args[0], obj);
                Console.WriteLine(result.ToString());
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Error during project file parsing: {0}", e);
                return -1;
            }
            return 0;
        }

        // We have to set _isMono explicitly if running in Mono, otherwise we will have PlatformNotSupportedException when TryFromVisualStudioProcess
        // _isMono can be found in https://github.com/Microsoft/msbuild/blob/master/src/Shared/NativeMethodsShared.cs
        // TryFromVisualStudioProcess can be found in https://github.com/Microsoft/msbuild/blob/master/src/Shared/BuildEnvironmentHelper.cs
        private static void SetIsMonoExplicitly()
        {
            if (Type.GetType("Mono.Runtime") != null)
            {
                var nativeSharedMethod = typeof(SolutionFile).Assembly.GetType("Microsoft.Build.Shared.NativeMethodsShared");
                var isMonoField = nativeSharedMethod.GetField("_isMono", BindingFlags.Static | BindingFlags.NonPublic);
                isMonoField.SetValue(null, true);
            }
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
