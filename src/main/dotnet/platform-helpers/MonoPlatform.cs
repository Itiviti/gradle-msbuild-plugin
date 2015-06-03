using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Build.BuildEngine;
using System.Collections;

namespace ProjectFileParser.platform_helpers
{
    public class MonoPlatform : PlatformProjectHelper
    {
        public override void AddNewItem(Project project, string name, string include)
        {
            project.AddNewItemGroup().AddNewItem(name, include);
        }

        public override IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project, bool ignoreCondition)
        {
            var dic = (IDictionary<string, BuildItemGroup>)typeof(Project).GetProperty(ignoreCondition ? "EvaluatedItemsByNameIgnoringCondition" : "EvaluatedItemsByName", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance).GetValue(project, new object[0]);
            return dic.Select(e => Tuple.Create(e.Key, e.Value.Cast<BuildItem>()));
        }

        public override IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj)
        {
            var dic = (IDictionary)typeof(BuildItem).GetField("evaluatedMetadata", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance).GetValue(proj);
            return dic.Cast<DictionaryEntry>().Select(e => Tuple.Create((string)e.Key, (string)e.Value));
        }
    }
}
