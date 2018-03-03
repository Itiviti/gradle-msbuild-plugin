﻿using Microsoft.Build.Construction;
using Newtonsoft.Json.Linq;
using NUnit.Framework;
using ProjectFileParser;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace ProjectFileParser_Tests
{
    [TestFixture]
    public class JsonifyTests
    {
        [SetUp]
        public void Setup()
        {
            var monoHack = new MonoHack();
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

        [TestCaseSource("ProjectData")]
        public void Project_ToJson_Success(string relativePath, int referenceCount, int compileCount, string projectName)
        {
            var path = GetResourcePath(relativePath);
            var project = ProjectHelpers.LoadProject(path, new Dictionary<string, string>());
            var json = Jsonify.ToJson(project);

            Assert.AreEqual(referenceCount, json[projectName]["Reference"].Count());
            Assert.AreEqual(compileCount, json[projectName]["Compile"].Count());
            Assert.AreEqual(projectName, json[projectName]["Properties"]["MSBuildProjectName"].Value<String>());
        }

        [TestCase]
        public void Project_Parameters_AreApplied()
        {
            var path = GetResourcePath("Resources/dummy.csproj");
            var args = new Dictionary<string, string>();
            args["Configuration"] = "Release";
            var project = ProjectHelpers.LoadProject(path, args);
            var json = Jsonify.ToJson(project);

            Assert.AreEqual("Release", json["dummy"]["Properties"]["Configuration"].Value<String>());
        }

        [TestCase]
        public void Project_NugetDependencies_AreParsed()
        {
            var path = GetResourcePath("Resources/dummy.csproj");
            var args = new Dictionary<string, string>();
            args["Configuration"] = "Release";
            var project = ProjectHelpers.LoadProject(path, args);
            var json = Jsonify.ToJson(project);

            var dependencies = ((JArray)json["dummy"]["NugetDependencies"]);
            Assert.AreEqual(5, dependencies.Count);
            AssertDependency(dependencies, 0, "Microsoft.Web.Xdt", "2.1.1");
            AssertDependency(dependencies, 1, "MSBuild", "0.1.2");
            AssertDependency(dependencies, 2, "Newtonsoft.Json", "8.0.2");
            AssertDependency(dependencies, 3, "NuGet.Core", "2.14.0");
            AssertDependency(dependencies, 4, "FooBar", "0.2.3.0-gerrit96533-SNAPSHOT", true);
        }

        private void AssertDependency(JArray dependencies, int index, string id, string version, bool isDevelopmentDependency = false)
        {
            Assert.AreEqual(id, dependencies[index]["Id"].Value<string>());
            Assert.AreEqual(version, dependencies[index]["Version"].Value<string>());
            Assert.AreEqual(isDevelopmentDependency, dependencies[index]["IsDevelopmentDependency"].Value<bool>());
        }

        [TestCase]
        public void Solution_Parameters_AreApplied()
        {
            var path = GetResourcePath("Resources/dummy.sln");
            var args = new Dictionary<string, string>();
            args["Configuration"] = "Release";
            args["Platform"] = "x86";
            var projects = ProjectHelpers.GetProjects(SolutionFile.Parse(path), args);
            var json = Jsonify.ToJson(projects);

            Assert.AreEqual("Release", json["dummy"]["Properties"]["Configuration"].Value<String>());
            Assert.AreEqual("AnyCPU", json["dummy"]["Properties"]["Platform"].Value<String>());
        }

        private static IEnumerable<TestCaseData> SolutionData()
        {
            // solution created by Visual Studio 2015
            yield return new TestCaseData("Resources/visualstudio14.sln", new string[] { "visualstudio14" });
            // solution created by Visual Studio 2005
            yield return new TestCaseData("Resources/POffice.sln", new string[] { "POffice", "POfficeExe" });
        }

        [TestCaseSource("SolutionData")]
        public void Solution_ToJson_Success(string relativePath, string[] projects)
        {
            var path = GetResourcePath(relativePath);
            SolutionFile solution = SolutionFile.Parse(path);

            var json = Jsonify.ToJson(ProjectHelpers.GetProjects(solution, new Dictionary<string, string>()));
            int i = 0;
            foreach (var project in json)
            {
                Assert.AreEqual(projects[i++], project.Key);
            }
        }
    }
}