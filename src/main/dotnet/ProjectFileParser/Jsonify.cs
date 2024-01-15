using Microsoft.Build.Evaluation;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace ProjectFileParser
{
    public static class Jsonify
    {
        public static Dictionary<string, Dictionary<string, object>> ToProperties(params Project[] projects)
        {
            var projectProperties = new Dictionary<string, Dictionary<string, object>>();
            foreach (var project in projects)
            {
                var projectJson = ProjectToJson(project);
                projectProperties[Path.GetFileNameWithoutExtension(project.FullPath)] = projectJson;
            }
            return projectProperties;
        }

        public static string ToJson(params Project[] projects)
        {
            return JsonSerializer.Serialize(ToProperties(projects));
        }

        private static Dictionary<string, object> ProjectToJson(Project project)
        {
            var result = new Dictionary<string, object>();
            // Project items like resources, sources and references
            foreach (var item in project.ItemsIgnoringCondition)
            {
                if (!result.ContainsKey(item.ItemType))
                {
                    result[item.ItemType] = new List<Dictionary<string, string>>();
                }
                var array = (List<Dictionary<string, string>>)result[item.ItemType];

                var jsonItem = new Dictionary<string, string>();
                jsonItem["Include"] = item.EvaluatedInclude;
                foreach (var metaData in item.Metadata)
                {
                    jsonItem[metaData.Name] = metaData.EvaluatedValue;
                }
                array.Add(jsonItem);
            }
            // Project properties
            var projectProperties = new Dictionary<string, object>();
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
                    var valueArray = new List<string>();
                    foreach (var v in values)
                    {
                        valueArray.Add(v);
                    }
                    projectProperties[property.Name] = valueArray;
                }
                else
                {
                    projectProperties[property.Name] = value.Trim();
                }
            }
            result["Properties"] = projectProperties;
            return result;
        }
    }
}