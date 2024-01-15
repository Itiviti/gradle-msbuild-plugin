using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.Build.Evaluation;
using Microsoft.Build.Construction;

namespace ProjectFileParser
{
    public static class ProjectHelpers
    {
        public static Project[] GetProjects(SolutionFile solution, IDictionary<string, string> args)
        {
            return solution.ProjectsInOrder
                .Where(p => p.ProjectType != SolutionProjectType.SolutionFolder)
                .Select(p => LoadProject(p.AbsolutePath, SpecializeArgsForSolutionProject(solution, p, args)))
                .ToArray();
        }

        private static IDictionary<string, string> SpecializeArgsForSolutionProject(SolutionFile solution, ProjectInSolution p, IDictionary<string, string> args)
        {
            var targetPlatform = args.ContainsKey("Platform") ? args["Platform"] : solution.GetDefaultPlatformName();
            var targetConfiguration = args.ContainsKey("Configuration") ? args["Configuration"] : solution.GetDefaultConfigurationName();
            var targetSolutionConfiguration =
                solution.SolutionConfigurations.FirstOrDefault(conf => conf.ConfigurationName == targetConfiguration && conf.PlatformName == targetPlatform)?.FullName;
            var foo = targetSolutionConfiguration == null || !p.ProjectConfigurations.ContainsKey(targetSolutionConfiguration) ? null : p.ProjectConfigurations[targetSolutionConfiguration];
            var copy = new Dictionary<string, string>(args);
            copy["Platform"] = foo == null ? targetPlatform : foo.PlatformName;
            copy["Configuration"] = foo == null ? targetConfiguration : foo.ConfigurationName;
            return copy;
        }

        public static Project LoadProject(string fullPath, IDictionary<string, string> args)
        {
            var collection = new ProjectCollection(args);
            return collection.LoadProject(fullPath, null);
        }
    }
}
