using Microsoft.Build.BuildEngine;
using Microsoft.Build.Evaluation;
using System;
using System.Collections.Generic;
using System.Linq;

namespace ProjectFileParser.platform_helpers
{
    internal class MsbuildPlatform : PlatformProjectHelper
    {
        public override void AddNewItem(Microsoft.Build.BuildEngine.Project project, string name, string include)
        {
            project.AddNewItem(name, include);
        }

        public override IEnumerable<Tuple<string, IEnumerable<ProjectItem>>> GetEvaluatedItemsByName(Microsoft.Build.Evaluation.Project project, bool ignoreCondition)
        {
            return project.ItemsIgnoringCondition.GroupBy(item => item.ItemType).Select(g => Tuple.Create(g.Key, g.Cast<ProjectItem>()));
        }

        public override IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Microsoft.Build.BuildEngine.Project project, bool ignoreCondition)
        {
            return (ignoreCondition ? project.EvaluatedItemsIgnoringCondition : project.EvaluatedItems).Cast<BuildItem>().GroupBy(item => item.Name).Select(g => Tuple.Create(g.Key, g.Cast<BuildItem>()));
        }

        public override IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(ProjectItem item)
        {
            var fileUtilesMetadata = FileUtilities.ItemSpecModifiers.Where(itemSpecMetadata => item.HasMetadata(itemSpecMetadata)).Select(itemSpecMetadata => new Tuple<string, string>(itemSpecMetadata, item.GetMetadataValue(itemSpecMetadata)));

            return item.Metadata
                .Select(metadata => new Tuple<string, string>(metadata.ItemType, metadata.EvaluatedValue))
                .Union(fileUtilesMetadata)
                .Where(tuple => !string.IsNullOrEmpty(tuple.Item2));
        }

        public override IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj)
        {
            return proj.MetadataNames.Cast<string>().Select(k => Tuple.Create(k, proj.GetEvaluatedMetadata(k)));
        }
    }
}
