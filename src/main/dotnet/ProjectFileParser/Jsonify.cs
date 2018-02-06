using Microsoft.Build.Evaluation;
using Newtonsoft.Json.Linq;
using NuGet;
using System;
using System.IO;
using System.Linq;

namespace ProjectFileParser
{
    public static class Jsonify
    {
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

                if (item.EvaluatedInclude?.EndsWith("packages.config") ?? false)
                {
                    jsonProject["NugetDependencies"] = ParseNugetDependencies(Path.Combine(Path.GetDirectoryName(project.FullPath), item.EvaluatedInclude));
                }
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

        private static JArray ParseNugetDependencies(string packagesConfigPath)
        {
            var nugetDependencies = new JArray();
            if (!File.Exists(packagesConfigPath))
            {
                Console.Error.WriteLine($"Unable to find nuget dependencies file during project file parsing: {packagesConfigPath}");
                return nugetDependencies;
            }

            var file = new PackageReferenceFile(packagesConfigPath);
            foreach (PackageReference packageReference in file.GetPackageReferences())
            {
                var dependency = new JObject();
                dependency["Id"] = packageReference.Id;
                dependency["Version"] = packageReference.Version.ToString();
                dependency["TargetFramework"] = packageReference.TargetFramework.ToString();
                dependency["RequireReinstallation"] = packageReference.RequireReinstallation;
                dependency["IsDevelopmentDependency"] = packageReference.IsDevelopmentDependency;
                JObject versionConstraint = null;
                if (packageReference.VersionConstraint != null)
                {
                    versionConstraint = new JObject();
                    versionConstraint["MinVersion"] = packageReference.VersionConstraint.MinVersion.ToString();
                    versionConstraint["MaxVersion"] = packageReference.VersionConstraint.MaxVersion.ToString();
                    versionConstraint["IsMinInclusive"] = packageReference.VersionConstraint.IsMinInclusive;
                    versionConstraint["IsMaxInclusive"] = packageReference.VersionConstraint.IsMaxInclusive;
                }
                dependency["VersionConstraint"] = versionConstraint;
                nugetDependencies.Add(dependency);
            }
            return nugetDependencies;
        }
    }
}
