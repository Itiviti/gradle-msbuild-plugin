using Microsoft.Build.BuildEngine;
using Newtonsoft.Json.Linq;
using System;
using System.IO;
using System.Linq;

namespace ProjectFileParser
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                Parse(args[0]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Error during project file parsing: {0}", e);
                Console.Error.WriteLine(e.StackTrace);
                Environment.ExitCode = -1;
            }
        }

        static void Parse(String file)
        {
            var obj = JObject.Parse(Console.In.ReadToEnd());
            var solution = new Project();
            solution.ParentEngine.RegisterLogger(new ConsoleLogger());
            foreach (var prop in obj)
            {
                solution.GlobalProperties[prop.Key] = new BuildProperty(prop.Key, prop.Value.ToString());
            }
            using (PlatformProjectHelper.Instance.Load(solution, file))
            {
                var result = ToJson(solution);
                if (Path.GetExtension(file) == ".sln")
                {
                    var jSolution = result;
                    result = new JObject();
                    result["Solution"] = jSolution;
                    foreach (var proj in PlatformProjectHelper.Instance.GetBuildLevelItems(solution))
                    {
                        var project = solution.ParentEngine.CreateNewProject();
                        foreach (var meta in PlatformProjectHelper.Instance.GetEvaluatedMetadata(proj))
                        {
                            project.GlobalProperties[meta.Item1] = new BuildProperty(meta.Item1, meta.Item2);
                        }
                        using (PlatformProjectHelper.Instance.Load(project, proj.FinalItemSpec))
                        {
                            var jProject = ToJson(project);
                            result[Path.GetFileNameWithoutExtension(proj.FinalItemSpec)] = jProject;
                        }
                    }
                }
                Console.WriteLine(result.ToString());
            }
        }

        private static JObject ToJson(Project project)
        {
            JObject jProject = new JObject();
            foreach (var entry in PlatformProjectHelper.Instance.GetEvaluatedItemsByName(project, false))
            {
                JArray items = new JArray();
                foreach (var item in entry.Item2)
                {
                    var it = new JObject();
                    it["Include"] = item.FinalItemSpec;
                    foreach (var meta in PlatformProjectHelper.Instance.GetEvaluatedMetadata(item))
                    {
                        it[meta.Item1] = meta.Item2;
                    }
                    items.Add(it);
                }
                jProject[entry.Item1] = items;
            }
            JObject properties = new JObject();
            Func<BuildProperty, string> ToKey = s => "__" + s.Name + "__";

            project.EvaluatedProperties
                .Cast<BuildProperty>()
                .Where(p => p.ToString().Trim().StartsWith("@"))
                .Select(p =>
                {
                    PlatformProjectHelper.Instance.AddNewItem(project, ToKey(p), p.ToString());
                    return p;
                })
                .ToList()
                .ForEach(p =>
                {
                    var grp = PlatformProjectHelper.Instance.GetEvaluatedItemsByName(project, true).Where(t => t.Item1 == ToKey(p)).SelectMany(t => t.Item2);
                    properties[p.Name] = string.Join(";", grp.Select(i => i.FinalItemSpec));
                });
            foreach (var entry in project.EvaluatedProperties.Cast<BuildProperty>().Where(p => !p.ToString().Trim().StartsWith("@")))
            {
                var value = entry.ToString();
                properties[entry.Name] = entry.ToString();
            }
            jProject["Properties"] = properties;
            return jProject;
        }
    }
}
