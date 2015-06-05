using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Build.BuildEngine;
using System.Xml;
using System.IO;
using System.Text.RegularExpressions;

namespace ProjectFileParser.platform_helpers
{
    internal class MsbuildPlatform : PlatformProjectHelper
    {
        public override void AddNewItem(Project project, string name, string include)
        {
            project.AddNewItem(name, include);
        }

        public override IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project, bool ignoreCondition)
        {
            return (ignoreCondition ? project.EvaluatedItemsIgnoringCondition : project.EvaluatedItems).Cast<BuildItem>().GroupBy(item => item.Name).Select(g => Tuple.Create(g.Key, g.Cast<BuildItem>()));
        }

        public override IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj)
        {
            return proj.MetadataNames.Cast<string>().Select(k => Tuple.Create(k, proj.GetEvaluatedMetadata(k)));
        }

        public override IDisposable Load(Project project, string file)
        {
            try
            {
                return base.Load(project, file);
            }
            finally
            {
                ReplaceThisFileProperties(project);
            }
        }

        // MSBuild engine doesn't support newer MSBuildThisFile* properties
        // Work around that by replacing them with individual per-file global
        // properties.
        // Would probably need to go through BuildItems as well, but that's
        // a first step.
        private void ReplaceThisFileProperties(Project project)
        {
            var dic = new Dictionary<string, string>();
            foreach (var p in project.PropertyGroups.Cast<BuildPropertyGroup>().SelectMany(g => g.Cast<BuildProperty>()))
            {
                var value = TryChangeValue(dic, p, p.Value);
                if (value != null)
                    Reflection.Invoke(p, "SetValue", value);
                value = TryChangeValue(dic, p, p.Condition);
                if (value != null)
                {
                    var xml = Reflection.GetProperty<XmlElement, BuildProperty>(p, "PropertyElement");
                    xml.SetAttribute("Condition", value);
                    Reflection.SetField(p, "conditionAttribute", xml.Attributes["Condition"]);
                    Reflection.Invoke(p, "MarkPropertyAsDirty");
                }
            }
            foreach (var entry in dic)
            {
                project.GlobalProperties[entry.Key] = new BuildProperty(entry.Key, entry.Value);
            }
        }

        class Mapper
        {
            static readonly Regex PathToProperty = new Regex("[^a-zA-Z]+", RegexOptions.Compiled);
            readonly string suffix, property;
            readonly Func<string, string> map;

            private Mapper(string suffix, Func<string, string> mapping)
            {
                this.suffix = suffix;
                this.map = mapping;
                this.property = "MSBuildThisFile" + suffix;
            }

            public string Map(IDictionary<string, string> extras, BuildProperty p, string value)
            {
                if (!value.Contains(property))
                    return value;
                var thisFile = map(GetThisFilePath(p));
                var thisFileProp = "__" + PathToProperty.Replace(thisFile, "") + suffix + "__";
                extras[thisFileProp] = thisFile;
                return value.Replace(property, thisFileProp);
            }

            public static IEnumerable<Mapper> CreateMappers()
            {
                yield return new Mapper ( "DirectoryNoRoot", GetDirNoRoot );
                yield return new Mapper ( "Directory"      , Path.GetDirectoryName );
                yield return new Mapper ( "FullPath"       , f => f );
                yield return new Mapper ( "Name"           , Path.GetFileNameWithoutExtension );
                yield return new Mapper ( "Extension"      , Path.GetExtension );
                yield return new Mapper ( ""               , Path.GetFileName );
            }

            private static string GetDirNoRoot(string f)
            {
                string dir = f + Path.DirectorySeparatorChar;
                return dir.Substring(Path.GetPathRoot(dir).Length);
            }
        }

        static readonly IEnumerable<Mapper> Mappers = Mapper.CreateMappers().ToList();

        static string TryChangeValue(IDictionary<string, string> extraProps, BuildProperty p, string value)
        {
            if (!value.Contains("MSBuildThisFile")) return null;
            var orig = value;
            var val = value;
            while (true)
            {
                if (p.Name == "BuildDirectory")
                foreach (var map in Mappers)
                {
                    val = map.Map(extraProps, p, val);
                }
                if (val == value) break;
                value = val;
            }
            return val == orig ? null : val;
        }

        static string GetThisFilePath(BuildProperty p)
        {
            var xml = Reflection.GetProperty<XmlElement, BuildProperty>(p, "PropertyElement");
            return new Uri(xml.BaseURI).LocalPath;
        }
    }
}
