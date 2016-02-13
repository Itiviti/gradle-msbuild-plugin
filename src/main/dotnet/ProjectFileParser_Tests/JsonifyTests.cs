using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using Microsoft.Build.Construction;
using Newtonsoft.Json.Linq;
using NUnit.Framework;
using ProjectFileParser;

namespace ProjectFileParser_Tests
{
    [TestFixture]
    public class JsonifyTests
    {
        private string GetResourcePath(string relativePath)
        {
            return Path.Combine(Path.GetDirectoryName(Assembly.GetCallingAssembly().Location), relativePath);
        }

        private static IEnumerable<TestCaseData> ProjectData()
        {
            yield return new TestCaseData("Resources/iReport.Service.csproj", 54, 46, "iReport.Service");
            yield return new TestCaseData("Resources/visualstudio14.csproj", 3, 2, "visualstudio14");
        }

        [TestCaseSource("ProjectData")]
        public void Project_ToJson_Success(string relativePath, int referenceCount, int compileCount, string projectName)
        {
            var path = GetResourcePath(relativePath);
            var project = ProjectHelpers.Load(path);
            var json = Jsonify.ToJson(project);

            Assert.AreEqual(referenceCount, json[projectName]["Reference"].Count());
            Assert.AreEqual(compileCount, json[projectName]["Compile"].Count());
            Assert.AreEqual(projectName, json[projectName]["Properties"]["MSBuildProjectName"].Value<String>());
        }

        private static IEnumerable<TestCaseData> SolutionData()
        {
            // solution created by Visual Studio 2015
            yield return new TestCaseData("Resources/visualstudio14.sln", new string[] { "visualstudio14" });
            // solution created by Visual Studio 2005
            yield return new TestCaseData("Resources/POffice.sln", new string[] { "POffice", "POfficeExe"});
        }

        [TestCaseSource("SolutionData")]
        public void Solution_ToJson_Success(string relativePath, string[] projects)
        {
            var path = GetResourcePath(relativePath);
            SolutionFile solution = SolutionFile.Parse(path);

            var json = Jsonify.ToJson(solution);
            int i = 0;
            foreach (var project in json)
            {
                Assert.AreEqual(projects[i++], project.Key);
            }
        }
    }
}