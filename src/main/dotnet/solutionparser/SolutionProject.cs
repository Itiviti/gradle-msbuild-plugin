using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.Reflection;

namespace ProjectFileParser.solutionparser
{
    /// <summary>
    /// Wrapper class over the internal class Microsoft.Build.Construction.ProjectInSolution
    /// </summary>
    [DebuggerDisplay("{ProjectName}, {RelativePath}, {ProjectGuid}")]
    public class SolutionProject
    {
        static readonly Type s_ProjectInSolution;
        static readonly PropertyInfo s_ProjectInSolution_ProjectName;
        static readonly PropertyInfo s_ProjectInSolution_RelativePath;
        static readonly PropertyInfo s_ProjectInSolution_ProjectGuid;
        static readonly PropertyInfo s_ProjectInSolution_ProjectType;
        static readonly PropertyInfo s_ProjectInSolution_ProjectConfigurations;

        static SolutionProject()
        {
            s_ProjectInSolution = Type.GetType("Microsoft.Build.Construction.ProjectInSolution, Microsoft.Build, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b03f5f7f11d50a3a", false, false);
            if (s_ProjectInSolution != null)
            {
                s_ProjectInSolution_ProjectName = s_ProjectInSolution.GetProperty("ProjectName", BindingFlags.NonPublic | BindingFlags.Instance);
                s_ProjectInSolution_RelativePath = s_ProjectInSolution.GetProperty("RelativePath", BindingFlags.NonPublic | BindingFlags.Instance);
                s_ProjectInSolution_ProjectGuid = s_ProjectInSolution.GetProperty("ProjectGuid", BindingFlags.NonPublic | BindingFlags.Instance);
                s_ProjectInSolution_ProjectType = s_ProjectInSolution.GetProperty("ProjectType", BindingFlags.NonPublic | BindingFlags.Instance);
                s_ProjectInSolution_ProjectConfigurations = s_ProjectInSolution.GetProperty("ProjectConfigurations", BindingFlags.NonPublic | BindingFlags.Instance);
            }
        }

        public string ProjectName { get; private set; }
        public string RelativePath { get; private set; }
        public string ProjectGuid { get; private set; }
        public SolutionProjectType ProjectType { get; private set; }
        public Dictionary<string, ProjectConfigurationInSolution> ProjectConfigurations { get; private set; }

        public SolutionProject(object solutionProject)
        {
            this.ProjectName = s_ProjectInSolution_ProjectName.GetValue(solutionProject, null) as string;
            this.RelativePath = s_ProjectInSolution_RelativePath.GetValue(solutionProject, null) as string;
            this.ProjectGuid = s_ProjectInSolution_ProjectGuid.GetValue(solutionProject, null) as string;
            this.ProjectType = (SolutionProjectType)Enum.Parse(typeof(SolutionProjectType), s_ProjectInSolution_ProjectType.GetValue(solutionProject, null).ToString());
            var obj = s_ProjectInSolution_ProjectConfigurations.GetValue(solutionProject, null);
            var idict = obj as IDictionary;
            if (idict == null)
                return;
            var newDict = new Dictionary<string, ProjectConfigurationInSolution>();
            foreach (DictionaryEntry entry in idict)
            {
                newDict.Add((string)entry.Key, new ProjectConfigurationInSolution(entry.Value));
            }
            ProjectConfigurations = newDict;
        }
    }
}
