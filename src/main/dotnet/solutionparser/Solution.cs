using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace ProjectFileParser.solutionparser
{
    /// <summary>
    /// Wrapper class over the internal Microsoft.Build.Construction.SolutionParser
    /// </summary>
    public class Solution
    {
        //internal class SolutionParser
        //Name: Microsoft.Build.Construction.SolutionParser
        //Assembly: Microsoft.Build, Version=4.0.0.0

        static readonly Type s_SolutionParser;
        static readonly PropertyInfo s_SolutionParser_solutionReader;
        static readonly MethodInfo s_SolutionParser_parseSolution;
        static readonly PropertyInfo s_SolutionParser_projects;
        static readonly PropertyInfo s_SolutionParser_projectsInOrder;
        static readonly MethodInfo s_SolutionParser_getDefaultConfigurationName;
        static readonly MethodInfo s_SolutionParser_getDefaultPlatformName;

        static Solution()
        {
            s_SolutionParser = Type.GetType("Microsoft.Build.Construction.SolutionParser, Microsoft.Build, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b03f5f7f11d50a3a", false, false);
            if (s_SolutionParser != null)
            {
                s_SolutionParser_solutionReader = s_SolutionParser.GetProperty("SolutionReader", BindingFlags.NonPublic | BindingFlags.Instance);
                s_SolutionParser_projects = s_SolutionParser.GetProperty("Projects", BindingFlags.NonPublic | BindingFlags.Instance);
                s_SolutionParser_projectsInOrder = s_SolutionParser.GetProperty("ProjectsInOrder", BindingFlags.NonPublic | BindingFlags.Instance);
                s_SolutionParser_parseSolution = s_SolutionParser.GetMethod("ParseSolution", BindingFlags.NonPublic | BindingFlags.Instance);
                s_SolutionParser_getDefaultConfigurationName = s_SolutionParser.GetMethod("GetDefaultConfigurationName", BindingFlags.NonPublic | BindingFlags.Instance);
                s_SolutionParser_getDefaultPlatformName = s_SolutionParser.GetMethod("GetDefaultPlatformName", BindingFlags.NonPublic | BindingFlags.Instance);
            }
        }

        public List<SolutionProject> Projects { get; private set; }
        public List<SolutionProject> ProjectsInOrder { get; private set; }
        public string DefaultConfigurationName { get; private set; }
        public string DefaultPlatformName { get; private set; }

        public Solution(string solutionFileName)
        {
            if (s_SolutionParser == null)
            {
                throw new InvalidOperationException("Can not find type 'Microsoft.Build.Construction.SolutionParser' are you missing a assembly reference to 'Microsoft.Build.dll'?");
            }
            var solutionParser = s_SolutionParser.GetConstructors(BindingFlags.Instance | BindingFlags.NonPublic).First().Invoke(null);
            using (var streamReader = new StreamReader(solutionFileName))
            {
                s_SolutionParser_solutionReader.SetValue(solutionParser, streamReader, null);
                s_SolutionParser_parseSolution.Invoke(solutionParser, null);
            }
            var projects = new List<SolutionProject>();
            var array = (ArrayList)s_SolutionParser_projectsInOrder.GetValue(solutionParser, null);

            foreach (var entry in array)
            {
                projects.Add(new SolutionProject(entry));
            }
            this.ProjectsInOrder = projects;
            this.Projects = projects;
            this.DefaultConfigurationName = s_SolutionParser_getDefaultConfigurationName.Invoke(solutionParser, null) as string;
            this.DefaultPlatformName = s_SolutionParser_getDefaultPlatformName.Invoke(solutionParser, null) as string;
        }
    }
}
