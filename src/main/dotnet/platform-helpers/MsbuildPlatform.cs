using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Build.BuildEngine;
using System.Collections;

namespace ProjectFileParser.platform_helpers
{
    public class MsbuildPlatform : PlatformProjectHelper
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
    }
}
