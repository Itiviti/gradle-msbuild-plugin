using Microsoft.Build.BuildEngine;
using ProjectFileParser.platform_helpers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace ProjectFileParser
{
    public abstract class PlatformProjectHelper
    {
        public static bool RunningMono
        {
            get
            {
                return Type.GetType("Mono.Runtime") != null;
            }
        }

        public static PlatformProjectHelper Instance
        {
            get
            {
                return RunningMono ? (PlatformProjectHelper) new MonoPlatform() : new MsbuildPlatform();
            }
        }

        public IEnumerable<BuildItem> GetBuildLevelItems(Project project)
        {
            return GetEvaluatedItemsByName(project, false).Where(entry => entry.Item1.StartsWith("BuildLevel")).SelectMany(entry => entry.Item2);
        }

        public abstract IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Project project, bool ignoreCondition);

        public abstract IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj);

        public abstract void AddNewItem(Project project, string name, string include);
    }
}
