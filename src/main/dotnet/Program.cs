using Microsoft.Build.Evaluation;
using Microsoft.Build.Logging;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace ProjectFileParser
{
    class Program
    {
        public static bool EndingWithSln { get; private set; }

        static int Main(string[] args)
        {
            try
            {
                Parse(args[0]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Error during project file parsing: {0}", e);
                return -1;
            }
            return 0;
        }

        /// <summary>
        /// The goal is to parse the solution file to retrieve the different projects and serialize all
        /// The parsing of the solution is done with the deprecated BuildEngine.Project class because the format hasn't
        /// change so much. Projects are parsed with the new Evaluation.Project class and support all new tag in msbuild 4.0
        /// </summary>
        static void Parse(String file)
        {
            var obj = JObject.Parse(Console.In.ReadToEnd());
            EndingWithSln = Path.GetExtension(file).Equals(".sln");

            ProjectCollection projects = new ProjectCollection(obj.ToObject<Dictionary<string, string>>(), new[] { new ConsoleLogger() }, ToolsetDefinitionLocations.ConfigurationFile | ToolsetDefinitionLocations.Registry);
            using (PlatformProjectHelper.Instance.Load(projects, file))
            {
                JObject result = new JObject();
                if (EndingWithSln)
                {
                    result["Solution"] = ToJson(LoadSolution(file));
                }
                foreach (var entry in (EndingWithSln ? ToJson(projects) : ToJson(projects.LoadedProjects.First())))
                {
                    result[entry.Key] = entry.Value;
                }
                Console.WriteLine(result.ToString());
            }
        }

        private static Microsoft.Build.BuildEngine.Project LoadSolution(string file)
        {
            var solution = new Microsoft.Build.BuildEngine.Project();
            solution.Load(file);
            return solution;
        }

        private static JObject ToJson(Microsoft.Build.BuildEngine.Project project)
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
            jProject["Properties"] = RetrieveSolutionProperties(project);
            return jProject;
        }

        private static JObject RetrieveSolutionProperties(Microsoft.Build.BuildEngine.Project project)
        {
            JObject properties = new JObject();
            Func<Microsoft.Build.BuildEngine.BuildProperty, string> ToKey = s => "__" + s.Name + "__";

            project.EvaluatedProperties
                .Cast<Microsoft.Build.BuildEngine.BuildProperty>()
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
            foreach (var entry in project.EvaluatedProperties.Cast<Microsoft.Build.BuildEngine.BuildProperty>().Where(p => !p.ToString().Trim().StartsWith("@")))
            {
                properties[entry.Name] = entry.ToString();
            }
            return properties;
        }

        private static JObject ToJson(Microsoft.Build.Evaluation.Project project)
        {
            JObject jProject = new JObject();
            foreach (var entry in PlatformProjectHelper.Instance.GetEvaluatedItemsByName(project, false))
            {
                JArray items = new JArray();
                foreach (var item in entry.Item2)
                {
                    var it = new JObject();
                    it["Include"] = item.EvaluatedInclude;
                    foreach (var meta in PlatformProjectHelper.Instance.GetEvaluatedMetadata(item))
                    {
                        it[meta.Item1] = meta.Item2;
                    }
                    items.Add(it);
                }
                jProject[entry.Item1] = items;
            }
            JObject properties = new JObject();
            foreach (var property in project.AllEvaluatedProperties)
            {
                properties[property.Name] = project.ExpandString(property.EvaluatedValue);
            }
            jProject["Properties"] = properties;
            return jProject;
        }

        private static JObject ToJson(ProjectCollection solution)
        {
            JObject jProjects = new JObject();
            foreach (var project in solution.LoadedProjects)
            {
                var projectJson = ToJson(project);
                jProjects[Path.GetFileNameWithoutExtension(project.FullPath)] = projectJson;
            }
            return jProjects;
        }
    }
}
