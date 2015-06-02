using Microsoft.Build.BuildEngine;
using Newtonsoft.Json.Linq;
using System;
using System.Collections;
using System.Collections.Generic;
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

        static bool RunningMono
        {
            get
            {
                return Type.GetType("Mono.Runtime") != null;
            }
        }

        interface Evaluator
        {
            IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project);
            IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj);
        }

        class MonoEvaluator : Evaluator
        {
            public IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project)
            {
                var dic = (IDictionary<string, BuildItemGroup>)typeof(Project).GetProperty("EvaluatedItemsByName", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance).GetValue(project, new object[0]);
                return dic.Select(e => Tuple.Create(e.Key, e.Value.Cast<BuildItem>()));
            }

            public IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj)
            {
                var dic = (IDictionary)typeof(BuildItem).GetField("evaluatedMetadata", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance).GetValue(proj);
                return dic.Cast<DictionaryEntry>().Select(e => Tuple.Create((string)e.Key, (string)e.Value));
            }
        }

        class MSBuildEvaluator : Evaluator
        {
            public IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project)
            {
                return project.EvaluatedItems.Cast<BuildItem>().GroupBy(item => item.Name).Select(g => Tuple.Create(g.Key, g.Cast<BuildItem>()));
            }

            public IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj)
            {
                return proj.MetadataNames.Cast<string>().Select(k => Tuple.Create(k, proj.GetEvaluatedMetadata(k)));
            }
        }

        static readonly Evaluator eval = RunningMono ? (Evaluator)new MonoEvaluator() : new MSBuildEvaluator();

        static IEnumerable<BuildItem> GetBuildLevelItems(Project project)
        {
            return eval.GetEvaluatedItemsByName(project).Where(entry => entry.Item1.StartsWith("BuildLevel")).SelectMany(entry => entry.Item2);
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
            Directory.SetCurrentDirectory(Path.GetDirectoryName(Path.GetFullPath(file)));

            solution.Load(Path.GetFileName(file));
            var result = ToJson(solution);
            if (Path.GetExtension(file) == ".sln")
            {
                var jSolution = result;
                result = new JObject();
                result["Solution"] = jSolution;
                foreach (var proj in GetBuildLevelItems(solution))
                {
                    var project = solution.ParentEngine.CreateNewProject();
                    foreach (var meta in eval.GetEvaluatedMetadata(proj))
                    {
                        project.GlobalProperties[meta.Item1] = new BuildProperty(meta.Item1, meta.Item2);
                    }
                    project.Load(proj.FinalItemSpec);
                    var jProject = ToJson(project);
                    result[Path.GetFileNameWithoutExtension(proj.FinalItemSpec)] = jProject;
                }
            }
            Console.WriteLine(result.ToString());
        }

        private static JObject ToJson(Project project)
        {
            JObject jProject = new JObject();
            JObject properties = new JObject();
            jProject["Properties"] = properties;
            foreach (var entry in project.EvaluatedProperties.Cast<BuildProperty>())
            {
                properties[entry.Name] = entry.ToString();
            }
            foreach (var entry in eval.GetEvaluatedItemsByName(project))
            {
                JArray items = new JArray();
                jProject[entry.Item1] = items;
                foreach (var item in entry.Item2)
                {
                    var it = new JObject();
                    it["Include"] = item.FinalItemSpec;
                    foreach (var meta in eval.GetEvaluatedMetadata(item))
                    {
                        it[meta.Item1] = meta.Item2;
                    }
                    items.Add(it);
                }
            }
            return jProject;
        }
    }
}
