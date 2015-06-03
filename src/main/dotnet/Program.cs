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
            IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project, bool ignoreCondition);
            IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj);
        }

        class MonoEvaluator : Evaluator
        {
            public IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project, bool ignoreCondition)
            {
                var dic = (IDictionary<string, BuildItemGroup>)typeof(Project).GetProperty(ignoreCondition ? "EvaluatedItemsByNameIgnoringCondition" : "EvaluatedItemsByName", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance).GetValue(project, new object[0]);
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
            public IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project, bool ignoreCondition)
            {
                return (ignoreCondition ? project.EvaluatedItemsIgnoringCondition : project.EvaluatedItems).Cast<BuildItem>().GroupBy(item => item.Name).Select(g => Tuple.Create(g.Key, g.Cast<BuildItem>()));
            }

            public IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj)
            {
                return proj.MetadataNames.Cast<string>().Select(k => Tuple.Create(k, proj.GetEvaluatedMetadata(k)));
            }
        }

        static readonly Evaluator eval = RunningMono ? (Evaluator)new MonoEvaluator() : new MSBuildEvaluator();

        static IEnumerable<BuildItem> GetBuildLevelItems(Project project)
        {
            return eval.GetEvaluatedItemsByName(project, false).Where(entry => entry.Item1.StartsWith("BuildLevel")).SelectMany(entry => entry.Item2);
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
            foreach (var entry in eval.GetEvaluatedItemsByName(project, false))
            {
                JArray items = new JArray();
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
                jProject[entry.Item1] = items;
            }
            JObject properties = new JObject();
            Func<BuildProperty, string> ToKey = s => "__" + s.Name + "__";

            project.EvaluatedProperties
                .Cast<BuildProperty>()
                .Where(p => p.ToString().Trim().StartsWith("@"))
                .ToList()
                .Select(p =>
                {
                    if (RunningMono)
                        project.AddNewItemGroup().AddNewItem(ToKey(p), p.ToString());
                    else
                        project.AddNewItem(ToKey(p), p.ToString());
                    return p;
                })
                .ToList()
                .ForEach(p =>
                {
                    var grp = eval.GetEvaluatedItemsByName(project, true).Where(t => t.Item1 == ToKey(p)).SelectMany(t => t.Item2);
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
