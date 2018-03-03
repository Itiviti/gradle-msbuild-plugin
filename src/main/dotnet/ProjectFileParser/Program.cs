using System;
using System.IO;
using Microsoft.Build.Construction;
using Newtonsoft.Json.Linq;
using System.Collections.Generic;

namespace ProjectFileParser
{
    class Program
    {
        // We have to set MSBUILD_EXE_PATH to some valid path for faking the BuildEnvironment on Mono
        // or else will throw PlatformUnsupportedException
        private class MonoHack : IDisposable
        {
            private const string MSBUILD_EXE_PATH = "MSBUILD_EXE_PATH";
            private readonly string _msbuildExePath;
            private readonly bool _isMono;

            public MonoHack()
            {
                _isMono = Type.GetType("Mono.Runtime") != null;
                if (_isMono)
                {
                    _msbuildExePath = Environment.GetEnvironmentVariable(MSBUILD_EXE_PATH);
                    Environment.SetEnvironmentVariable(MSBUILD_EXE_PATH, typeof(Program).Assembly.Location);
                }
            }

            public void Dispose()
            {
                if (_isMono)
                {
                    Environment.SetEnvironmentVariable(MSBUILD_EXE_PATH, _msbuildExePath);
                }
            }
        }

        static int Main(string[] args)
        {
            using (new MonoHack())
            {
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
