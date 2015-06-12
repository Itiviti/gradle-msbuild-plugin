using Microsoft.Build.BuildEngine;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;

namespace ProjectFileParser.platform_helpers
{
    internal class MonoPlatform : PlatformProjectHelper
    {
        public override void AddNewItem(Project project, string name, string include)
        {
            project.AddNewItemGroup().AddNewItem(name, include);
        }

        public override IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project, bool ignoreCondition)
        {
            var dic = Reflection.GetProperty<IDictionary<string, BuildItemGroup>, Project>(project, ignoreCondition ? "EvaluatedItemsByNameIgnoringCondition" : "EvaluatedItemsByName");
            return dic.Select(e => Tuple.Create(e.Key, e.Value.Cast<BuildItem>()));
        }

        public override IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj)
        {
            var dic = Reflection.GetField<IDictionary, BuildItem>(proj, "evaluatedMetadata");
            return dic.Cast<DictionaryEntry>().Select(e => Tuple.Create((string)e.Key, (string)e.Value));
        }

        public override IEnumerable<Tuple<string, IEnumerable<Microsoft.Build.Evaluation.ProjectItem>>> GetEvaluatedItemsByName(Microsoft.Build.Evaluation.Project project, bool ignoreCondition)
        {
            throw new NotImplementedException();
        }

        public override IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(Microsoft.Build.Evaluation.ProjectItem proj)
        {
            throw new NotImplementedException();
        }
    }
}
