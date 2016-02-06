using System;
using System.IO;
using Microsoft.Build.Construction;
using Newtonsoft.Json.Linq;

namespace ProjectFileParser
{
    class Program
    {
        static int Main(string[] args)
        {
            try
            {
                var result = Parse(args[0]);
                Console.WriteLine(result.ToString());
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Error during project file parsing: {0}", e);
                return -1;
            }
            return 0;
        }

        static JObject Parse(string file)
        {
            var isSolution = Path.GetExtension(file).Equals(".sln", StringComparison.InvariantCultureIgnoreCase);
            return isSolution ? ParseSolution(file) : ParseProject(file);
        }

        static JObject ParseSolution(string file)
        {
            var solution = SolutionFile.Parse(file);
            return Jsonify.ToJson(solution);
        }

        static JObject ParseProject(string file)
        {
            var project = ProjectHelpers.Load(file);
            return Jsonify.ToJson(project);
        }
    }
}
