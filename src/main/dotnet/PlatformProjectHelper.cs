using Microsoft.Build.BuildEngine;
using Microsoft.Build.Evaluation;
using ProjectFileParser.platform_helpers;
using ProjectFileParser.solutionparser;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

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

        public virtual IDisposable Load(ProjectCollection collection, string file)
        {
            var backup = Directory.GetCurrentDirectory();
            Directory.SetCurrentDirectory(Path.GetDirectoryName(Path.GetFullPath(file)));
            foreach (var currentProjectFile in RetrieveProjects(file))
            {
                collection.LoadProject(currentProjectFile);
            }
            return Disposable.Create(() => Directory.SetCurrentDirectory(backup));
        }

        private IEnumerable<string> RetrieveProjects(string file)
        {
            IEnumerable<string> projects = Enumerable.Empty<string>();
            if (Program.EndingWithSln)
                return new Solution(file).ProjectsInOrder.Where(proj => proj.ProjectType == SolutionProjectType.KnownToBeMSBuildFormat).Select(proj => proj.RelativePath);
            else
                return new[] { file };
        }

        public abstract IEnumerable<Tuple<string, IEnumerable<ProjectItem>>> GetEvaluatedItemsByName(Microsoft.Build.Evaluation.Project project, bool ignoreCondition);

        public abstract IEnumerable<Tuple<string, IEnumerable<BuildItem>>> GetEvaluatedItemsByName(Microsoft.Build.BuildEngine.Project project, bool ignoreCondition);

        public abstract IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(ProjectItem proj);

        public abstract IEnumerable<Tuple<string, string>> GetEvaluatedMetadata(BuildItem proj);

        public abstract void AddNewItem(Microsoft.Build.BuildEngine.Project project, string name, string include);
    }
}
