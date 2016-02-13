using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.Build.Evaluation;

namespace ProjectFileParser
{
    public static class ProjectHelpers
    {
        public static Project Load(string fullPath)
        {
            var project = ProjectCollection.GlobalProjectCollection.GetLoadedProjects(fullPath).FirstOrDefault();
            if (project == null)
            {
                project = new Project(fullPath);
            }
            return project;
        }
    }
}
