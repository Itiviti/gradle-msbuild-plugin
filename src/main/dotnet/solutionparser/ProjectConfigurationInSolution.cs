using System;
using System.Reflection;

namespace ProjectFileParser.solutionparser
{
    /// <summary>
    /// Wrapper class over the internal class Microsoft.Build.Construction.ProjectConfigurationInSolution
    /// </summary>
    public class ProjectConfigurationInSolution
    {
        static readonly Type s_ProjectConfigurationInSolution;
        static readonly PropertyInfo s_ProjectConfigurationInSolution_ConfigurationName;
        static readonly PropertyInfo s_ProjectConfigurationInSolution_FullName;
        static readonly PropertyInfo s_ProjectConfigurationInSolution_PlatformName;
        static readonly PropertyInfo s_ProjectConfigurationInSolution_IncludeInBuild;

        static ProjectConfigurationInSolution()
        {
            s_ProjectConfigurationInSolution = Type.GetType("Microsoft.Build.Construction.ProjectConfigurationInSolution, Microsoft.Build, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b03f5f7f11d50a3a", false, false);
            if (s_ProjectConfigurationInSolution != null)
            {
                s_ProjectConfigurationInSolution_ConfigurationName = s_ProjectConfigurationInSolution.GetProperty("ConfigurationName", BindingFlags.NonPublic | BindingFlags.Instance);
                s_ProjectConfigurationInSolution_FullName = s_ProjectConfigurationInSolution.GetProperty("FullName", BindingFlags.NonPublic | BindingFlags.Instance);
                s_ProjectConfigurationInSolution_PlatformName = s_ProjectConfigurationInSolution.GetProperty("PlatformName", BindingFlags.NonPublic | BindingFlags.Instance);
                s_ProjectConfigurationInSolution_IncludeInBuild = s_ProjectConfigurationInSolution.GetProperty("IncludeInBuild", BindingFlags.NonPublic | BindingFlags.Instance);
            }
        }

        public string ConfigurationName { get; private set; }
        public string FullName { get; private set; }
        public string PlatformName { get; private set; }
        public bool IncludeInBuild { get; private set; }

        public ProjectConfigurationInSolution(object configurationInSolution)
        {
            ConfigurationName = s_ProjectConfigurationInSolution_ConfigurationName.GetValue(configurationInSolution, null) as string;
            FullName = s_ProjectConfigurationInSolution_FullName.GetValue(configurationInSolution, null) as string;
            PlatformName = s_ProjectConfigurationInSolution_PlatformName.GetValue(configurationInSolution, null) as string;
            IncludeInBuild = (bool)s_ProjectConfigurationInSolution_IncludeInBuild.GetValue(configurationInSolution, null);
        }
    }
}
