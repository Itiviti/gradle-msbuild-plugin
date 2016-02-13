using System;
using System.IO;
using System.Linq;
using Microsoft.Build.Construction;
using Microsoft.Build.Evaluation;
using Newtonsoft.Json.Linq;

namespace ProjectFileParser
{
    public static class Jsonify
    {
        public static JObject ToJson(SolutionFile solution)
        {
            var projects = solution.ProjectsInOrder.Select(p => ProjectHelpers.Load(p.AbsolutePath)).ToArray();
            return ToJson(projects);
        }

        public static JObject ToJson(params Project[] projects)
        {
            JObject json = new JObject();
            foreach (var project in projects)
            {
                var projectJson = ProjectToJson(project);
                json[Path.GetFileNameWithoutExtension(project.FullPath)] = projectJson;
            }
            return json;
        }

        private static JObject ProjectToJson(Project project)
        {
            JObject jsonProject = new JObject();
            // Project items like resources, sources and references
            foreach (var item in project.ItemsIgnoringCondition)
            {
                if (jsonProject[item.ItemType] == null)
                {
                    jsonProject[item.ItemType] = new JArray();
                }
                JArray array = (JArray)jsonProject[item.ItemType];

                var jsonItem = new JObject();
                jsonItem["Include"] = item.EvaluatedInclude;
                foreach (var metaData in item.Metadata)
                {
                    jsonItem[metaData.Name] = metaData.EvaluatedValue;
                }
                array.Add(jsonItem);
            }
            // Project properties
            JObject jsonProperties = new JObject();
            foreach (var property in project.AllEvaluatedProperties)
            {
                var value = project.ExpandString(property.EvaluatedValue);
                // Multiple values
                if (value.Contains(";") && value.Contains("\n"))
                {
                    var values = value
                        .Split(new char[] { ';' }, StringSplitOptions.RemoveEmptyEntries)
                        .Where(v => !string.IsNullOrWhiteSpace(v))
                        .Select(v => v.Trim());
                    JArray jsonValueArray = new JArray();
                    foreach (var v in values)
                    {
                        jsonValueArray.Add(v);
                    }
                    jsonProperties[property.Name] = jsonValueArray;
                }
                else
                {
                    jsonProperties[property.Name] = value.Trim();
                }
            }
            jsonProject["Properties"] = jsonProperties;
            return jsonProject;
        }
    }
}
