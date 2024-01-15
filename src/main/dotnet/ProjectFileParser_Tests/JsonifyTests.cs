using Microsoft.Build.Construction;
using NUnit.Framework;
using NUnit.Framework.Legacy;
using ProjectFileParser;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Reflection;

namespace ProjectFileParser_Tests
{
    public class JsonifyTests
    {
        [SetUp]
        public void SetUp()
        {
            MSBuildCustomLocator.Register();
        }

        private string GetResourcePath(string relativePath)
        {
            return Path.Combine(Path.GetDirectoryName(Assembly.GetCallingAssembly().Location), relativePath);
        }

        private static IEnumerable<TestCaseData> ProjectData()
        {
            yield return new TestCaseData("Resources/iReport.Service.csproj", 54, 46, "iReport.Service");
            yield return new TestCaseData("Resources/visualstudio14.csproj", 3, 2, "visualstudio14");
        }

        [TestCaseSource(nameof(ProjectData))]
        public void Project_ToJson_Success(string relativePath, int referenceCount, int compileCount, string projectName)
        {
            var path = GetResourcePath(relativePath);
            var project = ProjectHelpers.LoadProject(path, new Dictionary<string, string>());
            var json = Jsonify.ToProperties(project);

            ClassicAssert.AreEqual(referenceCount, (json[projectName]["Reference"] as IList).Count);
            ClassicAssert.AreEqual(compileCount, (json[projectName]["Compile"] as IList).Count);
            ClassicAssert.AreEqual(projectName, (json[projectName]["Properties"] as Dictionary<string, object>)["MSBuildProjectName"]);
        }

        [TestCase]
        public void Project_Parameters_AreApplied()
        {
            var path = GetResourcePath("Resources/dummy.csproj");
            var args = new Dictionary<string, string>();
            args["Configuration"] = "Release";
            var project = ProjectHelpers.LoadProject(path, args);
            var json = Jsonify.ToProperties(project);

            ClassicAssert.AreEqual("Release", (json["dummy"]["Properties"] as Dictionary<string, object>)["Configuration"]);
        }

        [TestCase]
        public void Solution_Parameters_AreApplied()
        {
            var path = GetResourcePath("Resources/dummy.sln");
            var args = new Dictionary<string, string>();
            args["Configuration"] = "Release";
            args["Platform"] = "x86";
            var projects = ProjectHelpers.GetProjects(SolutionFile.Parse(path), args);
            var json = Jsonify.ToProperties(projects);
            var properties = json["dummy"]["Properties"] as Dictionary<string, object>;

            ClassicAssert.AreEqual("Release", properties["Configuration"]);
            ClassicAssert.AreEqual("AnyCPU", properties["Platform"]);
        }

        private static IEnumerable<TestCaseData> SolutionData()
        {
            // solution created by Visual Studio 2015
            yield return new TestCaseData("Resources/visualstudio14.sln", new string[] { "visualstudio14" });
            // solution created by Visual Studio 2005
            yield return new TestCaseData("Resources/POffice.sln", new string[] { "POffice", "POfficeExe" });
        }

        [TestCaseSource(nameof(SolutionData))]
        public void Solution_ToJson_Success(string relativePath, string[] projects)
        {
            var path = GetResourcePath(relativePath);
            SolutionFile solution = SolutionFile.Parse(path);

            var json = Jsonify.ToProperties(ProjectHelpers.GetProjects(solution, new Dictionary<string, string>()));
            int i = 0;
            foreach (var project in json)
            {
                ClassicAssert.AreEqual(projects[i++], project.Key);
            }
        }
    }
}